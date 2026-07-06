package com.example.groupshop.common.util;

import com.example.groupshop.common.dto.ContentBlockData;
import com.example.groupshop.common.dto.ContentBlockRequest;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility for validating and converting content-related fields
 * (gallery image URLs, content blocks, product detail image URLs).
 *
 * <p>Centralizes JSON serialization/deserialization, structural validation,
 * and safety checks so that services don't scatter string manipulation.
 */
@Component
@RequiredArgsConstructor
public class ContentValidationUtil {

    public static final int MAX_GALLERY_URLS = 9;
    public static final int MAX_DETAIL_URLS = 9;
    private static final int MAX_URL_LENGTH = 512;
    private static final int MAX_CONTENT_BLOCKS = 20;

    private static final int MAX_PARAGRAPH_TEXT = 1000;
    private static final int MAX_SECTION_TITLE = 40;
    private static final int MAX_SECTION_TEXT = 1000;
    private static final int MAX_IMAGE_URL_LENGTH = 512;
    private static final int MAX_IMAGE_CAPTION = 80;
    private static final int MAX_LIST_ITEMS = 10;
    private static final int MIN_LIST_ITEMS = 1;
    private static final int MAX_LIST_ITEM_TEXT = 80;
    private static final int MAX_DELIVERY_NOTE_TEXT = 1000;

    private static final Set<String> ALLOWED_BLOCK_TYPES = Set.of(
            "paragraph", "section", "image", "list", "deliveryNote"
    );

    private final ObjectMapper objectMapper;

    // ── URL list validation ────────────────────────────────────────────

