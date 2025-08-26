-- PostgreSQL initialization script for Knowledge-Aware LLM Middleware

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Create database if it doesn't exist (this will be done by Docker environment)
-- The actual schema will be created by Flyway migrations

-- Grant necessary permissions
GRANT ALL PRIVILEGES ON DATABASE knowledge_middleware TO postgres;
