-- Football EDA Database Initialization
-- This script runs when the PostgreSQL container starts for the first time

-- Ensure the database exists (created by POSTGRES_DB env var)
-- Grant all privileges to the football user
GRANT ALL PRIVILEGES ON DATABASE football_eda TO football;

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
