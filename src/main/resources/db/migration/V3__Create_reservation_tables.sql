-- V3__Create_reservation_tables.sql
-- 예매 관련: schedule_ticket_stock, reservation, reservation_seat

-- Schedule Ticket Stock 테이블 (회차별 좌석등급 재고)
CREATE TABLE IF NOT EXISTS schedule_ticket_stock (
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

COMMENT ON TABLE schedule_ticket_stock IS '회차별 재고';
COMMENT ON COLUMN schedule_ticket_stock.id IS '재고 ID';
COMMENT ON COLUMN schedule_ticket_stock.schedule_id IS '회차 ID (FK)';
COMMENT ON COLUMN schedule_ticket_stock.ticket_option_id IS '티켓 옵션 ID (FK)';
COMMENT ON COLUMN schedule_ticket_stock.total_quantity IS '총 수량';
COMMENT ON COLUMN schedule_ticket_stock.remaining_quantity IS '잔여 수량';
COMMENT ON COLUMN schedule_ticket_stock.created_at IS '생성일시';
COMMENT ON COLUMN schedule_ticket_stock.updated_at IS '수정일시';

-- Reservation 테이블 (예매)
CREATE TABLE IF NOT EXISTS reservation (
    id BIGSERIAL PRIMARY KEY,
    reservation_number VARCHAR(20) NOT NULL UNIQUE,
    schedule_ticket_stock_id BIGINT NOT NULL,
    schedule_id BIGINT,
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
    FOREIGN KEY (schedule_ticket_stock_id) REFERENCES schedule_ticket_stock(id),
    FOREIGN KEY (schedule_id) REFERENCES performance_schedule(id) ON DELETE SET NULL
);

COMMENT ON TABLE reservation IS '예매 정보';
COMMENT ON COLUMN reservation.id IS '예매 ID';
COMMENT ON COLUMN reservation.reservation_number IS '예매 번호';
COMMENT ON COLUMN reservation.schedule_ticket_stock_id IS '재고 ID (FK)';
COMMENT ON COLUMN reservation.schedule_id IS '회차 ID (FK)';
COMMENT ON COLUMN reservation.user_id IS '사용자 ID';
COMMENT ON COLUMN reservation.user_name IS '예매자 이름';
COMMENT ON COLUMN reservation.user_phone IS '연락처';
COMMENT ON COLUMN reservation.user_email IS '이메일';
COMMENT ON COLUMN reservation.quantity IS '수량';
COMMENT ON COLUMN reservation.total_price IS '총 금액';
COMMENT ON COLUMN reservation.status IS '상태 (PENDING, CONFIRMED, CANCELLED)';
COMMENT ON COLUMN reservation.reserved_at IS '예매 일시';
COMMENT ON COLUMN reservation.confirmed_at IS '확정 일시';
COMMENT ON COLUMN reservation.cancelled_at IS '취소 일시';
COMMENT ON COLUMN reservation.created_at IS '생성일시';
COMMENT ON COLUMN reservation.updated_at IS '수정일시';

CREATE INDEX IF NOT EXISTS idx_reservation_user_id ON reservation(user_id);
CREATE INDEX IF NOT EXISTS idx_reservation_status ON reservation(status);
CREATE INDEX IF NOT EXISTS idx_reservation_reserved_at ON reservation(reserved_at);

-- Reservation Seat 테이블 (예매-좌석 연결)
CREATE TABLE IF NOT EXISTS reservation_seat (
    id BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    schedule_seat_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (reservation_id) REFERENCES reservation(id) ON DELETE CASCADE,
    FOREIGN KEY (schedule_seat_id) REFERENCES schedule_seat(id) ON DELETE CASCADE,
    UNIQUE (reservation_id, schedule_seat_id)
);

COMMENT ON TABLE reservation_seat IS '예매-좌석 연결';
COMMENT ON COLUMN reservation_seat.id IS 'ID';
COMMENT ON COLUMN reservation_seat.reservation_id IS '예매 ID (FK)';
COMMENT ON COLUMN reservation_seat.schedule_seat_id IS '회차별 좌석 ID (FK)';
COMMENT ON COLUMN reservation_seat.created_at IS '생성일시';
COMMENT ON COLUMN reservation_seat.updated_at IS '수정일시';
