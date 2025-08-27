package middleware.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "content_variants")
public class ContentVariant {
    
    @Id
    private String id;
    
    @Column(name = "knowledge_object_id", nullable = false)
    private String knowledgeObjectId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "variant", nullable = false)
    private ContentVariantType variant;
    
    @Column(name = "content", columnDefinition = "TEXT")
    private String content; // nullable if stored in blob
    
    @Column(name = "tokens")
    private Integer tokens;
    
    @Column(name = "embedding_id")
    private String embeddingId;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "storage_uri")
    private String storageUri; // for blob storage
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Constructors
    public ContentVariant() {}
    
    public ContentVariant(String knowledgeObjectId, ContentVariantType variant, String content) {
        this.knowledgeObjectId = knowledgeObjectId;
        this.variant = variant;
        this.content = content;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
        public String getKnowledgeObjectId() {
        return knowledgeObjectId;
    }

    public void setKnowledgeObjectId(String knowledgeObjectId) {
        this.knowledgeObjectId = knowledgeObjectId;
    }
    
    public ContentVariantType getVariant() {
        return variant;
    }
    
    public void setVariant(ContentVariantType variant) {
        this.variant = variant;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Integer getTokens() {
        return tokens;
    }
    
    public void setTokens(Integer tokens) {
        this.tokens = tokens;
    }
    
    public String getEmbeddingId() {
        return embeddingId;
    }
    
    public void setEmbeddingId(String embeddingId) {
        this.embeddingId = embeddingId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getStorageUri() {
        return storageUri;
    }
    
    public void setStorageUri(String storageUri) {
        this.storageUri = storageUri;
    }
}
