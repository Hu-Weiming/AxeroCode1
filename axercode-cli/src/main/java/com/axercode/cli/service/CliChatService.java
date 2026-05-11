package com.axercode.cli.service;

import com.axercode.agent.AgentConversationTurn;
import com.axercode.agent.ConversationAgent;
import com.axercode.cli.config.AxerCodeProviderProperties;
import com.axercode.core.session.ConversationMessage;
import com.axercode.core.session.SessionContext;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * CLI-facing service that chooses the effective model and delegates the full turn to the agent layer.
 */
@Service
public class CliChatService {

    private final ConversationAgent conversationAgent;
    private final AxerCodeProviderProperties properties;
    private final ShellContextAugmenter shellContextAugmenter;

    public CliChatService(ConversationAgent conversationAgent, AxerCodeProviderProperties properties) {
        this(conversationAgent, properties, new InMemoryShellStateStore());
    }

    @Autowired
    public CliChatService(
            ConversationAgent conversationAgent,
            AxerCodeProviderProperties properties,
            ShellStateStore shellStateStore
    ) {
        this.conversationAgent = conversationAgent;
        this.properties = properties;
        this.shellContextAugmenter = new ShellContextAugmenter(shellStateStore);
    }

    public String ask(String prompt, String modelOverride) {
        return askTurn(prompt, modelOverride).reply();
    }

    public String resolveModel(String modelOverride) {
        return modelOverride == null || modelOverride.isBlank()
                ? properties.getDefaultModel()
                : modelOverride;
    }

    public CliChatTurn askTurn(String prompt, String modelOverride) {
        return continueConversation(SessionContext.start(), prompt, modelOverride);
    }

    public CliChatTurn askTurnStreaming(String prompt, String modelOverride, Consumer<String> textDeltaConsumer) {
        return continueConversationStreaming(SessionContext.start(), prompt, modelOverride, textDeltaConsumer);
    }

    public CliChatTurn continueConversation(SessionContext sessionContext, String prompt, String modelOverride) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("prompt must not be blank");
        }
        if (sessionContext == null) {
            throw new IllegalArgumentException("sessionContext must not be null");
        }

        String model = resolveModel(modelOverride);
        List<ConversationMessage> contextMessages = shellContextAugmenter.systemMessages();
        SessionContext augmentedSessionContext = prependContextMessages(sessionContext, contextMessages);

        AgentConversationTurn turn = conversationAgent.continueConversation(augmentedSessionContext, prompt, model);
        SessionContext cleanedSessionContext = stripPrependedMessages(turn.sessionContext(), contextMessages.size());
        return new CliChatTurn(cleanedSessionContext, turn.reply(), turn.toolResults());
    }

    public CliChatTurn continueConversationStreaming(
            SessionContext sessionContext,
            String prompt,
            String modelOverride,
            Consumer<String> textDeltaConsumer
    ) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("prompt must not be blank");
        }
        if (sessionContext == null) {
            throw new IllegalArgumentException("sessionContext must not be null");
        }
        if (textDeltaConsumer == null) {
            throw new IllegalArgumentException("textDeltaConsumer must not be null");
        }

        String model = resolveModel(modelOverride);
        List<ConversationMessage> contextMessages = shellContextAugmenter.systemMessages();
        SessionContext augmentedSessionContext = prependContextMessages(sessionContext, contextMessages);

        AgentConversationTurn turn = conversationAgent.continueConversationStreaming(
                augmentedSessionContext,
                prompt,
                model,
                textDeltaConsumer
        );
        SessionContext cleanedSessionContext = stripPrependedMessages(turn.sessionContext(), contextMessages.size());
        return new CliChatTurn(cleanedSessionContext, turn.reply(), turn.toolResults());
    }

    private SessionContext prependContextMessages(SessionContext sessionContext, List<ConversationMessage> contextMessages) {
        if (contextMessages.isEmpty()) {
            return sessionContext;
        }

        List<ConversationMessage> merged = new ArrayList<>(contextMessages);
        merged.addAll(sessionContext.messages());
        return new SessionContext(sessionContext.sessionId(), merged);
    }

    private SessionContext stripPrependedMessages(SessionContext sessionContext, int prependedCount) {
        if (prependedCount == 0) {
            return sessionContext;
        }

        List<ConversationMessage> cleanedMessages = new ArrayList<>(
                sessionContext.messages().subList(prependedCount, sessionContext.messages().size())
        );
        return new SessionContext(sessionContext.sessionId(), cleanedMessages);
    }
}
