package com.example.groupshop.common.dto;

import lombok.Data;

import java.util.List;

/**
 * Request DTO for a content block in group buy create / update payloads.
 *
 * <p>Same shape as {@link ContentBlockData} but without builder — used
 * for Jackson deserialization of incoming JSON.
 */
@Data
public class ContentBlockRequest {

    private String type;
    private String text;
    private String title;
    private String url;
    private String caption;
    private List<String> items;
}
