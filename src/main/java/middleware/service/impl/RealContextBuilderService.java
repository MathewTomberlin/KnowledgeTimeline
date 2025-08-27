package middleware.service.impl;

import middleware.model.ContentVariant;
import middleware.model.KnowledgeObject;
import middleware.repository.ContentVariantRepository;
import middleware.repository.KnowledgeObjectRepository;
import middleware.service.ContextBuilderService;
import middleware.service.VectorStoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Real implementation of ContextBuilderService with MMR algorithm for diversity.
 * Provides intelligent context building with token budget management and relevance scoring.
 */
@Service
@Profile({"local", "docker"})  // Only active for production profiles
public class RealContextBuilderService implements ContextBuilderService {
    
    private static final Logger logger = LoggerFactory.getLogger(RealContextBuilderService.class);
    
    @Autowired
    private VectorStoreService vectorStoreService;
    
    @Autowired
    private KnowledgeObjectRepository knowledgeObjectRepository;
    
    @Autowired
    private ContentVariantRepository contentVariantRepository;
    
    private static final int DEFAULT_TOKEN_BUDGET = 2000;
    private static final double DEFAULT_DIVERSITY = 0.3;
    private static final double DEFAULT_RELEVANCE_WEIGHT = 0.7;
    
    @Override
    public String buildContext(String tenantId, String sessionId, String userPrompt, 
                             Map<String, Object> knowledgeContext) {
        logger.info("Building context for tenant: {}, session: {}", tenantId, sessionId);
        
        try {
            // Get relevant knowledge using vector similarity search
            List<KnowledgeObject> relevantKnowledge = getRelevantKnowledge(tenantId, userPrompt, 20, null);
            
            if (relevantKnowledge.isEmpty()) {
                logger.info("No relevant knowledge found for prompt: {}", userPrompt);
                return "No relevant knowledge found.";
            }
            
            // Get token budget for this tenant
            int tokenBudget = getTokenBudget(tenantId);
            
            // Pack knowledge into context with diversity
            double diversity = DEFAULT_DIVERSITY;
            if (knowledgeContext != null && knowledgeContext.containsKey("diversity")) {
                diversity = (Double) knowledgeContext.get("diversity");
            }
            
            return packKnowledgeIntoContext(relevantKnowledge, tokenBudget, diversity);
            
        } catch (Exception e) {
            logger.error("Error building context for tenant: {}, session: {}", tenantId, sessionId, e);
            return "Error retrieving knowledge context.";
        }
    }
    
