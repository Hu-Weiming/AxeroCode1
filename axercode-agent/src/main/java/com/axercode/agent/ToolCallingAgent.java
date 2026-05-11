package com.axercode.agent;

import com.axercode.core.provider.ProviderRequest;
import com.axercode.core.provider.ProviderResponse;
import com.axercode.core.provider.ProviderStopReason;
import com.axercode.core.provider.ProviderToolRound;
import com.axercode.core.session.ConversationMessage;
import com.axercode.core.session.MessageRole;
import com.axercode.core.session.SessionContext;
import com.axercode.core.session.SessionContextWindow;
import com.axercode.core.tool.ToolExecutionResult;
import com.axercode.core.tool.ToolExecutionStatus;
import com.axercode.provider.api.LlmProvider;
import com.axercode.tools.execution.ToolExecutor;
import com.axercode.tools.registry.ToolRegistry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Lightweight iterative agent loop with bounded tool rounds.
 */
public class ToolCallingAgent implements ConversationAgent {

    private static final Set<String> FILESYSTEM_MUTATION_TOOL_NAMES = Set.of(
            "create_directory",
            "write_file",
            "run_shell"
    );

    private final LlmProvider llmProvider;
    private final ToolExecutor toolExecutor;
    private final ToolRegistry toolRegistry;
    private final int maxToolRounds;
    private final SessionContextWindow sessionContextWindow;
    private final int maxReflectionRounds;

    public ToolCallingAgent(LlmProvider llmProvider, ToolExecutor toolExecutor, ToolRegistry toolRegistry) {
        this(llmProvider, toolExecutor, toolRegistry, 3, new SessionContextWindow(12), 1);
    }

    public ToolCallingAgent(
            LlmProvider llmProvider,
            ToolExecutor toolExecutor,
            ToolRegistry toolRegistry,
            int maxToolRounds
    ) {
        this(llmProvider, toolExecutor, toolRegistry, maxToolRounds, new SessionContextWindow(12), 1);
    }

    public ToolCallingAgent(
            LlmProvider llmProvider,
            ToolExecutor toolExecutor,
            ToolRegistry toolRegistry,
            int maxToolRounds,
            SessionContextWindow sessionContextWindow
    ) {
        this(llmProvider, toolExecutor, toolRegistry, maxToolRounds, sessionContextWindow, 1);
    }

    public ToolCallingAgent(
            LlmProvider llmProvider,
            ToolExecutor toolExecutor,
            ToolRegistry toolRegistry,
            int maxToolRounds,
            SessionContextWindow sessionContextWindow,
            int maxReflectionRounds
    ) {
        this.llmProvider = llmProvider;
        this.toolExecutor = toolExecutor;
        this.toolRegistry = toolRegistry;
        if (maxToolRounds < 1) {
            throw new IllegalArgumentException("maxToolRounds must be at least 1");
        }
        if (maxReflectionRounds < 0) {
            throw new IllegalArgumentException("maxReflectionRounds must not be negative");
        }
        this.maxToolRounds = maxToolRounds;
        this.sessionContextWindow = sessionContextWindow;
        this.maxReflectionRounds = maxReflectionRounds;
    }

    @Override
    public AgentConversationTurn continueConversation(SessionContext sessionContext, String prompt, String model) {
        return continueConversation(sessionContext, prompt, model, TurnInterruptionSource.none());
    }

    @Override
    public AgentConversationTurn continueConversation(
            SessionContext sessionContext,
            String prompt,
            String model,
            TurnInterruptionSource turnInterruptionSource
    ) {
        if (turnInterruptionSource == null) {
            throw new IllegalArgumentException("turnInterruptionSource must not be null");
        }
        return continueConversationInternal(sessionContext, prompt, model, null, turnInterruptionSource);
    }

    @Override
    public AgentConversationTurn continueConversationStreaming(
            SessionContext sessionContext,
            String prompt,
            String model,
            Consumer<String> textDeltaConsumer
    ) {
        return continueConversationStreaming(
                sessionContext,
                prompt,
                model,
                textDeltaConsumer,
                TurnInterruptionSource.none()
        );
    }

    @Override
    public AgentConversationTurn continueConversationStreaming(
            SessionContext sessionContext,
            String prompt,
            String model,
            Consumer<String> textDeltaConsumer,
            TurnInterruptionSource turnInterruptionSource
    ) {
        if (textDeltaConsumer == null) {
            throw new IllegalArgumentException("textDeltaConsumer must not be null");
        }
        if (turnInterruptionSource == null) {
            throw new IllegalArgumentException("turnInterruptionSource must not be null");
        }
        return continueConversationInternal(sessionContext, prompt, model, textDeltaConsumer, turnInterruptionSource);
    }

