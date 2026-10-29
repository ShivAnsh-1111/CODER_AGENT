package com.coderagent.api;

import com.coderagent.chat.ChatRequest;
import com.coderagent.chat.ChatResponse;
import com.coderagent.chat.ChatService;
import com.coderagent.config.OllamaProperties;
import com.coderagent.llm.LlmProvider;
import com.coderagent.llm.LlmProviderException;
import com.coderagent.llm.OllamaModel;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AgentController {

    private final LlmProvider provider;
    private final OllamaProperties properties;
    private final ChatService chatService;

    public AgentController(LlmProvider provider, OllamaProperties properties, ChatService chatService) {
        this.provider = provider;
        this.properties = properties;
        this.chatService = chatService;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        try {
            List<OllamaModel> models = provider.listModels();
            boolean configuredModelAvailable = models.stream()
                    .anyMatch(model -> properties.model().equals(model.name())
                            || properties.model().equals(model.model()));
            return new HealthResponse(
                    configuredModelAvailable ? "UP" : "DEGRADED",
                    true,
                    properties.model(),
                    configuredModelAvailable);
        } catch (LlmProviderException exception) {
            return new HealthResponse("DEGRADED", false, properties.model(), false);
        }
    }

    @GetMapping("/models")
    public ModelsResponse models() {
        return new ModelsResponse(properties.model(), provider.listModels());
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(chatService.chat(request));
    }

    @DeleteMapping("/chat/{sessionId}")
    public ResponseEntity<Void> resetChat(@PathVariable String sessionId) {
        if (!sessionId.matches("[A-Za-z0-9._-]{1,80}")) {
            throw new IllegalArgumentException("Invalid chat session identifier.");
        }
        chatService.reset(sessionId);
        return ResponseEntity.noContent().build();
    }
}
