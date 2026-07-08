package com.example.groupshop.groupbuy;

import com.example.groupshop.common.dto.ContentBlockData;
import com.example.groupshop.common.util.ContentValidationUtil;
import com.example.groupshop.common.util.CurrentStoreHelper;
import com.example.groupshop.groupbuy.dto.GroupBuyAiPolishRequest;
import com.example.groupshop.groupbuy.dto.GroupBuyAiPolishResponse;
import com.example.groupshop.groupbuy.service.OpenAiGroupBuyPolishClient;
import com.example.groupshop.groupbuy.service.GroupBuyAiPolishService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.groupshop.model.entity.Leader;
import com.example.groupshop.model.entity.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupBuyAiPolishServiceTest {

    private CurrentStoreHelper currentStoreHelper;
    private OpenAiGroupBuyPolishClient openAiClient;
    private GroupBuyAiPolishService service;

    @BeforeEach
    void setUp() {
        currentStoreHelper = mock(CurrentStoreHelper.class);
        openAiClient = mock(OpenAiGroupBuyPolishClient.class);
        when(currentStoreHelper.getLeaderAndStore(1L))
                .thenReturn(new CurrentStoreHelper.LeaderAndStore(new Leader(), new Store()));
        when(openAiClient.generate(org.mockito.ArgumentMatchers.any()))
                .thenReturn(OpenAiGroupBuyPolishClient.GenerationResult.fallback("未启用 OpenAI：GROUPSHOP_AI_POLISH_PROVIDER=local"));
        service = new GroupBuyAiPolishService(
                currentStoreHelper,
                new ContentValidationUtil(new ObjectMapper()),
                openAiClient
        );
    }

    @Test
    void polish_shouldUseItemsAndDeliveryContext() {
        GroupBuyAiPolishRequest request = new GroupBuyAiPolishRequest();
        request.setTitle("周末蜜桃");
        request.setIntroduction("全网最低，香甜多汁");
        request.setDeliveryType("express");
        request.setEndTime("2026-07-01T12:00:00+08:00");

        GroupBuyAiPolishRequest.ItemContext item = new GroupBuyAiPolishRequest.ItemContext();
        item.setDisplayName("白玉蜜桃 5 斤装");
        item.setGroupPriceAmount(2990L);
        item.setGroupStock(20);
        item.setDescription("山东蒙阴产地直发");
        request.setItems(List.of(item));

        GroupBuyAiPolishResponse response = service.polish(1L, request);

        assertThat(response.getSource()).isEqualTo("local");
        assertThat(response.getFallbackReason()).isEqualTo("未启用 OpenAI：GROUPSHOP_AI_POLISH_PROVIDER=local");
        assertThat(response.getTitle()).isEqualTo("周末蜜桃团购");
        assertThat(response.getIntroduction()).doesNotContain("全网最低");
        assertThat(response.getIntroduction()).contains("本团共 1 个商品");
        assertThat(response.getContentBlocks()).extracting("type")
                .contains("paragraph", "section", "deliveryNote");
        assertThat(response.getContentBlocks())
                .flatExtracting(block -> block.getItems() == null ? List.of() : block.getItems())
                .contains("白玉蜜桃 5 斤装，团购价 ¥29.90，限量 20 份");
        verify(currentStoreHelper).getLeaderAndStore(1L);
    }

    @Test
    void polish_shouldReturnFallbackWhenContextEmpty() {
        GroupBuyAiPolishResponse response = service.polish(1L, new GroupBuyAiPolishRequest());

        assertThat(response.getTitle()).isEqualTo("本周社区精选团购");
        assertThat(response.getIntroduction()).contains("社区精选好物");
        assertThat(response.getContentBlocks()).isNotEmpty();
        assertThat(response.getContentBlocks()).extracting("type")
                .contains("paragraph", "deliveryNote");
    }

    @Test
    void polish_shouldFilterRiskyClaimsAndHtml() {
        GroupBuyAiPolishRequest request = new GroupBuyAiPolishRequest();
        request.setTitle("<b>绝对好货</b>");
        request.setIntroduction("100%保证，包治");

        GroupBuyAiPolishResponse response = service.polish(1L, request);

        assertThat(response.getTitle()).doesNotContain("<b>", "绝对");
        assertThat(response.getIntroduction()).doesNotContain("100%保证", "包治");
    }

    @Test
    void polish_shouldNotRequireDeliveryTime() {
        GroupBuyAiPolishRequest request = new GroupBuyAiPolishRequest();
        request.setTitle("鸡蛋");

        GroupBuyAiPolishResponse response = service.polish(1L, request);

        assertThat(response.getTitle()).isEqualTo("鸡蛋团购");
        assertThat(response.getContentBlocks()).isNotEmpty();
    }

    @Test
    void polish_shouldUseConfiguredModelSuggestionWhenAvailable() {
        GroupBuyAiPolishRequest request = new GroupBuyAiPolishRequest();
        request.setTitle("桃子");

        when(openAiClient.generate(request)).thenReturn(OpenAiGroupBuyPolishClient.GenerationResult.success(GroupBuyAiPolishResponse.builder()
                .source("openai")
                .title("社区蜜桃安心团")
                .introduction("这次主推应季蜜桃，适合家庭日常分享。")
                .contentBlocks(List.of(
                        ContentBlockData.builder()
                                .type("paragraph")
                                .text("团长整理好规格和库存，大家按需下单。")
                                .build(),
                        ContentBlockData.builder()
                                .type("list")
                                .title("推荐理由")
                                .items(List.of("规格清楚", "统一履约"))
                                .build()
                ))
                .build()));

        GroupBuyAiPolishResponse response = service.polish(1L, request);

        assertThat(response.getSource()).isEqualTo("openai");
        assertThat(response.getFallbackReason()).isNull();
        assertThat(response.getTitle()).isEqualTo("社区蜜桃安心团");
        assertThat(response.getContentBlocks()).extracting("type")
                .containsExactly("paragraph", "list");
    }

    @Test
    void polish_shouldFallBackToLocalBlocksWhenModelReturnsInvalidBlocks() {
        GroupBuyAiPolishRequest request = new GroupBuyAiPolishRequest();
        request.setTitle("桃子");

        when(openAiClient.generate(request)).thenReturn(OpenAiGroupBuyPolishClient.GenerationResult.success(GroupBuyAiPolishResponse.builder()
                .source("openai")
                .title("绝对好桃")
                .introduction("100%保证，香甜多汁")
                .contentBlocks(List.of(ContentBlockData.builder()
                        .type("section")
                        .title("")
                        .text("")
                        .build()))
                .build()));

        GroupBuyAiPolishResponse response = service.polish(1L, request);

        assertThat(response.getSource()).isEqualTo("openai");
        assertThat(response.getTitle()).doesNotContain("绝对");
        assertThat(response.getIntroduction()).doesNotContain("100%保证");
        assertThat(response.getFallbackReason()).contains("OpenAI 正文块未通过校验");
        assertThat(response.getContentBlocks()).isNotEmpty();
        assertThat(response.getContentBlocks()).extracting("type").contains("paragraph");
    }
}
