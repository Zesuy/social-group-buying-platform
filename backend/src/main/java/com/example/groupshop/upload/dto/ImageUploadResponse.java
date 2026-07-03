package com.example.groupshop.upload.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImageUploadResponse {

    private Long assetId;

    private String url;

    private String objectKey;

    private String originalFilename;

    private String contentType;

    private long size;

    private String status;
}
