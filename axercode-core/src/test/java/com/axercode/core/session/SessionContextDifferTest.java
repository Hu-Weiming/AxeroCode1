package com.axercode.core.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SessionContextDifferTest {

    @Test
    void diffReportsIdenticalSessions() {
        SessionContext current = new SessionContext(
                SessionId.create(),
                List.of(
                        message(MessageRole.USER, "hello", "2026-04-18T18:00:00Z"),
                        message(MessageRole.ASSISTANT, "reply", "2026-04-18T18:00:05Z")
                )
        );
        SessionContext reference = new SessionContext(
                SessionId.create(),
                List.of(
                        message(MessageRole.USER, "hello", "2026-04-18T18:00:00Z"),
                        message(MessageRole.ASSISTANT, "reply", "2026-04-18T18:00:05Z")
                )
        );

        SessionDiff diff = SessionContextDiffer.diff(current, reference);

        assertEquals(2, diff.commonPrefixCount());
        assertTrue(diff.currentOnlyMessages().isEmpty());
        assertTrue(diff.referenceOnlyMessages().isEmpty());
        assertTrue(diff.identical());
    }

    @Test
    void diffReportsDivergingCurrentAndReferenceTails() {
        SessionContext current = new SessionContext(
                SessionId.create(),
                List.of(
                        message(MessageRole.USER, "hello", "2026-04-18T18:01:00Z"),
                        message(MessageRole.ASSISTANT, "current", "2026-04-18T18:01:05Z"),
                        message(MessageRole.USER, "follow-up", "2026-04-18T18:01:10Z")
                )
        );
        SessionContext reference = new SessionContext(
                SessionId.create(),
                List.of(
                        message(MessageRole.USER, "hello", "2026-04-18T18:01:00Z"),
                        message(MessageRole.ASSISTANT, "reference", "2026-04-18T18:01:05Z")
                )
        );

        SessionDiff diff = SessionContextDiffer.diff(current, reference);

        assertEquals(1, diff.commonPrefixCount());
        assertEquals(List.of("current", "follow-up"), diff.currentOnlyMessages().stream().map(ConversationMessage::content).toList());
        assertEquals(List.of("reference"), diff.referenceOnlyMessages().stream().map(ConversationMessage::content).toList());
        assertTrue(!diff.identical());
    }

    private static ConversationMessage message(MessageRole role, String content, String instant) {
        return new ConversationMessage(
                UUID.randomUUID(),
                role,
                content,
                Instant.parse(instant)
        );
    }
}
