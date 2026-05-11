package com.axercode.server.service;

/**
 * Server-facing session summary returned to the web sidebar.
 */
public record ServerSessionSummary(
        String sessionId,
        String title,
        String preview,
        String updatedAt,
        int messageCount
) {
}
