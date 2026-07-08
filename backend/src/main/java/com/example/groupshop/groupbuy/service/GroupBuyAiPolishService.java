package com.example.groupshop.groupbuy.service;

import com.example.groupshop.common.dto.ContentBlockData;
import com.example.groupshop.common.dto.ContentBlockRequest;
import com.example.groupshop.common.util.ContentValidationUtil;
import com.example.groupshop.common.util.CurrentStoreHelper;
import com.example.groupshop.groupbuy.dto.GroupBuyAiPolishRequest;
import com.example.groupshop.groupbuy.dto.GroupBuyAiPolishResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Copy assistant for the group-buy publish form.
 *
 * <p>The controller contract stays stable: a configured model provider can
 * generate structured suggestions, while the deterministic local generator
 * remains the fallback for tests and offline development.
 */
@Service
@RequiredArgsConstructor
public class GroupBuyAiPolishService {

    private static final int MAX_TITLE_LENGTH = 48;
    private static final int MAX_INTRO_LENGTH = 180;

    private final CurrentStoreHelper currentStoreHelper;
    private final ContentValidationUtil contentValidationUtil;
    private final OpenAiGroupBuyPolishClient openAiClient;

    public GroupBuyAiPolishResponse polish(Long userId, GroupBuyAiPolishRequest request) {
        currentStoreHelper.getLeaderAndStore(userId);

        GroupBuyAiPolishRequest safeRequest = request == null ? new GroupBuyAiPolishRequest() : request;
        GroupBuyAiPolishResponse fallback = buildLocalSuggestion(safeRequest, "local", null);

        OpenAiGroupBuyPolishClient.GenerationResult modelResult = openAiClient.generate(safeRequest);
        if (!modelResult.hasResponse()) {
            fallback.setFallbackReason(modelResult.fallbackReason());
            return fallback;
        }
        return sanitizeModelResponse(modelResult.response(), fallback);
    }

    private GroupBuyAiPolishResponse buildLocalSuggestion(
            GroupBuyAiPolishRequest safeRequest,
            String source,
            String fallbackReason) {
        List<GroupBuyAiPolishRequest.ItemContext> items = safeItems(safeRequest.getItems());
        String primaryItemName = primaryItemName(items);
        String title = polishTitle(safeRequest.getTitle(), primaryItemName);
        String introduction = polishIntroduction(safeRequest, items, primaryItemName);
        List<ContentBlockData> blocks = buildContentBlocks(safeRequest, items, introduction);

        return GroupBuyAiPolishResponse.builder()
                .title(title)
                .introduction(introduction)
                .contentBlocks(blocks)
                .source(source)
                .fallbackReason(fallbackReason)
                .build();
    }

    private GroupBuyAiPolishResponse sanitizeModelResponse(
            GroupBuyAiPolishResponse modelResponse,
            GroupBuyAiPolishResponse fallback) {
        String title = limit(removeRiskyClaims(clean(modelResponse.getTitle())), MAX_TITLE_LENGTH);
        boolean titleFallback = title.isBlank();
        if (titleFallback) title = fallback.getTitle();

        String introduction = limit(
                removeRiskyClaims(clean(modelResponse.getIntroduction())),
                MAX_INTRO_LENGTH
        );
        boolean introductionFallback = introduction.isBlank();
        if (introductionFallback) introduction = fallback.getIntroduction();

        List<String> fallbackReasons = new ArrayList<>();
        if (titleFallback) {
            fallbackReasons.add("OpenAI 标题为空，已使用本地标题");
        }
        if (introductionFallback) {
            fallbackReasons.add("OpenAI 介绍为空，已使用本地介绍");
        }

        List<ContentBlockData> blocks = sanitizeBlocks(modelResponse.getContentBlocks());
        if (blocks.isEmpty()) {
            blocks = fallback.getContentBlocks();
            fallbackReasons.add(modelResponse.getContentBlocks() == null || modelResponse.getContentBlocks().isEmpty()
                    ? "OpenAI 未返回可用正文块，已使用本地正文块"
                    : "OpenAI 正文块未通过校验，已使用本地正文块");
        }

        return GroupBuyAiPolishResponse.builder()
                .title(title)
                .introduction(introduction)
                .contentBlocks(blocks)
                .source(modelResponse.getSource() == null || modelResponse.getSource().isBlank()
                        ? "openai"
                        : modelResponse.getSource())
                .fallbackReason(fallbackReasons.isEmpty() ? null : String.join("；", fallbackReasons))
                .build();
    }

