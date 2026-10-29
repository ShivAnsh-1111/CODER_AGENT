package com.coderagent.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank
                @Size(max = 80)
                @Pattern(regexp = "[A-Za-z0-9._-]+", message = "must contain only letters, digits, dot, underscore or hyphen")
                String sessionId,
        @NotBlank @Size(max = 20000) String message) {
}
