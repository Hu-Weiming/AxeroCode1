package com.axercode.core.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SessionContextBrancherTest {

    @Test
    void branchCreatesNewSessionAndNewMessageIdsWhilePreservingContent() {
        SessionContext source = new SessionContext(
                SessionId.create(),
                List.of(
                        new ConversationMessage(UUID.randomUUID(), MessageRole.USER, "hello", Instant.parse("2026-04-18T12:00:00Z")),
                        new ConversationMessage(UUID.randomUUID(), MessageRole.ASSISTANT, "reply", Instant.parse("2026-04-18T12:00:05Z"))
                )
        );

        SessionContext branched = SessionContextBrancher.branch(source);

        assertNotEquals(source.sessionId(), branched.sessionId());
        assertEquals(List.of("hello", "reply"), branched.messages().stream().map(ConversationMessage::content).toList());
        assertNotEquals(source.messages().get(0).id(), branched.messages().get(0).id());
        assertNotEquals(source.messages().get(1).id(), branched.messages().get(1).id());
        assertEquals(source.messages().get(0).createdAt(), branched.messages().get(0).createdAt());
        assertEquals(source.messages().get(1).createdAt(), branched.messages().get(1).createdAt());
    }
}
