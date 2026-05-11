package com.axercode.core.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SessionContextWindowTest {

    @Test
    void constructorRejectsNonPositiveRecentMessageLimit() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SessionContextWindow(0)
        );

        assertEquals("maxRecentMessages must be at least 1", exception.getMessage());
    }

    @Test
    void trimReturnsOriginalMessagesWhenWithinLimit() {
        SessionContext context = new SessionContext(
                SessionId.create(),
                List.of(
                        system("Focus path"),
                        ConversationMessage.user("hello"),
                        assistant("reply")
                )
        );

        SessionContext trimmed = new SessionContextWindow(2).trim(context);

        assertEquals(3, trimmed.messages().size());
        assertEquals("Focus path", trimmed.messages().get(0).content());
        assertEquals("hello", trimmed.messages().get(1).content());
        assertEquals("reply", trimmed.messages().get(2).content());
    }

    @Test
    void trimKeepsAllSystemMessagesAndOnlyRecentNonSystemMessages() {
        SessionContext context = new SessionContext(
                SessionId.create(),
                List.of(
                        system("Focus path"),
                        ConversationMessage.user("u1"),
                        assistant("a1"),
                        system("Plan mode"),
                        tool("t1"),
                        ConversationMessage.user("u2"),
                        assistant("a2")
                )
        );

        SessionContext trimmed = new SessionContextWindow(3).trim(context);

        assertEquals(5, trimmed.messages().size());
        assertEquals(List.of("Focus path", "Plan mode", "t1", "u2", "a2"),
                trimmed.messages().stream().map(ConversationMessage::content).toList());
        assertEquals(List.of("t1", "u2", "a2"), trimmed.messages().stream()
                .filter(message -> message.role() != MessageRole.SYSTEM)
                .map(ConversationMessage::content)
                .toList());
        assertTrue(trimmed.messages().stream().noneMatch(message -> "u1".equals(message.content())));
        assertTrue(trimmed.messages().stream().noneMatch(message -> "a1".equals(message.content())));
    }

    private static ConversationMessage system(String content) {
        return new ConversationMessage(UUID.randomUUID(), MessageRole.SYSTEM, content, Instant.now());
    }

    private static ConversationMessage assistant(String content) {
        return new ConversationMessage(UUID.randomUUID(), MessageRole.ASSISTANT, content, Instant.now());
    }

    private static ConversationMessage tool(String content) {
        return new ConversationMessage(UUID.randomUUID(), MessageRole.TOOL, content, Instant.now());
    }
}
