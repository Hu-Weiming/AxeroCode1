package com.axercode.server.service;

import com.axercode.agent.TurnInterruptionSource;
import com.axercode.core.session.ConversationMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * Tracks active streaming turns so BTW messages can be queued into the next provider round.
 */
final class ServerActiveTurnRegistry {

    private final ConcurrentMap<String, ActiveTurn> activeTurns = new ConcurrentHashMap<>();

    public TurnInterruptionSource begin(String sessionId) {
        requireSessionId(sessionId);
        ActiveTurn nextTurn = new ActiveTurn();
        ActiveTurn existingTurn = activeTurns.putIfAbsent(sessionId, nextTurn);
        if (existingTurn != null) {
            throw new IllegalStateException("A streaming turn is already active for session: " + sessionId);
        }
        return nextTurn::drainPendingMessages;
    }

    public void finish(String sessionId) {
        requireSessionId(sessionId);
        activeTurns.remove(sessionId);
    }

    public int enqueue(String sessionId, String message) {
        requireSessionId(sessionId);
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        ActiveTurn activeTurn = activeTurns.get(sessionId);
        if (activeTurn == null) {
            throw new IllegalStateException("No active streaming turn for session: " + sessionId);
        }
        return activeTurn.enqueue(message.trim());
    }

    private void requireSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
    }

    private static final class ActiveTurn {
        private final ConcurrentLinkedQueue<String> pendingMessages = new ConcurrentLinkedQueue<>();

        private int enqueue(String message) {
            pendingMessages.add(message);
            return pendingMessages.size();
        }

        private List<ConversationMessage> drainPendingMessages() {
            List<ConversationMessage> drainedMessages = new ArrayList<>();
            String pendingMessage;
            while ((pendingMessage = pendingMessages.poll()) != null) {
                drainedMessages.add(ConversationMessage.user(pendingMessage));
            }
            return List.copyOf(drainedMessages);
        }
    }
}
