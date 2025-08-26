-- Fix parent_id column type in knowledge_objects table
-- Change from UUID to VARCHAR to match entity definition

ALTER TABLE knowledge_objects 
ALTER COLUMN parent_id TYPE VARCHAR(255);
