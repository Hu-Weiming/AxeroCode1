package com.axercode.core.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConversationMessageTest {

    @Test
    void userFactoryCreatesUserMessageWithGeneratedMetadata() {
        ConversationMessage message = ConversationMessage.user("Explain the last diff");

        assertEquals(MessageRole.USER, message.role());
        assertEquals("Explain the last diff", message.content());
        assertNotNull(message.id());
        assertNotNull(message.createdAt());
    }

    @Test
    void constructorRejectsBlankContent() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ConversationMessage(UUID.randomUUID(), MessageRole.ASSISTANT, " ", Instant.now())
        );

        assertEquals("content must not be blank", exception.getMessage());
    }
}
