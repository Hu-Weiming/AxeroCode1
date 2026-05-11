package com.axercode.server.service;

import com.axercode.core.session.SessionContext;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Loaded session snapshot together with its remembered provider/model runtime metadata.
 */
public record ServerStoredSession(
        SessionContext sessionContext,
        String provider,
        String model,
        Map<UUID, String> messageModels
) {

    public ServerStoredSession {
        Objects.requireNonNull(sessionContext, "sessionContext must not be null");
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("provider must not be blank");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("model must not be blank");
        }
        Objects.requireNonNull(messageModels, "messageModels must not be null");
        messageModels = Map.copyOf(messageModels);
    }

    public ServerStoredSession(SessionContext sessionContext, String provider, String model) {
        this(sessionContext, provider, model, Map.of());
    }
}
