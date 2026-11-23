CREATE TABLE orders (
    id                  UUID          PRIMARY KEY,
    customer_email      VARCHAR(254)  NOT NULL,
    status              VARCHAR(16)   NOT NULL
        CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED')),
    cancellation_reason VARCHAR(32)
        CHECK (cancellation_reason IN ('STOCK_UNAVAILABLE', 'PAYMENT_DECLINED',
                                       'PAYMENT_UNAVAILABLE', 'INVENTORY_UNAVAILABLE')),
    currency            VARCHAR(3)    NOT NULL,
    total_amount        NUMERIC(12,2) NOT NULL CHECK (total_amount >= 0),
    created_at          TIMESTAMPTZ   NOT NULL,
    updated_at          TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ck_orders_reason_matches_status
        CHECK ((status = 'CANCELLED') = (cancellation_reason IS NOT NULL))
);

CREATE TABLE order_items (
    id         UUID          PRIMARY KEY,
    order_id   UUID          NOT NULL REFERENCES orders (id),
    product_id UUID          NOT NULL,
    quantity   INTEGER       NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);