    @Override
    public List<ContextBuilderService.KnowledgeObject> getRelevantKnowledge(String tenantId, String query, 
                                                    int maxResults, Map<String, Object> filters) {
        logger.debug("Finding relevant knowledge for query: {}", query);
        
        try {
            // Use vector store to find similar content with MMR
            List<VectorStoreService.SimilarityMatch> matches = vectorStoreService.findSimilar(
                query, maxResults, filters, true, DEFAULT_DIVERSITY
            );
            
            if (matches.isEmpty()) {
                return new ArrayList<>();
            }
            
            // Convert similarity matches to KnowledgeObject format
            List<ContextBuilderService.KnowledgeObject> knowledgeObjects = new ArrayList<>();
            Set<String> processedObjectIds = new HashSet<>();
            
            for (VectorStoreService.SimilarityMatch match : matches) {
                if (processedObjectIds.contains(match.getObjectId())) {
                    continue; // Skip if we already have this object
                }
                
                // Fetch the actual KnowledgeObject from database
                Optional<middleware.model.KnowledgeObject> knowledgeObjectOpt = knowledgeObjectRepository.findById(match.getObjectId());
                if (knowledgeObjectOpt.isPresent()) {
                    middleware.model.KnowledgeObject knowledgeObject = knowledgeObjectOpt.get();
                    
                    // Verify tenant ownership
                    if (!tenantId.equals(knowledgeObject.getTenantId())) {
                        continue; // Skip objects from other tenants
                    }
                    
                    // Get content variant for this object
                    List<ContentVariant> variants = contentVariantRepository.findByKnowledgeObjectId(knowledgeObject.getId());
                    ContentVariant bestVariant = selectBestVariant(variants);
                    
                    // Create DTO KnowledgeObject
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("similarity_score", match.getSimilarityScore());
                    metadata.put("variant_id", match.getVariantId());
                    metadata.put("tenant_id", knowledgeObject.getTenantId());
                    metadata.put("type", knowledgeObject.getType() != null ? knowledgeObject.getType().toString() : "UNKNOWN");
                    
                    ContextBuilderService.KnowledgeObject dtoObject = new ContextBuilderService.KnowledgeObject(
                        knowledgeObject.getId(),
                        knowledgeObject.getType() != null ? knowledgeObject.getType().toString() : "UNKNOWN",
                        "Knowledge Object", // Default title
                        bestVariant != null ? bestVariant.getContent() : "",
                        bestVariant != null ? bestVariant.getVariant().toString() : "UNKNOWN",
                        match.getSimilarityScore(),
                        metadata
                    );
                    
                    knowledgeObjects.add(dtoObject);
                    processedObjectIds.add(match.getObjectId());
                }
            }
            
            logger.debug("Found {} relevant knowledge objects", knowledgeObjects.size());
            return knowledgeObjects;
            
        } catch (Exception e) {
            logger.error("Error finding relevant knowledge for query: {}", query, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public String packKnowledgeIntoContext(List<ContextBuilderService.KnowledgeObject> knowledgeObjects, 
                                        int tokenBudget, double diversity) {
        if (knowledgeObjects.isEmpty()) {
            return "No relevant knowledge found.";
        }
        
        logger.debug("Packing {} knowledge objects into context with {} token budget", 
                    knowledgeObjects.size(), tokenBudget);
        
        try {
            // Apply MMR algorithm for diversity
            List<ContextBuilderService.KnowledgeObject> diverseObjects = applyMMR(knowledgeObjects, diversity);
            
            // Pack into context within token budget
            StringBuilder context = new StringBuilder();
            context.append("Relevant knowledge:\n\n");
            
            int currentTokens = 0;
            int maxTokens = tokenBudget - 100; // Reserve tokens for formatting
            int objectCount = 0;
            
            for (ContextBuilderService.KnowledgeObject knowledgeObject : diverseObjects) {
                String content = knowledgeObject.getContent();
                
                if (content == null || content.trim().isEmpty()) {
                    continue;
                }
                
                int estimatedTokens = estimateTokens(content);
                
                if (currentTokens + estimatedTokens > maxTokens) {
                    break;
                }
                
                // Format the knowledge entry
                context.append("• ").append(content.trim());
                context.append(" [src:").append(knowledgeObject.getId());
                
                // Add type information if available
                if (knowledgeObject.getType() != null) {
                    context.append(", type:").append(knowledgeObject.getType());
                }
                
                context.append("]\n\n");
                
                currentTokens += estimatedTokens;
                objectCount++;
            }
            
            if (objectCount == 0) {
                return "No relevant knowledge found.";
            }
            
            logger.debug("Packed {} knowledge objects into context using {} tokens", 
                        objectCount, currentTokens);
            
            return context.toString();
            
        } catch (Exception e) {
            logger.error("Error packing knowledge into context", e);
            return "Error building knowledge context.";
        }
    }
    
    @Override
    public int getTokenBudget(String tenantId) {
        // In a real implementation, this would be configurable per tenant
        // For now, return default budget
        return DEFAULT_TOKEN_BUDGET;
    }
    
    /**
     * Apply Maximal Marginal Relevance algorithm for diversity.
     */
    private List<ContextBuilderService.KnowledgeObject> applyMMR(List<ContextBuilderService.KnowledgeObject> knowledgeObjects, 
                                         double diversity) {
        if (knowledgeObjects.size() <= 1) {
            return knowledgeObjects;
        }
        
        List<ContextBuilderService.KnowledgeObject> selected = new ArrayList<>();
        List<ContextBuilderService.KnowledgeObject> remaining = new ArrayList<>(knowledgeObjects);
        
        // Start with the most relevant object
        ContextBuilderService.KnowledgeObject first = remaining.remove(0);
        selected.add(first);
        
        // Apply MMR for remaining objects
        while (!remaining.isEmpty() && selected.size() < 10) { // Limit to 10 objects
            double maxScore = -1;
            int maxIndex = -1;
            
            for (int i = 0; i < remaining.size(); i++) {
                ContextBuilderService.KnowledgeObject candidate = remaining.get(i);
                
                // Calculate relevance score (from similarity)
                double relevance = getRelevanceScore(candidate);
                
                // Calculate diversity penalty
                double diversityPenalty = calculateDiversityPenalty(candidate, selected);
                
                // MMR score = relevance * (1 - diversity) + diversity * diversity_penalty
                double mmrScore = relevance * (1 - diversity) + diversity * diversityPenalty;
                
                if (mmrScore > maxScore) {
                    maxScore = mmrScore;
                    maxIndex = i;
                }
            }
            
            if (maxIndex >= 0) {
                selected.add(remaining.remove(maxIndex));
            } else {
                break;
            }
        }
        
        return selected;
    }
    
    /**
     * Get relevance score from metadata.
     */
    private double getRelevanceScore(ContextBuilderService.KnowledgeObject knowledgeObject) {
        try {
            if (knowledgeObject.getMetadata() != null) {
                // Get similarity score from metadata
                Object similarityScore = knowledgeObject.getMetadata().get("similarity_score");
                if (similarityScore instanceof Number) {
                    return ((Number) similarityScore).doubleValue();
                }
            }
        } catch (Exception e) {
            logger.warn("Error parsing relevance score from metadata", e);
        }
        return 0.5; // Default score
    }
    
    /**
     * Calculate diversity penalty based on content similarity.
     */
    private double calculateDiversityPenalty(ContextBuilderService.KnowledgeObject candidate, 
                                           List<ContextBuilderService.KnowledgeObject> selected) {
        if (selected.isEmpty()) {
            return 1.0; // No penalty if no previous selections
        }
        
        double maxSimilarity = 0.0;
        String candidateContent = candidate.getContent();
        
        for (ContextBuilderService.KnowledgeObject selectedObj : selected) {
            String selectedContent = selectedObj.getContent();
            double similarity = calculateContentSimilarity(candidateContent, selectedContent);
            maxSimilarity = Math.max(maxSimilarity, similarity);
        }
        
        return 1.0 - maxSimilarity; // Higher penalty for more similar content
    }
    
    /**
     * Calculate content similarity using simple Jaccard similarity.
     */
    private double calculateContentSimilarity(String content1, String content2) {
        if (content1.isEmpty() || content2.isEmpty()) {
            return 0.0;
        }
        
        // Simple word-based similarity
        Set<String> words1 = new HashSet<>(Arrays.asList(content1.toLowerCase().split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(content2.toLowerCase().split("\\s+")));
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    /**
     * Select the best content variant (prefer SHORT, then others).
     */
    private ContentVariant selectBestVariant(List<ContentVariant> variants) {
        if (variants == null || variants.isEmpty()) {
            return null;
        }
        
        // Prefer SHORT variant
        Optional<ContentVariant> shortVariant = variants.stream()
            .filter(v -> v.getVariant() == middleware.model.ContentVariantType.SHORT)
            .findFirst();
        
        if (shortVariant.isPresent()) {
            return shortVariant.get();
        }
        
        // Fallback to first available variant
        return variants.get(0);
    }
    
    /**
     * Estimate token count for text.
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        // Simple token estimation: roughly 4 characters per token
        // In a real implementation, this would use a proper tokenizer
        return Math.max(1, text.length() / 4);
    }
}
