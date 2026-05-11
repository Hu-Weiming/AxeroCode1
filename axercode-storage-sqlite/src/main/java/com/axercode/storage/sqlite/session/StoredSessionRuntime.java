package com.axercode.storage.sqlite.session;

/**
 * Persisted per-session runtime selection used to remember which provider/model a conversation is attached to.
 */
public record StoredSessionRuntime(
        String providerId,
        String modelName
) {

    public StoredSessionRuntime {
        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("providerId must not be blank");
        }
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalArgumentException("modelName must not be blank");
        }
    }
}
