package com.axercode.server.api;

public record BtwResponse(
        String sessionId,
        int queuedCount
) {
}
