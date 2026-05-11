package com.axercode.core.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SessionContextTest {

    @Test
    void startCreatesEmptyContextWithGeneratedSessionId() {
        SessionContext context = SessionContext.start();

        assertNotNull(context.sessionId());
        assertEquals(0, context.messages().size());
    }

    @Test
    void appendReturnsNewContextWithoutMutatingExistingMessages() {
        SessionContext empty = SessionContext.start();
        ConversationMessage message = ConversationMessage.user("Explain this repository");

        SessionContext updated = empty.append(message);

        assertEquals(0, empty.messages().size());
        assertEquals(1, updated.messages().size());
        assertEquals(message, updated.messages().getFirst());
        assertEquals(empty.sessionId(), updated.sessionId());
    }

    @Test
    void constructorCopiesMessagesDefensively() {
        List<ConversationMessage> messages = new ArrayList<>();
        messages.add(ConversationMessage.user("Inspect pom"));

        SessionContext context = new SessionContext(SessionId.create(), messages);
        messages.clear();

        assertEquals(1, context.messages().size());
    }

    @Test
    void parseRejectsBlankSessionId() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> SessionId.from(" ")
        );

        assertEquals("sessionId must not be blank", exception.getMessage());
    }
}
