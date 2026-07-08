package com.example.groupshop.groupbuy.dto;

import com.example.groupshop.common.dto.ContentBlockData;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Polished publishing copy suggested to the leader.
 */
@Data
@Builder
public class GroupBuyAiPolishResponse {

    private String title;
    private String introduction;
    private List<ContentBlockData> contentBlocks;
    private String source;
    private String fallbackReason;
}
