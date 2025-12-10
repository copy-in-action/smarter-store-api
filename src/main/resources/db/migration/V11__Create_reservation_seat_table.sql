-- V11__Create_reservation_seat_table.sql

-- ä-Œ ð° Lt
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

COMMENT ON TABLE reservation_seat IS 'ä-Œ ð° Lt';
COMMENT ON COLUMN reservation_seat.reservation_id IS 'ä ID';
COMMENT ON COLUMN reservation_seat.schedule_seat_id IS 'Œ(Ä Œ ID';
