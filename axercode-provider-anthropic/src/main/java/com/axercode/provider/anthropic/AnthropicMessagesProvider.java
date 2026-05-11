package com.axercode.provider.anthropic;

import com.axercode.core.provider.ProviderRequest;
import com.axercode.core.provider.ProviderResponse;
import com.axercode.core.provider.ProviderToolRound;
import com.axercode.core.session.ConversationMessage;
import com.axercode.core.session.MessageRole;
import com.axercode.core.tool.ToolCall;
import com.axercode.core.tool.ToolDefinition;
import com.axercode.core.tool.ToolExecutionResult;
import com.axercode.core.tool.ToolExecutionStatus;
import com.axercode.provider.api.LlmProvider;
import com.axercode.provider.api.ProviderException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Anthropic Messages API adapter with support for text completions, SSE streaming, and tool-use follow-up rounds.
 */
public final class AnthropicMessagesProvider implements LlmProvider {

    private static final String PROVIDER_NAME = "anthropic";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String anthropicVersion;
    private final int maxTokens;

    public AnthropicMessagesProvider(String baseUrl, String apiKey, String anthropicVersion, int maxTokens) {
        this(
                RestClient.builder()
                        .baseUrl(normalizeBaseUrl(baseUrl))
                        .build(),
                new ObjectMapper(),
                apiKey,
                anthropicVersion,
                maxTokens
        );
    }

    AnthropicMessagesProvider(
            RestClient restClient,
            ObjectMapper objectMapper,
            String apiKey,
            String anthropicVersion,
            int maxTokens
    ) {
        this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.anthropicVersion = anthropicVersion == null || anthropicVersion.isBlank()
                ? "2023-06-01"
                : anthropicVersion.trim();
        if (maxTokens < 1) {
            throw new IllegalArgumentException("maxTokens must be at least 1");
        }
        this.maxTokens = maxTokens;
    }

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public ProviderResponse generate(ProviderRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        ensureConfigured("generate");

        try {
            String serializedRequest = objectMapper.writeValueAsString(buildRequestBody(request, false));
            String responseJson = restClient.post()
                    .uri("/v1/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", anthropicVersion)
                    .body(serializedRequest)
                    .retrieve()
                    .body(String.class);

            if (responseJson == null || responseJson.isBlank()) {
                throw new ProviderException(PROVIDER_NAME, "generate", "Anthropic /v1/messages returned an empty response.");
            }

            return mapResponse(objectMapper.readTree(responseJson), "generate");
        } catch (RestClientException exception) {
            throw new ProviderException(PROVIDER_NAME, "generate", "Failed to call Anthropic /v1/messages.", exception);
        } catch (JsonProcessingException exception) {
            throw new ProviderException(PROVIDER_NAME, "generate", "Failed to parse Anthropic /v1/messages JSON.", exception);
        }
    }

    @Override
    public ProviderResponse streamGenerate(ProviderRequest request, Consumer<String> textDeltaConsumer) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(textDeltaConsumer, "textDeltaConsumer must not be null");
        ensureConfigured("streamGenerate");

        ProviderRequest streamingRequest = ProviderRequest.create(
                request.model(),
                request.messages(),
                request.availableTools(),
                true,
                request.recentToolRound()
        );

        try {
            String serializedRequest = objectMapper.writeValueAsString(buildRequestBody(streamingRequest, true));
            return restClient.post()
                    .uri("/v1/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", anthropicVersion)
                    .body(serializedRequest)
                    .exchange((clientRequest, clientResponse) -> {
                        if (clientResponse.getStatusCode().isError()) {
                            throw new ProviderException(PROVIDER_NAME, "streamGenerate", "Failed to call Anthropic /v1/messages.");
                        }
                        return readStreamingResponse(clientResponse, textDeltaConsumer);
                    });
        } catch (RestClientException exception) {
            throw new ProviderException(PROVIDER_NAME, "streamGenerate", "Failed to call Anthropic /v1/messages.", exception);
        } catch (JsonProcessingException exception) {
            throw new ProviderException(PROVIDER_NAME, "streamGenerate", "Failed to serialize Anthropic /v1/messages JSON.", exception);
        }
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    private ObjectNode buildRequestBody(ProviderRequest request, boolean stream) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", request.model());
        root.put("max_tokens", maxTokens);
        root.put("stream", stream);

