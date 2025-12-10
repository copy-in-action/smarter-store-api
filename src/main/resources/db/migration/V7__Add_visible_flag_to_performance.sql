-- V7__Add_visible_flag_to_performance.sql

-- Add visible column to performance table
ALTER TABLE performance ADD COLUMN IF NOT EXISTS visible BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN performance.visible IS 'υπ x μ€';
