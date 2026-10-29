package com.coderagent.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coderagent.config.OllamaProperties;
import com.coderagent.llm.ChatMessage;
import com.coderagent.chat.ChatCapacityException;
import com.coderagent.llm.LlmProvider;
import com.coderagent.llm.LlmProviderException;
import com.coderagent.llm.OllamaModel;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private LlmProvider provider;

    private ChatService service;

    @BeforeEach
    void setUp() {
        service = new ChatService(provider, new OllamaProperties(
                URI.create("http://localhost:11434"),
                "coder-model",
                Duration.ofSeconds(1),
                Duration.ofSeconds(5),
                2,
                100000,
                1,
                1000,
                512,
                1000));
    }

    @Test
    void sendsConfiguredModelAndRetainsBoundedConversationHistory() {
        when(provider.listModels()).thenReturn(List.of(model("coder-model")));
        when(provider.chat(eq("coder-model"), anyList(), eq(512)))
                .thenReturn(new ChatMessage("assistant", "first"))
                .thenReturn(new ChatMessage("assistant", "second"))
                .thenReturn(new ChatMessage("assistant", "third"));

        service.chat(new ChatRequest("session-1", "question one"));
        service.chat(new ChatRequest("session-1", "question two"));
        ChatResponse response = service.chat(new ChatRequest("session-1", "question three"));

        assertThat(response.reply()).isEqualTo("third");
        ArgumentCaptor<List<ChatMessage>> messages = ArgumentCaptor.captor();
        verify(provider, org.mockito.Mockito.times(3)).chat(eq("coder-model"), messages.capture(), eq(512));
        List<ChatMessage> sentMessages = messages.getAllValues().get(2);
        assertThat(sentMessages)
                .extracting(ChatMessage::role)
                .containsExactly("system", "user", "assistant", "user");
        assertThat(sentMessages.get(1).content()).isEqualTo("question two");
    }

    @Test
    void refusesToCallChatWhenConfiguredModelIsMissing() {
        when(provider.listModels()).thenReturn(List.of(model("other-model")));

        assertThatThrownBy(() -> service.chat(new ChatRequest("session-1", "hello")))
                .isInstanceOf(LlmProviderException.class)
                .hasMessageContaining("is not installed");
        verify(provider, never()).chat(eq("coder-model"), anyList(), eq(512));
    }

    @Test
    void doesNotAddFailedExchangeToHistory() {
        when(provider.listModels()).thenReturn(List.of(model("coder-model")));
        when(provider.chat(eq("coder-model"), anyList(), eq(512)))
                .thenThrow(new LlmProviderException("offline"))
                .thenReturn(new ChatMessage("assistant", "recovered"));

        assertThatThrownBy(() -> service.chat(new ChatRequest("session-1", "first")))
                .isInstanceOf(LlmProviderException.class);
        service.chat(new ChatRequest("session-1", "second"));

        ArgumentCaptor<List<ChatMessage>> messages = ArgumentCaptor.captor();
        verify(provider, org.mockito.Mockito.times(2)).chat(eq("coder-model"), messages.capture(), eq(512));
        List<ChatMessage> retriedMessages = messages.getAllValues().get(1);
        assertThat(retriedMessages).extracting(ChatMessage::role).containsExactly("system", "user");
        assertThat(retriedMessages.get(1).content()).isEqualTo("second");
    }

    @Test
    void rejectsConfiguredMessageLimitBeforeCallingProvider() {
        assertThatThrownBy(() -> service.chat(new ChatRequest("session-1", "x".repeat(1001))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("character limit");
        verify(provider, never()).listModels();
    }

    @Test
    void enforcesSessionCapacity() {
        when(provider.listModels()).thenReturn(List.of(model("coder-model")));
        when(provider.chat(eq("coder-model"), anyList(), eq(512)))
                .thenReturn(new ChatMessage("assistant", "reply"));
        service.chat(new ChatRequest("session-1", "hello"));

        assertThatThrownBy(() -> service.chat(new ChatRequest("session-2", "hello")))
                .isInstanceOf(ChatCapacityException.class);
    }

    @Test
    void resetClearsSessionHistory() {
        when(provider.listModels()).thenReturn(List.of(model("coder-model")));
        when(provider.chat(eq("coder-model"), anyList(), eq(512)))
                .thenReturn(new ChatMessage("assistant", "first"))
                .thenReturn(new ChatMessage("assistant", "after reset"));
        service.chat(new ChatRequest("session-1", "before reset"));
        service.reset("session-1");

        service.chat(new ChatRequest("session-1", "after reset"));

        ArgumentCaptor<List<ChatMessage>> messages = ArgumentCaptor.captor();
        verify(provider, org.mockito.Mockito.times(2)).chat(eq("coder-model"), messages.capture(), eq(512));
        List<ChatMessage> freshHistory = messages.getAllValues().get(1);
        assertThat(freshHistory).extracting(ChatMessage::role).containsExactly("system", "user");
        assertThat(freshHistory.get(1).content()).isEqualTo("after reset");
    }

    @Test
    void trimsOldTurnsToStayWithinContextCharacterLimit() {
        OllamaProperties constrainedProperties = new OllamaProperties(
                URI.create("http://localhost:11434"),
                "coder-model",
                Duration.ofSeconds(1),
                Duration.ofSeconds(5),
                20,
                400,
                1,
                1000,
                512,
                1000);
        ChatService constrainedService = new ChatService(provider, constrainedProperties);
        when(provider.listModels()).thenReturn(List.of(model("coder-model")));
        when(provider.chat(eq("coder-model"), anyList(), eq(512)))
                .thenReturn(new ChatMessage("assistant", "short answer"))
                .thenReturn(new ChatMessage("assistant", "another answer"));
        constrainedService.chat(new ChatRequest("bounded", "x".repeat(150)));

        constrainedService.chat(new ChatRequest("bounded", "y".repeat(150)));

        ArgumentCaptor<List<ChatMessage>> messages = ArgumentCaptor.captor();
        verify(provider, org.mockito.Mockito.times(2)).chat(eq("coder-model"), messages.capture(), eq(512));
        List<ChatMessage> secondRequest = messages.getAllValues().get(1);
        assertThat(secondRequest).extracting(ChatMessage::role).containsExactly("system", "user");
        assertThat(secondRequest.getLast().content()).isEqualTo("y".repeat(150));
    }

    private OllamaModel model(String name) {
        return new OllamaModel(name, name, Instant.now(), 1, "digest");
    }
}
