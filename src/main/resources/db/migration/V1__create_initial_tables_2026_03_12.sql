CREATE TABLE IF NOT EXISTS public.roles (
    id                                      character (26) NOT NULL,
    name                                    character varying (50) NOT NULL,
    CONSTRAINT roles_pkey PRIMARY KEY (id),
    CONSTRAINT roles_name_unique UNIQUE (name)
);

CREATE TABLE permissions (
    id                                      character (26) NOT NULL,
    name                                    character varying (100) NOT NULL,
    description                             text,
    CONSTRAINT permissions_pkey PRIMARY KEY (id),
    CONSTRAINT permissions_name_unique UNIQUE (name)
);

CREATE TABLE role_permissions (
    role_id                                 character (26) NOT NULL,
    permission_id                           character (26) NOT NULL,
    created_at                              timestamp with time zone NOT NULL DEFAULT now(),
    created_by                              character (26),
    CONSTRAINT role_permissions_pkey PRIMARY KEY (role_id, permission_id),
    CONSTRAINT role_permissions_role_fkey FOREIGN KEY (role_id) REFERENCES public.roles (id) ON DELETE CASCADE,
    CONSTRAINT role_permissions_permission_fkey FOREIGN KEY (permission_id) REFERENCES public.permissions (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.users (
    id                                      character (26) NOT NULL,
    firstname                               character varying (255) NOT NULL,
    lastname                                character varying (255) NOT NULL,
    othername                               character varying (255),
    email                                   character varying (255) NOT NULL,
    phone                                   character varying (20) NOT NULL,
    password_hash                           character varying (255) NOT NULL,
    user_status                             character varying (50) NOT NULL,
    role_id                                 character(26) NOT NULL,
    created_at                              timestamp with time zone NOT NULL DEFAULT now(),
    updated_at                              timestamp with time zone NOT NULL,
    deleted_at                              timestamp with time zone,
    created_by                              character (26),
    updated_by                              character (26),
    deleted_by                              character (26),
    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT users_email_unique UNIQUE (email),
    CONSTRAINT users_phone_unique UNIQUE (phone),
    CONSTRAINT users_role_fkey FOREIGN KEY (role_id) REFERENCES public.roles (id) ON DELETE RESTRICT,
    CONSTRAINT users_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users (id) ON DELETE SET NULL,
    CONSTRAINT users_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES public.users (id) ON DELETE SET NULL,
    CONSTRAINT users_deleted_by_fkey FOREIGN KEY (deleted_by) REFERENCES public.users (id) ON DELETE SET NULL
);

ALTER TABLE public.role_permissions
ADD CONSTRAINT role_permissions_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users (id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS public.merchants (
    id                                      character (26) NOT NULL,
    user_id                                 character (26) NOT NULL,
    address                                 text,
    kyc_status                              character varying (50) NOT NULL,
    merchant_status                         character varying (50) NOT NULL,
    tier                                    character varying (50) NOT NULL,
    created_at                              timestamp with time zone NOT NULL DEFAULT now(),
    updated_at                              timestamp with time zone NOT NULL,
    deleted_at                              timestamp with time zone,
    created_by                              character (26),
    updated_by                              character (26),
    deleted_by                              character (26),
    CONSTRAINT merchant_pkey PRIMARY KEY (id),
    CONSTRAINT merchant_user_fkey FOREIGN KEY (user_id) REFERENCES public.users (id) ON DELETE RESTRICT,
    CONSTRAINT merchant_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users (id) ON DELETE SET NULL,
    CONSTRAINT merchant_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES public.users (id) ON DELETE SET NULL,
    CONSTRAINT merchant_deleted_by_fkey FOREIGN KEY (deleted_by) REFERENCES public.users (id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS public.accounts (
    id                                      character (26) NOT NULL,
    merchant_id                             character (26) NOT NULL,
    account_number                          character varying (20) NOT NULL,
    account_type                            character varying (50) NOT NULL,
    currency                                character (3) NOT NULL,
    balance                                 numeric(19, 4) NOT NULL DEFAULT 0,
    ledger_balance                          numeric(19, 4) NOT NULL DEFAULT 0,
    account_status                          character varying (50) NOT NULL,
    created_at                              timestamp with time zone NOT NULL DEFAULT now(),
    updated_at                              timestamp with time zone NOT NULL,
    deleted_at                              timestamp with time zone,
    created_by                              character (26),
    updated_by                              character (26),
    deleted_by                              character (26),
    CONSTRAINT account_pkey PRIMARY KEY (id),
    CONSTRAINT account_number_unique UNIQUE (account_number),
    CONSTRAINT account_merchant_fkey FOREIGN KEY (merchant_id) REFERENCES public.merchants(id) ON DELETE RESTRICT,
    CONSTRAINT account_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id) ON DELETE SET NULL,
    CONSTRAINT account_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES public.users(id) ON DELETE SET NULL,
    CONSTRAINT account_deleted_by_fkey FOREIGN KEY (deleted_by) REFERENCES public.users(id) ON DELETE SET NULL
);


CREATE TABLE IF NOT EXISTS public.cards (
    id                                      character (26) NOT NULL,
    card_number                             character varying (19) NOT NULL,  -- masked, e.g. 4111********1111
    account_id                              character (26) NOT NULL,
    card_type                               character varying (50) NOT NULL,  -- VIRTUAL, PHYSICAL
    scheme                                  character varying (50) NOT NULL,  -- VISA, MASTERCARD, VERVE
    expiry_month                            smallint NOT NULL,
    expiry_year                             smallint NOT NULL,
    card_status                             character varying (50) NOT NULL,  -- ACTIVE, BLOCKED, EXPIRED
    created_at                              timestamp with time zone NOT NULL DEFAULT now(),
    updated_at                              timestamp with time zone NOT NULL,
    deleted_at                              timestamp with time zone,
    created_by                              character (26),
    updated_by                              character (26),
    deleted_by                              character (26),
    CONSTRAINT cards_pkey PRIMARY KEY (id),
    CONSTRAINT cards_number_unique UNIQUE (card_number),
    CONSTRAINT cards_account_fkey FOREIGN KEY (account_id) REFERENCES public.account(id) ON DELETE RESTRICT,
    CONSTRAINT cards_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id) ON DELETE SET NULL,
    CONSTRAINT cards_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES public.users(id) ON DELETE SET NULL,
    CONSTRAINT cards_deleted_by_fkey FOREIGN KEY (deleted_by) REFERENCES public.users(id) ON DELETE SET NULL
);


CREATE TABLE IF NOT EXISTS public.tier_config (
    id                                      character (26) NOT NULL,
    tier                                    character varying (50) NOT NULL,
    daily_transaction_limit                 numeric (19, 4) NOT NULL,
    single_transaction_limit                numeric (19, 4) NOT NULL,
    monthly_transaction_limit               numeric (19, 4) NOT NULL,
    max_cards                               integer NOT NULL,
    max_accounts                            integer NOT NULL,
    created_at                              timestamp with time zone NOT NULL DEFAULT now(),
    updated_at                              timestamp with time zone NOT NULL,
    created_by                              character (26),
    updated_by                              character (26),
    CONSTRAINT tier_config_pkey PRIMARY KEY (id),
    CONSTRAINT tier_config_tier_unique UNIQUE (tier),
    CONSTRAINT tier_config_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users (id) ON DELETE SET NULL,
    CONSTRAINT tier_config_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES public.users (id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS public.transactions (
    id                                      character (26) NOT NULL,
    account_id                              character (26) NOT NULL,
    card_id                                 character (26),
    transaction_type                        character varying (50) NOT NULL,
    channel                                 character varying (50) NOT NULL,
    amount                                  numeric (19, 4) NOT NULL,
    fee                                     numeric (19, 4) NOT NULL DEFAULT 0,
    currency                                character (3) NOT NULL,
    transaction_status                      character varying (50) NOT NULL,
    created_at                              timestamp with time zone NOT NULL DEFAULT now(),
    updated_at                              timestamp with time zone NOT NULL,
    created_by                              character (26),
    updated_by                              character (26),
    CONSTRAINT transactions_pkey PRIMARY KEY (id),
    CONSTRAINT transactions_reference_unique UNIQUE (reference),
    CONSTRAINT transactions_account_fkey FOREIGN KEY (account_id) REFERENCES public.account (id) ON DELETE RESTRICT,
    CONSTRAINT transactions_card_fkey FOREIGN KEY (card_id) REFERENCES public.cards (id) ON DELETE RESTRICT,
    CONSTRAINT transactions_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users (id) ON DELETE SET NULL,
    CONSTRAINT transactions_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES public.users (id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS public.transfers (
    id                                      character (26) NOT NULL,
    reference                               character varying (100) NOT NULL,
    from_account_id                         character (26) NOT NULL,
    to_account_id                           character (26) NOT NULL,
    amount                                  numeric (19, 4) NOT NULL,
    currency                                character (3) NOT NULL,
    transfer_status                         character varying (50) NOT NULL,
    description                             text,
    metadata                                jsonb,
    created_at                              timestamp with time zone NOT NULL DEFAULT now(),
    updated_at                              timestamp with time zone NOT NULL,
    created_by                              character (26),
    updated_by                              character (26),
    CONSTRAINT transfers_pkey PRIMARY KEY (id),
    CONSTRAINT transfers_reference_unique UNIQUE (reference),
    CONSTRAINT transfers_from_to_different CHECK (from_account_id <> to_account_id),
    CONSTRAINT transfers_amount_positive CHECK (amount > 0),
    CONSTRAINT transfers_reference_unique UNIQUE (reference),
    CONSTRAINT transfers_from_account_fkey FOREIGN KEY (from_account_id) REFERENCES public.accounts (id) ON DELETE RESTRICT,
    CONSTRAINT transfers_to_account_fkey FOREIGN KEY (to_account_id) REFERENCES public.accounts (id) ON DELETE RESTRICT,
    CONSTRAINT transfers_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users (id) ON DELETE SET NULL,
    CONSTRAINT transfers_updated_by_fkey FOREIGN KEY (updated_by) REFERENCES public.users (id) ON DELETE SET NULL
);

-- most common: fetch all transactions for an account
CREATE INDEX IF NOT EXISTS idx_transactions_account_id
    on public.transactions (account_id);

-- card-based lookups
CREATE INDEX IF NOT EXISTS idx_transactions_card_id
    on public.transactions (card_id);

-- daily/monthly limit checks: sum transactions per account within a date range
CREATE INDEX IF NOT EXISTS idx_transactions_account_created_at
    on public.transactions (account_id, created_at);

-- status filtering: find all pending transactions
CREATE INDEX IF NOT EXISTS idx_transactions_status
    on public.transactions (transaction_status);

-- compound: account + status (e.g. all failed txns for an account)
CREATE INDEX IF NOT EXISTS idx_transactions_account_status
    on public.transactions (account_id, transaction_status);

ALTER TABLE public.transactions
    ADD COLUMN transfer_id                  character (26),
    ADD CONSTRAINT transactions_transfer_fkey FOREIGN KEY (transfer_id) REFERENCES public.transfers (id) ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_transactions_transfer_id
    on public.transactions (transfer_id);