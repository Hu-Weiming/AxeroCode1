package com.axercode.core.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.axercode.core.session.ConversationMessage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentRequestTest {

    @Test
    void createCopiesMessagesDefensively() {
        List<ConversationMessage> messages = new ArrayList<>();
        messages.add(ConversationMessage.user("Open README"));

        AgentRequest request = AgentRequest.create(messages);
        messages.clear();

        assertEquals(1, request.messages().size());
    }

    @Test
    void createRejectsEmptyMessages() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AgentRequest.create(List.of())
        );

        assertEquals("messages must not be empty", exception.getMessage());
    }
}
