-- Table Comments
COMMENT ON TABLE users IS '사용자 정보';
COMMENT ON TABLE refresh_tokens IS '리프레시 토큰 정보';
COMMENT ON TABLE admins IS '관리자 정보';
COMMENT ON TABLE venue IS '공연장 정보';
COMMENT ON TABLE performance IS '공연 정보';
COMMENT ON TABLE performance_schedule IS '공연 스케줄 정보';
COMMENT ON TABLE ticket_option IS '티켓 옵션 정보';

-- Column Comments for users table
COMMENT ON COLUMN users.id IS '사용자 ID';
COMMENT ON COLUMN users.email IS '사용자 이메일';
COMMENT ON COLUMN users.username IS '사용자 이름';
COMMENT ON COLUMN users.password_hash IS '비밀번호 해시';
COMMENT ON COLUMN users.role IS '사용자 역할 (USER, ADMIN)';
COMMENT ON COLUMN users.created_at IS '생성일시';
COMMENT ON COLUMN users.updated_at IS '수정일시';

-- Column Comments for refresh_tokens table
COMMENT ON COLUMN refresh_tokens.id IS '리프레시 토큰 ID';
COMMENT ON COLUMN refresh_tokens.user_id IS '사용자 ID (FK)';
COMMENT ON COLUMN refresh_tokens.token IS '리프레시 토큰 값';
COMMENT ON COLUMN refresh_tokens.expiry_date IS '토큰 만료일시';
COMMENT ON COLUMN refresh_tokens.created_at IS '생성일시';
COMMENT ON COLUMN refresh_tokens.updated_at IS '수정일시';

-- Column Comments for admin table
COMMENT ON COLUMN admins.id IS '관리자 ID';
COMMENT ON COLUMN admins.login_id IS '관리자 로그인 ID';
COMMENT ON COLUMN admins.password_hash IS '비밀번호 해시';
COMMENT ON COLUMN admins.created_at IS '생성일시';
COMMENT ON COLUMN admins.updated_at IS '수정일시';

-- Column Comments for venue table
COMMENT ON COLUMN venue.id IS '공연장 ID';
COMMENT ON COLUMN venue.name IS '공연장 이름';
COMMENT ON COLUMN venue.address IS '공연장 주소';
COMMENT ON COLUMN venue.seating_chart_url IS '좌석 배치도 URL';
COMMENT ON COLUMN venue.created_at IS '생성일시';
COMMENT ON COLUMN venue.updated_at IS '수정일시';

-- Column Comments for performance table
COMMENT ON COLUMN performance.id IS '공연 ID';
COMMENT ON COLUMN performance.title IS '공연명';
COMMENT ON COLUMN performance.description IS '공연 상세 설명';
COMMENT ON COLUMN performance.category IS '공연 카테고리';
COMMENT ON COLUMN performance.running_time IS '공연 시간 (분)';
COMMENT ON COLUMN performance.age_rating IS '관람 연령';
COMMENT ON COLUMN performance.main_image_url IS '대표 이미지 URL';
COMMENT ON COLUMN performance.venue_id IS '공연장 ID (FK)';
COMMENT ON COLUMN performance.start_date IS '공연 시작일';
COMMENT ON COLUMN performance.end_date IS '공연 종료일';
COMMENT ON COLUMN performance.created_at IS '생성일시';
COMMENT ON COLUMN performance.updated_at IS '수정일시';

-- Column Comments for performance_schedule table
COMMENT ON COLUMN performance_schedule.id IS '공연 스케줄 ID';
COMMENT ON COLUMN performance_schedule.performance_id IS '공연 ID (FK)';
COMMENT ON COLUMN performance_schedule.show_datetime IS '공연 일시';
COMMENT ON COLUMN performance_schedule.sale_start_datetime IS '티켓 판매 시작 일시';
COMMENT ON COLUMN performance_schedule.created_at IS '생성일시';
COMMENT ON COLUMN performance_schedule.updated_at IS '수정일시';

-- Column Comments for ticket_option table
COMMENT ON COLUMN ticket_option.id IS '티켓 옵션 ID';
COMMENT ON COLUMN ticket_option.performance_id IS '공연 ID (FK)';
COMMENT ON COLUMN ticket_option.name IS '티켓/좌석 등급명';
COMMENT ON COLUMN ticket_option.price IS '티켓 가격';
COMMENT ON COLUMN ticket_option.total_quantity IS '총 판매 가능 수량';
COMMENT ON COLUMN ticket_option.created_at IS '생성일시';
COMMENT ON COLUMN ticket_option.updated_at IS '수정일시';
