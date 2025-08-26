package middleware.model;

/**
 * Types of relationships between knowledge objects.
 * 
 * SUPPORTS: One object supports or reinforces another
 * REFERENCES: One object references or cites another
 * CONTRADICTS: Objects contradict each other
 */
public enum RelationshipType {
    SUPPORTS,
    REFERENCES,
    CONTRADICTS
}
