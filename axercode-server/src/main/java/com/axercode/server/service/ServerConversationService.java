package com.axercode.server.service;

import com.axercode.agent.AgentConversationTurn;
import com.axercode.agent.ConversationAgent;
import com.axercode.agent.TurnInterruptionSource;
import com.axercode.core.session.ConversationMessage;
import com.axercode.core.session.SessionContext;
import com.axercode.core.session.SessionId;
import com.axercode.provider.api.LlmProvider;
import com.axercode.provider.api.LlmProviderRegistry;
import com.axercode.provider.api.ProviderDescriptor;
import com.axercode.provider.api.ProviderIds;
import com.axercode.provider.api.ProviderModelRef;
import com.axercode.provider.api.ProviderException;
import com.axercode.provider.api.RegisteredLlmProvider;
import com.axercode.storage.sqlite.shell.SqliteShellStateRepository;
import com.axercode.storage.sqlite.session.SqliteSessionRepository;
import com.axercode.storage.sqlite.session.StoredSessionRuntime;
import com.axercode.storage.sqlite.session.StoredSessionSummary;
import java.util.Objects;
import java.util.List;
import java.util.function.Consumer;

/**
 * Server-local orchestration layer for session loading, model resolution, agent execution, and persistence.
 */
public class ServerConversationService {

    private final ConversationAgent conversationAgent;
    private final SqliteSessionRepository sessionRepository;
    private final LlmProviderRegistry providerRegistry;
    private final String defaultModel;
    private final ServerShellStateService shellStateService;
    private final ServerActiveTurnRegistry activeTurnRegistry;

    public ServerConversationService(
            ConversationAgent conversationAgent,
            SqliteSessionRepository sessionRepository,
            String defaultModel
    ) {
        this(
                conversationAgent,
                sessionRepository,
                defaultRegistry(defaultModel),
                defaultModel
        );
    }

    public ServerConversationService(
            ConversationAgent conversationAgent,
            SqliteSessionRepository sessionRepository,
            LlmProviderRegistry providerRegistry,
            String defaultModel
    ) {
        this(
                conversationAgent,
                sessionRepository,
                providerRegistry,
                defaultModel,
                new ServerShellStateService(new SqliteShellStateRepository(sessionRepository)),
                new ServerActiveTurnRegistry()
        );
    }

    ServerConversationService(
            ConversationAgent conversationAgent,
            SqliteSessionRepository sessionRepository,
            LlmProviderRegistry providerRegistry,
            String defaultModel,
            ServerShellStateService shellStateService,
            ServerActiveTurnRegistry activeTurnRegistry
    ) {
        this.conversationAgent = Objects.requireNonNull(conversationAgent, "conversationAgent must not be null");
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository must not be null");
        this.providerRegistry = Objects.requireNonNull(providerRegistry, "providerRegistry must not be null");
        if (defaultModel == null || defaultModel.isBlank()) {
            throw new IllegalArgumentException("defaultModel must not be blank");
        }
        this.defaultModel = defaultModel;
        this.shellStateService = Objects.requireNonNull(shellStateService, "shellStateService must not be null");
        this.activeTurnRegistry = Objects.requireNonNull(activeTurnRegistry, "activeTurnRegistry must not be null");
    }

    public ServerConversationTurn chat(String prompt, String sessionId, String modelOverride) {
        return chat(prompt, sessionId, null, modelOverride);
    }

    public ServerConversationTurn chat(String prompt, String sessionId, String providerOverride, String modelOverride) {
        ResolvedTurnConfig resolvedConfig = resolveTurnConfig(sessionId, providerOverride, modelOverride);
        List<ConversationMessage> contextMessages = shellStateService.contextMessages();
        SessionContext augmentedSession = prependContextMessages(resolvedConfig.sessionContext(), contextMessages);
        AgentConversationTurn turn = conversationAgent.continueConversation(
                augmentedSession,
                requirePrompt(prompt),
                resolvedConfig.routedModel()
        );
        SessionContext cleanedSession = stripPrependedMessages(turn.sessionContext(), contextMessages.size());
        sessionRepository.saveSession(cleanedSession, resolvedConfig.providerId(), resolvedConfig.model());
        return new ServerConversationTurn(cleanedSession, turn.reply(), turn.toolResults(), resolvedConfig.providerId(), resolvedConfig.model());
    }

