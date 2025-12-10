-- V11__Create_reservation_seat_table.sql

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

COMMENT ON TABLE reservation_seat IS '예매-좌석 연결 테이블';
COMMENT ON COLUMN reservation_seat.reservation_id IS '예매 ID';
COMMENT ON COLUMN reservation_seat.schedule_seat_id IS '회차별 좌석 ID';
