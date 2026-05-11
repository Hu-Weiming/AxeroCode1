package com.axercode.core.session;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable chat message model shared by CLI, providers, storage, and agent orchestration.
 */
public record ConversationMessage(UUID id, MessageRole role, String content, Instant createdAt) {

    public ConversationMessage {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
    }

    public static ConversationMessage user(String content) {
        return new ConversationMessage(UUID.randomUUID(), MessageRole.USER, content, Instant.now());
    }
}
