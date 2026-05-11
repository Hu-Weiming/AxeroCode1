package com.axercode.provider.ollama;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record OllamaChatResponse(
        String model,
        OllamaChatMessage message,
        boolean done,
        @JsonProperty("done_reason") String doneReason
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OllamaChatMessage(
            String role,
            String content,
            @JsonProperty("tool_calls") List<OllamaToolCall> toolCalls
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OllamaToolCall(OllamaFunction function) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OllamaFunction(String name, JsonNode arguments) {
    }
}
