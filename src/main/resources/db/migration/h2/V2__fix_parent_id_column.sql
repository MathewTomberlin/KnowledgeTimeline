-- Fix parent_id column type in knowledge_objects table for H2
-- Change from UUID to VARCHAR to match entity definition

ALTER TABLE knowledge_objects 
ALTER COLUMN parent_id VARCHAR(255);
