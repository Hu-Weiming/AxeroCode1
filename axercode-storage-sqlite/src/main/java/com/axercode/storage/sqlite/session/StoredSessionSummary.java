package com.axercode.storage.sqlite.session;

import java.util.Objects;

/**
 * Lightweight persisted session summary used for sidebar session lists.
 */
public record StoredSessionSummary(
        String sessionId,
        String title,
        String preview,
        String updatedAt,
        int messageCount
) {

    public StoredSessionSummary {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (preview == null || preview.isBlank()) {
            throw new IllegalArgumentException("preview must not be blank");
        }
        if (updatedAt == null || updatedAt.isBlank()) {
            throw new IllegalArgumentException("updatedAt must not be blank");
        }
        if (messageCount < 0) {
            throw new IllegalArgumentException("messageCount must not be negative");
        }
    }
}
