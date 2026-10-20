-- 이번 실습은 주문당 하나의 결제 시도를 사용합니다.
ALTER TABLE payment_attempts
    ADD CONSTRAINT uk_payment_order UNIQUE (order_id);

CREATE TABLE refund_requests (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    payment_attempt_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_refund_payment UNIQUE (payment_attempt_id),
    CONSTRAINT uk_refund_key UNIQUE (idempotency_key),
    CONSTRAINT fk_refund_payment FOREIGN KEY (payment_attempt_id) REFERENCES payment_attempts(id)
);
