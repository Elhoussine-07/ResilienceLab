CREATE TABLE payments (
    id              UUID          PRIMARY KEY,
    order_id        UUID          NOT NULL,
    idempotency_key VARCHAR(100)  NOT NULL,
    amount          NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    currency        VARCHAR(3)    NOT NULL,
    status          VARCHAR(16)   NOT NULL CHECK (status IN ('APPROVED', 'DECLINED')),
    failure_reason  VARCHAR(255),
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uq_payments_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT uq_payments_order UNIQUE (order_id)
);