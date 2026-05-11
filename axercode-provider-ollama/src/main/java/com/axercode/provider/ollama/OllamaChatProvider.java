package com.axercode.provider.ollama;

import com.axercode.core.provider.ProviderRequest;
import com.axercode.core.provider.ProviderResponse;
import com.axercode.core.tool.ToolCall;
import com.axercode.provider.api.LlmProvider;
import com.axercode.provider.api.ProviderException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Minimal Ollama adapter that converts the shared provider contracts into a local `/api/chat` call.
 */
public final class OllamaChatProvider implements LlmProvider {

    private static final String PROVIDER_NAME = "ollama";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OllamaChatProvider(String baseUrl) {
        this(
                RestClient.builder()
                        .baseUrl(normalizeBaseUrl(baseUrl))
                        .build(),
                new ObjectMapper()
        );
    }

    OllamaChatProvider(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public ProviderResponse generate(ProviderRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        if (request.stream()) {
            throw new ProviderException(PROVIDER_NAME, "generate", "Streaming requests are not implemented yet for Ollama /api/chat.");
        }

        try {
            String serializedRequest = objectMapper.writeValueAsString(OllamaChatRequest.from(request, objectMapper));
            String responseJson = restClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(serializedRequest)
                    .retrieve()
                    .body(String.class);

            if (responseJson == null || responseJson.isBlank()) {
                throw new ProviderException(PROVIDER_NAME, "generate", "Ollama /api/chat returned an empty response.");
            }

            OllamaChatResponse response = objectMapper.readValue(responseJson, OllamaChatResponse.class);
            return mapResponse(response);
        } catch (RestClientException exception) {
            throw new ProviderException(PROVIDER_NAME, "generate", "Failed to call Ollama /api/chat.", exception);
        } catch (JsonProcessingException exception) {
            throw new ProviderException(PROVIDER_NAME, "generate", "Failed to parse Ollama /api/chat JSON.", exception);
        }
    }

    @Override
    public ProviderResponse streamGenerate(ProviderRequest request, Consumer<String> textDeltaConsumer) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(textDeltaConsumer, "textDeltaConsumer must not be null");

        ProviderRequest streamingRequest = ProviderRequest.create(
                request.model(),
                request.messages(),
                request.availableTools(),
                true
        );

        try {
            String serializedRequest = objectMapper.writeValueAsString(OllamaChatRequest.from(streamingRequest, objectMapper));
            return restClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.parseMediaType("application/x-ndjson"), MediaType.APPLICATION_JSON)
                    .body(serializedRequest)
                    .exchange((clientRequest, clientResponse) -> {
                        if (clientResponse.getStatusCode().isError()) {
                            throw new ProviderException(
                                    PROVIDER_NAME,
                                    "streamGenerate",
                                    "Failed to call Ollama /api/chat."
                            );
                        }
                        return readStreamingResponse(clientResponse, textDeltaConsumer);
                    });
        } catch (RestClientException exception) {
            throw new ProviderException(PROVIDER_NAME, "streamGenerate", "Failed to call Ollama /api/chat.", exception);
        } catch (JsonProcessingException exception) {
            throw new ProviderException(PROVIDER_NAME, "streamGenerate", "Failed to serialize Ollama /api/chat JSON.", exception);
        }
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    private ProviderResponse mapResponse(OllamaChatResponse response) throws JsonProcessingException {
        if (response.message() == null) {
            throw new ProviderException(PROVIDER_NAME, "generate", "Ollama /api/chat returned no message payload.");
        }

        List<ToolCall> mappedToolCalls = mapToolCalls(response.message().toolCalls());
        if (!mappedToolCalls.isEmpty()) {
            return ProviderResponse.toolCalls(mappedToolCalls);
        }

        return ProviderResponse.complete(response.message().content());
    }

    private ProviderResponse readStreamingResponse(
            org.springframework.http.client.ClientHttpResponse clientResponse,
            Consumer<String> textDeltaConsumer
    ) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(clientResponse.getBody(), StandardCharsets.UTF_8)
        )) {
            String line;
            StringBuilder streamedContent = new StringBuilder();
            OllamaChatResponse finalChunk = null;
            LinkedHashMap<String, ToolCall> streamedToolCalls = new LinkedHashMap<>();

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                OllamaChatResponse chunk = objectMapper.readValue(line, OllamaChatResponse.class);
                finalChunk = chunk;
                for (ToolCall toolCall : mapToolCalls(chunk.message() == null ? null : chunk.message().toolCalls())) {
                    String key = toolCall.name() + "\u0000" + toolCall.argumentsJson();
                    streamedToolCalls.putIfAbsent(key, toolCall);
                }

                if (chunk.message() != null && chunk.message().content() != null && !chunk.message().content().isEmpty()) {
                    streamedContent.append(chunk.message().content());
                    textDeltaConsumer.accept(chunk.message().content());
                }
            }

            if (finalChunk == null) {
                throw new ProviderException(PROVIDER_NAME, "streamGenerate", "Ollama /api/chat returned an empty stream.");
            }

            return mapStreamingResponse(finalChunk, streamedContent.toString(), List.copyOf(streamedToolCalls.values()));
        } catch (IOException exception) {
            throw new ProviderException(PROVIDER_NAME, "streamGenerate", "Failed to read Ollama /api/chat stream.", exception);
        }
    }

    private ProviderResponse mapStreamingResponse(
            OllamaChatResponse response,
            String streamedContent,
            List<ToolCall> streamedToolCalls
    ) throws JsonProcessingException {
        if (response.message() == null) {
            throw new ProviderException(PROVIDER_NAME, "streamGenerate", "Ollama /api/chat returned no message payload.");
        }

        List<ToolCall> mappedToolCalls = streamedToolCalls.isEmpty()
                ? mapToolCalls(response.message().toolCalls())
                : streamedToolCalls;
        if (!mappedToolCalls.isEmpty()) {
            return ProviderResponse.toolCalls(mappedToolCalls);
        }

        String finalContent = streamedContent == null || streamedContent.isBlank()
                ? response.message().content()
                : streamedContent;
        if (finalContent == null || finalContent.isBlank()) {
            throw new ProviderException(PROVIDER_NAME, "streamGenerate", "Ollama /api/chat stream returned no assistant content.");
        }

        return ProviderResponse.complete(finalContent);
    }

    private List<ToolCall> mapToolCalls(List<OllamaChatResponse.OllamaToolCall> toolCalls) throws JsonProcessingException {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return List.of();
        }

        List<ToolCall> mapped = new ArrayList<>();
        for (OllamaChatResponse.OllamaToolCall toolCall : toolCalls) {
            if (toolCall == null || toolCall.function() == null) {
                continue;
            }
            String argumentsJson = toolCall.function().arguments() == null
                    ? "{}"
                    : objectMapper.writeValueAsString(toolCall.function().arguments());
            mapped.add(ToolCall.create(toolCall.function().name(), argumentsJson));
        }
        return List.copyOf(mapped);
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl must not be blank");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
