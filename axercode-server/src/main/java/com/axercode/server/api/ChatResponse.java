package com.axercode.server.api;

import com.axercode.server.service.ServerConversationTurn;
import java.util.List;

public record ChatResponse(
        String sessionId,
        String provider,
        String model,
        String reply,
        List<ToolResultResponse> toolResults
) {

    public static ChatResponse from(ServerConversationTurn turn) {
        return new ChatResponse(
                turn.sessionContext().sessionId().toString(),
                turn.provider(),
                turn.model(),
                turn.reply(),
                turn.toolResults().stream().map(ToolResultResponse::from).toList()
        );
    }
}
