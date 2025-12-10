-- V7__Add_visible_flag_to_performance.sql

ALTER TABLE performance ADD COLUMN IF NOT EXISTS visible BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN performance.visible IS '공연 노출 여부';
