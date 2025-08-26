package middleware.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "knowledge_relationships")
public class KnowledgeRelationship {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "source_id", nullable = false)
    private UUID sourceId;
    
    @Column(name = "target_id", nullable = false)
    private UUID targetId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private RelationshipType type;
    
    @Column(name = "confidence")
    private Double confidence;
    
    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence; // JSON object
    
    @Column(name = "detected_by")
    private String detectedBy;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Constructors
    public KnowledgeRelationship() {}
    
    public KnowledgeRelationship(UUID sourceId, UUID targetId, RelationshipType type) {
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.type = type;
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getSourceId() {
        return sourceId;
    }
    
    public void setSourceId(UUID sourceId) {
        this.sourceId = sourceId;
    }
    
    public UUID getTargetId() {
        return targetId;
    }
    
    public void setTargetId(UUID targetId) {
        this.targetId = targetId;
    }
    
    public RelationshipType getType() {
        return type;
    }
    
    public void setType(RelationshipType type) {
        this.type = type;
    }
    
    public Double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
    
    public String getEvidence() {
        return evidence;
    }
    
    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }
    
    public String getDetectedBy() {
        return detectedBy;
    }
    
    public void setDetectedBy(String detectedBy) {
        this.detectedBy = detectedBy;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
