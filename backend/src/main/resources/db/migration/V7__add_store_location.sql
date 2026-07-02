-- Add location fields to stores table for store positioning and distance display.
ALTER TABLE stores ADD latitude  DECIMAL(10,7) NULL;
ALTER TABLE stores ADD longitude DECIMAL(10,7) NULL;