    /**
     * Validate a list of image URLs (gallery or detail).
     *
     * @param urls       the URL list, may be null
     * @param maxCount   maximum allowed count
     * @param fieldLabel label for error messages (e.g. "galleryImageUrls")
     */
    public void validateImageUrls(List<String> urls, int maxCount, String fieldLabel) {
        if (urls == null || urls.isEmpty()) {
            return;
        }
        if (urls.size() > maxCount) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    fieldLabel + " 最多 " + maxCount + " 张图片");
        }
        for (int i = 0; i < urls.size(); i++) {
            String url = urls.get(i);
            if (url == null || url.isBlank()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        fieldLabel + " 第 " + (i + 1) + " 项不能为空");
            }
            if (url.length() > MAX_URL_LENGTH) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        fieldLabel + " 第 " + (i + 1) + " 项 URL 长度不能超过 " + MAX_URL_LENGTH);
            }
            if (!isAllowedImageUrl(url)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        fieldLabel + " 第 " + (i + 1) + " 项只允许 http://、https:// 或 /uploads/ 图片 URL");
            }
            if (containsHtml(url)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        fieldLabel + " 第 " + (i + 1) + " 项不能包含 HTML 标签");
            }
        }
    }

    // ── Content block validation ───────────────────────────────────────

    /**
     * Validate a list of content blocks.
     *
     * @param blocks the content blocks, may be null
     */
    public void validateContentBlocks(List<ContentBlockRequest> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return;
        }
        if (blocks.size() > MAX_CONTENT_BLOCKS) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "contentBlocks 最多 " + MAX_CONTENT_BLOCKS + " 块");
        }
        for (int i = 0; i < blocks.size(); i++) {
            ContentBlockRequest block = blocks.get(i);
            if (block.getType() == null || block.getType().isBlank()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "contentBlocks 第 " + (i + 1) + " 块类型不能为空");
            }
            if (!ALLOWED_BLOCK_TYPES.contains(block.getType())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "contentBlocks 第 " + (i + 1) + " 块类型 \"" + block.getType()
                                + "\" 不合法，只允许: " + String.join(", ", ALLOWED_BLOCK_TYPES));
            }
            validateBlockFields(block, i);
        }
    }

    private void validateBlockFields(ContentBlockRequest block, int index) {
        switch (block.getType()) {
            case "paragraph":
                if (block.getText() == null || block.getText().isBlank()) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                            "contentBlocks 第 " + (index + 1) + " 块 paragraph.text 不能为空");
                }
                checkMaxLength(block.getText(), MAX_PARAGRAPH_TEXT,
                        "contentBlocks 第 " + (index + 1) + " 块 paragraph.text");
                checkHtmlSafety(block.getText(), "contentBlocks 第 " + (index + 1) + " 块 paragraph.text");
                break;
            case "section":
                if ((block.getTitle() == null || block.getTitle().isBlank())
                        && (block.getText() == null || block.getText().isBlank())) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                            "contentBlocks 第 " + (index + 1) + " 块 section.title 和 section.text 不能同时为空");
                }
                checkMaxLength(block.getTitle(), MAX_SECTION_TITLE,
                        "contentBlocks 第 " + (index + 1) + " 块 section.title");
                checkMaxLength(block.getText(), MAX_SECTION_TEXT,
                        "contentBlocks 第 " + (index + 1) + " 块 section.text");
                checkHtmlSafety(block.getTitle(), "contentBlocks 第 " + (index + 1) + " 块 section.title");
                checkHtmlSafety(block.getText(), "contentBlocks 第 " + (index + 1) + " 块 section.text");
                break;
            case "image":
                if (block.getUrl() == null || block.getUrl().isBlank()) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                            "contentBlocks 第 " + (index + 1) + " 块 image.url 不能为空");
                }
                if (block.getUrl().length() > MAX_IMAGE_URL_LENGTH) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                            "contentBlocks 第 " + (index + 1) + " 块 image.url 长度不能超过 " + MAX_IMAGE_URL_LENGTH);
                }
                if (!isAllowedImageUrl(block.getUrl())) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                            "contentBlocks 第 " + (index + 1) + " 块 image.url 只允许 http://、https:// 或 /uploads/");
                }
                if (containsHtml(block.getUrl())) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                            "contentBlocks 第 " + (index + 1) + " 块 image.url 不能包含 HTML 标签");
                }
                checkMaxLength(block.getCaption(), MAX_IMAGE_CAPTION,
                        "contentBlocks 第 " + (index + 1) + " 块 image.caption");
                checkHtmlSafety(block.getCaption(), "contentBlocks 第 " + (index + 1) + " 块 image.caption");
                break;
            case "list":
                if (block.getItems() == null || block.getItems().size() < MIN_LIST_ITEMS) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                            "contentBlocks 第 " + (index + 1) + " 块 list.items 至少 " + MIN_LIST_ITEMS + " 项");
                }
                if (block.getItems().size() > MAX_LIST_ITEMS) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                            "contentBlocks 第 " + (index + 1) + " 块 list.items 最多 " + MAX_LIST_ITEMS + " 项");
                }
                for (int j = 0; j < block.getItems().size(); j++) {
                    String item = block.getItems().get(j);
                    checkMaxLength(item, MAX_LIST_ITEM_TEXT,
                            "contentBlocks 第 " + (index + 1) + " 块 list.items 第 " + (j + 1) + " 项");
                    checkHtmlSafety(item,
                            "contentBlocks 第 " + (index + 1) + " 块 list.items 第 " + (j + 1) + " 项");
                }
                break;
            case "deliveryNote":
                if (block.getText() == null || block.getText().isBlank()) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                            "contentBlocks 第 " + (index + 1) + " 块 deliveryNote.text 不能为空");
                }
                checkMaxLength(block.getText(), MAX_DELIVERY_NOTE_TEXT,
                        "contentBlocks 第 " + (index + 1) + " 块 deliveryNote.text");
                checkHtmlSafety(block.getText(), "contentBlocks 第 " + (index + 1) + " 块 deliveryNote.text");
                break;
            default:
                // Already validated above, unreachable
        }
    }

    // ── HTML safety checks ─────────────────────────────────────────────

    private static void checkHtmlSafety(String text, String label) {
        if (text == null || text.isBlank()) return;
        if (containsHtml(text)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    label + " 不能包含 HTML/script/iframe/style/javascript/data URL 内容");
        }
    }

    private static boolean containsHtml(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        // Check for script/iframe/style tags
        if (lower.contains("<script") || lower.contains("</script")
                || lower.contains("<iframe") || lower.contains("</iframe")
                || lower.contains("<style") || lower.contains("</style")) {
            return true;
        }
        // Check for event attributes (onclick, onerror, etc.)
        if (lower.matches(".*\\bon\\w+\\s*=.*")) {
            return true;
        }
        // Check for javascript: and data: in href/src
        if (lower.contains("javascript:") || lower.contains("data:")) {
            return true;
        }
        return false;
    }

    private static boolean isAllowedImageUrl(String url) {
        return url.startsWith("http://")
                || url.startsWith("https://")
                || url.startsWith("/uploads/");
    }

    private static void checkMaxLength(String text, int max, String label) {
        if (text != null && text.length() > max) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    label + " 长度不能超过 " + max + " 字");
        }
    }

    // ── JSON serialization / deserialization ───────────────────────────

    /**
     * Serialize a list of image URLs to a JSON string for storage.
     * Returns null if the list is null or empty.
     */
    public String serializeImageUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(urls);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片 URL 序列化失败");
        }
    }

    /**
     * Deserialize a JSON string to a list of image URLs.
     * Returns empty list if the string is null or blank.
     */
    public List<String> deserializeImageUrls(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片 URL 反序列化失败");
        }
    }

    /**
     * Serialize a list of content blocks to a JSON string for storage.
     * Returns null if the list is null or empty.
     */
    public String serializeContentBlocks(List<ContentBlockData> blocks) {
        if (blocks == null || blocks.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(blocks);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "内容块序列化失败");
        }
    }

    /**
     * Deserialize a JSON string to a list of content block data objects.
     * Returns empty list if the string is null or blank.
     */
    public List<ContentBlockData> deserializeContentBlocks(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<ContentBlockData>>() {});
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "内容块反序列化失败");
        }
    }

    /**
     * Convert a list of {@link ContentBlockRequest} to {@link ContentBlockData}.
     */
    public List<ContentBlockData> toContentBlockData(List<ContentBlockRequest> requests) {
        if (requests == null) return null;
        return requests.stream()
                .map(r -> ContentBlockData.builder()
                        .type(r.getType())
                        .text(r.getText())
                        .title(r.getTitle())
                        .url(r.getUrl())
                        .caption(r.getCaption())
                        .items(r.getItems() != null ? new ArrayList<>(r.getItems()) : null)
                        .build())
                .collect(Collectors.toList());
    }
}
