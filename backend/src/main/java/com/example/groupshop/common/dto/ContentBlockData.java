package com.example.groupshop.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Value object for a single content block in a group buy's structured body.
 *
 * <p>Used in API responses to represent the deserialized content_blocks JSON.
 * Each block has a {@code type} from the allowed set and type-dependent fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentBlockData {

    private String type;
    private String text;
    private String title;
    private String url;
    private String caption;
    private List<String> items;
}
