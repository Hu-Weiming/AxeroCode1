package com.axercode.server.api;

import com.axercode.core.session.ConversationMessage;
import com.axercode.core.session.MessageRole;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SessionMessageResponse(
        String role,
        String content,
        Instant createdAt,
        String model
) {

    public static SessionMessageResponse from(
            ConversationMessage message,
            String fallbackAssistantModel,
            Map<UUID, String> messageModels
    ) {
        String model = message.role() == MessageRole.ASSISTANT
                ? messageModels.getOrDefault(message.id(), fallbackAssistantModel)
                : null;
        return new SessionMessageResponse(
                message.role().name(),
                message.content(),
                message.createdAt(),
                model
        );
    }
}
