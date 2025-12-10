-- V9__Create_seat_table.sql

CREATE TABLE IF NOT EXISTS seat (
    id BIGSERIAL PRIMARY KEY,
    venue_id BIGINT NOT NULL,
    section VARCHAR(50) NOT NULL,
    seat_row VARCHAR(10) NOT NULL,
    seat_number INT NOT NULL,
    seat_grade VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    position_x INT NOT NULL,
    position_y INT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (venue_id) REFERENCES venue(id) ON DELETE CASCADE,
    UNIQUE (venue_id, seat_row, seat_number)
);

COMMENT ON TABLE seat IS '공연장 좌석 정보';
COMMENT ON COLUMN seat.venue_id IS '공연장 ID';
COMMENT ON COLUMN seat.section IS '구역';
COMMENT ON COLUMN seat.seat_row IS '열';
COMMENT ON COLUMN seat.seat_number IS '좌석 번호';
COMMENT ON COLUMN seat.seat_grade IS '좌석 등급 (VIP, STANDARD, ECONOMY)';
COMMENT ON COLUMN seat.position_x IS 'X 좌표';
COMMENT ON COLUMN seat.position_y IS 'Y 좌표';
