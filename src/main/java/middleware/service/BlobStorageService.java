package middleware.service;

import java.io.InputStream;
import java.util.Map;

/**
 * Service interface for blob storage operations.
 * Provides abstraction for different blob storage providers (LocalDisk, OCI Object Storage, etc.).
 */
public interface BlobStorageService {
    
    /**
     * Store a blob with the given content.
     * 
     * @param tenantId The tenant ID
     * @param blobId The blob ID
     * @param content The content to store
     * @param metadata Optional metadata
     * @return The storage URI
     */
    String storeBlob(String tenantId, String blobId, String content, Map<String, Object> metadata);
    
    /**
     * Store a blob from an input stream.
     * 
     * @param tenantId The tenant ID
     * @param blobId The blob ID
     * @param inputStream The input stream
     * @param contentType The content type
     * @param metadata Optional metadata
     * @return The storage URI
     */
    String storeBlob(String tenantId, String blobId, InputStream inputStream, 
                    String contentType, Map<String, Object> metadata);
    
    /**
     * Retrieve a blob's content.
     * 
     * @param storageUri The storage URI
     * @return The blob content
     */
    String retrieveBlob(String storageUri);
    
    /**
     * Retrieve a blob as an input stream.
     * 
     * @param storageUri The storage URI
     * @return The input stream
     */
    InputStream retrieveBlobAsStream(String storageUri);
    
    /**
     * Delete a blob.
     * 
     * @param storageUri The storage URI
     * @return true if deletion was successful
     */
    boolean deleteBlob(String storageUri);
    
    /**
     * Check if a blob exists.
     * 
     * @param storageUri The storage URI
     * @return true if the blob exists
     */
    boolean blobExists(String storageUri);
    
    /**
     * Get blob metadata.
     * 
     * @param storageUri The storage URI
     * @return The blob metadata
     */
    BlobMetadata getBlobMetadata(String storageUri);
    
    /**
     * Generate a pre-signed URL for direct access.
     * 
     * @param storageUri The storage URI
     * @param expirationMinutes Minutes until URL expires
     * @return The pre-signed URL
     */
    String generatePresignedUrl(String storageUri, int expirationMinutes);
    
    /**
     * Check if the blob storage service is healthy and available.
     * 
     * @return true if the service is healthy
     */
    boolean isHealthy();
    
    /**
     * DTO for blob metadata.
     */
    class BlobMetadata {
        private String blobId;
        private String tenantId;
        private String contentType;
        private long size;
        private String etag;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime lastModified;
        private Map<String, Object> metadata;
        
        public BlobMetadata(String blobId, String tenantId, String contentType, long size) {
            this.blobId = blobId;
            this.tenantId = tenantId;
            this.contentType = contentType;
            this.size = size;
            this.createdAt = java.time.LocalDateTime.now();
            this.lastModified = java.time.LocalDateTime.now();
        }
        
        // Getters and Setters
        public String getBlobId() {
            return blobId;
        }
        
        public void setBlobId(String blobId) {
            this.blobId = blobId;
        }
        
        public String getTenantId() {
            return tenantId;
        }
        
        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }
        
        public String getContentType() {
            return contentType;
        }
        
        public void setContentType(String contentType) {
            this.contentType = contentType;
        }
        
        public long getSize() {
            return size;
        }
        
        public void setSize(long size) {
            this.size = size;
        }
        
        public String getEtag() {
            return etag;
        }
        
        public void setEtag(String etag) {
            this.etag = etag;
        }
        
        public java.time.LocalDateTime getCreatedAt() {
            return createdAt;
        }
        
        public void setCreatedAt(java.time.LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
        
        public java.time.LocalDateTime getLastModified() {
            return lastModified;
        }
        
        public void setLastModified(java.time.LocalDateTime lastModified) {
            this.lastModified = lastModified;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }
}
