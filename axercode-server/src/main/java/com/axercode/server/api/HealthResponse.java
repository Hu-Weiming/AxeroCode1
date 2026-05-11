package com.axercode.server.api;

public record HealthResponse(
        String status,
        String application
) {
}
