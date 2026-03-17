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