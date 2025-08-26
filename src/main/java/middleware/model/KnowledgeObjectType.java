package middleware.model;

/**
 * Types of knowledge objects in the system.
 * 
 * TURN: Individual conversation turns
 * FILE_CHUNK: Chunks of uploaded files
 * SUMMARY: Summarized content
 * EXTRACTED_FACT: Structured facts extracted from content
 * SESSION_MEMORY: Session-level memories
 */
public enum KnowledgeObjectType {
    TURN,
    FILE_CHUNK,
    SUMMARY,
    EXTRACTED_FACT,
    SESSION_MEMORY
}
