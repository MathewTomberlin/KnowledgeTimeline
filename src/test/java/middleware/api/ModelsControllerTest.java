package middleware.api;

import middleware.dto.Model;
import middleware.service.LLMClientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for ModelsController.
 */
@WebMvcTest(ModelsController.class)
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
})
public class ModelsControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private LLMClientService llmClientService;
    
    @Test
    public void testGetModels() throws Exception {
        // Create mock models
        List<Model> mockModels = Arrays.asList(
            Model.builder()
                .id("gpt-3.5-turbo")
                .ownedBy("openai")
                .maxTokens(4096)
                .knowledgeAware(true)
                .build(),
            Model.builder()
                .id("gpt-4")
                .ownedBy("openai")
                .maxTokens(8192)
                .knowledgeAware(true)
                .build()
        );
        
        when(llmClientService.getAvailableModels()).thenReturn(mockModels);
        when(llmClientService.isHealthy()).thenReturn(true);
        
        // Test models endpoint
        mockMvc.perform(get("/v1/models"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.object").value("list"))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].id").value("gpt-3.5-turbo"))
            .andExpect(jsonPath("$.data[1].id").value("gpt-4"));
    }
    
    @Test
    public void testHealth() throws Exception {
        when(llmClientService.isHealthy()).thenReturn(true);
        
        // Test health endpoint
        mockMvc.perform(get("/v1/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("healthy"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.llm_service").value(true));
    }
}
