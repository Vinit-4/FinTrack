-- ---------------------------------------------------------------
-- FinTrack schema (reference only).
--
-- You do NOT have to run this file: Hibernate creates these tables
-- from the entity classes when the app starts (spring.jpa.hibernate.ddl-auto=update).
-- It is kept here so the design is readable without opening Java code,
-- and so you can show the schema in an interview.
-- ---------------------------------------------------------------

CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL,
    password    VARCHAR(255) NOT NULL,           -- BCrypt hash, never plain text
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS transactions (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT         NOT NULL,
    type             VARCHAR(10)    NOT NULL,    -- INCOME | EXPENSE
    amount           NUMERIC(15, 2) NOT NULL,
    category         VARCHAR(30)    NOT NULL,
    description      VARCHAR(255),
    transaction_date DATE           NOT NULL,
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_transactions_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_transactions_type CHECK (type IN ('INCOME', 'EXPENSE'))
);

-- Every query in the app starts with "this user's rows", usually within a date
-- range, so these two indexes cover almost all of the read traffic.
CREATE INDEX IF NOT EXISTS idx_transactions_user      ON transactions (user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_user_date ON transactions (user_id, transaction_date);
