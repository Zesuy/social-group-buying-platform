package com.example.groupshop.groupbuy.service;

import com.example.groupshop.common.dto.ContentBlockData;
import com.example.groupshop.groupbuy.config.AiPolishProperties;
import com.example.groupshop.groupbuy.dto.GroupBuyAiPolishRequest;
import com.example.groupshop.groupbuy.dto.GroupBuyAiPolishResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI Chat Completions API client for structured group-buy copy generation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiGroupBuyPolishClient {

    private static final String SYSTEM_PROMPT = """
            你是微信私域社群团购的发布文案助手。你只根据用户提供的表单上下文生成发布建议。
            输出必须是结构化 JSON，不能包含 Markdown、HTML、脚本、iframe、style 或 javascript/data URL。
            不要编造产地、品牌、功效、认证、最低价、库存、配送承诺或售后承诺。
            避免“全网最低”“绝对”“100%保证”“包治”等夸大和高风险表达。
            文案要像团长发给社区邻居看的说明，清楚、可信、具体，不要像平台大促广告。
            contentBlocks 建议 3 到 6 块：paragraph 开场说明，section 推荐理由，list 商品要点，deliveryNote 履约提醒。
            不生成 image 块，因为图片必须来自用户上传。
            """;

    private final AiPolishProperties properties;
    private final ObjectMapper objectMapper;

    public GenerationResult generate(GroupBuyAiPolishRequest request) {
        if (!properties.useOpenAi()) {
            String reason = openAiDisabledReason();
            log.debug("Group-buy AI polish uses local fallback: {}", reason);
            return GenerationResult.fallback(reason);
        }
        if (containsWhitespace(properties.getOpenai().getApiKey())) {
            return GenerationResult.fallback("OPENAI_API_KEY 含空格，请检查复制的 Key");
        }

        try {
            String requestJson = objectMapper.writeValueAsString(buildRequestBody(request));
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(normalizedBaseUrl() + "/chat/completions"))
                    .timeout(Duration.ofSeconds(properties.getOpenai().getTimeoutSeconds()))
                    .header("Authorization", "Bearer " + properties.getOpenai().getApiKey().trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(properties.getOpenai().getTimeoutSeconds()))
                    .build();
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("OpenAI group-buy polish request failed with status {}, falling back to local",
                        response.statusCode());
                return GenerationResult.fallback("OpenAI 请求失败，HTTP "
                        + response.statusCode()
                        + safeUpstreamErrorMessage(response.body()));
            }
            GroupBuyAiPolishResponse parsed = parseResponse(response.body());
            if (parsed == null) {
                return GenerationResult.fallback("OpenAI 响应为空或未返回 message.content");
            }
            log.info("OpenAI group-buy polish request succeeded with model {}", properties.getOpenai().getModel());
            return GenerationResult.success(parsed);
        } catch (JsonProcessingException e) {
            log.warn("OpenAI group-buy polish response parse failed, falling back to local: {}", e.getMessage());
            return GenerationResult.fallback("OpenAI 响应 JSON 解析失败");
        } catch (IOException | InterruptedException | RuntimeException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                log.warn("OpenAI group-buy polish request interrupted, falling back to local");
                return GenerationResult.fallback("OpenAI 请求被中断");
            }
            log.warn("OpenAI group-buy polish request failed, falling back to local: {}", e.getMessage());
            return GenerationResult.fallback("OpenAI 请求异常或超时");
        }
    }

    private Map<String, Object> buildRequestBody(GroupBuyAiPolishRequest request) throws JsonProcessingException {
        String userContext = objectMapper.writeValueAsString(Map.of(
                "title", safe(request.getTitle()),
                "introduction", safe(request.getIntroduction()),
                "deliveryType", safe(request.getDeliveryType()),
                "startTime", safe(request.getStartTime()),
                "endTime", safe(request.getEndTime()),
                "shippingTime", safe(request.getShippingTime()),
                "items", safeItems(request.getItems())
        ));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getOpenai().getModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", buildUserPrompt(userContext))
        ));
        body.put("response_format", responseFormat());
        return body;
    }

    private String buildUserPrompt(String userContext) {
        if (!usesJsonObjectResponseFormat()) {
            return "请基于以下团购发布上下文生成一次结构化润色建议，必须只输出一个 JSON 对象，不要包含 Markdown 或解释：\n" + userContext;
        }
        return """
                请基于以下团购发布上下文生成一次结构化润色建议。
                必须只输出一个 JSON 对象，不要包含 Markdown 或解释。
                JSON 顶层必须包含且只能包含 title、introduction、contentBlocks 三个字段。
                title 必须是 1 到 48 字的字符串。
                introduction 必须是 1 到 180 字的字符串。
                contentBlocks 必须是 3 到 6 个对象组成的数组。
                contentBlocks 每个对象只能使用 type、title、text、url、caption、items 字段。
                允许的 type 只有 paragraph、section、list、deliveryNote。
                paragraph 和 deliveryNote 必须填写 text。
                section 至少填写 title 或 text。
                list 必须填写 items 字符串数组。
                不要输出 image 块，url 和 caption 可省略。
                输出示例形状：
                {"title":"周末鲜果社区团","introduction":"这次整理了适合家庭分享的鲜果，按需下单，统一履约。","contentBlocks":[{"type":"paragraph","text":"本次团购适合家庭日常分享。"},{"type":"section","title":"团长推荐","text":"规格清楚，按需下单。"},{"type":"list","title":"商品要点","items":["规格清楚","统一履约"]},{"type":"deliveryNote","text":"截单和履约时间以页面展示为准。"}]}
                团购发布上下文：
                """ + userContext;
    }

    private Map<String, Object> responseFormat() {
        if (usesJsonObjectResponseFormat()) {
            return Map.of("type", "json_object");
        }
        return Map.of(
                "type", "json_schema",
                "json_schema", Map.of(
                        "name", "group_buy_polish",
                        "strict", true,
                        "schema", responseSchema()
                )
        );
    }

    private boolean usesJsonObjectResponseFormat() {
        String baseUrl = normalizedBaseUrl().toLowerCase();
        return baseUrl.contains("api.deepseek.com");
    }

    private Map<String, Object> responseSchema() {
        Map<String, Object> nullableString = Map.of("type", List.of("string", "null"));
        Map<String, Object> nullableItems = Map.of(
                "anyOf", List.of(
                        Map.of("type", "array", "items", Map.of("type", "string"), "maxItems", 10),
                        Map.of("type", "null")
                )
        );

        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("title", "introduction", "contentBlocks"),
                "properties", Map.of(
                        "title", Map.of("type", "string", "maxLength", 48),
                        "introduction", Map.of("type", "string", "maxLength", 180),
                        "contentBlocks", Map.of(
                                "type", "array",
                                "minItems", 1,
                                "maxItems", 8,
                                "items", Map.of(
                                        "type", "object",
                                        "additionalProperties", false,
                                        "required", List.of("type", "title", "text", "url", "caption", "items"),
                                        "properties", Map.of(
                                                "type", Map.of("type", "string", "enum", List.of("paragraph", "section", "list", "deliveryNote")),
                                                "title", nullableString,
                                                "text", nullableString,
                                                "url", nullableString,
                                                "caption", nullableString,
                                                "items", nullableItems
                                        )
                                )
                        )
                )
        );
    }

    private GroupBuyAiPolishResponse parseResponse(String responseBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);
        String text = root.path("choices").path(0).path("message").path("content").asText(null);
        if (text == null || text.isBlank()) return null;

        JsonNode suggestion = objectMapper.readTree(text);
        List<ContentBlockData> blocks = new ArrayList<>();
        JsonNode blockNodes = suggestion.get("contentBlocks");
        if (blockNodes != null && blockNodes.isArray()) {
            for (JsonNode blockNode : blockNodes) {
                blocks.add(ContentBlockData.builder()
                        .type(textOrNull(blockNode, "type"))
                        .title(textOrNull(blockNode, "title"))
                        .text(textOrNull(blockNode, "text"))
                        .url(textOrNull(blockNode, "url"))
                        .caption(textOrNull(blockNode, "caption"))
                        .items(listOrNull(blockNode.get("items")))
                        .build());
            }
        }

        return GroupBuyAiPolishResponse.builder()
                .title(textOrEmpty(suggestion, "title"))
                .introduction(textOrEmpty(suggestion, "introduction"))
                .contentBlocks(blocks)
                .source("openai")
                .build();
    }

    private List<Map<String, Object>> safeItems(List<GroupBuyAiPolishRequest.ItemContext> items) {
        if (items == null) return List.of();
        return items.stream()
                .filter(item -> item != null && item.getDisplayName() != null && !item.getDisplayName().isBlank())
                .limit(12)
                .map(item -> Map.<String, Object>of(
                        "productId", safe(item.getProductId()),
                        "displayName", safe(item.getDisplayName()),
                        "groupPriceAmount", item.getGroupPriceAmount() == null ? 0 : item.getGroupPriceAmount(),
                        "groupStock", item.getGroupStock() == null ? 0 : item.getGroupStock(),
                        "description", safe(item.getDescription())
                ))
                .toList();
    }

    private String normalizedBaseUrl() {
        String baseUrl = properties.getOpenai().getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) return "https://api.openai.com/v1";
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String textOrEmpty(JsonNode node, String field) {
        String value = textOrNull(node, field);
        return value == null ? "" : value;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private List<String> listOrNull(JsonNode node) {
        if (node == null || node.isNull() || !node.isArray()) return null;
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (item != null && item.isTextual()) values.add(item.asText());
        }
        return values;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String openAiDisabledReason() {
        String provider = properties.getProvider();
        if (!"openai".equalsIgnoreCase(provider == null ? "" : provider.trim())) {
            return "未启用 OpenAI：GROUPSHOP_AI_POLISH_PROVIDER=" + (provider == null || provider.isBlank()
                    ? "(空)"
                    : provider);
        }
        String apiKey = properties.getOpenai().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return "缺少 OpenAI API Key：OPENAI_API_KEY 为空";
        }
        return "未启用 OpenAI 或缺少 API Key";
    }

    private boolean containsWhitespace(String value) {
        return value != null && value.chars().anyMatch(Character::isWhitespace);
    }

    private String safeUpstreamErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return "";
        try {
            String message = objectMapper.readTree(responseBody).path("error").path("message").asText("");
            message = cleanUpstreamMessage(message);
            if (message.isBlank()) return "";
            return "：" + limit(message, 120);
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    private String cleanUpstreamMessage(String message) {
        if (message == null) return "";
        return message
                .replaceAll("sk-[A-Za-z0-9_-]+", "sk-***")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value == null ? "" : value;
        return value.substring(0, maxLength - 1) + "…";
    }

    public record GenerationResult(GroupBuyAiPolishResponse response, String fallbackReason) {

        public static GenerationResult success(GroupBuyAiPolishResponse response) {
            return new GenerationResult(response, null);
        }

        public static GenerationResult fallback(String reason) {
            return new GenerationResult(null, reason);
        }

        public boolean hasResponse() {
            return response != null;
        }
    }
}
