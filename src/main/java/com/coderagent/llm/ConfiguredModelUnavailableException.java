package com.coderagent.llm;

public class ConfiguredModelUnavailableException extends LlmProviderException {

    public ConfiguredModelUnavailableException(String model) {
        super("Configured Ollama model '" + model + "' is not installed.");
    }
}
