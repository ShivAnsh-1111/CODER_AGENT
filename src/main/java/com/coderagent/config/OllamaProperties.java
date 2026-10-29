package com.coderagent.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "coder-agent.ollama")
public record OllamaProperties(
        @NotNull @DefaultValue("http://localhost:11434") URI baseUrl,
        @NotBlank @DefaultValue("qwen2.5-coder:7b") String model,
        @NotNull @DefaultValue("3s") Duration connectTimeout,
        @NotNull @DefaultValue("120s") Duration requestTimeout,
        @Min(2) @Max(100) @DefaultValue("20") int maxHistoryMessages,
        @Min(256) @Max(2000000) @DefaultValue("100000") int maxContextChars,
        @Min(1) @Max(10000) @DefaultValue("500") int maxSessions,
        @Min(1) @Max(100000) @DefaultValue("20000") int maxMessageChars,
        @Min(1) @Max(32768) @DefaultValue("2048") int maxOutputTokens,
        @Min(1) @Max(200000) @DefaultValue("30000") int maxResponseChars) {
}
