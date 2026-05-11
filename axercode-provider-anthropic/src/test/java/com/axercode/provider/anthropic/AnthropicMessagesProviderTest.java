package com.axercode.provider.anthropic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axercode.core.provider.ProviderRequest;
import com.axercode.core.provider.ProviderStopReason;
import com.axercode.core.provider.ProviderToolRound;
import com.axercode.core.session.ConversationMessage;
import com.axercode.core.tool.ToolCall;
import com.axercode.core.tool.ToolDefinition;
import com.axercode.core.tool.ToolExecutionResult;
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

class AnthropicMessagesProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void generateMapsPlainTextCompletion() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = startServer(exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            assertEquals("test-key", exchange.getRequestHeaders().getFirst("x-api-key"));
            assertEquals("2023-06-01", exchange.getRequestHeaders().getFirst("anthropic-version"));
            writeJson(exchange, 200, """
                    {
                      "id":"msg_1",
                      "type":"message",
                      "role":"assistant",
                      "model":"claude-3-5-sonnet-latest",
                      "content":[{"type":"text","text":"Anthropic ready"}],
                      "stop_reason":"end_turn"
                    }
                    """);
        });

        AnthropicMessagesProvider provider = new AnthropicMessagesProvider(baseUrl(), "test-key", "2023-06-01", 2048);

        var response = provider.generate(ProviderRequest.create(
                "claude-3-5-sonnet-latest",
                List.of(ConversationMessage.user("hello")),
                List.of(),
                false
        ));

        assertEquals(ProviderStopReason.COMPLETE, response.stopReason());
        assertEquals("Anthropic ready", response.content());

        JsonNode sentRequest = objectMapper.readTree(requestBody.get());
        assertEquals("claude-3-5-sonnet-latest", sentRequest.get("model").asText());
        assertEquals(2048, sentRequest.get("max_tokens").asInt());
        assertEquals(false, sentRequest.get("stream").asBoolean());
        assertEquals("user", sentRequest.get("messages").get(0).get("role").asText());
    }

    @Test
    void generateMapsToolUseBlocksIntoProviderResponse() throws Exception {
        server = startServer(exchange -> writeJson(exchange, 200, """
                {
                  "id":"msg_1",
                  "type":"message",
                  "role":"assistant",
                  "model":"claude-3-5-sonnet-latest",
                  "content":[
                    {"type":"tool_use","id":"toolu_123","name":"read_file","input":{"path":"README.md"}}
                  ],
                  "stop_reason":"tool_use"
                }
                """));

        AnthropicMessagesProvider provider = new AnthropicMessagesProvider(baseUrl(), "test-key", "2023-06-01", 2048);

        var response = provider.generate(ProviderRequest.create(
                "claude-3-5-sonnet-latest",
                List.of(ConversationMessage.user("inspect")),
                List.of(),
                false
        ));

        assertEquals(ProviderStopReason.TOOL_CALLS, response.stopReason());
        assertEquals("toolu_123", response.toolCalls().getFirst().id());
        assertEquals("read_file", response.toolCalls().getFirst().name());
        assertEquals("{\"path\":\"README.md\"}", response.toolCalls().getFirst().argumentsJson());
    }

    @Test
    void generateSendsStructuredToolRoundFollowUpBlocks() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = startServer(exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            writeJson(exchange, 200, """
                    {
                      "id":"msg_1",
                      "type":"message",
                      "role":"assistant",
                      "model":"claude-3-5-sonnet-latest",
                      "content":[{"type":"text","text":"done"}],
                      "stop_reason":"end_turn"
                    }
                    """);
        });

        AnthropicMessagesProvider provider = new AnthropicMessagesProvider(baseUrl(), "test-key", "2023-06-01", 2048);
        ToolCall toolCall = ToolCall.create("toolu_456", "run_shell", "{\"command\":\"echo hello\"}");
        ToolExecutionResult result = ToolExecutionResult.success(toolCall, "hello");

        provider.generate(ProviderRequest.create(
                "claude-3-5-sonnet-latest",
                List.of(
                        ConversationMessage.user("run it"),
                        new ConversationMessage(java.util.UUID.randomUUID(), com.axercode.core.session.MessageRole.TOOL, "TOOL run_shell [SUCCESS]\nhello", java.time.Instant.now()),
                        ConversationMessage.user("remember this")
                ),
                List.of(new ToolDefinition(
                        "run_shell",
                        "Run shell",
                        "{\"type\":\"object\",\"required\":[\"command\"],\"properties\":{\"command\":{\"type\":\"string\"}}}"
                )),
                false,
                new ProviderToolRound(List.of(toolCall), List.of(result))
        ));

        JsonNode sentRequest = objectMapper.readTree(requestBody.get());
        JsonNode messages = sentRequest.get("messages");
        assertEquals("user", messages.get(0).get("role").asText());
        assertEquals("assistant", messages.get(1).get("role").asText());
        assertEquals("tool_use", messages.get(1).get("content").get(0).get("type").asText());
        assertEquals("toolu_456", messages.get(1).get("content").get(0).get("id").asText());
        assertEquals("user", messages.get(2).get("role").asText());
        assertEquals("tool_result", messages.get(2).get("content").get(0).get("type").asText());
        assertEquals("toolu_456", messages.get(2).get("content").get(0).get("tool_use_id").asText());
        assertEquals("text", messages.get(2).get("content").get(1).get("type").asText());
        assertEquals("remember this", messages.get(2).get("content").get(1).get("text").asText());
    }

    @Test
    void streamGenerateEmitsTextDeltasAndReturnsFinalCompletion() throws Exception {
        server = startServer(exchange -> writeStream(exchange,
                "message_start", """
                {"type":"message_start","message":{"id":"msg_1","type":"message","role":"assistant","content":[]}}
                """,
                "content_block_start", """
                {"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}
                """,
                "content_block_delta", """
                {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Anthropic "}}
                """,
                "content_block_delta", """
                {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"stream"}}
                """,
                "message_stop", """
                {"type":"message_stop"}
                """
        ));

        AnthropicMessagesProvider provider = new AnthropicMessagesProvider(baseUrl(), "test-key", "2023-06-01", 2048);
        StringBuilder streamed = new StringBuilder();

        var response = provider.streamGenerate(ProviderRequest.create(
                "claude-3-5-sonnet-latest",
                List.of(ConversationMessage.user("hello")),
                List.of(),
                true
        ), streamed::append);

        assertEquals("Anthropic stream", streamed.toString());
        assertEquals(ProviderStopReason.COMPLETE, response.stopReason());
        assertEquals("Anthropic stream", response.content());
    }

    @Test
    void streamGenerateReturnsToolCallsFromStreamingToolUseBlocks() throws Exception {
        server = startServer(exchange -> writeStream(exchange,
                "message_start", """
                {"type":"message_start","message":{"id":"msg_1","type":"message","role":"assistant","content":[]}}
                """,
                "content_block_start", """
                {"type":"content_block_start","index":0,"content_block":{"type":"tool_use","id":"toolu_789","name":"read_file","input":{}}}
                """,
                "content_block_delta", """
                {"type":"content_block_delta","index":0,"delta":{"type":"input_json_delta","partial_json":"{\\"path\\":\\"README.md\\"}"}}
                """,
                "message_stop", """
                {"type":"message_stop"}
                """
        ));

        AnthropicMessagesProvider provider = new AnthropicMessagesProvider(baseUrl(), "test-key", "2023-06-01", 2048);
        StringBuilder streamed = new StringBuilder();

        var response = provider.streamGenerate(ProviderRequest.create(
                "claude-3-5-sonnet-latest",
                List.of(ConversationMessage.user("inspect")),
                List.of(),
                true
        ), streamed::append);

        assertEquals("", streamed.toString());
        assertEquals(ProviderStopReason.TOOL_CALLS, response.stopReason());
        assertEquals("toolu_789", response.toolCalls().getFirst().id());
        assertEquals("read_file", response.toolCalls().getFirst().name());
        assertEquals("{\"path\":\"README.md\"}", response.toolCalls().getFirst().argumentsJson());
    }

    @Test
    void generateRejectsMissingApiKey() {
        AnthropicMessagesProvider provider = new AnthropicMessagesProvider("http://127.0.0.1:8080", "", "2023-06-01", 2048);

        ProviderException exception = assertThrows(
                ProviderException.class,
                () -> provider.generate(ProviderRequest.create(
                        "claude-3-5-sonnet-latest",
                        List.of(ConversationMessage.user("hello")),
                        List.of(),
                        false
                ))
        );

        assertTrue(exception.getMessage().contains("API key"));
    }

    private HttpServer startServer(ExchangeHandler handler) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/v1/messages", exchange -> {
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

    private void writeStream(HttpExchange exchange, String... frames) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
        exchange.sendResponseHeaders(200, 0);
        try (PrintWriter writer = new PrintWriter(exchange.getResponseBody(), true, StandardCharsets.UTF_8)) {
            for (int index = 0; index < frames.length; index += 2) {
                writer.println("event: " + frames[index]);
                writer.println("data: " + frames[index + 1].strip());
                writer.println();
                writer.flush();
            }
        }
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
