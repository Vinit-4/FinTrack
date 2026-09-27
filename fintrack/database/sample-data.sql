-- ---------------------------------------------------------------
-- Sample development data.
--
-- IMPORTANT: register the demo account through the API first, so that the
-- password is hashed by BCrypt exactly the way the app expects. No password
-- hash is committed in this repository.
--
--   curl -X POST http://localhost:8080/api/auth/register \
--     -H "Content-Type: application/json" \
--     -d '{"name":"Demo User","email":"demo@fintrack.local","password":"Demo@12345"}'
--
-- Then run:  psql -U postgres -d fintrack -f database/sample-data.sql
--
-- Every INSERT looks the user up by email, so it works whatever id was assigned.
-- Dates are written relative to today, so the dashboard and the 6-month charts
-- always have something to show.
-- ---------------------------------------------------------------

INSERT INTO transactions (user_id, type, amount, category, description, transaction_date, created_at)
SELECT u.id, v.type, v.amount, v.category, v.description,
       (date_trunc('month', CURRENT_DATE) - (v.months_ago || ' month')::interval + (v.day_offset || ' day')::interval)::date,
       NOW()
FROM users u
CROSS JOIN (VALUES
    -- months_ago, day_offset, type, amount, category, description
    (0, 0,  'INCOME',  52000.00, 'SALARY',        'Monthly salary'),
    (0, 2,  'EXPENSE',  4500.00, 'FOOD',          'Groceries'),
    (0, 3,  'EXPENSE', 12000.00, 'RENT',          'Flat rent'),
    (0, 5,  'EXPENSE',  2000.00, 'TRANSPORT',     'Metro card recharge'),
    (0, 8,  'EXPENSE',  3200.00, 'SHOPPING',      'Winter jacket'),
    (0, 11, 'EXPENSE',  1500.00, 'BILLS',         'Electricity bill'),
    (0, 14, 'EXPENSE',   899.00, 'ENTERTAINMENT', 'Streaming subscription'),
    (0, 16, 'INCOME',    6000.00,'INVESTMENT',    'Mutual fund payout'),

    (1, 1,  'INCOME',  52000.00, 'SALARY',        'Monthly salary'),
    (1, 4,  'EXPENSE', 12000.00, 'RENT',          'Flat rent'),
    (1, 6,  'EXPENSE',  5100.00, 'FOOD',          'Groceries and eating out'),
    (1, 9,  'EXPENSE',  1800.00, 'TRANSPORT',     'Cab rides'),
    (1, 13, 'EXPENSE',  2400.00, 'HEALTH',        'Dental check-up'),
    (1, 18, 'EXPENSE',  7000.00, 'EDUCATION',     'Online course'),

    (2, 1,  'INCOME',  50000.00, 'SALARY',        'Monthly salary'),
    (2, 3,  'EXPENSE', 12000.00, 'RENT',          'Flat rent'),
    (2, 7,  'EXPENSE',  4100.00, 'FOOD',          'Groceries'),
    (2, 10, 'EXPENSE',  2600.00, 'SHOPPING',      'Running shoes'),
    (2, 15, 'EXPENSE',  1450.00, 'BILLS',         'Internet bill'),

    (3, 2,  'INCOME',  50000.00, 'SALARY',        'Monthly salary'),
    (3, 5,  'EXPENSE', 12000.00, 'RENT',          'Flat rent'),
    (3, 8,  'EXPENSE',  3900.00, 'FOOD',          'Groceries'),
    (3, 12, 'EXPENSE',  2200.00, 'ENTERTAINMENT', 'Concert tickets'),
    (3, 20, 'EXPENSE',  1100.00, 'OTHER',         'Gift'),

    (4, 1,  'INCOME',  50000.00, 'SALARY',        'Monthly salary'),
    (4, 4,  'EXPENSE', 12000.00, 'RENT',          'Flat rent'),
    (4, 9,  'EXPENSE',  4700.00, 'FOOD',          'Groceries'),
    (4, 17, 'EXPENSE',  3000.00, 'TRANSPORT',     'Train tickets home'),

    (5, 2,  'INCOME',  48000.00, 'SALARY',        'Monthly salary'),
    (5, 6,  'EXPENSE', 12000.00, 'RENT',          'Flat rent'),
    (5, 11, 'EXPENSE',  4300.00, 'FOOD',          'Groceries'),
    (5, 19, 'EXPENSE',  2800.00, 'HEALTH',        'Gym membership')
) AS v(months_ago, day_offset, type, amount, category, description)
WHERE u.email = 'demo@fintrack.local';
