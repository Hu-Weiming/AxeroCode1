package com.axercode.server.api;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.axercode.agent.TurnInterruptionSource;
import com.axercode.core.session.ConversationMessage;
import com.axercode.core.session.MessageRole;
import com.axercode.core.session.SessionContext;
import com.axercode.provider.api.ProviderDescriptor;
import com.axercode.provider.api.ProviderIds;
import com.axercode.server.service.PreparedStreamingSession;
import com.axercode.server.service.ServerConversationService;
import com.axercode.server.service.ServerConversationTurn;
import com.axercode.server.service.ServerShellState;
import com.axercode.server.service.ServerSessionSummary;
import com.axercode.server.service.ServerStoredSession;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AxerCodeChatControllerTest {

    @Test
    void healthEndpointReturnsOk() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AxerCodeChatController(Mockito.mock(ServerConversationService.class)))
                .build();

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.application").value("axercode-server"));
    }

    @Test
    void providersEndpointReturnsConfiguredProviderCatalog() throws Exception {
        ServerConversationService service = Mockito.mock(ServerConversationService.class);
        when(service.providers()).thenReturn(List.of(
                new ProviderDescriptor(ProviderIds.OLLAMA, "Ollama", "qwen2.5:7b", true, false, true),
                new ProviderDescriptor(ProviderIds.ANTHROPIC, "Anthropic", "claude-3-5-sonnet-latest", false, false, true),
                new ProviderDescriptor(ProviderIds.OPENAI, "OpenAI-compatible", "gpt-4o-mini", false, true, true)
        ));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AxerCodeChatController(service)).build();

        mockMvc.perform(get("/api/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("ollama"))
                .andExpect(jsonPath("$[1].id").value("anthropic"))
                .andExpect(jsonPath("$[2].comingSoon").value(true));
    }

    @Test
    void chatEndpointReturnsJsonTurn() throws Exception {
        ServerConversationService service = Mockito.mock(ServerConversationService.class);
        SessionContext session = SessionContext.start()
                .append(ConversationMessage.user("hello"))
                .append(new ConversationMessage(UUID.randomUUID(), MessageRole.ASSISTANT, "reply", Instant.now()));
        when(service.chat("hello", null, "anthropic", "claude-3-5-sonnet-latest"))
                .thenReturn(new ServerConversationTurn(session, "reply", List.of(), "anthropic", "claude-3-5-sonnet-latest"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AxerCodeChatController(service)).build();

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prompt":"hello","provider":"anthropic","model":"claude-3-5-sonnet-latest"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(session.sessionId().toString()))
                .andExpect(jsonPath("$.provider").value("anthropic"))
                .andExpect(jsonPath("$.model").value("claude-3-5-sonnet-latest"))
                .andExpect(jsonPath("$.reply").value("reply"))
                .andExpect(jsonPath("$.toolResults").isArray());
    }

    @Test
    void streamEndpointReturnsTokenAndCompleteEvents() throws Exception {
        ServerConversationService service = Mockito.mock(ServerConversationService.class);
        SessionContext session = SessionContext.start()
                .append(ConversationMessage.user("hello"))
                .append(new ConversationMessage(UUID.randomUUID(), MessageRole.ASSISTANT, "stream-reply", Instant.now()));
        PreparedStreamingSession preparedSession = new PreparedStreamingSession(
                session,
                "anthropic",
                "claude-3-5-sonnet-latest",
                "anthropic::claude-3-5-sonnet-latest",
                TurnInterruptionSource.none()
        );
        when(service.prepareStreamingSession(null, "anthropic", "claude-3-5-sonnet-latest")).thenReturn(preparedSession);
        doAnswer(invocation -> {
            Consumer<String> consumer = invocation.getArgument(2);
            consumer.accept("stream-");
            consumer.accept("reply");
            return new ServerConversationTurn(session, "stream-reply", List.of(), "anthropic", "claude-3-5-sonnet-latest");
        }).when(service).chatStreaming(eq(preparedSession), eq("hello"), any());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AxerCodeChatController(service)).build();

        MvcResult mvcResult = mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prompt":"hello","provider":"anthropic","model":"claude-3-5-sonnet-latest"}
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();
        mvcResult.getAsyncResult(5_000);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("event:session")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("event:token")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("event:complete")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("stream-reply")));
    }

    @Test
    void streamEndpointDoesNotWaitForConversationCompletionBeforeReturningEmitter() throws Exception {
        ServerConversationService service = Mockito.mock(ServerConversationService.class);
        SessionContext session = SessionContext.start()
                .append(ConversationMessage.user("hello"))
                .append(new ConversationMessage(UUID.randomUUID(), MessageRole.ASSISTANT, "stream-reply", Instant.now()));
        PreparedStreamingSession preparedSession = new PreparedStreamingSession(
                session,
                "ollama",
                "qwen2.5:7b",
                "ollama::qwen2.5:7b",
                TurnInterruptionSource.none()
        );
        CountDownLatch enteredStreamingCall = new CountDownLatch(1);
        CountDownLatch allowCompletion = new CountDownLatch(1);

        when(service.prepareStreamingSession(null, null, null)).thenReturn(preparedSession);
        doAnswer(invocation -> {
            Consumer<String> consumer = invocation.getArgument(2);
            enteredStreamingCall.countDown();
            consumer.accept("stream-");
            assertTrue(allowCompletion.await(2, TimeUnit.SECONDS), "test should release mocked streaming call");
            consumer.accept("reply");
            return new ServerConversationTurn(session, "stream-reply", List.of(), "ollama", "qwen2.5:7b");
        }).when(service).chatStreaming(eq(preparedSession), eq("hello"), any());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AxerCodeChatController(service)).build();

        CompletableFuture<MvcResult> future = CompletableFuture.supplyAsync(() -> {
            try {
                return mockMvc.perform(post("/api/chat/stream")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"prompt":"hello"}
                                        """))
                        .andExpect(request().asyncStarted())
                        .andReturn();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });

        assertTrue(enteredStreamingCall.await(1, TimeUnit.SECONDS), "mocked streaming call should start");
        Thread.sleep(150);
        assertTrue(future.isDone(), "controller should return the emitter before streaming finishes");

        allowCompletion.countDown();
        MvcResult mvcResult = future.get(2, TimeUnit.SECONDS);
        mvcResult.getAsyncResult(5_000);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("stream-reply")));
    }

    @Test
    void streamEndpointReturnsStructuredErrorEvent() throws Exception {
        ServerConversationService service = Mockito.mock(ServerConversationService.class);
        SessionContext session = SessionContext.start()
                .append(ConversationMessage.user("hello"));
        PreparedStreamingSession preparedSession = new PreparedStreamingSession(
                session,
                "openai",
                "gpt-4o-mini",
                "openai::gpt-4o-mini",
                TurnInterruptionSource.none()
        );

        when(service.prepareStreamingSession(null, "openai", "gpt-4o-mini")).thenReturn(preparedSession);
        doAnswer(invocation -> {
            throw new IllegalStateException("当前功能开发难度较大，目前正在加急修复中。");
        }).when(service).chatStreaming(eq(preparedSession), eq("hello"), any());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AxerCodeChatController(service)).build();

        MvcResult mvcResult = mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prompt":"hello","provider":"openai","model":"gpt-4o-mini"}
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();
        mvcResult.getAsyncResult(5_000);

        MvcResult dispatched = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andReturn();
        String body = new String(dispatched.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);

        assertTrue(body.contains("event:error"));
        assertTrue(body.contains("当前功能开发难度较大，目前正在加急修复中。"));
    }

    @Test
    void sessionEndpointReturnsStoredSession() throws Exception {
        ServerConversationService service = Mockito.mock(ServerConversationService.class);
        SessionContext session = SessionContext.start()
                .append(ConversationMessage.user("hello"))
                .append(new ConversationMessage(UUID.randomUUID(), MessageRole.ASSISTANT, "reply", Instant.now()));
        when(service.loadSession(session.sessionId().toString()))
                .thenReturn(new ServerStoredSession(session, "anthropic", "claude-3-5-sonnet-latest"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AxerCodeChatController(service)).build();

        mockMvc.perform(get("/api/sessions/{sessionId}", session.sessionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(session.sessionId().toString()))
                .andExpect(jsonPath("$.provider").value("anthropic"))
                .andExpect(jsonPath("$.model").value("claude-3-5-sonnet-latest"))
                .andExpect(jsonPath("$.messageCount").value(2))
                .andExpect(jsonPath("$.messages[0].content").value("hello"))
                .andExpect(jsonPath("$.messages[1].role").value("ASSISTANT"))
                .andExpect(jsonPath("$.messages[1].content").value("reply"))
                .andExpect(jsonPath("$.messages[1].model").value("claude-3-5-sonnet-latest"));
    }

    @Test
    void sessionsEndpointReturnsSidebarSessionSummaries() throws Exception {
        ServerConversationService service = Mockito.mock(ServerConversationService.class);
        when(service.listSessions(5)).thenReturn(List.of(
                new ServerSessionSummary(
                        "session-2",
                        "Build the sidebar",
                        "Connected the history list.",
                        "2026-04-18T21:00:00Z",
                        4
                ),
                new ServerSessionSummary(
                        "session-1",
                        "Refresh the layout",
                        "Outlined the new structure.",
                        "2026-04-18T20:55:00Z",
                        2
                )
        ));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AxerCodeChatController(service)).build();

        mockMvc.perform(get("/api/sessions").queryParam("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sessionId").value("session-2"))
                .andExpect(jsonPath("$[0].title").value("Build the sidebar"))
                .andExpect(jsonPath("$[0].preview").value("Connected the history list."))
                .andExpect(jsonPath("$[0].messageCount").value(4))
                .andExpect(jsonPath("$[1].sessionId").value("session-1"));
    }

    @Test
    void shellStateEndpointReturnsPlanModeAndFocusMetadata() throws Exception {
        ServerConversationService service = Mockito.mock(ServerConversationService.class);
        when(service.loadShellState()).thenReturn(new ServerShellState(
                true,
                "D:\\AeroCode1",
                "alpha",
                "feature-a",
                2
        ));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AxerCodeChatController(service)).build();

        mockMvc.perform(get("/api/shell-state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planModeEnabled").value(true))
                .andExpect(jsonPath("$.focusPath").value("D:\\AeroCode1"))
                .andExpect(jsonPath("$.activeCheckpointName").value("alpha"))
                .andExpect(jsonPath("$.activeBranchName").value("feature-a"))
                .andExpect(jsonPath("$.checkpointCount").value(2));
    }

    @Test
    void planModeEndpointUpdatesPlanModeState() throws Exception {
        ServerConversationService service = Mockito.mock(ServerConversationService.class);
        when(service.setPlanModeEnabled(true)).thenReturn(new ServerShellState(
                true,
                null,
                null,
                null,
                0
        ));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AxerCodeChatController(service)).build();

        mockMvc.perform(post("/api/shell-state/plan-mode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planModeEnabled").value(true));
    }

    @Test
    void btwEndpointQueuesInterjectionForTheActiveTurn() throws Exception {
        ServerConversationService service = Mockito.mock(ServerConversationService.class);
        when(service.queueBtwMessage("session-1", "one more thing")).thenReturn(1);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AxerCodeChatController(service)).build();

        mockMvc.perform(post("/api/chat/btw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sessionId":"session-1","message":"one more thing"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("session-1"))
                .andExpect(jsonPath("$.queuedCount").value(1));
    }
}