    public ServerConversationTurn chatStreaming(
            String prompt,
            String sessionId,
            String modelOverride,
            Consumer<String> textDeltaConsumer
    ) {
        PreparedStreamingSession preparedSession = prepareStreamingSession(sessionId, null, modelOverride);
        try {
            return chatStreaming(preparedSession, prompt, textDeltaConsumer);
        } finally {
            finishStreamingSession(preparedSession.sessionId());
        }
    }

    public ServerStoredSession loadSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        SessionContext sessionContext = sessionRepository.loadSession(SessionId.from(sessionId))
                .orElseThrow(() -> new IllegalArgumentException("Unknown session: " + sessionId));
        StoredSessionRuntime runtime = resolvedStoredRuntime(sessionContext.sessionId());
        return new ServerStoredSession(
                sessionContext,
                runtime.providerId(),
                runtime.modelName(),
                sessionRepository.loadSessionMessageModels(sessionContext.sessionId())
        );
    }

    public List<ServerSessionSummary> listSessions(int limit) {
        return sessionRepository.listSessions(limit).stream()
                .map(this::toServerSessionSummary)
                .toList();
    }

    public List<ProviderDescriptor> providers() {
        return providerRegistry.descriptors();
    }

    public String defaultModel() {
        return defaultModel;
    }

    public ServerShellState loadShellState() {
        return shellStateService.loadState();
    }

    public ServerShellState setPlanModeEnabled(boolean enabled) {
        shellStateService.setPlanModeEnabled(enabled);
        return shellStateService.loadState();
    }

    public PreparedStreamingSession prepareStreamingSession(String sessionId, String modelOverride) {
        return prepareStreamingSession(sessionId, null, modelOverride);
    }

    public PreparedStreamingSession prepareStreamingSession(String sessionId, String providerOverride, String modelOverride) {
        ResolvedTurnConfig resolvedConfig = resolveTurnConfig(sessionId, providerOverride, modelOverride);
        return new PreparedStreamingSession(
                resolvedConfig.sessionContext(),
                resolvedConfig.providerId(),
                resolvedConfig.model(),
                resolvedConfig.routedModel(),
                activeTurnRegistry.begin(resolvedConfig.sessionContext().sessionId().toString())
        );
    }

    public ServerConversationTurn chatStreaming(
            PreparedStreamingSession preparedSession,
            String prompt,
            Consumer<String> textDeltaConsumer
    ) {
        Objects.requireNonNull(preparedSession, "preparedSession must not be null");
        Objects.requireNonNull(textDeltaConsumer, "textDeltaConsumer must not be null");

        List<ConversationMessage> contextMessages = shellStateService.contextMessages();
        SessionContext augmentedSession = prependContextMessages(preparedSession.sessionContext(), contextMessages);
        AgentConversationTurn turn = conversationAgent.continueConversationStreaming(
                augmentedSession,
                requirePrompt(prompt),
                preparedSession.routedModel(),
                textDeltaConsumer,
                preparedSession.turnInterruptionSource()
        );
        SessionContext cleanedSession = stripPrependedMessages(turn.sessionContext(), contextMessages.size());
        sessionRepository.saveSession(cleanedSession, preparedSession.providerId(), preparedSession.model());
        return new ServerConversationTurn(
                cleanedSession,
                turn.reply(),
                turn.toolResults(),
                preparedSession.providerId(),
                preparedSession.model()
        );
    }

    public void finishStreamingSession(String sessionId) {
        activeTurnRegistry.finish(sessionId);
    }

    public int queueBtwMessage(String sessionId, String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        return activeTurnRegistry.enqueue(sessionId, message);
    }

    private SessionContext resolveSession(String sessionId) {
        return sessionId == null || sessionId.isBlank()
                ? SessionContext.start()
                : loadSession(sessionId).sessionContext();
    }

    private String requirePrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("prompt must not be blank");
        }
        return prompt;
    }

    private SessionContext prependContextMessages(SessionContext sessionContext, List<ConversationMessage> contextMessages) {
        if (contextMessages.isEmpty()) {
            return sessionContext;
        }

        List<ConversationMessage> merged = new java.util.ArrayList<>(contextMessages);
        merged.addAll(sessionContext.messages());
        return new SessionContext(sessionContext.sessionId(), merged);
    }

    private SessionContext stripPrependedMessages(SessionContext sessionContext, int prependedCount) {
        if (prependedCount == 0) {
            return sessionContext;
        }

        return new SessionContext(
                sessionContext.sessionId(),
                sessionContext.messages().subList(prependedCount, sessionContext.messages().size())
        );
    }

    private ServerSessionSummary toServerSessionSummary(StoredSessionSummary summary) {
        return new ServerSessionSummary(
                summary.sessionId(),
                summary.title(),
                summary.preview(),
                summary.updatedAt(),
                summary.messageCount()
        );
    }

    private ResolvedTurnConfig resolveTurnConfig(String sessionId, String providerOverride, String modelOverride) {
        SessionContext sessionContext = resolveSession(sessionId);
        StoredSessionRuntime storedRuntime = sessionId == null || sessionId.isBlank()
                ? null
                : resolvedStoredRuntime(sessionContext.sessionId());
        String providerId = providerRegistry.resolveProviderId(
                providerOverride == null || providerOverride.isBlank()
                        ? storedRuntime != null ? storedRuntime.providerId() : providerRegistry.defaultProviderId()
                        : providerOverride
        );
        providerRegistry.requireProvider(providerId);
        String model = resolveTurnModel(providerId, storedRuntime, providerOverride, modelOverride);
        return new ResolvedTurnConfig(
                sessionContext,
                providerId,
                model,
                ProviderModelRef.compose(providerId, model)
        );
    }

    private String resolveTurnModel(
            String providerId,
            StoredSessionRuntime storedRuntime,
            String providerOverride,
            String modelOverride
    ) {
        if (modelOverride != null && !modelOverride.isBlank()) {
            return modelOverride;
        }
        if ((providerOverride == null || providerOverride.isBlank())
                && storedRuntime != null
                && providerId.equals(storedRuntime.providerId())
                && storedRuntime.modelName() != null
                && !storedRuntime.modelName().isBlank()) {
            return storedRuntime.modelName();
        }
        return providerRegistry.resolveModel(providerId, null);
    }

    private StoredSessionRuntime resolvedStoredRuntime(SessionId sessionId) {
        StoredSessionRuntime storedRuntime = sessionRepository.loadSessionRuntime(sessionId).orElse(null);
        if (storedRuntime == null) {
            return new StoredSessionRuntime(
                    providerRegistry.defaultProviderId(),
                    providerRegistry.resolveModel(providerRegistry.defaultProviderId(), defaultModel)
            );
        }
        try {
            providerRegistry.requireProvider(storedRuntime.providerId());
            String model = storedRuntime.modelName() == null || storedRuntime.modelName().isBlank()
                    ? providerRegistry.resolveModel(storedRuntime.providerId(), null)
                    : storedRuntime.modelName();
            return new StoredSessionRuntime(storedRuntime.providerId(), model);
        } catch (IllegalArgumentException exception) {
            return new StoredSessionRuntime(
                    providerRegistry.defaultProviderId(),
                    providerRegistry.resolveModel(providerRegistry.defaultProviderId(), defaultModel)
            );
        }
    }

    private static LlmProviderRegistry defaultRegistry(String defaultModel) {
        return new LlmProviderRegistry(
                ProviderIds.OLLAMA,
                List.of(new RegisteredLlmProvider(
                        new ProviderDescriptor(ProviderIds.OLLAMA, "Ollama", defaultModel, true, false, true),
                        new NoopProvider()
                ))
        );
    }

    private record ResolvedTurnConfig(
            SessionContext sessionContext,
            String providerId,
            String model,
            String routedModel
    ) {
    }

    private static final class NoopProvider implements LlmProvider {
        @Override
        public String providerName() {
            return ProviderIds.OLLAMA;
        }

        @Override
        public com.axercode.core.provider.ProviderResponse generate(com.axercode.core.provider.ProviderRequest request) {
            throw new ProviderException(providerName(), "generate", "No-op provider should not be used in this code path.");
        }
    }
}
