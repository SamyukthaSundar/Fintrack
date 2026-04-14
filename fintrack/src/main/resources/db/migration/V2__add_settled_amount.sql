-- Migration: Add settled_amount column to expense_splits
-- This supports partial settlement tracking

ALTER TABLE expense_splits 
ADD COLUMN IF NOT EXISTS settled_amount DECIMAL(12, 2) DEFAULT 0.00;
