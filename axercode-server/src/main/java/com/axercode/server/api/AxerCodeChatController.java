package com.axercode.server.api;

import com.axercode.server.service.ServerConversationService;
import com.axercode.server.service.ServerConversationTurn;
import com.axercode.server.service.PreparedStreamingSession;
import com.axercode.server.service.ServerSessionSummary;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api")
public class AxerCodeChatController {

    private final ServerConversationService conversationService;
    private final TaskExecutor streamingExecutor;

    @Autowired
    public AxerCodeChatController(ServerConversationService conversationService) {
        this(conversationService, defaultStreamingExecutor());
    }

    AxerCodeChatController(ServerConversationService conversationService, TaskExecutor streamingExecutor) {
        this.conversationService = conversationService;
        this.streamingExecutor = streamingExecutor;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("ok", "axercode-server");
    }

    @GetMapping(path = "/sessions", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ServerSessionSummary> sessions(@RequestParam(defaultValue = "20") int limit) {
        return conversationService.listSessions(limit);
    }

    @GetMapping(path = "/providers", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ProviderOptionResponse> providers() {
        return conversationService.providers().stream()
                .map(ProviderOptionResponse::from)
                .toList();
    }

    @GetMapping(path = "/shell-state", produces = MediaType.APPLICATION_JSON_VALUE)
    public ShellStateResponse shellState() {
        return ShellStateResponse.from(conversationService.loadShellState());
    }

    @PostMapping(path = "/shell-state/plan-mode", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ShellStateResponse setPlanMode(@RequestBody PlanModeRequest request) {
        return ShellStateResponse.from(conversationService.setPlanModeEnabled(request.enabled()));
    }

    @PostMapping(path = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ChatResponse chat(@RequestBody ChatRequest request) {
        ServerConversationTurn turn = conversationService.chat(
                request.prompt(),
                request.sessionId(),
                request.provider(),
                request.model()
        );
        return ChatResponse.from(turn);
    }

    @PostMapping(path = "/chat/stream", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody ChatRequest request) {
        PreparedStreamingSession preparedSession = conversationService.prepareStreamingSession(
                request.sessionId(),
                request.provider(),
                request.model()
        );
        SseEmitter emitter = new SseEmitter(0L);
        streamingExecutor.execute(() -> {
            try {
                sendEvent(emitter, "session", new SessionStartedResponse(preparedSession.sessionId()));
                ServerConversationTurn turn = conversationService.chatStreaming(
                        preparedSession,
                        request.prompt(),
                        delta -> sendEvent(emitter, "token", delta)
                );
                sendEvent(emitter, "complete", ChatResponse.from(turn));
                emitter.complete();
            } catch (RuntimeException exception) {
                try {
                    sendEvent(emitter, "error", new StreamErrorResponse(messageOf(exception)));
                    emitter.complete();
                } catch (RuntimeException sendFailure) {
                    emitter.completeWithError(sendFailure);
                }
            } finally {
                conversationService.finishStreamingSession(preparedSession.sessionId());
            }
        });
        return emitter;
    }

    @PostMapping(path = "/chat/btw", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public BtwResponse queueBtw(@RequestBody BtwRequest request) {
        try {
            return new BtwResponse(
                    request.sessionId(),
                    conversationService.queueBtwMessage(request.sessionId(), request.message())
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(BAD_REQUEST, exception.getMessage(), exception);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(CONFLICT, exception.getMessage(), exception);
        }
    }

    @GetMapping(path = "/sessions/{sessionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public SessionResponse session(@PathVariable String sessionId) {
        return SessionResponse.from(conversationService.loadSession(sessionId));
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to send SSE event: " + eventName, exception);
        }
    }

    private static TaskExecutor defaultStreamingExecutor() {
        return new SimpleAsyncTaskExecutor("axercode-sse-");
    }

    private String messageOf(RuntimeException exception) {
        if (exception.getMessage() != null && !exception.getMessage().isBlank()) {
            return exception.getMessage();
        }
        if (exception.getCause() != null
                && exception.getCause().getMessage() != null
                && !exception.getCause().getMessage().isBlank()) {
            return exception.getCause().getMessage();
        }
        return "The request did not complete.";
    }
}
