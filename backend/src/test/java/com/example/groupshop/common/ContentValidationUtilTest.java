package com.example.groupshop.common;

import com.example.groupshop.base.ServiceTestBase;
import com.example.groupshop.common.dto.ContentBlockRequest;
import com.example.groupshop.common.dto.ContentBlockData;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.common.util.ContentValidationUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ContentValidationUtil}.
 */
class ContentValidationUtilTest extends ServiceTestBase {

    @Autowired
    private ContentValidationUtil contentValidationUtil;

    // ── Gallery image URL validation ─────────────────────────────────

    @Test
    void validateImageUrls_shouldAcceptValidUrls() {
        contentValidationUtil.validateImageUrls(List.of(
                "https://example.com/1.jpg",
                "http://example.com/2.png",
                "/uploads/images/2026/07/local-upload.jpg"
        ), 9, "galleryImageUrls");
        // no exception
    }

    @Test
    void validateImageUrls_shouldRejectTooManyUrls() {
        List<String> urls = List.of("https://example.com/1.jpg",
                "https://example.com/2.jpg", "https://example.com/3.jpg",
                "https://example.com/4.jpg", "https://example.com/5.jpg",
                "https://example.com/6.jpg", "https://example.com/7.jpg",
                "https://example.com/8.jpg", "https://example.com/9.jpg",
                "https://example.com/10.jpg");
        assertThatThrownBy(() -> contentValidationUtil.validateImageUrls(urls, 9, "galleryImageUrls"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("最多");
    }

    @Test
    void validateImageUrls_shouldRejectBlankUrl() {
        List<String> urls = List.of("https://example.com/1.jpg", "");
        assertThatThrownBy(() -> contentValidationUtil.validateImageUrls(urls, 9, "galleryImageUrls"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能为空");
    }

    @Test
    void validateImageUrls_shouldRejectNonHttpUrl() {
        List<String> urls = List.of("ftp://example.com/1.jpg");
        assertThatThrownBy(() -> contentValidationUtil.validateImageUrls(urls, 9, "galleryImageUrls"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只允许");
    }

    @Test
    void validateImageUrls_shouldAcceptNull() {
        contentValidationUtil.validateImageUrls(null, 9, "galleryImageUrls");
        // no exception
    }

    @Test
    void validateImageUrls_shouldAcceptEmpty() {
        contentValidationUtil.validateImageUrls(List.of(), 9, "galleryImageUrls");
        // no exception
    }

    // ── Content block validation ─────────────────────────────────────

    @Test
    void validateContentBlocks_shouldAcceptValidBlocks() {
        ContentBlockRequest p = new ContentBlockRequest();
        p.setType("paragraph");
        p.setText("这是一段正文");

        ContentBlockRequest s = new ContentBlockRequest();
        s.setType("section");
        s.setTitle("小标题");
        s.setText("正文内容");

        ContentBlockRequest img = new ContentBlockRequest();
        img.setType("image");
        img.setUrl("/uploads/images/2026/07/photo.png");
        img.setCaption("图片说明");

        ContentBlockRequest list = new ContentBlockRequest();
        list.setType("list");
        list.setItems(List.of("卖点一", "卖点二"));

        ContentBlockRequest note = new ContentBlockRequest();
        note.setType("deliveryNote");
        note.setText("48小时内发货");

        contentValidationUtil.validateContentBlocks(List.of(p, s, img, list, note));
        // no exception
    }

    @Test
    void validateContentBlocks_shouldRejectUnknownType() {
        ContentBlockRequest block = new ContentBlockRequest();
        block.setType("unknown");
        block.setText("测试");

        assertThatThrownBy(() -> contentValidationUtil.validateContentBlocks(List.of(block)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不合法");
    }

    @Test
    void validateContentBlocks_shouldRejectScriptInText() {
        ContentBlockRequest block = new ContentBlockRequest();
        block.setType("paragraph");
        block.setText("<script>alert(1)</script>");

        assertThatThrownBy(() -> contentValidationUtil.validateContentBlocks(List.of(block)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("HTML");
    }

    @Test
    void validateContentBlocks_shouldRejectIframeInText() {
        ContentBlockRequest block = new ContentBlockRequest();
        block.setType("paragraph");
        block.setText("<iframe src='x'></iframe>");

        assertThatThrownBy(() -> contentValidationUtil.validateContentBlocks(List.of(block)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("HTML");
    }

    @Test
    void validateContentBlocks_shouldRejectStyleInText() {
        ContentBlockRequest block = new ContentBlockRequest();
        block.setType("paragraph");
        block.setText("<style>body{color:red}</style>");

        assertThatThrownBy(() -> contentValidationUtil.validateContentBlocks(List.of(block)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("HTML");
    }

    @Test
    void validateContentBlocks_shouldRejectEmptyType() {
        ContentBlockRequest block = new ContentBlockRequest();
        block.setType("");

        assertThatThrownBy(() -> contentValidationUtil.validateContentBlocks(List.of(block)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能为空");
    }

    @Test
    void validateContentBlocks_shouldRejectEmptyParagraph() {
        ContentBlockRequest block = new ContentBlockRequest();
        block.setType("paragraph");

        assertThatThrownBy(() -> contentValidationUtil.validateContentBlocks(List.of(block)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("paragraph.text");
    }

    @Test
    void validateContentBlocks_shouldRejectEmptySection() {
        ContentBlockRequest block = new ContentBlockRequest();
        block.setType("section");

        assertThatThrownBy(() -> contentValidationUtil.validateContentBlocks(List.of(block)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("同时为空");
    }

    @Test
    void validateContentBlocks_shouldRejectEmptyDeliveryNote() {
        ContentBlockRequest block = new ContentBlockRequest();
        block.setType("deliveryNote");

        assertThatThrownBy(() -> contentValidationUtil.validateContentBlocks(List.of(block)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("deliveryNote.text");
    }

    @Test
    void validateContentBlocks_shouldRejectTooManyBlocks() {
        ContentBlockRequest block = new ContentBlockRequest();
        block.setType("paragraph");
        block.setText(".");
        List<ContentBlockRequest> blocks = java.util.stream.Stream.generate(() -> {
            ContentBlockRequest b = new ContentBlockRequest();
            b.setType("paragraph");
            b.setText(".");
            return b;
        }).limit(21).collect(java.util.stream.Collectors.toList());

        assertThatThrownBy(() -> contentValidationUtil.validateContentBlocks(blocks))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("最多");
    }

    @Test
    void validateContentBlocks_shouldRejectImageWithoutUrl() {
        ContentBlockRequest block = new ContentBlockRequest();
        block.setType("image");

        assertThatThrownBy(() -> contentValidationUtil.validateContentBlocks(List.of(block)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能为空");
    }

    @Test
    void validateContentBlocks_shouldRejectImageWithJavascriptUrl() {
        ContentBlockRequest block = new ContentBlockRequest();
        block.setType("image");
        block.setUrl("javascript:alert(1)");

        assertThatThrownBy(() -> contentValidationUtil.validateContentBlocks(List.of(block)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只允许");
    }

    @Test
    void validateContentBlocks_shouldRejectListWithNoItems() {
        ContentBlockRequest block = new ContentBlockRequest();
        block.setType("list");

        assertThatThrownBy(() -> contentValidationUtil.validateContentBlocks(List.of(block)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("至少");
    }

    // ── JSON serialization / deserialization ─────────────────────────

    @Test
    void serializeDeserializeImageUrls_shouldRoundTrip() {
        List<String> original = List.of("https://example.com/a.jpg", "https://example.com/b.jpg");
        String json = contentValidationUtil.serializeImageUrls(original);
        List<String> result = contentValidationUtil.deserializeImageUrls(json);
        assertThat(result).containsExactlyElementsOf(original);
    }

    @Test
    void serializeImageUrls_shouldReturnNullForNullInput() {
        assertThat(contentValidationUtil.serializeImageUrls(null)).isNull();
    }

    @Test
    void deserializeImageUrls_shouldReturnEmptyForNullInput() {
        assertThat(contentValidationUtil.deserializeImageUrls(null)).isEmpty();
    }

    @Test
    void deserializeImageUrls_shouldReturnEmptyForBlankInput() {
        assertThat(contentValidationUtil.deserializeImageUrls("")).isEmpty();
    }

    @Test
    void serializeDeserializeContentBlocks_shouldRoundTrip() {
        ContentBlockData block = ContentBlockData.builder()
                .type("section")
                .title("标题")
                .text("正文")
                .build();
        String json = contentValidationUtil.serializeContentBlocks(List.of(block));
        List<ContentBlockData> result = contentValidationUtil.deserializeContentBlocks(json);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo("section");
        assertThat(result.get(0).getTitle()).isEqualTo("标题");
    }

    @Test
    void deserializeContentBlocks_shouldReturnEmptyForNullInput() {
        assertThat(contentValidationUtil.deserializeContentBlocks(null)).isEmpty();
    }
}
