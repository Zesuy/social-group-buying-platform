package com.example.groupshop.idempotency;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.model.entity.IdempotencyKey;
import com.example.groupshop.model.mapper.IdempotencyKeyMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.function.Supplier;

/**
 * Service for Idempotency-Key support.
 *
 * <p>Decorates critical operations (order creation, payment, cancel,
 * complete, ship) so that the same user sending the same key to the
 * same endpoint always gets the same result. Only applies when an
 * {@code Idempotency-Key} header is present; without it the underlying
 * service operates unchanged.
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final IdempotencyKeyMapper idempotencyKeyMapper;
    private final ObjectMapper objectMapper;

    private static final String STATUS_PROCESSING = "processing";
    private static final String STATUS_SUCCEEDED = "succeeded";
    private static final String STATUS_FAILED = "failed";

    private static final long PROCESSING_TIMEOUT_MS = 10_000;

    // ── Execute with idempotency ────────────────────────────────────────

    /**
     * Execute a supplier function with idempotency protection.
     *
     * <p>If {@code idempotencyKey} is null or blank, the supplier is
     * executed directly without idempotency checks.
     *
     * @param userId        the authenticated user
     * @param method        the HTTP method (e.g. "POST")
     * @param path          the actual request path
     * @param idempotencyKey the value of the Idempotency-Key header
     * @param requestBody   the raw request body (for hash comparison), may be null
     * @param responseType  the class of the response type (for replay deserialization)
     * @param supplier      the actual business logic
     * @param <T>           the response type
     * @return the result
     */
    public <T> T execute(Long userId, String method, String path,
                         String idempotencyKey, String requestBody,
                         Class<T> responseType,
                         Supplier<T> supplier) {
        // No idempotency key — execute directly (backward compatible)
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return supplier.get();
        }

        String requestHash = hashRequestBody(requestBody);

        // Try to find existing record (shared read, no lock)
        IdempotencyKey existing = idempotencyKeyMapper.selectOne(
                new LambdaQueryWrapper<IdempotencyKey>()
                        .eq(IdempotencyKey::getUserId, userId)
                        .eq(IdempotencyKey::getMethod, method)
                        .eq(IdempotencyKey::getPath, path)
                        .eq(IdempotencyKey::getIdempotencyKey, idempotencyKey));

        if (existing != null) {
            return handleExisting(existing, requestHash, responseType, supplier);
        }

        // First request — insert new processing record
        IdempotencyKey record = new IdempotencyKey();
        record.setUserId(userId);
        record.setMethod(method);
        record.setPath(path);
        record.setIdempotencyKey(idempotencyKey);
        record.setRequestHash(requestHash);
        record.setStatus(STATUS_PROCESSING);

        try {
            idempotencyKeyMapper.insert(record);
        } catch (Exception e) {
            // Unique constraint violation: concurrent insert won
            // Re-read and handle as existing
            IdempotencyKey concurrent = idempotencyKeyMapper.selectOne(
                    new LambdaQueryWrapper<IdempotencyKey>()
                            .eq(IdempotencyKey::getUserId, userId)
                            .eq(IdempotencyKey::getMethod, method)
                            .eq(IdempotencyKey::getPath, path)
                            .eq(IdempotencyKey::getIdempotencyKey, idempotencyKey));
            if (concurrent != null) {
                return handleExisting(concurrent, requestHash, responseType, supplier);
            }
            throw e;
        }

        return executeAndUpdate(record, supplier);
    }

    // ── Handle existing record ──────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private <T> T handleExisting(IdempotencyKey existing, String requestHash,
                                  Class<T> responseType, Supplier<T> supplier) {
        // Check hash mismatch
        if (!existing.getRequestHash().equals(requestHash)) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_MISMATCH,
                    "同一 Idempotency-Key 请求体不一致");
        }

        switch (existing.getStatus()) {
            case STATUS_SUCCEEDED:
                // Replay the cached success response
                return replaySuccess(existing, responseType);
            case STATUS_FAILED:
                // Replay the cached error
                throw replayFailure(existing);
            case STATUS_PROCESSING:
                // First request may still be in-flight — wait briefly then re-check
                return handleProcessing(existing, requestHash, responseType, supplier);
            default:
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "未知的幂等状态: " + existing.getStatus());
        }
    }

    /**
     * Handle the case where the existing record is still in {@code processing} state.
     * Waits briefly for the first request to complete, then re-reads.
     *
     * <p>Does NOT re-execute the supplier on timeout — doing so would break
     * idempotency if the first request actually succeeded but crashed before
     * persisting the {@code succeeded} record. The client must retry with the
     * same key; once the record lands in {@code succeeded} / {@code failed},
     * the retry will replay the stored result.
     */
    private <T> T handleProcessing(IdempotencyKey existing, String requestHash,
                                     Class<T> responseType, Supplier<T> supplier) {
        // Check if the processing record has timed out
        if (existing.getCreatedAt() != null
                && Duration.between(existing.getCreatedAt(), LocalDateTime.now()).toMillis() > PROCESSING_TIMEOUT_MS) {
            // Timeout — first request may have crashed. Do NOT re-execute.
            // Client must retry; if the record eventually lands in succeeded/failed,
            // the retry will replay it.
            throw new BusinessException(ErrorCode.IDEMPOTENCY_IN_PROCESSING,
                    "请求正在处理中，请稍后重试");
        }

        // Brief wait and re-check
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        IdempotencyKey refreshed = idempotencyKeyMapper.selectById(existing.getId());
        if (refreshed != null && !STATUS_PROCESSING.equals(refreshed.getStatus())) {
            // Completed now — handle normally
            return handleExisting(refreshed, requestHash, responseType, supplier);
        }

        // Still processing after wait — tell client to retry
        throw new BusinessException(ErrorCode.IDEMPOTENCY_IN_PROCESSING,
                "请求正在处理中，请稍后重试");
    }

    // ── Execute supplier and update record ──────────────────────────────

    @SuppressWarnings("unchecked")
    private <T> T executeAndUpdate(IdempotencyKey record, Supplier<T> supplier) {
        try {
            T result = supplier.get();

            // Serialize result to JSON for caching
            String responseJson;
            try {
                responseJson = objectMapper.writeValueAsString(result);
            } catch (Exception e) {
                log.warn("Failed to serialize idempotency response, storing null: {}", e.getMessage());
                responseJson = null;
            }

            record.setStatus(STATUS_SUCCEEDED);
            record.setResponseBodyJson(responseJson);
            idempotencyKeyMapper.updateById(record);

            return result;
        } catch (BusinessException e) {
            // Store the business error for replay
            record.setStatus(STATUS_FAILED);
            record.setErrorCode(e.getCode());
            record.setErrorMessage(e.getMessage());
            idempotencyKeyMapper.updateById(record);

            throw e;
        }
    }

    // ── Replay helpers ──────────────────────────────────────────────────

    private <T> T replaySuccess(IdempotencyKey record, Class<T> responseType) {
        if (record.getResponseBodyJson() != null && responseType != null) {
            try {
                return objectMapper.readValue(record.getResponseBodyJson(), responseType);
            } catch (Exception e) {
                log.warn("Failed to deserialize cached idempotency response as {}: {}",
                        responseType.getSimpleName(), e.getMessage());
            }
        }
        return null;
    }

    private BusinessException replayFailure(IdempotencyKey record) {
        ErrorCode errorCode = resolveErrorCode(record.getErrorCode());
        return new BusinessException(errorCode, record.getErrorMessage() != null
                ? record.getErrorMessage()
                : errorCode.getDefaultMessage());
    }

    // ── Hash helper ─────────────────────────────────────────────────────

    /**
     * Generate a SHA-256 hex hash of the request body.
     * For null/empty bodies, use the path as the hash source.
     */
    private String hashRequestBody(String requestBody) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String source = (requestBody != null && !requestBody.isBlank()) ? requestBody : "";
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    // ── Error code resolver ─────────────────────────────────────────────

    private ErrorCode resolveErrorCode(String code) {
        if (code == null) {
            return ErrorCode.INTERNAL_ERROR;
        }
        for (ErrorCode ec : ErrorCode.values()) {
            if (ec.getCode().equals(code)) {
                return ec;
            }
        }
        return ErrorCode.INTERNAL_ERROR;
    }
}
