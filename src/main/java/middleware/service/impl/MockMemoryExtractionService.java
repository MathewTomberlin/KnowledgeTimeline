package middleware.service.impl;

import middleware.service.MemoryExtractionService;
import middleware.service.MemoryExtractionService.*;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of MemoryExtractionService for testing and local development.
 * Provides simulated memory extraction functionality.
 */
@Service
@Primary  // This bean takes precedence when multiple MemoryExtractionService beans are present
@Profile({"test", "integration"})  // Only active for test and integration profiles
public class MockMemoryExtractionService implements MemoryExtractionService {

    private final ConcurrentHashMap<String, MemoryExtraction> extractions = new ConcurrentHashMap<>();

    @Override
    public MemoryExtraction extractMemory(String userMessage, String assistantMessage, Map<String, Object> context) {
        // Mock extraction results
        List<Fact> facts = List.of(
            new Fact("User asked a question about the topic", "conversation", 0.8),
            new Fact("Assistant provided a helpful response", "conversation", 0.9)
        );
        
        List<Entity> entities = List.of(
            new Entity("topic", "general_inquiry", "A general inquiry topic", 0.7),
            new Entity("user_intent", "information_seeking", "User seeking information", 0.8)
        );
        
        List<Task> tasks = List.of(
            new Task("Research additional details on the topic", "pending")
        );
        
        return new MemoryExtraction(
            facts,
            entities,
            tasks,
            0.85,
            Map.of("extraction_method", "mock", "confidence", 0.85)
        );
    }

    @Override
    public List<MemoryExtraction> extractMemoryBatch(List<ConversationTurn> conversations) {
        return conversations.stream()
            .map(turn -> extractMemory(turn.getUserMessage(), turn.getAssistantMessage(), turn.getContext()))
            .toList();
    }

    @Override
    public List<Fact> validateAndDeduplicateFacts(List<Fact> facts) {
        // Mock deduplication - just return the facts as-is
        return facts;
    }

    @Override
    public void storeMemoryExtraction(String tenantId, String sessionId, MemoryExtraction memoryExtraction) {
        String key = tenantId + ":" + sessionId;
        extractions.put(key, memoryExtraction);
    }
}
