package com.axercode.provider.ollama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axercode.core.provider.ProviderRequest;
import com.axercode.core.provider.ProviderStopReason;
import com.axercode.core.session.ConversationMessage;
import com.axercode.core.tool.ToolDefinition;
import com.axercode.provider.api.ProviderException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OllamaChatProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void generateMapsPlainTextCompletionFromApiChat() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = startServer(exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            writeJson(exchange, 200, """
                    {
                      "model":"qwen2.5:7b",
                      "message":{"role":"assistant","content":"AxerCode ready"},
                      "done":true,
                      "done_reason":"stop"
                    }
                    """);
        });

        OllamaChatProvider provider = new OllamaChatProvider(baseUrl());
        ProviderRequest request = ProviderRequest.create(
                "qwen2.5:7b",
                List.of(ConversationMessage.user("hello")),
                List.of(),
                false
        );

        var response = provider.generate(request);

        assertEquals(ProviderStopReason.COMPLETE, response.stopReason());
        assertEquals("AxerCode ready", response.content());

        JsonNode sentRequest = objectMapper.readTree(requestBody.get());
        assertEquals("qwen2.5:7b", sentRequest.get("model").asText());
        assertTrue(sentRequest.get("messages").isArray());
        assertEquals("user", sentRequest.get("messages").get(0).get("role").asText());
        assertEquals("hello", sentRequest.get("messages").get(0).get("content").asText());
        assertFalseValue(sentRequest.get("stream").asBoolean());
    }

    @Test
    void generateMapsToolCallsIntoProviderResponse() throws Exception {
        server = startServer(exchange -> writeJson(exchange, 200, """
                {
                  "model":"qwen2.5:7b",
                  "message":{
                    "role":"assistant",
                    "content":"",
                    "tool_calls":[
                      {
                        "function":{
                          "name":"read_file",
                          "arguments":{"path":"README.md"}
                        }
                      }
                    ]
                  },
                  "done":true,
                  "done_reason":"stop"
                }
                """));

        OllamaChatProvider provider = new OllamaChatProvider(baseUrl());
        ProviderRequest request = ProviderRequest.create(
                "qwen2.5:7b",
                List.of(ConversationMessage.user("read the readme")),
                List.of(),
                false
        );

        var response = provider.generate(request);

        assertEquals(ProviderStopReason.TOOL_CALLS, response.stopReason());
        assertEquals(1, response.toolCalls().size());
        assertEquals("read_file", response.toolCalls().getFirst().name());
        assertEquals("{\"path\":\"README.md\"}", response.toolCalls().getFirst().argumentsJson());
    }

    @Test
    void generateSerializesStructuredToolsIntoOllamaRequest() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = startServer(exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            writeJson(exchange, 200, """
                    {
                      "model":"qwen2.5:7b",
                      "message":{"role":"assistant","content":"tool-aware"},
                      "done":true,
                      "done_reason":"stop"
                    }
                    """);
        });

        OllamaChatProvider provider = new OllamaChatProvider(baseUrl());
        ProviderRequest request = ProviderRequest.create(
                "qwen2.5:7b",
                List.of(ConversationMessage.user("hello")),
                List.of(new ToolDefinition(
                        "read_file",
                        "Read file content",
                        "{\"type\":\"object\",\"required\":[\"path\"],\"properties\":{\"path\":{\"type\":\"string\"}}}"
                )),
                false
        );

        var response = provider.generate(request);

        assertEquals(ProviderStopReason.COMPLETE, response.stopReason());
        JsonNode sentRequest = objectMapper.readTree(requestBody.get());
        assertEquals("read_file", sentRequest.get("tools").get(0).get("function").get("name").asText());
        assertEquals("Read file content", sentRequest.get("tools").get(0).get("function").get("description").asText());
        assertEquals("object", sentRequest.get("tools").get(0).get("function").get("parameters").get("type").asText());
    }

    @Test
    void generateWrapsHttpErrorsWithFriendlyProviderException() throws Exception {
        server = startServer(exchange -> writeJson(exchange, 500, "{\"error\":\"boom\"}"));

        OllamaChatProvider provider = new OllamaChatProvider(baseUrl());
        ProviderRequest request = ProviderRequest.create(
                "qwen2.5:7b",
                List.of(ConversationMessage.user("hello")),
                List.of(),
                false
        );

        ProviderException exception = assertThrows(
                ProviderException.class,
                () -> provider.generate(request)
        );

        assertEquals("ollama", exception.providerName());
        assertEquals("generate", exception.operation());
        assertTrue(exception.getMessage().contains("/api/chat"));
    }

    @Test
    void generateRejectsStreamingRequestsForNow() throws Exception {
        server = startServer(exchange -> writeJson(exchange, 200, """
                {
                  "model":"qwen2.5:7b",
                  "message":{"role":"assistant","content":"ignored"},
                  "done":true,
                  "done_reason":"stop"
                }
                """));

        OllamaChatProvider provider = new OllamaChatProvider(baseUrl());
        ProviderRequest request = ProviderRequest.create(
                "qwen2.5:7b",
                List.of(ConversationMessage.user("hello")),
                List.of(),
                true
        );

        ProviderException exception = assertThrows(
                ProviderException.class,
                () -> provider.generate(request)
        );

        assertTrue(exception.getMessage().contains("Streaming requests are not implemented yet"));
    }

    @Test
    void streamGenerateEmitsTextDeltasAndReturnsFinalCompletion() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = startServer(exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            writeStream(exchange,
                    """
                    {"model":"qwen2.5:7b","message":{"role":"assistant","content":"Axer"},"done":false}
                    """,
                    """
                    {"model":"qwen2.5:7b","message":{"role":"assistant","content":"Code"},"done":false}
                    """,
                    """
                    {"model":"qwen2.5:7b","message":{"role":"assistant","content":""},"done":true,"done_reason":"stop"}
                    """
            );
        });

        OllamaChatProvider provider = new OllamaChatProvider(baseUrl());
        ProviderRequest request = ProviderRequest.create(
                "qwen2.5:7b",
                List.of(ConversationMessage.user("hello")),
                List.of(),
                true
        );
        StringBuilder streamed = new StringBuilder();

        var response = provider.streamGenerate(request, streamed::append);

        assertTrue(provider.supportsStreaming());
        assertEquals("AxerCode", streamed.toString());
        assertEquals(ProviderStopReason.COMPLETE, response.stopReason());
        assertEquals("AxerCode", response.content());

        JsonNode sentRequest = objectMapper.readTree(requestBody.get());
        assertTrue(sentRequest.get("stream").asBoolean());
    }

    @Test
    void streamGenerateReturnsToolCallsWhenFinalChunkContainsToolCalls() throws Exception {
        server = startServer(exchange -> writeStream(exchange,
                """
                {"model":"qwen2.5:7b","message":{"role":"assistant","content":""},"done":false}
                """,
                """
                {"model":"qwen2.5:7b","message":{"role":"assistant","content":"","tool_calls":[{"function":{"name":"read_file","arguments":{"path":"README.md"}}}]},"done":true,"done_reason":"stop"}
                """
        ));

        OllamaChatProvider provider = new OllamaChatProvider(baseUrl());
        ProviderRequest request = ProviderRequest.create(
                "qwen2.5:7b",
                List.of(ConversationMessage.user("inspect")),
                List.of(),
                true
        );
        StringBuilder streamed = new StringBuilder();

        var response = provider.streamGenerate(request, streamed::append);

        assertEquals("", streamed.toString());
        assertEquals(ProviderStopReason.TOOL_CALLS, response.stopReason());
        assertEquals("read_file", response.toolCalls().getFirst().name());
    }

    @Test
    void streamGenerateReturnsToolCallsWhenEarlierChunkContainsToolCalls() throws Exception {
        server = startServer(exchange -> writeStream(exchange,
                """
                {"model":"qwen2.5:7b","message":{"role":"assistant","content":"","tool_calls":[{"function":{"name":"run_shell","arguments":{"command":"echo hello","timeoutSeconds":10}}}]},"done":false}
                """,
                """
                {"model":"qwen2.5:7b","message":{"role":"assistant","content":""},"done":true,"done_reason":"stop"}
                """
        ));

        OllamaChatProvider provider = new OllamaChatProvider(baseUrl());
        ProviderRequest request = ProviderRequest.create(
                "qwen2.5:7b",
                List.of(ConversationMessage.user("inspect")),
                List.of(),
                true
        );
        StringBuilder streamed = new StringBuilder();

        var response = provider.streamGenerate(request, streamed::append);

        assertEquals("", streamed.toString());
        assertEquals(ProviderStopReason.TOOL_CALLS, response.stopReason());
        assertEquals("run_shell", response.toolCalls().getFirst().name());
        assertEquals("{\"command\":\"echo hello\",\"timeoutSeconds\":10}", response.toolCalls().getFirst().argumentsJson());
    }

    @Test
    void generateIgnoresAdditionalOllamaMetadataFields() throws Exception {
        server = startServer(exchange -> writeJson(exchange, 200, """
                {
                  "model":"qwen2.5:7b",
                  "created_at":"2026-04-18T06:35:09.8174079Z",
                  "message":{"role":"assistant","content":"Hello! How can I assist you today?"},
                  "done":true,
                  "done_reason":"stop",
                  "total_duration":826522800,
                  "load_duration":227743700,
                  "prompt_eval_count":30,
                  "prompt_eval_duration":323837600,
                  "eval_count":10,
                  "eval_duration":266840500
                }
                """));

        OllamaChatProvider provider = new OllamaChatProvider(baseUrl());
        ProviderRequest request = ProviderRequest.create(
                "qwen2.5:7b",
                List.of(ConversationMessage.user("hello")),
                List.of(),
                false
        );

        var response = provider.generate(request);

        assertEquals(ProviderStopReason.COMPLETE, response.stopReason());
        assertEquals("Hello! How can I assist you today?", response.content());
    }

    private HttpServer startServer(ExchangeHandler handler) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/api/chat", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        httpServer.start();
        return httpServer;
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void writeJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, payload.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(payload);
        }
    }

    private void writeStream(HttpExchange exchange, String... lines) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/x-ndjson");
        exchange.sendResponseHeaders(200, 0);
        try (PrintWriter writer = new PrintWriter(exchange.getResponseBody(), true, StandardCharsets.UTF_8)) {
            for (String line : lines) {
                writer.println(line.strip());
                writer.flush();
            }
        }
    }

    private void assertFalseValue(boolean value) {
        assertEquals(false, value);
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
