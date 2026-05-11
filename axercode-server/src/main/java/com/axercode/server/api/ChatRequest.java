package com.axercode.server.api;

public record ChatRequest(
        String prompt,
        String sessionId,
        String provider,
        String model
) {
}
