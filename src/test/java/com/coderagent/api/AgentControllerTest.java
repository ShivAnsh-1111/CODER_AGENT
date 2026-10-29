package com.coderagent.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coderagent.chat.ChatResponse;
import com.coderagent.chat.ChatService;
import com.coderagent.config.OllamaProperties;
import com.coderagent.llm.LlmProvider;
import com.coderagent.llm.LlmProviderException;
import com.coderagent.llm.OllamaModel;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AgentController.class)
@Import(ApiExceptionHandler.class)
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LlmProvider provider;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private OllamaProperties properties;

    @Test
    void healthReportsConfiguredModelAvailability() throws Exception {
        when(properties.model()).thenReturn("coder:latest");
        when(provider.listModels()).thenReturn(List.of(new OllamaModel(
                "coder:latest", "coder:latest", Instant.now(), 10, "digest")));

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.ollamaAvailable").value(true))
                .andExpect(jsonPath("$.configuredModelAvailable").value(true));
    }

    @Test
    void healthReportsDegradedWhenOllamaCannotBeReached() throws Exception {
        when(properties.model()).thenReturn("coder:latest");
        when(provider.listModels()).thenThrow(new LlmProviderException("offline"));

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEGRADED"))
                .andExpect(jsonPath("$.ollamaAvailable").value(false));
    }

    @Test
    void rejectsInvalidChatRequest() throws Exception {
        mockMvc.perform(post("/api/v1/chat")
                        .contentType("application/json")
                        .content("""
                                {"sessionId":"bad session","message":"hello"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsCodingChatReply() throws Exception {
        when(chatService.chat(any())).thenReturn(new ChatResponse("session-1", "coder:latest", "Hello"));

        mockMvc.perform(post("/api/v1/chat")
                        .contentType("application/json")
                        .content("""
                                {"sessionId":"session-1","message":"Explain records"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Hello"));
    }
}