        List<JsonNode> systemBlocks = buildSystemBlocks(request.messages());
        if (!systemBlocks.isEmpty()) {
            root.set("system", objectMapper.valueToTree(systemBlocks));
        }

        root.set("messages", objectMapper.valueToTree(buildMessages(request)));

        List<JsonNode> tools = buildTools(request.availableTools());
        if (!tools.isEmpty()) {
            root.set("tools", objectMapper.valueToTree(tools));
        }
        return root;
    }

    private List<JsonNode> buildSystemBlocks(List<ConversationMessage> messages) {
        List<JsonNode> blocks = new ArrayList<>();
        for (ConversationMessage message : messages) {
            if (message.role() != MessageRole.SYSTEM) {
                continue;
            }
            blocks.add(textBlock(message.content()));
        }
        return List.copyOf(blocks);
    }

    private List<JsonNode> buildTools(List<ToolDefinition> toolDefinitions) {
        List<JsonNode> tools = new ArrayList<>();
        for (ToolDefinition toolDefinition : toolDefinitions) {
            ObjectNode tool = objectMapper.createObjectNode();
            tool.put("name", toolDefinition.name());
            tool.put("description", toolDefinition.description());
            try {
                tool.set("input_schema", objectMapper.readTree(toolDefinition.parametersJsonSchema()));
            } catch (JsonProcessingException exception) {
                throw new ProviderException(PROVIDER_NAME, "buildRequest", "Failed to parse tool schema for " + toolDefinition.name(), exception);
            }
            tools.add(tool);
        }
        return List.copyOf(tools);
    }

    private List<JsonNode> buildMessages(ProviderRequest request) {
        List<ConversationMessage> nonSystemMessages = request.messages().stream()
                .filter(message -> message.role() != MessageRole.SYSTEM)
                .toList();
        ProviderToolRound recentToolRound = request.recentToolRound();
        if (recentToolRound == null) {
            return normalizePlainMessages(nonSystemMessages);
        }

        MessageSplit messageSplit = splitRecentToolWindow(nonSystemMessages);
        List<JsonNode> messages = new ArrayList<>(normalizePlainMessages(messageSplit.prefixMessages()));
        messages.add(toolUseAssistantMessage(recentToolRound.toolCalls()));

        List<JsonNode> toolResultBlocks = new ArrayList<>();
        for (ToolExecutionResult result : recentToolRound.toolResults()) {
            toolResultBlocks.add(toolResultBlock(result));
        }

        List<JsonNode> trailingMessages = normalizePlainMessages(messageSplit.trailingMessages());
        if (trailingMessages.isEmpty()) {
            messages.add(message("user", toolResultBlocks));
            return List.copyOf(messages);
        }

        JsonNode firstTrailingMessage = trailingMessages.getFirst();
        if ("user".equals(firstTrailingMessage.get("role").asText())) {
            List<JsonNode> mergedContent = new ArrayList<>(toolResultBlocks);
            firstTrailingMessage.get("content").forEach(block -> mergedContent.add(block));
            messages.add(message("user", mergedContent));
            messages.addAll(trailingMessages.subList(1, trailingMessages.size()));
            return List.copyOf(messages);
        }

        messages.add(message("user", toolResultBlocks));
        messages.addAll(trailingMessages);
        return List.copyOf(messages);
    }

    private List<JsonNode> normalizePlainMessages(List<ConversationMessage> messages) {
        List<JsonNode> normalized = new ArrayList<>();
        String currentRole = null;
        List<JsonNode> currentBlocks = new ArrayList<>();

        for (ConversationMessage message : messages) {
            String targetRole = mapPlainMessageRole(message.role());
            if (currentRole != null && !currentRole.equals(targetRole)) {
                normalized.add(message(currentRole, currentBlocks));
                currentBlocks = new ArrayList<>();
            }
            currentRole = targetRole;
            currentBlocks.add(textBlock(message.content()));
        }

        if (currentRole != null && !currentBlocks.isEmpty()) {
            normalized.add(message(currentRole, currentBlocks));
        }
        return List.copyOf(normalized);
    }

    private MessageSplit splitRecentToolWindow(List<ConversationMessage> messages) {
        int lastToolIndex = -1;
        for (int index = messages.size() - 1; index >= 0; index--) {
            if (messages.get(index).role() == MessageRole.TOOL) {
                lastToolIndex = index;
                break;
            }
        }

        if (lastToolIndex < 0) {
            return new MessageSplit(messages, List.of());
        }

        int firstToolIndex = lastToolIndex;
        while (firstToolIndex > 0 && messages.get(firstToolIndex - 1).role() == MessageRole.TOOL) {
            firstToolIndex--;
        }

        return new MessageSplit(
                messages.subList(0, firstToolIndex),
                messages.subList(lastToolIndex + 1, messages.size())
        );
    }

    private JsonNode toolUseAssistantMessage(List<ToolCall> toolCalls) {
        List<JsonNode> content = new ArrayList<>();
        for (ToolCall toolCall : toolCalls) {
            ObjectNode block = objectMapper.createObjectNode();
            block.put("type", "tool_use");
            block.put("id", toolCall.id());
            block.put("name", toolCall.name());
            try {
                block.set("input", objectMapper.readTree(toolCall.argumentsJson()));
            } catch (JsonProcessingException exception) {
                throw new ProviderException(PROVIDER_NAME, "buildRequest", "Failed to parse tool arguments for " + toolCall.name(), exception);
            }
            content.add(block);
        }
        return message("assistant", content);
    }

    private JsonNode toolResultBlock(ToolExecutionResult result) {
        ObjectNode block = objectMapper.createObjectNode();
        block.put("type", "tool_result");
        block.put("tool_use_id", result.toolCall().id());
        block.put("content", result.output());
        if (result.status() == ToolExecutionStatus.FAILURE) {
            block.put("is_error", true);
        }
        return block;
    }

    private JsonNode textBlock(String text) {
        ObjectNode block = objectMapper.createObjectNode();
        block.put("type", "text");
        block.put("text", text);
        return block;
    }

    private JsonNode message(String role, List<JsonNode> contentBlocks) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", role);
        message.set("content", objectMapper.valueToTree(contentBlocks));
        return message;
    }

    private ProviderResponse mapResponse(JsonNode root, String operation) {
        JsonNode content = root.path("content");
        if (!content.isArray()) {
            throw new ProviderException(PROVIDER_NAME, operation, "Anthropic /v1/messages returned no content array.");
        }

        StringBuilder text = new StringBuilder();
        List<ToolCall> toolCalls = new ArrayList<>();

        for (JsonNode block : content) {
            String type = block.path("type").asText();
            if ("text".equals(type)) {
                text.append(block.path("text").asText());
                continue;
            }
            if ("tool_use".equals(type)) {
                toolCalls.add(ToolCall.create(
                        block.path("id").asText(),
                        block.path("name").asText(),
                        serializeJson(block.path("input"))
                ));
            }
        }

        if (!toolCalls.isEmpty()) {
            return ProviderResponse.toolCalls(toolCalls);
        }
        if (text.toString().isBlank()) {
            throw new ProviderException(PROVIDER_NAME, operation, "Anthropic /v1/messages returned no assistant content.");
        }
        return ProviderResponse.complete(text.toString());
    }

    private ProviderResponse readStreamingResponse(
            org.springframework.http.client.ClientHttpResponse clientResponse,
            Consumer<String> textDeltaConsumer
    ) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(clientResponse.getBody(), StandardCharsets.UTF_8)
        )) {
            StreamingAccumulator accumulator = new StreamingAccumulator();
            String line;
            String currentEvent = null;
            StringBuilder currentData = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    consumeSseFrame(currentEvent, currentData.toString(), accumulator, textDeltaConsumer);
                    currentEvent = null;
                    currentData = new StringBuilder();
                    continue;
                }

                if (line.startsWith("event:")) {
                    currentEvent = line.substring(6).trim();
                    continue;
                }

                if (line.startsWith("data:")) {
                    if (currentData.length() > 0) {
                        currentData.append('\n');
                    }
                    currentData.append(line.substring(5).trim());
                }
            }

            consumeSseFrame(currentEvent, currentData.toString(), accumulator, textDeltaConsumer);
            return accumulator.toProviderResponse();
        } catch (IOException exception) {
            throw new ProviderException(PROVIDER_NAME, "streamGenerate", "Failed to read Anthropic /v1/messages stream.", exception);
        }
    }

    private void consumeSseFrame(
            String eventName,
            String data,
            StreamingAccumulator accumulator,
            Consumer<String> textDeltaConsumer
    ) throws JsonProcessingException {
        if (eventName == null || eventName.isBlank() || data == null || data.isBlank()) {
            return;
        }

        JsonNode payload = objectMapper.readTree(data);
        String payloadType = payload.path("type").asText();
        if ("error".equals(eventName) || "error".equals(payloadType)) {
            String message = payload.path("error").path("message").asText("Anthropic streaming request failed.");
            throw new ProviderException(PROVIDER_NAME, "streamGenerate", message);
        }
        if ("ping".equals(eventName)) {
            return;
        }

        if ("content_block_start".equals(eventName)) {
            JsonNode contentBlock = payload.path("content_block");
            if ("tool_use".equals(contentBlock.path("type").asText())) {
                accumulator.startToolBlock(
                        payload.path("index").asInt(),
                        contentBlock.path("id").asText(),
                        contentBlock.path("name").asText(),
                        contentBlock.path("input")
                );
            }
            if ("text".equals(contentBlock.path("type").asText())) {
                String initialText = contentBlock.path("text").asText("");
                if (!initialText.isEmpty()) {
                    accumulator.appendText(initialText);
                    textDeltaConsumer.accept(initialText);
                }
            }
            return;
        }

        if ("content_block_delta".equals(eventName)) {
            JsonNode delta = payload.path("delta");
            String deltaType = delta.path("type").asText();
            if ("text_delta".equals(deltaType)) {
                String text = delta.path("text").asText("");
                if (!text.isEmpty()) {
                    accumulator.appendText(text);
                    textDeltaConsumer.accept(text);
                }
            } else if ("input_json_delta".equals(deltaType)) {
                accumulator.appendToolInput(payload.path("index").asInt(), delta.path("partial_json").asText(""));
            }
        }
    }

    private void ensureConfigured(String operation) {
        if (apiKey.isBlank()) {
            throw new ProviderException(PROVIDER_NAME, operation, "Anthropic API key is not configured on the server.");
        }
    }

    private String mapPlainMessageRole(MessageRole role) {
        return switch (role) {
            case USER, TOOL -> "user";
            case ASSISTANT -> "assistant";
            case SYSTEM -> throw new IllegalArgumentException("SYSTEM messages are handled separately");
        };
    }

    private String serializeJson(JsonNode node) {
        try {
            if (node == null || node.isMissingNode() || node.isNull()) {
                return "{}";
            }
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException exception) {
            throw new ProviderException(PROVIDER_NAME, "serialize", "Failed to serialize Anthropic JSON content.", exception);
        }
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl must not be blank");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private record MessageSplit(
            List<ConversationMessage> prefixMessages,
            List<ConversationMessage> trailingMessages
    ) {
    }

    private final class StreamingAccumulator {
        private final StringBuilder text = new StringBuilder();
        private final Map<Integer, StreamingToolBlock> toolBlocks = new TreeMap<>();

        private void appendText(String delta) {
            text.append(delta);
        }

        private void startToolBlock(int index, String id, String name, JsonNode fallbackInput) {
            toolBlocks.put(index, new StreamingToolBlock(id, name, fallbackInput));
        }

        private void appendToolInput(int index, String partialJson) {
            StreamingToolBlock toolBlock = toolBlocks.get(index);
            if (toolBlock == null) {
                return;
            }
            toolBlock.inputJson.append(partialJson);
        }

        private ProviderResponse toProviderResponse() {
            if (!toolBlocks.isEmpty()) {
                List<ToolCall> toolCalls = new ArrayList<>();
                for (StreamingToolBlock block : toolBlocks.values()) {
                    String inputJson = block.inputJson.length() > 0
                            ? block.inputJson.toString()
                            : serializeJson(block.fallbackInput);
                    toolCalls.add(ToolCall.create(block.id, block.name, inputJson));
                }
                return ProviderResponse.toolCalls(toolCalls);
            }
            if (text.toString().isBlank()) {
                throw new ProviderException(PROVIDER_NAME, "streamGenerate", "Anthropic /v1/messages stream returned no assistant content.");
            }
            return ProviderResponse.complete(text.toString());
        }
    }

    private static final class StreamingToolBlock {
        private final String id;
        private final String name;
        private final JsonNode fallbackInput;
        private final StringBuilder inputJson = new StringBuilder();

        private StreamingToolBlock(String id, String name, JsonNode fallbackInput) {
            this.id = id;
            this.name = name;
            this.fallbackInput = fallbackInput;
        }
    }
}
