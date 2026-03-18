# VerveGuard

A high-performance real-time fraud detection sidecar service for payment systems. VerveGuard intercepts transactions before they reach the core switch, evaluating them against a suite of fraud rules and enforcing merchant tier limits — all with sub-100ms overhead at 200 concurrent requests.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Fraud Detection](#fraud-detection)
- [Transfers](#transfers)
- [Security](#security)
- [Testing](#testing)
- [Monitoring](#monitoring)
- [Scheduled Jobs](#scheduled-jobs)

---

## Overview

VerveGuard sits as a sidecar in front of a payment switch. Every transaction passes through it before money moves. It provides:

- Real-time fraud detection with rule-based scoring
- Merchant onboarding, KYC management and tier-based transaction limits
- Double-entry bookkeeping for all transfers
- JWT-secured admin dashboard for fraud attempt review
- Full audit trail with performance monitoring via AOP
- Balance reconciliation and stuck transfer recovery via scheduled jobs

---

## Architecture

```
Client → VerveGuard API
              │
              ├── Fraud Detection (Caffeine rate limiter + blacklist cache)
              │         │
              │         └── fraud_attempts (JDBC)
              │
              ├── Transfer Engine (atomic JDBC)
              │         ├── transactions (DEBIT + CREDIT)
              │         └── accounts (balance update)
              │
              ├── Domain Layer (JPA)
              │         ├── users / roles / permissions
              │         ├── merchants / tier_config
              │         └── cards
              │
              └── Scheduled Jobs
                        ├── Transfer Recovery  (00:00, 12:00)
                        └── Balance Reconciliation (00:30, 12:30)
```

**Hybrid persistence** — JPA for domain entities (users, merchants, roles, permissions, tier config), raw JDBC for high-speed transactional operations (accounts, cards, transactions, transfers, fraud attempts).

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 4 |
| Language | Java 25 |
| Database | PostgreSQL 15 |
| ORM | Spring Data JPA + Hibernate 7 |
| JDBC | NamedParameterJdbcTemplate |
| Security | Spring Security 6 + JWT (jjwt) |
| Cache | Caffeine (token blacklist, rate limiter, blacklist cache) |
| Migrations | Flyway |
| Mapping | MapStruct |
| Monitoring | Spring Actuator + Micrometer + Prometheus + Grafana |
| Logging | Logback + Loki |
| Testing | JUnit 5 + Testcontainers + MockMvc |
| Build | Maven |

---

## Getting Started

### Prerequisites

- Java 25
- Docker + Docker Compose
- Maven 3.9+

### Run with Docker Compose

```bash
# Start PostgreSQL, Prometheus, Grafana and Loki
docker-compose up -d

# Run the application
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Run Tests

```bash
# All tests (excludes stress tests)
mvn test

# Stress tests only
mvn test -Dgroups=stress

# All including stress
mvn test -DexcludedGroups=
```

### Default Credentials

| User | Email | Password | Role |
|---|---|---|---|
| Super Admin | superadmin@verveguard.com | Admin123! | SUPER_ADMIN |
| Demo Merchant | demo.merchant@verveguard.com | Admin123! | MERCHANT |

---

## Configuration

All configuration is via `application.properties`. Key properties:

```properties
# Database
spring.datasource.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# JWT
security.jwt.secret=${JWT_SECRET}
security.jwt.access-token-expiry=900
security.jwt.refresh-token-expiry=604800

# Flyway
spring.flyway.locations=classpath:db/migration
spring.jpa.hibernate.ddl-auto=validate
```

Environment variables via `.env`:

```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=verve_guard
DB_USERNAME=postgres
DB_PASSWORD=yourpassword
JWT_SECRET=your-256-bit-base64-secret
```

---

## API Reference

All endpoints are prefixed `/api/v1`. A full Postman collection is included at `VerveGuard.postman_collection.json`.

### Auth

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/auth/login` | Login, returns access + refresh tokens | Public |
| POST | `/auth/refresh` | Refresh access token | Refresh-Token header |
| POST | `/auth/logout` | Revoke current session | JWT |
| POST | `/auth/logout-all` | Revoke all sessions | JWT |

### Users

| Method | Endpoint | Description | Permission |
|---|---|---|---|
| POST | `/users` | Create user | `user:create` |
| GET | `/users` | List users (paginated) | `user:read` |
| GET | `/users/{id}` | Get user | `user:read` |
| PUT | `/users/{id}` | Update user | `user:update` |
| PATCH | `/users/{id}/status` | Change status | `user:update` |
| PATCH | `/users/{id}/role` | Change role | `user:update` |
| PATCH | `/users/{id}/password` | Change password | Authenticated |
| DELETE | `/users/{id}` | Soft delete | `user:delete` |

### Merchants

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/merchants` | Create merchant (admin) | `merchant:create` |
| POST | `/merchants/register` | Register new user as merchant | Public |
| POST | `/merchants/self-register` | Existing user becomes merchant | JWT |
| GET | `/merchants` | List merchants | `merchant:read` |
| GET | `/merchants/{id}` | Get merchant | `merchant:read` |
| PATCH | `/merchants/{id}/kyc` | Update KYC status | `merchant:kyc` |
| PATCH | `/merchants/{id}/tier/upgrade` | Upgrade tier | `merchant:update` |
| PATCH | `/merchants/{id}/tier/downgrade` | Downgrade tier | `merchant:update` |
| DELETE | `/merchants/{id}` | Soft delete | `merchant:delete` |

### Accounts

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/accounts` | Create account (admin) | ADMIN / SUPER_ADMIN |
| POST | `/accounts/me` | Create own account (merchant) | MERCHANT |
| GET | `/accounts/{id}` | Get account | `account:read` |
| GET | `/accounts/merchant/{id}` | List by merchant | `account:read` |
| PATCH | `/accounts/{id}/status` | Update status | `account:update` |
| DELETE | `/accounts/{id}` | Soft delete | `account:delete` |

### Cards

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/cards` | Create card (admin) | ADMIN / SUPER_ADMIN |
| POST | `/cards/me` | Create own card (merchant) | MERCHANT |
| GET | `/cards/{id}` | Get card | `card:read` |
| GET | `/cards/account/{id}` | List by account | `card:read` |
| PATCH | `/cards/{id}/block` | Block card (admin) | `card:block` |
| PATCH | `/cards/{id}/block/me` | Block own card (merchant) | MERCHANT |
| DELETE | `/cards/{id}` | Soft delete | `card:delete` |

### Transfers

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/transfers` | Create transfer (admin) | ADMIN / SUPER_ADMIN |
| POST | `/transfers/me` | Transfer from own account | MERCHANT |
| GET | `/transfers/{id}` | Get transfer | `transfer:read` |
| GET | `/transfers/account/{id}` | List by account | `transfer:read` |

### Fraud Detection

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/fraud/evaluate` | Evaluate transaction for fraud | `transaction:create` |
| GET | `/fraud/attempts` | View flagged attempts | SUPER_ADMIN |

---

## Fraud Detection

Every transfer is evaluated before processing. Five rules run in order:

**Hard blocks — transaction rejected immediately:**

| Rule | Trigger | Flag |
|---|---|---|
| Blacklist | Merchant on blacklist with no lifted date | `MERCHANT_BLACKLISTED` |
| Rate limit | >5 requests/min from same IP | `RATE_LIMITED` |

**Soft flags — transaction allowed but marked suspicious:**

| Rule | Trigger | Flag |
|---|---|---|
| Card velocity | Same card used 3+ times in 60 seconds | `CARD_VELOCITY_EXCEEDED` |
| Amount anomaly | Exceeds merchant tier single limit | `EXCEEDS_SINGLE_LIMIT` |
| Round amount | Amount divisible by 1000 | `ROUND_AMOUNT` |

All evaluations are logged to `fraud_attempts` regardless of outcome. The fraud log uses `REQUIRES_NEW` propagation so it commits independently — logs survive even if the transfer rolls back.

**Fraud statuses:** `CLEAN` `SUSPICIOUS` `BLOCKED`

---

## Transfers

Transfers use double-entry bookkeeping — every movement creates two transaction records (DEBIT + CREDIT) that always sum to zero.

**Flow:**

```
1. Fraud check
2. Reference uniqueness (idempotency)
3. Balance + currency validation
4. Tier limit check (single / daily / monthly)
5. Atomic DB transaction:
   ├── Insert transfer (PENDING)
   ├── Insert DEBIT transaction
   ├── Insert CREDIT transaction
   ├── Debit from_account balance
   ├── Credit to_account balance
   └── Update transfer (SUCCESS)
```

If anything fails — full rollback, no partial state. The client generates the `reference` before sending — reusing the same reference returns `409 Conflict` (idempotency).

---

## Security

- **JWT** — stateless, access token (15 min) + refresh token (7 days)
- **Token blacklist** — Caffeine-based blacklist. Logout revokes tokens immediately
- **Role-based** — `SUPER_ADMIN`, `ADMIN`, `MERCHANT`
- **Permission-based** — granular `resource:action` permissions (e.g. `merchant:kyc`, `transfer:create`)
- **`@PreAuthorize`** — method-level security on all controller endpoints

---

## Testing

Full integration test suite using Testcontainers (real PostgreSQL, no mocks).

```
src/test/java
├── base
│   ├── BaseIntegrationTest.java        ← @SpringBootTest + Testcontainers
│   └── BaseControllerIntegrationTest.java  ← + MockMvc helpers
├── service
│   ├── auth/
│   ├── service/
│   ├── merchant/
│   ├── account/
│   ├── card/
│   └── transfer/
├── controller
│   └── v1/
│       ├── auth/
│       ├── user/
│       ├── merchant/
│       ├── account/
│       ├── card/
│       └── transfer/
└── stress
    └── TransferStressTest.java         ← @StressTest, excluded from normal runs
```

**Stress test** — 200 concurrent transfer requests measuring avg, P95, P99 response times. Run separately:

```bash
mvn test -Dgroups=stress
```

Or via Postman Runner — select **Stress Tests** folder, set iterations to `200`, delay `0ms`.

---

## Monitoring

| Tool | URL | Purpose |
|---|---|---|
| Actuator | `http://localhost:8080/actuator` | Health, metrics, info |
| Prometheus | `http://localhost:9090` | Metrics scraping |
| Grafana | `http://localhost:3000` | Dashboards (import ID `6756` for Spring Boot) |
| Loki | `http://localhost:3100` | Log aggregation |

**Blacklist cache health indicator** — exposed at `/actuator/health/blacklist`. Reports cache hit rate — below 50% triggers `DOWN` status indicating excessive DB queries for blacklist checks.

**AOP performance monitoring** — every service method execution time is logged via `ObservabilityAspect` with traceId and spanId correlation.

---

## Scheduled Jobs

| Job | Schedule | Purpose |
|---|---|---|
| Transfer Recovery | 00:00, 12:00 daily | Marks stuck `PENDING` transfers as `FAILED` or `SUCCESS` based on whether transaction records exist |
| Balance Reconciliation | 00:30, 12:30 daily | Recalculates all account balances from transaction history, corrects any discrepancies |

Jobs are staggered — recovery runs first to clean up stuck transfers before reconciliation recalculates balances.