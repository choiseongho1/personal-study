CREATE TABLE work_items (
 id VARCHAR(100) NOT NULL PRIMARY KEY,
 kind VARCHAR(32) NOT NULL,
 reference_id BIGINT NOT NULL,
 status VARCHAR(32) NOT NULL,
 attempts INT NOT NULL,
 next_attempt_at DATETIME(6) NOT NULL,
 lease_until DATETIME(6),
 lease_token VARCHAR(36),
 last_error VARCHAR(1000),
 INDEX idx_work_due (status, next_attempt_at)
);
CREATE TABLE outbox_events (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 event_id VARCHAR(36) NOT NULL UNIQUE,
 payment_attempt_id BIGINT NOT NULL,
 order_id BIGINT NOT NULL,
 created_at DATETIME(6) NOT NULL,
 published_at DATETIME(6),
 CONSTRAINT fk_outbox_attempt FOREIGN KEY (payment_attempt_id) REFERENCES payment_attempts(id)
);
CREATE TABLE inbox_receipts (
 event_id VARCHAR(36) NOT NULL PRIMARY KEY,
 payment_attempt_id BIGINT NOT NULL,
 received_at DATETIME(6) NOT NULL
);
