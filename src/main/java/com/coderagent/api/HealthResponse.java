package com.coderagent.api;

public record HealthResponse(
        String status,
        boolean ollamaAvailable,
        String configuredModel,
        boolean configuredModelAvailable) {
}
