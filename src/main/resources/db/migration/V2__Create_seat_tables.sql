-- V2__Create_seat_tables.sql
-- 좌석 관련: seat, schedule_seat

-- Seat 테이블 (공연장 좌석 마스터)
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

COMMENT ON TABLE seat IS '좌석 마스터';
COMMENT ON COLUMN seat.id IS '좌석 ID';
COMMENT ON COLUMN seat.venue_id IS '공연장 ID (FK)';
COMMENT ON COLUMN seat.section IS '구역';
COMMENT ON COLUMN seat.seat_row IS '열';
COMMENT ON COLUMN seat.seat_number IS '좌석 번호';
COMMENT ON COLUMN seat.seat_grade IS '좌석 등급';
COMMENT ON COLUMN seat.position_x IS 'X 좌표';
COMMENT ON COLUMN seat.position_y IS 'Y 좌표';
COMMENT ON COLUMN seat.created_at IS '생성일시';
COMMENT ON COLUMN seat.updated_at IS '수정일시';

-- Schedule Seat 테이블 (회차별 좌석 상태)
CREATE TABLE IF NOT EXISTS schedule_seat (
    id BIGSERIAL PRIMARY KEY,
    schedule_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    ticket_option_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    held_until TIMESTAMP,
    held_by_user_id BIGINT,
    held_by_session_id VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (schedule_id) REFERENCES performance_schedule(id) ON DELETE CASCADE,
    FOREIGN KEY (seat_id) REFERENCES seat(id) ON DELETE CASCADE,
    FOREIGN KEY (ticket_option_id) REFERENCES ticket_option(id) ON DELETE CASCADE,
    UNIQUE (schedule_id, seat_id)
);

COMMENT ON TABLE schedule_seat IS '회차별 좌석 상태';
COMMENT ON COLUMN schedule_seat.id IS '회차별 좌석 ID';
COMMENT ON COLUMN schedule_seat.schedule_id IS '회차 ID (FK)';
COMMENT ON COLUMN schedule_seat.seat_id IS '좌석 ID (FK)';
COMMENT ON COLUMN schedule_seat.ticket_option_id IS '티켓 옵션 ID (FK)';
COMMENT ON COLUMN schedule_seat.status IS '상태 (AVAILABLE, HELD, RESERVED)';
COMMENT ON COLUMN schedule_seat.held_until IS '홀드 만료 시간';
COMMENT ON COLUMN schedule_seat.held_by_user_id IS '홀드한 사용자 ID';
COMMENT ON COLUMN schedule_seat.held_by_session_id IS '홀드한 세션 ID';
COMMENT ON COLUMN schedule_seat.version IS '낙관적 잠금 버전';
COMMENT ON COLUMN schedule_seat.created_at IS '생성일시';
COMMENT ON COLUMN schedule_seat.updated_at IS '수정일시';

CREATE INDEX IF NOT EXISTS idx_schedule_seat_status ON schedule_seat(status);
CREATE INDEX IF NOT EXISTS idx_schedule_seat_held_until ON schedule_seat(held_until);
