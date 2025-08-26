package middleware.service.impl;

import middleware.service.BlobStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Local disk implementation of BlobStorageService.
 * Stores files in the local filesystem under a configurable base directory.
 */
@Service
public class LocalDiskBlobStorage implements BlobStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(LocalDiskBlobStorage.class);
    
    @Value("${blob.storage.local.base-path:./data/blobs}")
    private String basePath;
    
    @Override
    public String storeBlob(String tenantId, String blobId, String content, Map<String, Object> metadata) {
        try {
            Path blobPath = buildBlobPath(tenantId, blobId);
            Path parentDir = blobPath.getParent();
            
            // Create parent directories if they don't exist
            if (!Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            // Handle null content
            String contentToStore = content != null ? content : "";
            
            // Write the content to file
            Files.write(blobPath, contentToStore.getBytes());
            
            // Store metadata about the blob
            storeMetadata(blobPath, "text/plain", contentToStore.length(), metadata);
            
            String storageUri = blobPath.toUri().toString();
            logger.debug("Stored blob for tenant {} blob {} at {}", 
                tenantId, blobId, storageUri);
            
            return storageUri;
            
        } catch (IOException e) {
            logger.error("Failed to store blob for tenant {} blob {}", 
                tenantId, blobId, e);
            throw new RuntimeException("Failed to store blob", e);
        }
    }
    
    @Override
    public String storeBlob(String tenantId, String blobId, InputStream inputStream, 
                           String contentType, Map<String, Object> metadata) {
        try {
            Path blobPath = buildBlobPath(tenantId, blobId);
            Path parentDir = blobPath.getParent();
            
            // Create parent directories if they don't exist
            if (!Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            // Write the input stream to file
            Files.copy(inputStream, blobPath);
            
            // Get the file size
            long contentLength = Files.size(blobPath);
            
            // Store metadata about the blob
            storeMetadata(blobPath, contentType, contentLength, metadata);
            
            String storageUri = blobPath.toUri().toString();
            logger.debug("Stored blob from stream for tenant {} blob {} at {}", 
                tenantId, blobId, storageUri);
            
            return storageUri;
            
        } catch (IOException e) {
            logger.error("Failed to store blob from stream for tenant {} blob {}", 
                tenantId, blobId, e);
            throw new RuntimeException("Failed to store blob", e);
        }
    }
    
    @Override
    public String retrieveBlob(String storageUri) {
        try {
            Path blobPath = uriToPath(storageUri);
            
            if (!Files.exists(blobPath)) {
                logger.warn("Blob not found at path: {}", blobPath);
                return null;
            }
            
            String content = Files.readString(blobPath);
            logger.debug("Retrieved blob from path: {}, size: {} characters", blobPath, content.length());
            
            return content;
            
        } catch (IOException e) {
            logger.error("Failed to retrieve blob from URI: {}", storageUri, e);
            return null;
        }
    }
    
    @Override
    public InputStream retrieveBlobAsStream(String storageUri) {
        try {
            Path blobPath = uriToPath(storageUri);
            
            if (!Files.exists(blobPath)) {
                logger.warn("Blob not found at path: {}", blobPath);
                return null;
            }
            
            InputStream stream = Files.newInputStream(blobPath);
            logger.debug("Retrieved blob stream from path: {}", blobPath);
            
            return stream;
            
        } catch (IOException e) {
            logger.error("Failed to retrieve blob stream from URI: {}", storageUri, e);
            return null;
        }
    }
    
    @Override
    public boolean deleteBlob(String storageUri) {
        try {
            if (storageUri == null) {
                return false;
            }
            
            Path blobPath = uriToPath(storageUri);
            
            if (!Files.exists(blobPath)) {
                logger.warn("Blob not found for deletion at path: {}", blobPath);
                return false;
            }
            
            // Delete the blob file
            boolean deleted = Files.deleteIfExists(blobPath);
            
            // Try to delete the metadata file if it exists
            Path metadataPath = blobPath.resolveSibling(blobPath.getFileName() + ".meta");
            Files.deleteIfExists(metadataPath);
            
            // Try to clean up empty directories
            cleanupEmptyDirectories(blobPath.getParent());
            
            logger.debug("Deleted blob at path: {}, success: {}", blobPath, deleted);
            return deleted;
            
        } catch (IOException e) {
            logger.error("Failed to delete blob at URI: {}", storageUri, e);
            return false;
        }
    }
    
    @Override
    public boolean blobExists(String storageUri) {
        try {
            if (storageUri == null) {
                return false;
            }
            
            Path blobPath = uriToPath(storageUri);
            return Files.exists(blobPath);
        } catch (Exception e) {
            logger.error("Failed to check blob existence at URI: {}", storageUri, e);
            return false;
        }
    }
    
    @Override
    public BlobMetadata getBlobMetadata(String storageUri) {
        try {
            if (storageUri == null) {
                return null;
            }
            
            Path blobPath = uriToPath(storageUri);
            
            if (!Files.exists(blobPath)) {
                logger.warn("Blob not found for metadata at path: {}", blobPath);
                return null;
            }
            
            // Read metadata from file
            Path metadataPath = blobPath.resolveSibling(blobPath.getFileName() + ".meta");
            if (!Files.exists(metadataPath)) {
                // Return basic metadata if no metadata file exists
                return new BlobMetadata(
                    blobPath.getFileName().toString(),
                    extractTenantIdFromPath(blobPath),
                    "application/octet-stream",
                    Files.size(blobPath)
                );
            }
            
            // Parse metadata file
            Map<String, Object> metadata = parseMetadataFile(metadataPath);
            String contentType = (String) metadata.getOrDefault("content-type", "application/octet-stream");
            long size = Files.size(blobPath);
            
            BlobMetadata blobMetadata = new BlobMetadata(
                blobPath.getFileName().toString(),
                extractTenantIdFromPath(blobPath),
                contentType,
                size
            );
            
            // Set additional metadata
            blobMetadata.setMetadata(metadata);
            blobMetadata.setCreatedAt(LocalDateTime.now()); // We don't store this in local files
            blobMetadata.setLastModified(LocalDateTime.now());
            
            return blobMetadata;
            
        } catch (IOException e) {
            logger.error("Failed to get blob metadata for URI: {}", storageUri, e);
            return null;
        }
    }
    
    @Override
    public String generatePresignedUrl(String storageUri, int expirationMinutes) {
        // Local disk doesn't support pre-signed URLs
        // Return the file URI for direct access
        logger.debug("Pre-signed URLs not supported for local disk storage, returning direct URI: {}", storageUri);
        return storageUri;
    }
    
    @Override
    public boolean isHealthy() {
        try {
            Path baseDir = Paths.get(basePath);
            if (!Files.exists(baseDir)) {
                Files.createDirectories(baseDir);
            }
            
            // Try to create a test file
            Path testFile = baseDir.resolve(".health-check");
            Files.write(testFile, "health".getBytes());
            Files.delete(testFile);
            
            return true;
        } catch (IOException e) {
            logger.error("Health check failed for local disk storage", e);
            return false;
        } catch (SecurityException e) {
            logger.error("Health check failed due to security restrictions", e);
            return false;
        } catch (Exception e) {
            logger.error("Health check failed with unexpected error", e);
            return false;
        }
    }
    
    private Path buildBlobPath(String tenantId, String blobId) {
        return Paths.get(basePath, tenantId, blobId);
    }
    
    private Path uriToPath(String storageUri) {
        if (storageUri == null) {
            throw new IllegalArgumentException("Storage URI cannot be null");
        }
        
        try {
            // Try to parse as URI first
            URI uri = new URI(storageUri);
            if ("file".equals(uri.getScheme())) {
                return Paths.get(uri);
            }
        } catch (Exception e) {
            // If URI parsing fails, try to treat it as a direct path
            logger.debug("Failed to parse URI: {}, treating as direct path", storageUri, e);
        }
        
        // Fallback: treat as direct path
        // Handle Windows paths that might start with drive letter
        if (storageUri.startsWith("/") && storageUri.length() > 2 && storageUri.charAt(2) == ':') {
            // This is a Windows path like "/C:/path" - convert to "C:/path"
            return Paths.get(storageUri.substring(1));
        }
        return Paths.get(storageUri);
    }
    
    private void storeMetadata(Path blobPath, String contentType, long contentLength, Map<String, Object> metadata) throws IOException {
        Path metadataPath = blobPath.resolveSibling(blobPath.getFileName() + ".meta");
        
        StringBuilder metadataContent = new StringBuilder();
        metadataContent.append("content-type: ").append(contentType).append("\n");
        metadataContent.append("content-length: ").append(contentLength).append("\n");
        metadataContent.append("stored-at: ").append(System.currentTimeMillis()).append("\n");
        
        if (metadata != null) {
            for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                metadataContent.append("meta-").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        
        Files.write(metadataPath, metadataContent.toString().getBytes());
    }
    
    private Map<String, Object> parseMetadataFile(Path metadataPath) throws IOException {
        Map<String, Object> metadata = new HashMap<>();
        String content = Files.readString(metadataPath);
        
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.contains(":")) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    
                    if (key.startsWith("meta-")) {
                        // Custom metadata
                        metadata.put(key.substring(5), value);
                    } else {
                        // Standard metadata
                        metadata.put(key, value);
                    }
                }
            }
        }
        
        return metadata;
    }
    
    private String extractTenantIdFromPath(Path blobPath) {
        try {
            // Extract tenant ID from path: basePath/tenantId/blobId
            Path relativePath = Paths.get(basePath).relativize(blobPath);
            if (relativePath.getNameCount() >= 2) {
                return relativePath.getName(0).toString();
            }
        } catch (Exception e) {
            logger.debug("Failed to extract tenant ID from path: {}", blobPath, e);
        }
        return "unknown";
    }
    
    private void cleanupEmptyDirectories(Path directory) {
        try {
            if (directory != null && Files.exists(directory)) {
                // Check if directory is empty
                try (var stream = Files.list(directory)) {
                    if (stream.findFirst().isEmpty()) {
                        Files.delete(directory);
                        // Recursively clean up parent directories
                        cleanupEmptyDirectories(directory.getParent());
                    }
                }
            }
        } catch (IOException e) {
            logger.debug("Failed to clean up empty directory: {}", directory, e);
        }
    }
}