    private List<ContentBlockData> sanitizeBlocks(List<ContentBlockData> rawBlocks) {
        if (rawBlocks == null || rawBlocks.isEmpty()) return List.of();

        List<ContentBlockRequest> requests = rawBlocks.stream()
                .map(this::sanitizeBlock)
                .filter(block -> block != null)
                .limit(20)
                .toList();
        if (requests.isEmpty()) return List.of();

        try {
            contentValidationUtil.validateContentBlocks(requests);
            return contentValidationUtil.toContentBlockData(requests);
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private ContentBlockRequest sanitizeBlock(ContentBlockData block) {
        if (block == null) return null;
        String type = clean(block.getType());
        ContentBlockRequest request = new ContentBlockRequest();
        request.setType(type);
        switch (type) {
            case "paragraph" -> {
                String text = limit(removeRiskyClaims(clean(block.getText())), 1000);
                if (text.isBlank()) return null;
                request.setText(text);
            }
            case "section" -> {
                String title = limit(removeRiskyClaims(clean(block.getTitle())), 40);
                String text = limit(removeRiskyClaims(clean(block.getText())), 1000);
                if (title.isBlank() && text.isBlank()) return null;
                request.setTitle(title);
                request.setText(text);
            }
            case "list" -> {
                List<String> items = block.getItems() == null ? List.of() : block.getItems().stream()
                        .map(item -> limit(removeRiskyClaims(clean(item)), 80))
                        .filter(item -> !item.isBlank())
                        .limit(10)
                        .toList();
                if (items.isEmpty()) return null;
                request.setTitle(limit(removeRiskyClaims(clean(block.getTitle())), 40));
                request.setItems(items);
            }
            case "deliveryNote" -> {
                String text = limit(removeRiskyClaims(clean(block.getText())), 1000);
                if (text.isBlank()) return null;
                request.setText(text);
            }
            default -> {
                return null;
            }
        }
        return request;
    }

    private String polishTitle(String rawTitle, String primaryItemName) {
        String cleaned = removeRiskyClaims(clean(rawTitle));
        if (cleaned.isBlank()) {
            cleaned = primaryItemName.isBlank() ? "本周社区精选团购" : primaryItemName + "社区团购";
        }
        if (!cleaned.contains("团") && !cleaned.contains("团购")) {
            cleaned = cleaned + "团购";
        }
        return limit(cleaned, MAX_TITLE_LENGTH);
    }

    private String polishIntroduction(
            GroupBuyAiPolishRequest request,
            List<GroupBuyAiPolishRequest.ItemContext> items,
            String primaryItemName) {
        String base = removeRiskyClaims(clean(request.getIntroduction()));
        if (base.isBlank()) {
            if (!primaryItemName.isBlank()) {
                base = "这次给大家整理了" + primaryItemName + "，适合家庭日常囤货和社群一起拼团。";
            } else {
                base = "这次给大家整理了一组社区精选好物，适合家庭日常囤货和社群一起拼团。";
            }
        }

        List<String> parts = new ArrayList<>();
        parts.add(ensureEndsWithPunctuation(base));
        if (!items.isEmpty()) {
            parts.add("本团共 " + items.size() + " 个商品，价格和库存以下单页展示为准。");
        }
        String deliveryText = deliveryText(request.getDeliveryType());
        if (!deliveryText.isBlank()) {
            parts.add("配送方式为" + deliveryText + "，截单和发货时间请以页面展示为准。");
        }
        return limit(String.join("", parts), MAX_INTRO_LENGTH);
    }

    private List<ContentBlockData> buildContentBlocks(
            GroupBuyAiPolishRequest request,
            List<GroupBuyAiPolishRequest.ItemContext> items,
            String introduction) {
        List<ContentBlockData> blocks = new ArrayList<>();

        blocks.add(ContentBlockData.builder()
                .type("paragraph")
                .text(introduction)
                .build());

        if (!items.isEmpty()) {
            blocks.add(ContentBlockData.builder()
                    .type("section")
                    .title("团长推荐")
                    .text(buildRecommendationText(items))
                    .build());

            List<String> points = items.stream()
                    .map(this::itemPoint)
                    .filter(point -> !point.isBlank())
                    .limit(5)
                    .toList();
            if (!points.isEmpty()) {
                blocks.add(ContentBlockData.builder()
                        .type("list")
                        .items(points)
                        .build());
            }
        }

        String deliveryNote = buildDeliveryNote(request);
        if (!deliveryNote.isBlank()) {
            blocks.add(ContentBlockData.builder()
                    .type("deliveryNote")
                    .text(deliveryNote)
                    .build());
        }

        blocks.add(ContentBlockData.builder()
                .type("section")
                .title("售后说明")
                .text("收到后如有破损、漏发或数量不符，请及时联系团长沟通处理。")
                .build());

        return blocks.stream().limit(20).toList();
    }

    private String buildRecommendationText(List<GroupBuyAiPolishRequest.ItemContext> items) {
        String first = primaryItemName(items);
        if (items.size() == 1) {
            String description = removeRiskyClaims(clean(items.get(0).getDescription()));
            if (!description.isBlank()) {
                return limit("本次主推" + first + "。" + ensureEndsWithPunctuation(description), 160);
            }
            return "本次主推" + first + "，适合日常购买和邻里拼团。";
        }
        return "本次把大家常买的几款商品放在一个团里，方便一次下单、统一履约。";
    }

    private String itemPoint(GroupBuyAiPolishRequest.ItemContext item) {
        String name = clean(item.getDisplayName());
        if (name.isBlank()) return "";
        StringBuilder builder = new StringBuilder(name);
        if (item.getGroupPriceAmount() != null && item.getGroupPriceAmount() > 0) {
            builder.append("，团购价 ").append(formatAmount(item.getGroupPriceAmount()));
        }
        if (item.getGroupStock() != null && item.getGroupStock() > 0) {
            builder.append("，限量 ").append(item.getGroupStock()).append(" 份");
        }
        return builder.toString();
    }

    private String buildDeliveryNote(GroupBuyAiPolishRequest request) {
        List<String> parts = new ArrayList<>();
        String delivery = deliveryText(request.getDeliveryType());
        if (!delivery.isBlank()) {
            parts.add("配送方式：" + delivery);
        }
        if (!clean(request.getEndTime()).isBlank()) {
            parts.add("截单时间以页面结束时间为准");
        }
        if (!clean(request.getShippingTime()).isBlank()) {
            parts.add("发货时间以页面发货时间为准");
        }
        if (parts.isEmpty()) {
            return "截单、发货和自提安排以团长在群内通知及页面展示为准。";
        }
        return String.join("；", parts) + "。";
    }

    private List<GroupBuyAiPolishRequest.ItemContext> safeItems(List<GroupBuyAiPolishRequest.ItemContext> items) {
        if (items == null) return List.of();
        return items.stream()
                .filter(item -> item != null && !clean(item.getDisplayName()).isBlank())
                .toList();
    }

    private String primaryItemName(List<GroupBuyAiPolishRequest.ItemContext> items) {
        if (items.isEmpty()) return "";
        return limit(clean(items.get(0).getDisplayName()), 32);
    }

    private String deliveryText(String deliveryType) {
        return switch (clean(deliveryType)) {
            case "express" -> "快递配送";
            case "pickup" -> "到店自提";
            case "local_delivery" -> "同城配送";
            default -> "";
        };
    }

    private String formatAmount(long amount) {
        return "¥" + String.format("%.2f", amount / 100.0);
    }

    private String clean(String value) {
        if (value == null) return "";
        return value.replaceAll("<[^>]+>", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String removeRiskyClaims(String value) {
        return value
                .replace("全网最低", "价格清楚")
                .replace("最低价", "实惠价")
                .replace("包治", "适合")
                .replace("100%保证", "尽量保障")
                .replace("百分百保证", "尽量保障")
                .replace("绝对", "比较")
                .replace("无敌", "很不错");
    }

    private String ensureEndsWithPunctuation(String value) {
        if (value.isBlank()) return value;
        if (value.endsWith("。") || value.endsWith("！") || value.endsWith("？")) return value;
        return value + "。";
    }

    private String limit(String value, int maxLength) {
        if (value == null) return "";
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength - 1) + "…";
    }
}
