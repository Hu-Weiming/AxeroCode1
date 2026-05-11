package com.axercode.core.session;

import java.util.Objects;
import java.util.UUID;

/**
 * Strongly typed identifier for a persisted or in-memory conversation session.
 */
public record SessionId(UUID value) {

    public SessionId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static SessionId create() {
        return new SessionId(UUID.randomUUID());
    }

    public static SessionId from(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        return new SessionId(UUID.fromString(sessionId));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
