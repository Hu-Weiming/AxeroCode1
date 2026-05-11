package com.axercode.server.api;

public record BtwRequest(
        String sessionId,
        String message
) {
}
