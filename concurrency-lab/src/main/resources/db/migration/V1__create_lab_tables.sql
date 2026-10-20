CREATE TABLE inventory (
    event_id BIGINT NOT NULL PRIMARY KEY,
    available INT NOT NULL,
    unit_price DECIMAL(19, 2) NOT NULL,
    CONSTRAINT ck_inventory_available CHECK (available >= 0),
    CONSTRAINT ck_inventory_price CHECK (unit_price >= 0)
);

CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_orders_user_event UNIQUE (user_id, event_id),
    CONSTRAINT fk_orders_inventory FOREIGN KEY (event_id) REFERENCES inventory(event_id),
    INDEX idx_orders_expiration (status, expires_at, id)
);

CREATE TABLE payment_attempts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    provider_payment_id VARCHAR(100),
    CONSTRAINT uk_payment_key UNIQUE (idempotency_key),
    CONSTRAINT uk_provider_payment UNIQUE (provider_payment_id),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT ck_payment_amount CHECK (amount >= 0)
);
