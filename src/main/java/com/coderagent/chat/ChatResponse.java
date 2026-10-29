package com.coderagent.chat;

public record ChatResponse(String sessionId, String model, String reply) {
}
