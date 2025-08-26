package middleware.api;

import middleware.dto.ChatCompletionRequest;
import middleware.dto.ChatCompletionResponse;
import middleware.dto.ChatMessage;
import middleware.dto.ErrorResponse;
import middleware.service.ContextBuilderService;
import middleware.service.LLMClientService;
import middleware.service.UsageTrackingService;
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

        // When
        ResponseEntity<?> response = chatController.createChatCompletion(validRequest);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ChatCompletionResponse);
    }
}