    private AgentConversationTurn continueConversationInternal(
            SessionContext sessionContext,
            String prompt,
            String model,
            Consumer<String> textDeltaConsumer,
            TurnInterruptionSource turnInterruptionSource
    ) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("prompt must not be blank");
        }
        if (sessionContext == null) {
            throw new IllegalArgumentException("sessionContext must not be null");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("model must not be blank");
        }

        SessionContext withUserMessage = sessionContext.append(ConversationMessage.user(prompt));
        List<ToolExecutionResult> toolResults = new ArrayList<>();
        SessionContext currentSession = withUserMessage;
        int completedToolRounds = 0;
        int usedReflectionRounds = 0;
        boolean reflectOnNextRound = false;
        ProviderToolRound recentToolRound = null;

        while (true) {
            if (completedToolRounds > 0) {
                currentSession = appendPendingMessages(currentSession, turnInterruptionSource);
            }
            boolean injectReflection = reflectOnNextRound && usedReflectionRounds < maxReflectionRounds;
            ProviderResponse response = requestProviderRound(
                    currentSession,
                    model,
                    completedToolRounds == 0 ? textDeltaConsumer : null,
                    injectReflection,
                    recentToolRound
            );
            recentToolRound = null;
            if (injectReflection) {
                usedReflectionRounds++;
            }

            if (response.stopReason() != ProviderStopReason.TOOL_CALLS) {
                return completeTurn(currentSession, response.content(), toolResults);
            }

            if (completedToolRounds >= maxToolRounds) {
                return completeTurn(
                        currentSession,
                        "[AxerCode] Reached max tool rounds (" + maxToolRounds + ").",
                        toolResults
                );
            }

            boolean hadToolFailure = false;
            List<ToolExecutionResult> roundResults = new ArrayList<>();
            for (var toolCall : response.toolCalls()) {
                ToolExecutionResult result = toolExecutor.execute(toolCall);
                toolResults.add(result);
                roundResults.add(result);
                hadToolFailure = hadToolFailure || result.status() == ToolExecutionStatus.FAILURE;
                currentSession = currentSession.append(new ConversationMessage(
                        UUID.randomUUID(),
                        MessageRole.TOOL,
                        formatToolObservation(result),
                        Instant.now()
                ));
            }
            reflectOnNextRound = hadToolFailure;
            recentToolRound = new ProviderToolRound(response.toolCalls(), roundResults);
            completedToolRounds++;
        }
    }

    private ProviderResponse requestProviderRound(
            SessionContext currentSession,
            String model,
            Consumer<String> textDeltaConsumer,
            boolean injectReflection,
            ProviderToolRound recentToolRound
    ) {
        SessionContext providerSession = maybeInjectReflection(
                maybeInjectFilesystemMutationSafety(sessionContextWindow.trim(currentSession)),
                injectReflection
        );

        if (textDeltaConsumer != null && llmProvider.supportsStreaming()) {
            return llmProvider.streamGenerate(ProviderRequest.create(
                    model,
                    providerSession.messages(),
                    toolRegistry.availableTools(),
                    true,
                    recentToolRound
            ), textDeltaConsumer);
        }

        return llmProvider.generate(ProviderRequest.create(
                model,
                providerSession.messages(),
                toolRegistry.availableTools(),
                false,
                recentToolRound
        ));
    }

    private SessionContext maybeInjectReflection(SessionContext providerSession, boolean injectReflection) {
        if (!injectReflection) {
            return providerSession;
        }

        List<ConversationMessage> messages = new ArrayList<>();
        messages.add(new ConversationMessage(
                UUID.randomUUID(),
                MessageRole.SYSTEM,
                "A previous tool call failed. Read the TOOL failure output carefully. Either issue a corrected tool call or answer directly without repeating the same failed action.",
                Instant.now()
        ));
        messages.addAll(providerSession.messages());
        return new SessionContext(providerSession.sessionId(), messages);
    }

    private SessionContext maybeInjectFilesystemMutationSafety(SessionContext providerSession) {
        if (!hasFilesystemMutationTools()) {
            return providerSession;
        }

        List<ConversationMessage> messages = new ArrayList<>();
        messages.add(new ConversationMessage(
                UUID.randomUUID(),
                MessageRole.SYSTEM,
                "For any filesystem mutation such as creating directories, writing files, editing files, renaming paths, or deleting content, you must use tools and may only claim completion after a successful tool result. If no mutation tool succeeded yet, explicitly say the change has not been applied. Prefer create_directory and write_file over generic shell commands when they cover the task.",
                Instant.now()
        ));
        messages.addAll(providerSession.messages());
        return new SessionContext(providerSession.sessionId(), messages);
    }

    private boolean hasFilesystemMutationTools() {
        return toolRegistry.availableTools().stream()
                .map(tool -> tool.name().toLowerCase())
                .anyMatch(FILESYSTEM_MUTATION_TOOL_NAMES::contains);
    }

    private SessionContext appendPendingMessages(
            SessionContext currentSession,
            TurnInterruptionSource turnInterruptionSource
    ) {
        List<ConversationMessage> pendingMessages = turnInterruptionSource.drainPendingMessages();
        if (pendingMessages == null || pendingMessages.isEmpty()) {
            return currentSession;
        }

        SessionContext updatedSession = currentSession;
        for (ConversationMessage pendingMessage : pendingMessages) {
            if (pendingMessage == null) {
                continue;
            }
            updatedSession = updatedSession.append(pendingMessage);
        }
        return updatedSession;
    }

    private AgentConversationTurn completeTurn(
            SessionContext sessionContext,
            String reply,
            List<ToolExecutionResult> toolResults
    ) {
        SessionContext updatedSession = sessionContext.append(new ConversationMessage(
                UUID.randomUUID(),
                MessageRole.ASSISTANT,
                reply,
                Instant.now()
        ));
        return new AgentConversationTurn(updatedSession, reply, toolResults);
    }

    private String formatToolObservation(ToolExecutionResult result) {
        return "TOOL " + result.toolCall().name() + " [" + result.status().name() + "]\n" + result.output();
    }
}
