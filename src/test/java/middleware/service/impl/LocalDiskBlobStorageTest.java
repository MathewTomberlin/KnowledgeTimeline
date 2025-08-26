package middleware.service.impl;

import middleware.service.BlobStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LocalDiskBlobStorageTest {

    @TempDir
    Path tempDir;
    
    private LocalDiskBlobStorage blobStorage;
    private String testBasePath;
    
    @BeforeEach
    void setUp() throws IOException {
        blobStorage = new LocalDiskBlobStorage();
        testBasePath = tempDir.resolve("blobs").toString();
        ReflectionTestUtils.setField(blobStorage, "basePath", testBasePath);
    }
    
    @Test
    void testStoreBlob_WithStringContent() {
        // Given
        String tenantId = "tenant1";
        String blobId = "test-blob-1";
        String content = "This is test content";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("test-key", "test-value");
        
        // When
        String storageUri = blobStorage.storeBlob(tenantId, blobId, content, metadata);
        
        // Then
        assertNotNull(storageUri);
        assertTrue(storageUri.contains(tenantId));
        assertTrue(storageUri.contains(blobId));
        
        // Verify content was stored
        String retrievedContent = blobStorage.retrieveBlob(storageUri);
        assertEquals(content, retrievedContent);
        
        // Verify metadata was stored
        BlobStorageService.BlobMetadata blobMetadata = blobStorage.getBlobMetadata(storageUri);
        assertNotNull(blobMetadata);
        assertEquals(blobId, blobMetadata.getBlobId());
        assertEquals(tenantId, blobMetadata.getTenantId());
        assertEquals("text/plain", blobMetadata.getContentType());
        assertEquals(content.length(), blobMetadata.getSize());
    }
    
    @Test
    void testStoreBlob_WithNullContent() {
        // Given
        String tenantId = "tenant2";
        String blobId = "test-blob-2";
        String content = null;
        Map<String, Object> metadata = new HashMap<>();
        
        // When
        String storageUri = blobStorage.storeBlob(tenantId, blobId, content, metadata);
        
        // Then
        assertNotNull(storageUri);
        String retrievedContent = blobStorage.retrieveBlob(storageUri);
        assertEquals("", retrievedContent);
    }
    
    @Test
    void testStoreBlob_WithInputStream() {
        // Given
        String tenantId = "tenant3";
        String blobId = "test-blob-3";
        String content = "Stream content test";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "stream");
        
        // When
        String storageUri = blobStorage.storeBlob(tenantId, blobId, inputStream, "text/plain", metadata);
        
        // Then
        assertNotNull(storageUri);
        String retrievedContent = blobStorage.retrieveBlob(storageUri);
        assertEquals(content, retrievedContent);
        
        // Verify metadata
        BlobStorageService.BlobMetadata blobMetadata = blobStorage.getBlobMetadata(storageUri);
        assertNotNull(blobMetadata);
        assertEquals("text/plain", blobMetadata.getContentType());
        assertEquals(content.length(), blobMetadata.getSize());
    }
    
    @Test
    void testStoreBlob_WithBinaryContent() {
        // Given
        String tenantId = "tenant4";
        String blobId = "test-blob-4";
        byte[] binaryContent = {0x01, 0x02, 0x03, 0x04, 0x05};
        InputStream inputStream = new ByteArrayInputStream(binaryContent);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("binary", true);
        
        // When
        String storageUri = blobStorage.storeBlob(tenantId, blobId, inputStream, "application/octet-stream", metadata);
        
        // Then
        assertNotNull(storageUri);
        InputStream retrievedStream = blobStorage.retrieveBlobAsStream(storageUri);
        assertNotNull(retrievedStream);
        
        try {
            byte[] retrievedBytes = retrievedStream.readAllBytes();
            assertArrayEquals(binaryContent, retrievedBytes);
        } catch (IOException e) {
            fail("Failed to read retrieved stream: " + e.getMessage());
        }
    }
    
    @Test
    void testRetrieveBlob_WhenBlobExists() {
        // Given
        String tenantId = "tenant5";
        String blobId = "test-blob-5";
        String content = "Content to retrieve";
        String storageUri = blobStorage.storeBlob(tenantId, blobId, content, new HashMap<>());
        
        // When
        String retrievedContent = blobStorage.retrieveBlob(storageUri);
        
        // Then
        assertEquals(content, retrievedContent);
    }
    
    @Test
    void testRetrieveBlob_WhenBlobDoesNotExist() {
        // Given
        String nonExistentUri = tempDir.resolve("non-existent-blob").toUri().toString();
        
        // When
        String retrievedContent = blobStorage.retrieveBlob(nonExistentUri);
        
        // Then
        assertNull(retrievedContent);
    }
    
    @Test
    void testRetrieveBlobAsStream_WhenBlobExists() {
        // Given
        String tenantId = "tenant6";
        String blobId = "test-blob-6";
        String content = "Stream content";
        String storageUri = blobStorage.storeBlob(tenantId, blobId, content, new HashMap<>());
        
        // When
        InputStream stream = blobStorage.retrieveBlobAsStream(storageUri);
        
        // Then
        assertNotNull(stream);
        try {
            String retrievedContent = new String(stream.readAllBytes());
            assertEquals(content, retrievedContent);
        } catch (IOException e) {
            fail("Failed to read stream: " + e.getMessage());
        }
    }
    
    @Test
    void testRetrieveBlobAsStream_WhenBlobDoesNotExist() {
        // Given
        String nonExistentUri = tempDir.resolve("non-existent-blob").toUri().toString();
        
        // When
        InputStream stream = blobStorage.retrieveBlobAsStream(nonExistentUri);
        
        // Then
        assertNull(stream);
    }
    
    @Test
    void testDeleteBlob_WhenBlobExists() {
        // Given
        String tenantId = "tenant7";
        String blobId = "test-blob-7";
        String content = "Content to delete";
        String storageUri = blobStorage.storeBlob(tenantId, blobId, content, new HashMap<>());
        
        // Verify blob exists
        assertTrue(blobStorage.blobExists(storageUri));
        
        // When
        boolean deleted = blobStorage.deleteBlob(storageUri);
        
        // Then
        assertTrue(deleted);
        assertFalse(blobStorage.blobExists(storageUri));
    }
    
    @Test
    void testDeleteBlob_WhenBlobDoesNotExist() {
        // Given
        String nonExistentUri = tempDir.resolve("non-existent-blob").toUri().toString();
        
        // When
        boolean deleted = blobStorage.deleteBlob(nonExistentUri);
        
        // Then
        assertFalse(deleted);
    }
    
    @Test
    void testDeleteBlob_WithNullUri() {
        // When
        boolean deleted = blobStorage.deleteBlob(null);
        
        // Then
        assertFalse(deleted);
    }
    
    @Test
    void testBlobExists_WhenBlobExists() {
        // Given
        String tenantId = "tenant8";
        String blobId = "test-blob-8";
        String content = "Content for existence check";
        String storageUri = blobStorage.storeBlob(tenantId, blobId, content, new HashMap<>());
        
        // When
        boolean exists = blobStorage.blobExists(storageUri);
        
        // Then
        assertTrue(exists);
    }
    
    @Test
    void testBlobExists_WhenBlobDoesNotExist() {
        // Given
        String nonExistentUri = tempDir.resolve("non-existent-blob").toUri().toString();
        
        // When
        boolean exists = blobStorage.blobExists(nonExistentUri);
        
        // Then
        assertFalse(exists);
    }
    
    @Test
    void testBlobExists_WithNullUri() {
        // When
        boolean exists = blobStorage.blobExists(null);
        
        // Then
        assertFalse(exists);
    }
    
    @Test
    void testGetBlobMetadata_WhenBlobExists() {
        // Given
        String tenantId = "tenant9";
        String blobId = "test-blob-9";
        String content = "Content for metadata";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("custom-key", "custom-value");
        metadata.put("version", "1.0");
        
        String storageUri = blobStorage.storeBlob(tenantId, blobId, content, metadata);
        
        // When
        BlobStorageService.BlobMetadata blobMetadata = blobStorage.getBlobMetadata(storageUri);
        
        // Then
        assertNotNull(blobMetadata);
        assertEquals(blobId, blobMetadata.getBlobId());
        assertEquals(tenantId, blobMetadata.getTenantId());
        assertEquals("text/plain", blobMetadata.getContentType());
        assertEquals(content.length(), blobMetadata.getSize());
        assertNotNull(blobMetadata.getMetadata());
        assertEquals("custom-value", blobMetadata.getMetadata().get("custom-key"));
        assertEquals("1.0", blobMetadata.getMetadata().get("version"));
    }
    
    @Test
    void testGetBlobMetadata_WhenBlobDoesNotExist() {
        // Given
        String nonExistentUri = tempDir.resolve("non-existent-blob").toUri().toString();
        
        // When
        BlobStorageService.BlobMetadata blobMetadata = blobStorage.getBlobMetadata(nonExistentUri);
        
        // Then
        assertNull(blobMetadata);
    }
    
    @Test
    void testGetBlobMetadata_WithNullUri() {
        // When
        BlobStorageService.BlobMetadata blobMetadata = blobStorage.getBlobMetadata(null);
        
        // Then
        assertNull(blobMetadata);
    }
    
    @Test
    void testGeneratePresignedUrl() {
        // Given
        String storageUri = "file:///test/path";
        int expirationMinutes = 60;
        
        // When
        String presignedUrl = blobStorage.generatePresignedUrl(storageUri, expirationMinutes);
        
        // Then
        assertEquals(storageUri, presignedUrl);
    }
    
    @Test
    void testIsHealthy_WhenStorageIsAccessible() {
        // When
        boolean healthy = blobStorage.isHealthy();
        
        // Then
        assertTrue(healthy);
    }
    
    @Test
    void testIsHealthy_WhenStorageIsNotAccessible() throws IOException {
        // Given - Set a path that cannot be created on Windows
        ReflectionTestUtils.setField(blobStorage, "basePath", "Z:\\non\\existent\\path\\that\\cannot\\be\\created");
        
        // When
        boolean healthy = blobStorage.isHealthy();
        
        // Then
        assertFalse(healthy);
    }
    
    @Test
    void testStoreBlob_CreatesDirectoryStructure() {
        // Given
        String tenantId = "tenant10";
        String blobId = "nested/path/blob";
        String content = "Nested content";
        
        // When
        String storageUri = blobStorage.storeBlob(tenantId, blobId, content, new HashMap<>());
        
        // Then
        assertNotNull(storageUri);
        
        // Verify the nested directory structure was created
        Path expectedPath = Path.of(testBasePath, tenantId, "nested", "path", "blob");
        assertTrue(Files.exists(expectedPath));
        
        // Verify content
        String retrievedContent = blobStorage.retrieveBlob(storageUri);
        assertEquals(content, retrievedContent);
    }
    
    @Test
    void testStoreBlob_WithSafeCharacters() {
        // Given
        String tenantId = "tenant-11";
        String blobId = "safe-chars-12345";
        String content = "Safe content with chars: 12345";
        
        // When
        String storageUri = blobStorage.storeBlob(tenantId, blobId, content, new HashMap<>());
        
        // Then
        assertNotNull(storageUri);
        String retrievedContent = blobStorage.retrieveBlob(storageUri);
        assertEquals(content, retrievedContent);
    }
    
    @Test
    void testStoreBlob_WithLargeContent() {
        // Given
        String tenantId = "tenant12";
        String blobId = "large-blob";
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeContent.append("Line ").append(i).append(": This is a large content test.\n");
        }
        String content = largeContent.toString();
        
        // When
        String storageUri = blobStorage.storeBlob(tenantId, blobId, content, new HashMap<>());
        
        // Then
        assertNotNull(storageUri);
        String retrievedContent = blobStorage.retrieveBlob(storageUri);
        assertEquals(content, retrievedContent);
        
        // Verify metadata size
        BlobStorageService.BlobMetadata blobMetadata = blobStorage.getBlobMetadata(storageUri);
        assertEquals(content.length(), blobMetadata.getSize());
    }
    
    @Test
    void testCleanupEmptyDirectories_AfterDeletion() {
        // Given
        String tenantId = "tenant13";
        String blobId = "nested/path/blob";
        String content = "Content for cleanup test";
        
        String storageUri = blobStorage.storeBlob(tenantId, blobId, content, new HashMap<>());
        
        // Verify nested structure exists
        Path nestedPath = Path.of(testBasePath, tenantId, "nested", "path");
        assertTrue(Files.exists(nestedPath));
        
        // When
        boolean deleted = blobStorage.deleteBlob(storageUri);
        
        // Then
        assertTrue(deleted);
        
        // Verify nested directories were cleaned up
        assertFalse(Files.exists(nestedPath));
        assertFalse(Files.exists(Path.of(testBasePath, tenantId, "nested")));
        assertFalse(Files.exists(Path.of(testBasePath, tenantId)));
    }
}
