package com.example.groupshop.groupbuy;

import com.example.groupshop.groupbuy.config.AiPolishProperties;
import com.example.groupshop.groupbuy.dto.GroupBuyAiPolishRequest;
import com.example.groupshop.groupbuy.dto.GroupBuyAiPolishResponse;
import com.example.groupshop.groupbuy.service.OpenAiGroupBuyPolishClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiGroupBuyPolishClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void generate_shouldCallChatCompletionsWithJsonSchemaAndParseSuggestion() throws Exception {
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<JsonNode> requestBody = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            path.set(exchange.getRequestURI().getPath());
            requestBody.set(objectMapper.readTree(exchange.getRequestBody()));
            byte[] response = chatCompletionResponse().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        OpenAiGroupBuyPolishClient client = new OpenAiGroupBuyPolishClient(properties(), objectMapper);
        GroupBuyAiPolishRequest request = new GroupBuyAiPolishRequest();
        request.setTitle("周末蜜桃");

        OpenAiGroupBuyPolishClient.GenerationResult result = client.generate(request);

        assertThat(path.get()).isEqualTo("/chat/completions");
        assertThat(requestBody.get().path("messages").isArray()).isTrue();
        assertThat(requestBody.get().path("response_format").path("type").asText()).isEqualTo("json_schema");
        assertThat(requestBody.get().path("response_format").path("json_schema").path("name").asText())
                .isEqualTo("group_buy_polish");
        assertThat(result.hasResponse()).isTrue();
        assertThat(result.fallbackReason()).isNull();
        assertThat(result.response().getSource()).isEqualTo("openai");
        assertThat(result.response().getTitle()).isEqualTo("周末蜜桃社区团");
        assertThat(result.response().getContentBlocks()).extracting("type").containsExactly("paragraph");
    }

    @Test
    void generate_shouldReturnFallbackReasonWhenProviderDisabled() {
        AiPolishProperties properties = new AiPolishProperties();
        properties.setProvider("local");

        OpenAiGroupBuyPolishClient client = new OpenAiGroupBuyPolishClient(properties, objectMapper);

        OpenAiGroupBuyPolishClient.GenerationResult result = client.generate(new GroupBuyAiPolishRequest());

        assertThat(result.hasResponse()).isFalse();
        assertThat(result.fallbackReason()).isEqualTo("未启用 OpenAI：GROUPSHOP_AI_POLISH_PROVIDER=local");
    }

    @Test
    void generate_shouldReturnFallbackReasonWhenApiKeyMissing() {
        AiPolishProperties properties = new AiPolishProperties();
        properties.setProvider("openai");
        properties.getOpenai().setApiKey("");

        OpenAiGroupBuyPolishClient client = new OpenAiGroupBuyPolishClient(properties, objectMapper);

        OpenAiGroupBuyPolishClient.GenerationResult result = client.generate(new GroupBuyAiPolishRequest());

        assertThat(result.hasResponse()).isFalse();
        assertThat(result.fallbackReason()).isEqualTo("缺少 OpenAI API Key：OPENAI_API_KEY 为空");
    }

    @Test
    void generate_shouldReturnFallbackReasonWhenOpenAiReturnsErrorStatus() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            byte[] response = "{\"error\":{\"message\":\"bad key\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(401, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        OpenAiGroupBuyPolishClient client = new OpenAiGroupBuyPolishClient(properties(), objectMapper);

        OpenAiGroupBuyPolishClient.GenerationResult result = client.generate(new GroupBuyAiPolishRequest());

        assertThat(result.hasResponse()).isFalse();
        assertThat(result.fallbackReason()).isEqualTo("OpenAI 请求失败，HTTP 401：bad key");
    }

    @Test
    void generate_shouldUseJsonObjectResponseFormatForDeepSeekCompatibleBaseUrl() throws Exception {
        AtomicReference<JsonNode> requestBody = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api.deepseek.com/chat/completions", exchange -> {
            requestBody.set(objectMapper.readTree(exchange.getRequestBody()));
            byte[] response = chatCompletionResponse().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        AiPolishProperties properties = properties();
        properties.getOpenai().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/api.deepseek.com");
        OpenAiGroupBuyPolishClient client = new OpenAiGroupBuyPolishClient(properties, objectMapper);

        OpenAiGroupBuyPolishClient.GenerationResult result = client.generate(new GroupBuyAiPolishRequest());

        assertThat(result.hasResponse()).isTrue();
        assertThat(requestBody.get().path("response_format").path("type").asText()).isEqualTo("json_object");
        assertThat(requestBody.get().path("response_format").has("json_schema")).isFalse();
    }

    private AiPolishProperties properties() {
        AiPolishProperties properties = new AiPolishProperties();
        properties.setProvider("openai");
        properties.getOpenai().setApiKey("test-key");
        properties.getOpenai().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.getOpenai().setModel("gpt-test");
        return properties;
    }

    private String chatCompletionResponse() throws IOException {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("type", "paragraph");
        block.put("title", null);
        block.put("text", "按需下单，统一履约。");
        block.put("url", null);
        block.put("caption", null);
        block.put("items", null);

        Map<String, Object> suggestion = new LinkedHashMap<>();
        suggestion.put("title", "周末蜜桃社区团");
        suggestion.put("introduction", "这次给大家整理了适合家庭分享的蜜桃。");
        suggestion.put("contentBlocks", List.of(block));
        String content = objectMapper.writeValueAsString(suggestion);

        return objectMapper.writeValueAsString(Map.of(
                "choices", List.of(Map.of(
                        "message", Map.of("content", content)
                ))
        ));
    }
}
