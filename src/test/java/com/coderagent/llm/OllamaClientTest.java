package com.coderagent.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

class OllamaClientTest {

    private MockRestServiceServer server;
    private OllamaClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:11434");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OllamaClient(builder.build());
    }

    @Test
    void discoversInstalledModelsFromOllamaTagsApi() {
        server.expect(requestTo("http://localhost:11434/api/tags"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {"models":[{"name":"coder:latest","model":"coder:latest","modified_at":"2025-01-02T03:04:05Z","size":12,"digest":"abc"}]}
                        """, APPLICATION_JSON));

        List<OllamaModel> models = client.listModels();

        assertThat(models).hasSize(1);
        assertThat(models.getFirst().name()).isEqualTo("coder:latest");
        server.verify();
    }

    @Test
    void sendsNonStreamingChatRequestToConfiguredModel() {
        server.expect(requestTo("http://localhost:11434/api/chat"))
                .andExpect(method(POST))
                .andExpect(content().json("""
                        {"model":"coder:latest","messages":[{"role":"user","content":"How?"}],"stream":false,"options":{"num_predict":256}}
                        """))
                .andRespond(withSuccess("""
                        {"model":"coder:latest","message":{"role":"assistant","content":"Use a record."},"done":true}
                        """, APPLICATION_JSON));

        ChatMessage message = client.chat(
                "coder:latest", List.of(new ChatMessage("user", "How?")), 256);

        assertThat(message).isEqualTo(new ChatMessage("assistant", "Use a record."));
        server.verify();
    }

    @Test
    void convertsOllamaHttpFailureToProviderExceptionWithoutLeakingResponseBody() {
        server.expect(requestTo("http://localhost:11434/api/tags"))
                .andRespond(withServerError().body("private server detail"));

        assertThatThrownBy(client::listModels)
                .isInstanceOf(LlmProviderException.class)
                .hasMessage("Could not retrieve models from Ollama.")
                .hasMessageNotContaining("private server detail");
    }
}
