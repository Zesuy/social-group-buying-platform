package com.example.groupshop.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Store entity — maps to the {@code stores} table.
 */
@Data
@TableName("stores")
public class Store {

    private Long id;
    private Long leaderId;
    private String name;
    private String logoUrl;
    private String description;
    private String defaultDeliveryType;
    private Boolean distributionEnabled;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
