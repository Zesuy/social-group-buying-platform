package com.example.groupshop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 幂等键记录表 — idempotency_keys
 *
 * <p>Records the processing state and result of requests carrying an
 * {@code Idempotency-Key} header. Enables safe retry of critical operations
 * (order creation, payment, cancel, complete, ship) without side effects.
 *
 * <p>Unique constraint on {@code (userId, method, path, idempotencyKey)}
 * ensures that the same user sending the same key to the same endpoint
 * always gets the same result.
 */
@Data
@TableName("idempotency_keys")
public class IdempotencyKey {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private String method;
    private String path;
    private String idempotencyKey;
    private String requestHash;

    /** processing / succeeded / failed */
    private String status;

    /** JSON snapshot of the response data when status=succeeded */
    private String responseBodyJson;

    /** Error code when status=failed */
    private String errorCode;

    /** Error message when status=failed */
    private String errorMessage;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
