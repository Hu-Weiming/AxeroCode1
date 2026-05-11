package com.axercode.server.api;

import com.axercode.server.service.ServerStoredSession;
import java.util.List;

public record SessionResponse(
        String sessionId,
        String provider,
        String model,
        int messageCount,
        List<SessionMessageResponse> messages
) {

    public static SessionResponse from(ServerStoredSession session) {
        return new SessionResponse(
                session.sessionContext().sessionId().toString(),
                session.provider(),
                session.model(),
                session.sessionContext().messages().size(),
                session.sessionContext().messages().stream()
                        .map(message -> SessionMessageResponse.from(
                                message,
                                session.model(),
                                session.messageModels()
                        ))
                        .toList()
        );
    }
}
