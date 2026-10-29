package com.coderagent.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaModel(
        String name,
        String model,
        @JsonProperty("modified_at") Instant modifiedAt,
        long size,
        String digest) {
}
