CREATE TABLE notifications (
    id                UUID          PRIMARY KEY,
    order_id          UUID          NOT NULL,
    notification_type VARCHAR(32)   NOT NULL
        CHECK (notification_type IN ('ORDER_CONFIRMED', 'ORDER_CANCELLED')),
    recipient         VARCHAR(254)  NOT NULL,
    subject           VARCHAR(200)  NOT NULL,
    body              VARCHAR(1000) NOT NULL,
    created_at        TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uq_notifications_order_type UNIQUE (order_id, notification_type)
);

CREATE TABLE processed_events (
    event_id     UUID        PRIMARY KEY,
    event_type   VARCHAR(64) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);