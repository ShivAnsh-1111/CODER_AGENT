package com.coderagent.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OllamaClient implements LlmProvider {

    private final RestClient restClient;

    public OllamaClient(RestClient ollamaRestClient) {
        this.restClient = ollamaRestClient;
    }

    @Override
    public List<OllamaModel> listModels() {
        try {
            TagsResponse response = restClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .body(TagsResponse.class);
            if (response == null || response.models() == null) {
                throw new LlmProviderException("Ollama returned an invalid model list.");
            }
            return List.copyOf(response.models());
        } catch (RestClientException exception) {
            throw new LlmProviderException("Could not retrieve models from Ollama.", exception);
        }
    }

    @Override
    public ChatMessage chat(String model, List<ChatMessage> messages, int maxOutputTokens) {
        ChatRequest request = new ChatRequest(model, messages, false, new ChatOptions(maxOutputTokens));
        try {
            ChatResponse response = restClient.post()
                    .uri("/api/chat")
                    .body(request)
                    .retrieve()
                    .body(ChatResponse.class);
            if (response == null || response.message() == null
                    || response.message().content() == null) {
                throw new LlmProviderException("Ollama returned an invalid chat response.");
            }
            return new ChatMessage("assistant", response.message().content());
        } catch (RestClientException exception) {
            throw new LlmProviderException("Ollama could not complete the chat request.", exception);
        }
    }

    private record TagsResponse(List<OllamaModel> models) {
    }

    private record ChatRequest(
            String model,
            List<ChatMessage> messages,
            boolean stream,
            ChatOptions options) {
    }

    private record ChatOptions(@JsonProperty("num_predict") int numPredict) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatResponse(ChatMessage message) {
    }
}
