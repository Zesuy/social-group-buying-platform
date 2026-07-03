package com.example.groupshop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("upload_asset_references")
public class UploadAssetReference {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long assetId;
    private String objectKey;
    private String refType;
    private Long refId;
    private String fieldName;
    private LocalDateTime createdAt;
}
