-- V3__seed_test_data_2026_03_12.sql

-- test users
INSERT INTO public.users (
    id, firstname, lastname, email, phone,
    password_hash, user_status, role_id,
    created_at, updated_at
) VALUES
      -- admin user
      (
          '01JTESTS0000000000000001AA',
          'Test', 'Admin',
          'testadmin@verveguard.com',
          '11111111111',
          crypt('Admin123!', gen_salt('bf', 10)),
          'ACTIVE',
          '01JROLES00000000000000002B',
          now(), now()
      ),
      -- merchant user
      (
          '01JTESTS0000000000000002BB',
          'Test', 'Merchant',
          'testmerchant@verveguard.com',
          '22222222222',
          crypt('Admin123!', gen_salt('bf', 10)),
          'ACTIVE',
          '01JROLES00000000000000003C',
          now(), now()
      ),
      -- suspended user
      (
          '01JTESTS0000000000000003CC',
          'Suspended', 'User',
          'suspended@verveguard.com',
          '33333333333',
          crypt('Admin123!', gen_salt('bf', 10)),
          'SUSPENDED',
          '01JROLES00000000000000003C',
          now(), now()
      ),
      -- deleted user
      (
          '01JTESTS0000000000000004DD',
          'Deleted', 'User',
          'deleted@verveguard.com',
          '44444444444',
          crypt('Admin123!', gen_salt('bf', 10)),
          'INACTIVE',
          '01JROLES00000000000000003C',
          now(), now()
      );

INSERT INTO public.users (
    id, firstname, lastname, email, phone,
    password_hash, user_status, role_id,
    created_at, updated_at
) VALUES (
             '01JUSERS0000000000000003CC',
             'Test', 'Merchant2',
             'testmerchant2@verveguard.com',
             '444114444444',
             crypt('Admin123!', gen_salt('bf', 10)),
             'ACTIVE',
             '01JROLES00000000000000003C',
             now(), now()
         );

INSERT INTO public.merchants (
    id, user_id, address, kyc_status, merchant_status, tier,
    created_at, updated_at
) VALUES (
             '01JMERCH0000000000000002BB',
             '01JUSERS0000000000000003CC',
             '2 Test Street, Lagos',
             'PENDING',
             'INACTIVE',
             'TIER_1',
             now(), now()
         );


INSERT INTO public.merchant_blacklist (
    id, merchant_id, reason, blacklisted_at
) VALUES (
             '01JBLKLIST000000000000001A',
             '01JMERCH0000000000000002BB',
             'Fraudulent activity detected',
             now()
         );

INSERT INTO public.accounts (
    id, merchant_id, account_number, account_type,
    currency, balance, ledger_balance, account_status,
    created_at, updated_at
) VALUES (
    '01JACCTS0000000000000002BB',
    '01JMERCH0000000000000001AA',
    '1000000002',
    'SETTLEMENT',
    'NGN',
    0.0000,
    0.0000,
    'ACTIVE',
    now(), now()
    );