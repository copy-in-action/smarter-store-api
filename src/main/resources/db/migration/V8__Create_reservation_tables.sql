-- 회차별 좌석등급 재고 테이블
CREATE TABLE schedule_ticket_stock (
    id BIGSERIAL PRIMARY KEY,
    schedule_id BIGINT NOT NULL,
    ticket_option_id BIGINT NOT NULL,
    total_quantity INT NOT NULL,
    remaining_quantity INT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (schedule_id) REFERENCES performance_schedule(id) ON DELETE CASCADE,
    FOREIGN KEY (ticket_option_id) REFERENCES ticket_option(id) ON DELETE CASCADE,
    UNIQUE (schedule_id, ticket_option_id)
);

COMMENT ON TABLE schedule_ticket_stock IS '회차별 좌석등급 재고';
COMMENT ON COLUMN schedule_ticket_stock.schedule_id IS '공연 회차 ID';
COMMENT ON COLUMN schedule_ticket_stock.ticket_option_id IS '좌석 등급 ID';
COMMENT ON COLUMN schedule_ticket_stock.total_quantity IS '총 좌석 수';
COMMENT ON COLUMN schedule_ticket_stock.remaining_quantity IS '잔여 좌석 수';

-- 예매 테이블
CREATE TABLE reservation (
    id BIGSERIAL PRIMARY KEY,
    reservation_number VARCHAR(20) NOT NULL UNIQUE,
    schedule_ticket_stock_id BIGINT NOT NULL,
    user_id BIGINT,
    user_name VARCHAR(100),
    user_phone VARCHAR(20),
    user_email VARCHAR(255),
    quantity INT NOT NULL,
    total_price DECIMAL(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reserved_at TIMESTAMP NOT NULL,
    confirmed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (schedule_ticket_stock_id) REFERENCES schedule_ticket_stock(id)
);

COMMENT ON TABLE reservation IS '예매 정보';
COMMENT ON COLUMN reservation.reservation_number IS '예매 번호';
COMMENT ON COLUMN reservation.schedule_ticket_stock_id IS '회차별 좌석등급 재고 ID';
COMMENT ON COLUMN reservation.user_id IS '회원 ID (비회원인 경우 NULL)';
COMMENT ON COLUMN reservation.user_name IS '예매자 이름';
COMMENT ON COLUMN reservation.user_phone IS '예매자 연락처';
COMMENT ON COLUMN reservation.user_email IS '예매자 이메일';
COMMENT ON COLUMN reservation.quantity IS '예매 수량';
COMMENT ON COLUMN reservation.total_price IS '총 결제 금액';
COMMENT ON COLUMN reservation.status IS '예매 상태 (PENDING, CONFIRMED, CANCELLED)';
COMMENT ON COLUMN reservation.reserved_at IS '예매 일시';
COMMENT ON COLUMN reservation.confirmed_at IS '확정 일시';
COMMENT ON COLUMN reservation.cancelled_at IS '취소 일시';

-- 인덱스
CREATE INDEX idx_reservation_user_id ON reservation(user_id);
CREATE INDEX idx_reservation_status ON reservation(status);
CREATE INDEX idx_reservation_reserved_at ON reservation(reserved_at);
