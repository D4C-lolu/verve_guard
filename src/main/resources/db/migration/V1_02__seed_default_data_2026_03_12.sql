CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- roles
INSERT INTO public.roles (id, name) VALUES
    ('01JROLES00000000000000001A', 'SUPER_ADMIN'),
    ('01JROLES00000000000000002B', 'ADMIN'),
    ('01JROLES00000000000000003C', 'MERCHANT');

-- permissions
INSERT INTO public.permissions (id, name, description) VALUES
    -- user management
    ('01JPERMS0000000000000001AA', 'user:read',           'View users'),
    ('01JPERMS0000000000000002BB', 'user:create',         'Create users'),
    ('01JPERMS0000000000000003CC', 'user:update',         'Update users'),
    ('01JPERMS0000000000000004DD', 'user:delete',         'Delete users'),

    -- merchant management
    ('01JPERMS0000000000000005EE', 'merchant:read',       'View merchants'),
    ('01JPERMS0000000000000006FF', 'merchant:create',     'Create merchants'),
    ('01JPERMS0000000000000007GG', 'merchant:update',     'Update merchants'),
    ('01JPERMS0000000000000008HH', 'merchant:delete',     'Delete merchants'),
    ('01JPERMS0000000000000009II', 'merchant:kyc',        'Manage merchant KYC'),
    ('01JPERMS000000000000000AJJ', 'merchant:blacklist',  'Blacklist merchants'),

    -- account management
    ('01JPERMS000000000000000BKK', 'account:read',        'View accounts'),
    ('01JPERMS000000000000000CLL', 'account:create',      'Create accounts'),
    ('01JPERMS000000000000000DMM', 'account:update',      'Update accounts'),
    ('01JPERMS000000000000000ENN', 'account:delete',      'Delete accounts'),

    -- card management
    ('01JPERMS000000000000000FOO', 'card:read',           'View cards'),
    ('01JPERMS000000000000000GPP', 'card:create',         'Create cards'),
    ('01JPERMS000000000000000HQQ', 'card:update',         'Update cards'),
    ('01JPERMS000000000000000IRR', 'card:delete',         'Delete cards'),
    ('01JPERMS000000000000000JSS', 'card:block',          'Block cards'),

    -- transaction management
    ('01JPERMS000000000000000KTT', 'transaction:read',    'View transactions'),
    ('01JPERMS000000000000000LUU', 'transaction:create',  'Create transactions'),
    ('01JPERMS000000000000000MVV', 'transaction:reverse', 'Reverse transactions'),

    -- transfer management
    ('01JPERMS000000000000000NWW', 'transfer:read',       'View transfers'),
    ('01JPERMS000000000000000OXX', 'transfer:create',     'Create transfers'),
    ('01JPERMS000000000000000PYY', 'transfer:reverse',    'Reverse transfers'),

    -- tier management
    ('01JPERMS000000000000000QZZ', 'tier:read',           'View tier config'),
    ('01JPERMS000000000000000R11', 'tier:update',         'Update tier config'),

    -- role & permission management
    ('01JPERMS000000000000000S22', 'role:read',           'View roles'),
    ('01JPERMS000000000000000T33', 'role:create',         'Create roles'),
    ('01JPERMS000000000000000U44', 'role:update',         'Update roles'),
    ('01JPERMS000000000000000V55', 'role:delete',         'Delete roles'),
    ('01JPERMS000000000000000W66', 'permission:read',     'View permissions'),
    ('01JPERMS000000000000000X77', 'permission:assign',   'Assign permissions to roles'),

    -- Actuator endpoints
    ('01JPERMS000000000000000Y88', 'system:monitor', 'Access actuator endpoints');

-- SUPER_ADMIN gets everything
INSERT INTO public.role_permissions (role_id, permission_id)
SELECT '01JROLES00000000000000001A', id FROM public.permissions;

-- ADMIN gets everything except role/permission management and blacklisting
INSERT INTO public.role_permissions (role_id, permission_id)
SELECT '01JROLES00000000000000002B', id FROM public.permissions
WHERE name NOT IN (
                   'role:create', 'role:delete',
                   'permission:assign',
                   'merchant:blacklist'
    );

-- MERCHANT gets read/transact on their own resources only
INSERT INTO public.role_permissions (role_id, permission_id)
SELECT '01JROLES00000000000000003C', id FROM public.permissions
WHERE name IN (
               'account:read', 'account:create',
               'card:read', 'card:create', 'card:block',
               'transaction:read', 'transaction:create',
               'transfer:read', 'transfer:create'
    );

-- default super admin user
INSERT INTO public.users (
    id, firstname, lastname, email, phone,
    password_hash, user_status, role_id,
    created_at, updated_at
) VALUES (
             '01JUSERS0000000000000001AA',
             'Super', 'Admin',
             'superadmin@verveguard.com',
             '00000000000',
             crypt('Admin123!', gen_salt('bf', 10)),
             'ACTIVE',
             '01JROLES00000000000000001A',
             now(), now()
         );


INSERT INTO public.tier_config (
    id, tier,
    daily_transaction_limit,
    single_transaction_limit,
    monthly_transaction_limit,
    max_cards, max_accounts,
    created_at, updated_at
) VALUES
      (
          '01JTIERCONFIG00000000001AA', 'TIER_1',
          100000.0000, 10000.0000, 1000000.0000,
          2, 1,
          now(), now()
      ),
      (
          '01JTIERCONFIG00000000002BB', 'TIER_2',
          500000.0000, 50000.0000, 5000000.0000,
          5, 3,
          now(), now()
      ),
      (
          '01JTIERCONFIG00000000003CC', 'TIER_3',
          2000000.0000, 200000.0000, 20000000.0000,
          10, 5,
          now(), now()
      );


-- test merchant user
INSERT INTO public.users (
    id, firstname, lastname, email, phone,
    password_hash, user_status, role_id,
    created_at, updated_at
) VALUES (
             '01JUSERS0000000000000002BB',
             'Demo', 'Merchant',
             'demo.merchant@verveguard.com',
             '11111111112',
             crypt('Admin123!', gen_salt('bf', 10)),
             'ACTIVE',
             '01JROLES00000000000000003C',
             now(), now()
         );

-- demo merchant
INSERT INTO public.merchants (
    id, user_id, address, kyc_status, merchant_status, tier,
    created_at, updated_at
) VALUES (
             '01JMERCH0000000000000001AA',
             '01JUSERS0000000000000002BB',
             '1 Demo Street, Lagos',
             'APPROVED',
             'ACTIVE',
             'TIER_2',
             now(), now()
         );

-- demo account with funds
INSERT INTO public.accounts (
    id, merchant_id, account_number, account_type,
    currency, balance, ledger_balance, account_status,
    created_at, updated_at
) VALUES (
             '01JACCTS0000000000000001AA',
             '01JMERCH0000000000000001AA',
             '0000000000',
             'SETTLEMENT',
             'NGN',
             500000000.0000,
             500000000.0000,
             'ACTIVE',
             now(), now()
         );

-- demo card linked to account
INSERT INTO public.cards (
    id, card_number, card_hash, account_id, card_type, scheme,
    expiry_month, expiry_year, card_status,
    created_at, updated_at
) VALUES (
             '01JCARDS0000000000000001AA',
             '4011********1111',
             encode(digest('4011111111111111', 'sha256'), 'hex'),
             '01JACCTS0000000000000001AA',
             'VIRTUAL',
             'VISA',
             12, 2028,
             'ACTIVE',
             now(), now()
         );