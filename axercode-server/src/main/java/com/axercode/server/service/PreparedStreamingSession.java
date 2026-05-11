package com.axercode.server.service;

import com.axercode.agent.TurnInterruptionSource;
import com.axercode.core.session.SessionContext;
import java.util.Objects;

/**
 * Streaming turn metadata prepared before the async SSE response starts.
 */
public record PreparedStreamingSession(
        SessionContext sessionContext,
        String providerId,
        String model,
        String routedModel,
        TurnInterruptionSource turnInterruptionSource
) {

    public PreparedStreamingSession {
        Objects.requireNonNull(sessionContext, "sessionContext must not be null");
        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("providerId must not be blank");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("model must not be blank");
        }
        if (routedModel == null || routedModel.isBlank()) {
            throw new IllegalArgumentException("routedModel must not be blank");
        }
        Objects.requireNonNull(turnInterruptionSource, "turnInterruptionSource must not be null");
    }

    public String sessionId() {
        return sessionContext.sessionId().toString();
    }
}
