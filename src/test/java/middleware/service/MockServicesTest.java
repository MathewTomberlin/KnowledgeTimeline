package middleware.service;

import middleware.service.impl.MockEmbeddingService;
import middleware.service.impl.MockTokenCountingService;
import middleware.service.impl.MockVectorStoreService;
import middleware.service.impl.MockContextBuilderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for mock service implementations.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false"
})
public class MockServicesTest {
    
    @Autowired
    private MockEmbeddingService embeddingService;
    
    @Autowired
    private MockTokenCountingService tokenCountingService;
    
    @Autowired
    private MockVectorStoreService vectorStoreService;
    
    @Autowired
    private MockContextBuilderService contextBuilderService;
    
    @Test
    public void testEmbeddingService() {
        // Test embedding generation
        List<Float> embedding = embeddingService.generateEmbedding("Test text");
        assertNotNull(embedding);
        assertEquals(384, embedding.size());
        
        // Test multiple embeddings
        List<List<Float>> embeddings = embeddingService.generateEmbeddings(
            List.of("Text 1", "Text 2", "Text 3")
        );
        assertEquals(3, embeddings.size());
        assertEquals(384, embeddings.get(0).size());
        
        // Test service health
        assertTrue(embeddingService.isHealthy());
        assertEquals("mock-embedding-model", embeddingService.getModelName());
        assertEquals(384, embeddingService.getEmbeddingDimension());
    }
    
    @Test
    public void testTokenCountingService() {
        // Test token counting
        int tokens = tokenCountingService.countTokens("This is a test message", "gpt-3.5-turbo");
        assertTrue(tokens > 0);
        
        // Test cost estimation
        double cost = tokenCountingService.estimateCost(1000, "gpt-3.5-turbo");
        assertTrue(cost > 0);
        
        // Test max tokens
        assertEquals(4096, tokenCountingService.getMaxTokens("gpt-3.5-turbo"));
        assertEquals(8192, tokenCountingService.getMaxTokens("gpt-4"));
        
        // Test token limit checks
        assertFalse(tokenCountingService.exceedsTokenLimit("Short text", "gpt-3.5-turbo"));
        
        // Test truncation
        String truncated = tokenCountingService.truncateToTokenLimit(
            "Very long text that exceeds the token limit", "gpt-3.5-turbo", 10
        );
        assertTrue(truncated.length() < 50);
    }
    
    @Test
    public void testVectorStoreService() {
        // Test embedding storage
        List<Float> embedding = List.of(0.1f, 0.2f, 0.3f);
        String embeddingId = vectorStoreService.storeEmbedding("obj1", "var1", "Test text", embedding);
        assertNotNull(embeddingId);
        
        // Test similarity search
        List<VectorStoreService.SimilarityMatch> matches = vectorStoreService.findSimilar(
            "test query", 5, null, true, 0.3
        );
        assertNotNull(matches);
        
        // Test service health
        assertTrue(vectorStoreService.isHealthy());
        
        // Test statistics
        Map<String, Object> stats = vectorStoreService.getStatistics();
        assertNotNull(stats);
        assertTrue(stats.containsKey("total_embeddings"));
        
        // Test embedding deletion
        vectorStoreService.deleteEmbedding(embeddingId);
    }
    
    @Test
    public void testContextBuilderService() {
        // Test context building
        String context = contextBuilderService.buildContext(
            "tenant1", "session1", "What is the weather?", null
        );
        assertNotNull(context);
        assertTrue(context.length() > 0);
        
        // Test knowledge retrieval
        List<ContextBuilderService.KnowledgeObject> knowledge = contextBuilderService.getRelevantKnowledge(
            "tenant1", "test query", 5, null
        );
        assertNotNull(knowledge);
        
        // Test token budget
        int budget = contextBuilderService.getTokenBudget("tenant1");
        assertEquals(2000, budget);
        
        // Test context packing
        List<ContextBuilderService.KnowledgeObject> mockKnowledge = List.of(
            new ContextBuilderService.KnowledgeObject("obj1", "SUMMARY", "Title", "Content", "SHORT", 0.8, null)
        );
        String packedContext = contextBuilderService.packKnowledgeIntoContext(
            mockKnowledge, 1000, 0.3
        );
        assertNotNull(packedContext);
        assertTrue(packedContext.contains("Content"));
    }
}
