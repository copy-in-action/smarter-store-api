-- =====================================================
-- 좌석 시스템 개선 데이터 마이그레이션 SQL
-- PostgreSQL용
-- =====================================================

-- 1. TicketOption.total_quantity 업데이트
-- seatingChart JSON에서 등급별 좌석 수 계산하여 저장
-- (seatGrades 배열에서 rows 또는 startRow/endRow 기반으로 계산)

UPDATE ticket_option t
SET total_quantity = COALESCE(
    (
        SELECT
            CASE
                -- rows 배열 형식인 경우: rows 개수 * columns
                WHEN sg->>'rows' IS NOT NULL THEN
                    jsonb_array_length((sg->>'rows')::jsonb) * (v.seating_chart::jsonb->>'columns')::int
                -- startRow/endRow 형식인 경우: (endRow - startRow + 1) * columns
                WHEN sg->>'startRow' IS NOT NULL AND sg->>'endRow' IS NOT NULL THEN
                    ((sg->>'endRow')::int - (sg->>'startRow')::int + 1) * (v.seating_chart::jsonb->>'columns')::int
                ELSE 0
            END
        FROM venue v
        JOIN performance p ON p.venue_id = v.id
        JOIN performance_schedule ps ON ps.performance_id = p.id
        CROSS JOIN LATERAL jsonb_array_elements(v.seating_chart::jsonb->'seatGrades') AS sg
        WHERE ps.id = t.performance_schedule_id
        AND sg->>'grade' = t.seat_grade
        LIMIT 1
    ),
    0
)
WHERE t.total_quantity = 0 OR t.total_quantity IS NULL;

-- 2. ScheduleSeatStatus.seat_grade 업데이트
-- seatingChart JSON에서 좌석 위치(row_num)에 해당하는 등급 조회

UPDATE schedule_seat_status s
SET seat_grade = COALESCE(
    (
        SELECT sg->>'grade'
        FROM venue v
        JOIN performance p ON p.venue_id = v.id
        JOIN performance_schedule ps ON ps.performance_id = p.id
        CROSS JOIN LATERAL jsonb_array_elements(v.seating_chart::jsonb->'seatGrades') AS sg
        WHERE ps.id = s.schedule_id
        AND (
            -- rows 배열 형식: row_num이 배열에 포함되어 있는지
            (sg->>'rows' IS NOT NULL AND
             s.row_num IN (SELECT (elem::text)::int FROM jsonb_array_elements((sg->>'rows')::jsonb) AS elem))
            OR
            -- startRow/endRow 형식: row_num이 범위 내에 있는지
            (sg->>'startRow' IS NOT NULL AND sg->>'endRow' IS NOT NULL AND
             s.row_num >= (sg->>'startRow')::int AND s.row_num <= (sg->>'endRow')::int)
        )
        LIMIT 1
    ),
    s.seat_grade  -- 조회 실패 시 기존 값 유지
)
WHERE seat_grade = 'R' OR seat_grade IS NULL;

-- 3. 마이그레이션 결과 확인용 쿼리
-- SELECT
--     'TicketOption' as table_name,
--     COUNT(*) as total,
--     COUNT(CASE WHEN total_quantity > 0 THEN 1 END) as migrated
-- FROM ticket_option
-- UNION ALL
-- SELECT
--     'ScheduleSeatStatus' as table_name,
--     COUNT(*) as total,
--     COUNT(CASE WHEN seat_grade != 'R' THEN 1 END) as migrated
-- FROM schedule_seat_status;
