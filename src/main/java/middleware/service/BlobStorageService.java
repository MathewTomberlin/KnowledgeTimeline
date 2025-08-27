package middleware.service;

import java.io.InputStream;
import java.util.Map;

/**
 * Service for blob storage operations including file upload, download, and metadata management.
 * Supports both local disk storage and cloud storage (OCI Object Storage, GCS).
 */
public interface BlobStorageService {
    
    /**
     * Store a blob with string content.
     *
     * @param tenantId The tenant ID for isolation
     * @param blobId The unique blob identifier
     * @param content The content to store
     * @param metadata Optional metadata for the blob
     * @return The storage URI for the stored blob
     */
    String storeBlob(String tenantId, String blobId, String content, Map<String, Object> metadata);
    
    /**
     * Store a blob with input stream content.
     *
     * @param tenantId The tenant ID for isolation
     * @param blobId The unique blob identifier
     * @param inputStream The input stream containing the content
     * @param contentType The MIME type of the content
     * @param metadata Optional metadata for the blob
     * @return The storage URI for the stored blob
     */
    String storeBlob(String tenantId, String blobId, InputStream inputStream, 
                     String contentType, Map<String, Object> metadata);
    
    /**
     * Retrieve a blob as a string.
     *
     * @param storageUri The storage URI of the blob
     * @return The blob content as a string, or null if not found
     */
    String retrieveBlob(String storageUri);
    
    /**
     * Retrieve a blob as an input stream.
     *
     * @param storageUri The storage URI of the blob
     * @return The blob content as an input stream, or null if not found
     */
    InputStream retrieveBlobAsStream(String storageUri);
    
    /**
     * Delete a blob.
     *
     * @param storageUri The storage URI of the blob to delete
     * @return True if deletion was successful
     */
    boolean deleteBlob(String storageUri);
    
    /**
     * Check if a blob exists.
     *
     * @param storageUri The storage URI to check
     * @return True if the blob exists
     */
    boolean blobExists(String storageUri);
    
    /**
     * Get metadata for a blob.
     *
     * @param storageUri The storage URI of the blob
     * @return The blob metadata, or null if not found
     */
    BlobMetadata getBlobMetadata(String storageUri);
    
    /**
     * Generate a pre-signed URL for temporary access to a blob.
     *
     * @param storageUri The storage URI of the blob
     * @param expirationMinutes The expiration time in minutes
     * @return A pre-signed URL for temporary access
     */
    String generatePresignedUrl(String storageUri, int expirationMinutes);
    
    /**
     * Check if the blob storage service is healthy.
     *
     * @return True if the service is healthy
     */
    boolean isHealthy();
    
    /**
     * Represents metadata for a stored blob.
     */
    class BlobMetadata {
        private String blobId;
        private String tenantId;
        private String contentType;
        private long size;
        private Map<String, Object> metadata;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime lastModified;
        
        public BlobMetadata(String blobId, String tenantId, String contentType, long size) {
            this.blobId = blobId;
            this.tenantId = tenantId;
            this.contentType = contentType;
            this.size = size;
        }
        
        // Getters and Setters
        public String getBlobId() { return blobId; }
        public void setBlobId(String blobId) { this.blobId = blobId; }
        
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
        
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
        
        public java.time.LocalDateTime getLastModified() { return lastModified; }
        public void setLastModified(java.time.LocalDateTime lastModified) { this.lastModified = lastModified; }
        
        @Override
        public String toString() {
            return String.format("BlobMetadata{blobId='%s', tenantId='%s', contentType='%s', size=%d}", 
                               blobId, tenantId, contentType, size);
        }
    }
}
