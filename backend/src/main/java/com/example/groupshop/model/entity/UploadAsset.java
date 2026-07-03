package com.example.groupshop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("upload_assets")
public class UploadAsset {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long uploaderUserId;
    private String objectKey;
    private String url;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    private String checksumSha256;
    private String status;
    private Integer referenceCount;
    private LocalDateTime lastReferencedAt;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
