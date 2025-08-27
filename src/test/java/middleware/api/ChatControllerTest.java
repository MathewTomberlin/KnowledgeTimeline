package middleware.api;

import middleware.dto.ChatCompletionRequest;
import middleware.dto.ChatCompletionResponse;
import middleware.dto.ChatMessage;
import middleware.dto.ErrorResponse;
import middleware.service.ContextBuilderService;
import middleware.service.LLMClientService;
import middleware.service.UsageTrackingService;
import middleware.service.MemoryStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for ChatController.
 */
@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private LLMClientService llmClientService;

    @Mock
    private ContextBuilderService contextBuilderService;

    @Mock
    private UsageTrackingService usageTrackingService;

    @Mock
    private MemoryStorageService memoryStorageService;

    @InjectMocks
    private ChatController chatController;

    private ChatCompletionRequest validRequest;
    private ChatCompletionRequest invalidRequest;

    @BeforeEach
    void setUp() {
        // Create valid request
        validRequest = new ChatCompletionRequest();
        validRequest.setModel("gpt-3.5-turbo");
        validRequest.setMessages(Arrays.asList(
            new ChatMessage("user", "Hello, how are you?")
        ));
        validRequest.setStream(false);
        validRequest.setTemperature(0.7);
        validRequest.setMaxTokens(100);

        // Create invalid request
        invalidRequest = new ChatCompletionRequest();
        invalidRequest.setModel(""); // Invalid: empty model
        invalidRequest.setMessages(Arrays.asList(
            new ChatMessage("user", "Hello") // Missing role
        ));
    }

    @Test
    void testCreateChatCompletion_ValidRequest_NonStreaming() {
        // Given
        when(contextBuilderService.buildContext(anyString(), anyString(), anyString(), any()))
            .thenReturn("Test context");
        
        ChatCompletionResponse mockResponse = new ChatCompletionResponse();
        mockResponse.setId("chatcmpl-test-response");
        mockResponse.setModel("gpt-3.5-turbo");
        when(llmClientService.createChatCompletion(any())).thenReturn(mockResponse);

        // When
        ResponseEntity<?> response = chatController.createChatCompletion(validRequest);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ChatCompletionResponse);
        
        ChatCompletionResponse chatResponse = (ChatCompletionResponse) response.getBody();
        assertEquals("gpt-3.5-turbo", chatResponse.getModel());
        assertNotNull(chatResponse.getId());
        assertTrue(chatResponse.getId().startsWith("chatcmpl-"));
    }

    @Test
    void testCreateChatCompletion_ValidRequest_Streaming() {
        // Given
        validRequest.setStream(true);

        // When
        ResponseEntity<?> response = chatController.createChatCompletion(validRequest);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.TEXT_EVENT_STREAM, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof SseEmitter);
    }

    @Test
    void testCreateChatCompletion_NullRequest() {
        // When
        ResponseEntity<?> response = chatController.createChatCompletion(null);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ErrorResponse);
        
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals("invalid_request_error", errorResponse.getError().getType());
        assertEquals("Request cannot be null", errorResponse.getError().getMessage());
    }

    @Test
    void testCreateChatCompletion_NullModel() {
        // Given
        validRequest.setModel(null);

        // When
        ResponseEntity<?> response = chatController.createChatCompletion(validRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ErrorResponse);
        
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals("invalid_request_error", errorResponse.getError().getType());
        assertEquals("Model is required", errorResponse.getError().getMessage());
    }

    @Test
    void testCreateChatCompletion_EmptyModel() {
        // Given
        validRequest.setModel("");

        // When
        ResponseEntity<?> response = chatController.createChatCompletion(validRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ErrorResponse);
        
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals("invalid_request_error", errorResponse.getError().getType());
        assertEquals("Model is required", errorResponse.getError().getMessage());
    }

    @Test
    void testCreateChatCompletion_NullMessages() {
        // Given
        validRequest.setMessages(null);

        // When
        ResponseEntity<?> response = chatController.createChatCompletion(validRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ErrorResponse);
        
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals("invalid_request_error", errorResponse.getError().getType());
        assertEquals("Messages cannot be empty", errorResponse.getError().getMessage());
    }

    @Test
    void testCreateChatCompletion_EmptyMessages() {
        // Given
        validRequest.setMessages(Arrays.asList());

        // When
        ResponseEntity<?> response = chatController.createChatCompletion(validRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ErrorResponse);
        
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals("invalid_request_error", errorResponse.getError().getType());
        assertEquals("Messages cannot be empty", errorResponse.getError().getMessage());
    }

    @Test
    void testCreateChatCompletion_MessageWithNullRole() {
        // Given
        List<ChatMessage> messages = Arrays.asList(
            new ChatMessage(null, "Hello")
        );
        validRequest.setMessages(messages);

        // When
        ResponseEntity<?> response = chatController.createChatCompletion(validRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ErrorResponse);
        
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals("invalid_request_error", errorResponse.getError().getType());
        assertEquals("Message 0 role is required", errorResponse.getError().getMessage());
    }

    @Test
    void testCreateChatCompletion_MessageWithEmptyRole() {
        // Given
        List<ChatMessage> messages = Arrays.asList(
            new ChatMessage("", "Hello")
        );
        validRequest.setMessages(messages);

        // When
        ResponseEntity<?> response = chatController.createChatCompletion(validRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ErrorResponse);
        
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals("invalid_request_error", errorResponse.getError().getType());
        assertEquals("Message 0 role is required", errorResponse.getError().getMessage());
    }

    @Test
    void testCreateChatCompletion_MessageWithNullContent() {
        // Given
        List<ChatMessage> messages = Arrays.asList(
            new ChatMessage("user", null)
        );
        validRequest.setMessages(messages);

        // When
        ResponseEntity<?> response = chatController.createChatCompletion(validRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ErrorResponse);
        
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals("invalid_request_error", errorResponse.getError().getType());
        assertEquals("Message 0 content is required", errorResponse.getError().getMessage());
    }

    @Test
    void testCreateChatCompletion_MessageWithEmptyContent() {
        // Given
        List<ChatMessage> messages = Arrays.asList(
            new ChatMessage("user", "")
        );
        validRequest.setMessages(messages);

        // When
        ResponseEntity<?> response = chatController.createChatCompletion(validRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ErrorResponse);
        
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals("invalid_request_error", errorResponse.getError().getType());
        assertEquals("Message 0 content is required", errorResponse.getError().getMessage());
    }

    @Test
    void testCreateChatCompletion_TemperatureTooLow() {
        // Given
        validRequest.setTemperature(-0.1);

        // When
        ResponseEntity<?> response = chatController.createChatCompletion(validRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ErrorResponse);
        
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals("invalid_request_error", errorResponse.getError().getType());
        assertEquals("Temperature must be between 0.0 and 2.0", errorResponse.getError().getMessage());
    }

    @Test
    void testCreateChatCompletion_TemperatureTooHigh() {
        // Given
        validRequest.setTemperature(2.1);

        // When
        ResponseEntity<?> response = chatController.createChatCompletion(validRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ErrorResponse);
        
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals("invalid_request_error", errorResponse.getError().getType());
        assertEquals("Temperature must be between 0.0 and 2.0", errorResponse.getError().getMessage());
    }

    @Test
    void testCreateChatCompletion_MaxTokensInvalid() {
        // Given
        validRequest.setMaxTokens(0);

        // When
        ResponseEntity<?> response = chatController.createChatCompletion(validRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ErrorResponse);
        
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals("invalid_request_error", errorResponse.getError().getType());
        assertEquals("Max tokens must be positive", errorResponse.getError().getMessage());
    }

    @Test
    void testCreateChatCompletion_MaxTokensNegative() {
        // Given
        validRequest.setMaxTokens(-10);

        // When
        ResponseEntity<?> response = chatController.createChatCompletion(validRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ErrorResponse);
        
        ErrorResponse errorResponse = (ErrorResponse) response.getBody();
        assertEquals("invalid_request_error", errorResponse.getError().getType());
        assertEquals("Max tokens must be positive", errorResponse.getError().getMessage());
    }

    @Test
    void testCreateChatCompletion_ValidTemperatureRange() {
        // Given
        validRequest.setTemperature(0.0); // Minimum valid value
        when(contextBuilderService.buildContext(anyString(), anyString(), anyString(), any()))
            .thenReturn("Test context");
        
        ChatCompletionResponse mockResponse = new ChatCompletionResponse();
        mockResponse.setId("test-response");
        mockResponse.setModel("gpt-3.5-turbo");
        when(llmClientService.createChatCompletion(any())).thenReturn(mockResponse);

        // When
        ResponseEntity<?> response = chatController.createChatCompletion(validRequest);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ChatCompletionResponse);
    }

    @Test
    void testCreateChatCompletion_ValidMaxTokens() {
        // Given
        validRequest.setMaxTokens(1000); // Valid positive value
        when(contextBuilderService.buildContext(anyString(), anyString(), anyString(), any()))
            .thenReturn("Test context");
        
        ChatCompletionResponse mockResponse = new ChatCompletionResponse();
        mockResponse.setId("test-response");
        mockResponse.setModel("gpt-3.5-turbo");
        when(llmClientService.createChatCompletion(any())).thenReturn(mockResponse);

        // When
        ResponseEntity<?> response = chatController.createChatCompletion(validRequest);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ChatCompletionResponse);
    }

    @Test
    void testCreateChatCompletion_MultipleMessages() {
        // Given
        List<ChatMessage> messages = Arrays.asList(
            new ChatMessage("system", "You are a helpful assistant."),
            new ChatMessage("user", "Hello, how are you?"),
            new ChatMessage("assistant", "I'm doing well, thank you!")
        );
        validRequest.setMessages(messages);
        
        when(contextBuilderService.buildContext(anyString(), anyString(), anyString(), any()))
            .thenReturn("Test context");
        
        ChatCompletionResponse mockResponse = new ChatCompletionResponse();
        mockResponse.setId("test-response");
        mockResponse.setModel("gpt-3.5-turbo");
        when(llmClientService.createChatCompletion(any())).thenReturn(mockResponse);

        // When
        ResponseEntity<?> response = chatController.createChatCompletion(validRequest);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ChatCompletionResponse);
    }

    @Test
    void testCreateChatCompletion_NonStreaming_WithMemoryStorage() {
        // Given
        when(contextBuilderService.buildContext(anyString(), anyString(), anyString(), any()))
            .thenReturn("Test context");
        
        ChatCompletionResponse mockResponse = new ChatCompletionResponse();
        mockResponse.setId("test-response");
        mockResponse.setModel("gpt-3.5-turbo");
        when(llmClientService.createChatCompletion(any())).thenReturn(mockResponse);

        // When
        ResponseEntity<?> response = chatController.createChatCompletion(validRequest);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        // Verify memory storage was called
        verify(memoryStorageService).processConversationTurn(
            eq("default-tenant"), 
            anyString(), // session ID is generated dynamically
            eq("default-user"), 
            eq("Hello, how are you?"), 
            eq(""), // empty response since mock response has no choices
            any()
        );
    }

    @Test
    void testCreateChatCompletion_Streaming_WithMemoryStorage() {
        // Given
        validRequest.setStream(true);
        when(contextBuilderService.buildContext(anyString(), anyString(), anyString(), any()))
            .thenReturn("Test context");

        // When
        ResponseEntity<?> response = chatController.createChatCompletion(validRequest);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.TEXT_EVENT_STREAM, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof SseEmitter);
        
        // Note: Memory storage for streaming is tested in integration tests
        // since it happens asynchronously in CompletableFuture.runAsync
    }

    @Test
    void testCreateChatCompletion_MemoryStorageFailure_NonStreaming() {
        // Given
        when(contextBuilderService.buildContext(anyString(), anyString(), anyString(), any()))
            .thenReturn("Test context");
        
        ChatCompletionResponse mockResponse = new ChatCompletionResponse();
        mockResponse.setId("test-response");
        mockResponse.setModel("gpt-3.5-turbo");
        when(llmClientService.createChatCompletion(any())).thenReturn(mockResponse);
        
        // Simulate memory storage failure
        doThrow(new RuntimeException("Memory storage failed"))
            .when(memoryStorageService).processConversationTurn(anyString(), anyString(), anyString(), anyString(), anyString(), any());

        // When
        ResponseEntity<?> response = chatController.createChatCompletion(validRequest);

        // Then
        // Should still succeed even if memory storage fails
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        // Verify memory storage was attempted
        verify(memoryStorageService).processConversationTurn(
            eq("default-tenant"), 
            anyString(), 
            eq("default-user"), 
            eq("Hello, how are you?"), 
            eq(""), 
            any()
        );
    }

    @Test
    void testCreateChatCompletion_WithContextEnhancement() {
        // Given
        when(contextBuilderService.buildContext(anyString(), anyString(), anyString(), any()))
            .thenReturn("Enhanced context with knowledge");
        
        ChatCompletionResponse mockResponse = new ChatCompletionResponse();
        mockResponse.setId("test-response");
        mockResponse.setModel("gpt-3.5-turbo");
        when(llmClientService.createChatCompletion(any())).thenReturn(mockResponse);

        // When
        ResponseEntity<?> response = chatController.createChatCompletion(validRequest);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Verify that the LLM service was called with an enhanced request
        verify(llmClientService).createChatCompletion(argThat(request -> {
            // Check that the enhanced request has a system message with context
            return request.getMessages().stream()
                .anyMatch(msg -> "system".equals(msg.getRole()) && 
                               msg.getContent().contains("Enhanced context with knowledge"));
        }));
    }

    @Test
    void testCreateChatCompletion_LastUserMessageExtraction() {
        // Given
        List<ChatMessage> messages = Arrays.asList(
            new ChatMessage("assistant", "Previous response"),
            new ChatMessage("user", "Current user message"),
            new ChatMessage("system", "System instruction")
        );
        validRequest.setMessages(messages);
        
        when(contextBuilderService.buildContext(anyString(), anyString(), anyString(), any()))
            .thenReturn("Test context");
        
        ChatCompletionResponse mockResponse = new ChatCompletionResponse();
        mockResponse.setId("test-response");
        mockResponse.setModel("gpt-3.5-turbo");
        when(llmClientService.createChatCompletion(any())).thenReturn(mockResponse);

        // When
        ResponseEntity<?> response = chatController.createChatCompletion(validRequest);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Verify that the last user message ("Current user message") was extracted correctly
        verify(memoryStorageService).processConversationTurn(
            anyString(), anyString(), anyString(), 
            eq("Current user message"), // Should be the last user message
            anyString(), any()
        );
    }
}
