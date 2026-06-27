package com.example.groupshop.membercard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for listing member cards.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberCardListResponse {

    private List<MemberCardResponse> items;
}
