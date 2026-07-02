package com.example.groupshop.publicbrowsing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Viewer info for public browsing responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewerInfo {
    private boolean subscribed;
    private boolean favorited;
}
