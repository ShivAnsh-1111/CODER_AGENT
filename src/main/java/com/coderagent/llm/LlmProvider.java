package com.coderagent.llm;

import java.util.List;

public interface LlmProvider {

    List<OllamaModel> listModels();

    ChatMessage chat(String model, List<ChatMessage> messages, int maxOutputTokens);
}
