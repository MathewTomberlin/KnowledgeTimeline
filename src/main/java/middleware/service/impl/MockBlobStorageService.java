package middleware.service.impl;

import middleware.service.BlobStorageService;
import middleware.service.BlobStorageService.BlobMetadata;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of BlobStorageService for testing and local development.
 * Provides simulated blob storage functionality using in-memory storage.
 */
@Service
public class MockBlobStorageService implements BlobStorageService {

    private final ConcurrentHashMap<String, String> blobStorage = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BlobMetadata> metadataStorage = new ConcurrentHashMap<>();

    @Override
    public String storeBlob(String tenantId, String blobId, String content, Map<String, Object> metadata) {
        String storageUri = "mock://" + tenantId + "/" + blobId;
        blobStorage.put(storageUri, content);
        
        BlobMetadata blobMetadata = new BlobMetadata(blobId, tenantId, "text/plain", content.getBytes().length);
        blobMetadata.setMetadata(metadata);
        metadataStorage.put(storageUri, blobMetadata);
        
        return storageUri;
    }

    @Override
    public String storeBlob(String tenantId, String blobId, InputStream inputStream, String contentType, Map<String, Object> metadata) {
        try {
            String content = new String(inputStream.readAllBytes());
            String storageUri = "mock://" + tenantId + "/" + blobId;
            blobStorage.put(storageUri, content);
            
            BlobMetadata blobMetadata = new BlobMetadata(blobId, tenantId, contentType, content.getBytes().length);
            blobMetadata.setMetadata(metadata);
            metadataStorage.put(storageUri, blobMetadata);
            
            return storageUri;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read input stream", e);
        }
    }

    @Override
    public String retrieveBlob(String storageUri) {
        String content = blobStorage.get(storageUri);
        if (content == null) {
            throw new RuntimeException("Blob not found: " + storageUri);
        }
        return content;
    }

    @Override
    public InputStream retrieveBlobAsStream(String storageUri) {
        String content = retrieveBlob(storageUri);
        return new ByteArrayInputStream(content.getBytes());
    }

    @Override
    public boolean deleteBlob(String storageUri) {
        String removed = blobStorage.remove(storageUri);
        metadataStorage.remove(storageUri);
        return removed != null;
    }

    @Override
    public boolean blobExists(String storageUri) {
        return blobStorage.containsKey(storageUri);
    }

    @Override
    public BlobMetadata getBlobMetadata(String storageUri) {
        BlobMetadata metadata = metadataStorage.get(storageUri);
        if (metadata == null) {
            throw new RuntimeException("Blob metadata not found: " + storageUri);
        }
        return metadata;
    }

    @Override
    public String generatePresignedUrl(String storageUri, int expirationMinutes) {
        // Mock presigned URL
        return storageUri + "?expires=" + expirationMinutes;
    }

    @Override
    public boolean isHealthy() {
        return true; // Mock service is always healthy
    }
}
