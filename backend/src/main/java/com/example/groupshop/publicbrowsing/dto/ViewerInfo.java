package com.example.groupshop.publicbrowsing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Viewer info for public browsing responses.
 * Batch 9+ will return real subscription status for authenticated users.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewerInfo {
    private boolean subscribed;
}
