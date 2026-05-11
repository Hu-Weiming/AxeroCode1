package com.axercode.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axercode.core.provider.ProviderRequest;
import com.axercode.core.provider.ProviderResponse;
import com.axercode.core.session.ConversationMessage;
import com.axercode.core.session.SessionContext;
import com.axercode.core.session.SessionContextWindow;
import com.axercode.core.session.SessionId;
import com.axercode.core.tool.ToolCall;
import com.axercode.core.tool.ToolDefinition;
import com.axercode.core.tool.ToolExecutionResult;
import com.axercode.core.tool.ToolExecutionStatus;
import com.axercode.provider.api.LlmProvider;
import com.axercode.tools.AxerTool;
import com.axercode.tools.execution.ToolExecutor;
import com.axercode.tools.registry.ToolRegistry;
import java.util.ArrayList;
import java.util.List;
import java.time.Instant;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class ToolCallingAgentTest {

    @Test
    void continueConversationReturnsSingleProviderReplyWhenNoToolsAreRequested() {
        RecordingProvider provider = new RecordingProvider(List.of(
                ProviderResponse.complete("plain reply")
        ));
        ToolExecutor toolExecutor = new ToolExecutor(new ToolRegistry(List.of()));
        ToolCallingAgent agent = new ToolCallingAgent(provider, toolExecutor, new ToolRegistry(List.of()));

        AgentConversationTurn turn = agent.continueConversation(SessionContext.start(), "hello", "qwen2.5:7b");

        assertEquals(1, provider.requests().size());
        assertEquals(0, provider.requests().getFirst().availableTools().size());
        assertEquals("plain reply", turn.reply());
        assertEquals(2, turn.sessionContext().messages().size());
        assertEquals(0, turn.toolResults().size());
    }

    @Test
    void continueConversationExecutesToolCallsAndRequestsFinalReply() {
        RecordingProvider provider = new RecordingProvider(List.of(
                ProviderResponse.toolCalls(List.of(ToolCall.create("read_file", "{\"path\":\"README.md\"}"))),
                ProviderResponse.complete("final answer after tool")
        ));
        ToolRegistry toolRegistry = new ToolRegistry(List.of(new StubTool("read_file", "README.md contents")));
        ToolExecutor toolExecutor = new ToolExecutor(toolRegistry);
        ToolCallingAgent agent = new ToolCallingAgent(provider, toolExecutor, toolRegistry);

        AgentConversationTurn turn = agent.continueConversation(SessionContext.start(), "inspect the repo", "qwen2.5:7b");

        assertEquals(2, provider.requests().size());
        assertEquals(1, provider.requests().getFirst().availableTools().size());
        assertEquals("read_file", provider.requests().getFirst().availableTools().getFirst().name());
        assertEquals(1, turn.toolResults().size());
        assertEquals(ToolExecutionStatus.SUCCESS, turn.toolResults().getFirst().status());
        assertEquals("final answer after tool", turn.reply());
        assertEquals(3, turn.sessionContext().messages().size());
        assertEquals("inspect the repo", turn.sessionContext().messages().get(0).content());
        assertEquals("TOOL read_file [SUCCESS]\nREADME.md contents", turn.sessionContext().messages().get(1).content());
        assertEquals("final answer after tool", turn.sessionContext().messages().get(2).content());
        assertEquals("TOOL read_file [SUCCESS]\nREADME.md contents", provider.requests().get(1).messages().get(1).content());
    }

    @Test
    void continueConversationSupportsMultipleToolRoundsBeforeCompletion() {
        RecordingProvider provider = new RecordingProvider(List.of(
                ProviderResponse.toolCalls(List.of(ToolCall.create("read_file", "{\"path\":\"README.md\"}"))),
                ProviderResponse.toolCalls(List.of(ToolCall.create("list_directory", "{\"path\":\".\"}"))),
                ProviderResponse.complete("final answer after two rounds")
        ));
        ToolExecutor toolExecutor = new ToolExecutor(new ToolRegistry(List.of(
                new StubTool("read_file", "README.md contents"),
                new StubTool("list_directory", "[FILE] README.md")
        )));
        ToolRegistry toolRegistry = new ToolRegistry(List.of(
                new StubTool("read_file", "README.md contents"),
                new StubTool("list_directory", "[FILE] README.md")
        ));
        toolExecutor = new ToolExecutor(toolRegistry);
        ToolCallingAgent agent = new ToolCallingAgent(provider, toolExecutor, toolRegistry, 3);

        AgentConversationTurn turn = agent.continueConversation(SessionContext.start(), "inspect again", "qwen2.5:7b");

        assertEquals(3, provider.requests().size());
        assertEquals(2, turn.toolResults().size());
        assertEquals("final answer after two rounds", turn.reply());
        assertEquals(4, turn.sessionContext().messages().size());
        assertEquals("TOOL read_file [SUCCESS]\nREADME.md contents", provider.requests().get(1).messages().get(1).content());
        assertEquals("TOOL list_directory [SUCCESS]\n[FILE] README.md", provider.requests().get(2).messages().get(2).content());
    }

    @Test
    void continueConversationPreservesToolFailureObservationAndContinuesLoop() {
        RecordingProvider provider = new RecordingProvider(List.of(
                ProviderResponse.toolCalls(List.of(ToolCall.create("missing_tool", "{}"))),
                ProviderResponse.complete("handled the failure")
        ));
        ToolExecutor toolExecutor = new ToolExecutor(new ToolRegistry(List.of()));
        ToolCallingAgent agent = new ToolCallingAgent(provider, toolExecutor, new ToolRegistry(List.of()), 3);

        AgentConversationTurn turn = agent.continueConversation(SessionContext.start(), "try a missing tool", "qwen2.5:7b");

        assertEquals(2, provider.requests().size());
        assertEquals(ToolExecutionStatus.FAILURE, turn.toolResults().getFirst().status());
        assertEquals("Unknown tool: missing_tool", turn.toolResults().getFirst().output());
        assertTrue(provider.requests().get(1).messages().stream()
                .anyMatch(message -> "TOOL missing_tool [FAILURE]\nUnknown tool: missing_tool".equals(message.content())));
        assertEquals("handled the failure", turn.reply());
    }

    @Test
    void continueConversationStopsWhenMaximumToolRoundsIsReached() {
        RecordingProvider provider = new RecordingProvider(List.of(
                ProviderResponse.toolCalls(List.of(ToolCall.create("read_file", "{\"path\":\"README.md\"}"))),
                ProviderResponse.toolCalls(List.of(ToolCall.create("list_directory", "{\"path\":\".\"}")))
        ));
        ToolExecutor toolExecutor = new ToolExecutor(new ToolRegistry(List.of(
                new StubTool("read_file", "README.md contents"),
                new StubTool("list_directory", "[FILE] README.md")
        )));
        ToolRegistry toolRegistry = new ToolRegistry(List.of(
                new StubTool("read_file", "README.md contents"),
                new StubTool("list_directory", "[FILE] README.md")
        ));
        toolExecutor = new ToolExecutor(toolRegistry);
        ToolCallingAgent agent = new ToolCallingAgent(provider, toolExecutor, toolRegistry, 1);

        AgentConversationTurn turn = agent.continueConversation(SessionContext.start(), "inspect until stop", "qwen2.5:7b");

        assertEquals(2, provider.requests().size());
        assertEquals(1, turn.toolResults().size());
        assertEquals("[AxerCode] Reached max tool rounds (1).", turn.reply());
        assertEquals("[AxerCode] Reached max tool rounds (1).", turn.sessionContext().messages().getLast().content());
    }

    @Test
    void continueConversationTrimsProviderRequestButKeepsFullSessionHistory() {
        RecordingProvider provider = new RecordingProvider(List.of(
                ProviderResponse.complete("trimmed reply")
        ));
        ToolExecutor toolExecutor = new ToolExecutor(new ToolRegistry(List.of()));
        ToolCallingAgent agent = new ToolCallingAgent(
                provider,
                toolExecutor,
                new ToolRegistry(List.of()),
                3,
                new SessionContextWindow(3)
        );
        SessionContext existing = new SessionContext(
                SessionId.create(),
                List.of(
                        message(com.axercode.core.session.MessageRole.SYSTEM, "Focus path"),
                        message(com.axercode.core.session.MessageRole.USER, "u1"),
                        message(com.axercode.core.session.MessageRole.ASSISTANT, "a1"),
                        message(com.axercode.core.session.MessageRole.USER, "u2"),
                        message(com.axercode.core.session.MessageRole.ASSISTANT, "a2")
                )
        );

        AgentConversationTurn turn = agent.continueConversation(existing, "u3", "qwen2.5:7b");

        assertEquals(1, provider.requests().size());
        assertEquals(List.of("Focus path", "u2", "a2", "u3"),
                provider.requests().getFirst().messages().stream().map(message -> message.content()).toList());
        assertEquals(7, turn.sessionContext().messages().size());
        assertEquals("u1", turn.sessionContext().messages().get(1).content());
        assertEquals("trimmed reply", turn.reply());
    }

    @Test
    void continueConversationTrimsLaterToolRoundsButKeepsRecentToolObservation() {
        RecordingProvider provider = new RecordingProvider(List.of(
                ProviderResponse.toolCalls(List.of(ToolCall.create("read_file", "{\"path\":\"README.md\"}"))),
                ProviderResponse.complete("after tool")
        ));
        ToolRegistry toolRegistry = new ToolRegistry(List.of(new StubTool("read_file", "README.md contents")));
        ToolExecutor toolExecutor = new ToolExecutor(toolRegistry);
        ToolCallingAgent agent = new ToolCallingAgent(
                provider,
                toolExecutor,
                toolRegistry,
                3,
                new SessionContextWindow(2)
        );
        SessionContext existing = new SessionContext(
                SessionId.create(),
                List.of(
                        message(com.axercode.core.session.MessageRole.SYSTEM, "Focus path"),
                        message(com.axercode.core.session.MessageRole.USER, "u1"),
                        message(com.axercode.core.session.MessageRole.ASSISTANT, "a1"),
                        message(com.axercode.core.session.MessageRole.USER, "u2"),
                        message(com.axercode.core.session.MessageRole.ASSISTANT, "a2")
                )
        );

        AgentConversationTurn turn = agent.continueConversation(existing, "u3", "qwen2.5:7b");

        assertEquals(2, provider.requests().size());
        assertEquals(List.of("Focus path", "a2", "u3"),
                provider.requests().getFirst().messages().stream().map(message -> message.content()).toList());
        assertEquals(List.of("Focus path", "u3", "TOOL read_file [SUCCESS]\nREADME.md contents"),
                provider.requests().get(1).messages().stream().map(message -> message.content()).toList());
        assertEquals("after tool", turn.reply());
    }

    @Test
    void continueConversationStreamingEmitsAssistantDeltasWhenPlainReplyCompletes() {
        RecordingProvider provider = RecordingProvider.streaming(List.of(
                new StreamingScript(List.of("Axer", "Code"), ProviderResponse.complete("AxerCode"))
        ));
        ToolExecutor toolExecutor = new ToolExecutor(new ToolRegistry(List.of()));
        ToolCallingAgent agent = new ToolCallingAgent(provider, toolExecutor, new ToolRegistry(List.of()));
        StringBuilder streamed = new StringBuilder();

        AgentConversationTurn turn = agent.continueConversationStreaming(
                SessionContext.start(),
                "hello",
                "qwen2.5:7b",
                streamed::append
        );

        assertEquals(1, provider.streamRequests().size());
        assertEquals("AxerCode", streamed.toString());
        assertEquals("AxerCode", turn.reply());
    }

    @Test
    void continueConversationStreamingFallsBackToSynchronousFinalReplyAfterToolRound() {
        RecordingProvider provider = RecordingProvider.streamingWithFallback(
                List.of(new StreamingScript(List.of(), ProviderResponse.toolCalls(List.of(
                        ToolCall.create("read_file", "{\"path\":\"README.md\"}")
                )))),
                List.of(ProviderResponse.complete("final answer after tool"))
        );
        ToolRegistry toolRegistry = new ToolRegistry(List.of(new StubTool("read_file", "README.md contents")));
        ToolExecutor toolExecutor = new ToolExecutor(toolRegistry);
        ToolCallingAgent agent = new ToolCallingAgent(provider, toolExecutor, toolRegistry);
        StringBuilder streamed = new StringBuilder();

        AgentConversationTurn turn = agent.continueConversationStreaming(
                SessionContext.start(),
                "inspect",
                "qwen2.5:7b",
                streamed::append
        );

        assertEquals(1, provider.streamRequests().size());
        assertEquals(1, provider.requests().size());
        assertEquals("", streamed.toString());
        assertEquals("final answer after tool", turn.reply());
        assertEquals(1, turn.toolResults().size());
    }

    @Test
    void continueConversationInjectsReflectionGuidanceAfterToolFailure() {
        RecordingProvider provider = new RecordingProvider(List.of(
                ProviderResponse.toolCalls(List.of(ToolCall.create("missing_tool", "{}"))),
                ProviderResponse.complete("recovered after reflection")
        ));
        ToolCallingAgent agent = new ToolCallingAgent(
                provider,
                new ToolExecutor(new ToolRegistry(List.of())),
                new ToolRegistry(List.of()),
                3,
                new SessionContextWindow(12),
                1
        );

        AgentConversationTurn turn = agent.continueConversation(SessionContext.start(), "try again", "qwen2.5:7b");

        assertEquals(2, provider.requests().size());
        assertTrue(provider.requests().get(1).messages().stream()
                .anyMatch(message -> message.content().contains("A previous tool call failed.")));
        assertEquals("recovered after reflection", turn.reply());
    }

    @Test
    void continueConversationStopsInjectingReflectionAfterConfiguredLimit() {
        RecordingProvider provider = new RecordingProvider(List.of(
                ProviderResponse.toolCalls(List.of(ToolCall.create("missing_tool", "{}"))),
                ProviderResponse.toolCalls(List.of(ToolCall.create("missing_tool", "{}"))),
                ProviderResponse.complete("done after repeated failure")
        ));
        ToolCallingAgent agent = new ToolCallingAgent(
                provider,
                new ToolExecutor(new ToolRegistry(List.of())),
                new ToolRegistry(List.of()),
                3,
                new SessionContextWindow(12),
                1
        );

        AgentConversationTurn turn = agent.continueConversation(SessionContext.start(), "keep trying", "qwen2.5:7b");

        assertEquals(3, provider.requests().size());
        assertTrue(provider.requests().get(1).messages().stream()
                .anyMatch(message -> message.content().contains("A previous tool call failed.")));
        assertTrue(provider.requests().get(2).messages().stream()
                .noneMatch(message -> message.content().contains("A previous tool call failed.")));
        assertEquals("done after repeated failure", turn.reply());
    }

    @Test
    void continueConversationAppendsBtwMessagesBeforeTheNextProviderRound() {
        RecordingProvider provider = new RecordingProvider(List.of(
                ProviderResponse.toolCalls(List.of(ToolCall.create("read_file", "{\"path\":\"README.md\"}"))),
                ProviderResponse.complete("final answer after btw")
        ));
        ToolRegistry toolRegistry = new ToolRegistry(List.of(new StubTool("read_file", "README.md contents")));
        ToolExecutor toolExecutor = new ToolExecutor(toolRegistry);
        ToolCallingAgent agent = new ToolCallingAgent(provider, toolExecutor, toolRegistry);
        List<ConversationMessage> injected = new ArrayList<>(List.of(ConversationMessage.user("one more thing")));

        AgentConversationTurn turn = agent.continueConversation(
                SessionContext.start(),
                "inspect the repo",
                "qwen2.5:7b",
                () -> {
                    if (injected.isEmpty()) {
                        return List.of();
                    }
                    List<ConversationMessage> drained = List.copyOf(injected);
                    injected.clear();
                    return drained;
                }
        );

        assertEquals(2, provider.requests().size());
        assertEquals(
                List.of("inspect the repo", "TOOL read_file [SUCCESS]\nREADME.md contents", "one more thing"),
                provider.requests().get(1).messages().stream().map(message -> message.content()).toList()
        );
        assertEquals("final answer after btw", turn.reply());
    }

    @Test
    void continueConversationInjectsFilesystemMutationSafetyGuidanceWhenMutationToolsAreAvailable() {
        RecordingProvider provider = new RecordingProvider(List.of(
                ProviderResponse.complete("plain reply")
        ));
        ToolRegistry toolRegistry = new ToolRegistry(List.of(new StubTool("write_file", "done")));
        ToolExecutor toolExecutor = new ToolExecutor(toolRegistry);
        ToolCallingAgent agent = new ToolCallingAgent(provider, toolExecutor, toolRegistry);

        agent.continueConversation(SessionContext.start(), "create a file", "qwen2.5:7b");

        assertTrue(provider.requests().getFirst().messages().stream()
                .anyMatch(message -> message.content().contains("For any filesystem mutation")));
    }

    private static final class RecordingProvider implements LlmProvider {
        private final List<ProviderResponse> scriptedResponses;
        private final List<StreamingScript> scriptedStreamingResponses;
        private final List<ProviderRequest> requests = new ArrayList<>();
        private final List<ProviderRequest> streamRequests = new ArrayList<>();

        private RecordingProvider(List<ProviderResponse> scriptedResponses) {
            this(scriptedResponses, List.of());
        }

        private RecordingProvider(List<ProviderResponse> scriptedResponses, List<StreamingScript> scriptedStreamingResponses) {
            this.scriptedResponses = new ArrayList<>(scriptedResponses);
            this.scriptedStreamingResponses = new ArrayList<>(scriptedStreamingResponses);
        }

        private static RecordingProvider streaming(List<StreamingScript> scriptedStreamingResponses) {
            return new RecordingProvider(List.of(), scriptedStreamingResponses);
        }

        private static RecordingProvider streamingWithFallback(
                List<StreamingScript> scriptedStreamingResponses,
                List<ProviderResponse> scriptedResponses
        ) {
            return new RecordingProvider(scriptedResponses, scriptedStreamingResponses);
        }

        @Override
        public String providerName() {
            return "recording";
        }

        @Override
        public ProviderResponse generate(ProviderRequest request) {
            requests.add(request);
            return scriptedResponses.get(requests.size() - 1);
        }

        @Override
        public boolean supportsStreaming() {
            return true;
        }

        @Override
        public ProviderResponse streamGenerate(ProviderRequest request, Consumer<String> textDeltaConsumer) {
            streamRequests.add(request);
            StreamingScript script = scriptedStreamingResponses.get(streamRequests.size() - 1);
            for (String delta : script.deltas()) {
                textDeltaConsumer.accept(delta);
            }
            return script.response();
        }

        List<ProviderRequest> requests() {
            return requests;
        }

        List<ProviderRequest> streamRequests() {
            return streamRequests;
        }
    }

    private record StreamingScript(List<String> deltas, ProviderResponse response) {
    }

    private static com.axercode.core.session.ConversationMessage message(
            com.axercode.core.session.MessageRole role,
            String content
    ) {
        return new com.axercode.core.session.ConversationMessage(
                java.util.UUID.randomUUID(),
                role,
                content,
                Instant.now()
        );
    }

    private record StubTool(String name, String output) implements AxerTool {

        @Override
        public ToolDefinition definition() {
            return new ToolDefinition(name, "stub", "{}");
        }

        @Override
        public ToolExecutionResult execute(ToolCall toolCall) {
            return ToolExecutionResult.success(toolCall, output);
        }
    }
}
