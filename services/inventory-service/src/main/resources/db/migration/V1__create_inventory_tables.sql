CREATE TABLE stock_items (
    product_id         UUID        PRIMARY KEY,
    available_quantity INTEGER     NOT NULL CHECK (available_quantity >= 0),
    reserved_quantity  INTEGER     NOT NULL CHECK (reserved_quantity >= 0),
    updated_at         TIMESTAMPTZ NOT NULL
);

CREATE TABLE reservations (
    id         UUID        PRIMARY KEY,
    order_id   UUID        NOT NULL,
    product_id UUID        NOT NULL,
    quantity   INTEGER     NOT NULL CHECK (quantity > 0),
    status     VARCHAR(16) NOT NULL CHECK (status IN ('RESERVED', 'RELEASED')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_reservation_order_product UNIQUE (order_id, product_id)
);