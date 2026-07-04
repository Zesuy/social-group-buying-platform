package com.example.groupshop.auth.service;

import com.example.groupshop.auth.dto.SendAuthCodeRequest;
import com.example.groupshop.auth.dto.SendAuthCodeResponse;
import com.example.groupshop.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.groupshop.common.enums.ErrorCode.BUSINESS_RULE_VIOLATION;

/**
 * Demo verification-code service.
 *
 * <p>The public API shape matches a real SMS-code flow. For current demo and
 * development environments the code is fixed and returned to the frontend.
 */
@Service
public class AuthCodeService {

    private static final String DEMO_CODE = "123456";
    private static final int EXPIRES_IN_SECONDS = 300;

    private final Map<String, CodeRecord> records = new ConcurrentHashMap<>();

    public SendAuthCodeResponse sendCode(SendAuthCodeRequest request) {
        String key = buildKey(request.getPhone(), request.getScene());
        records.put(key, new CodeRecord(DEMO_CODE, LocalDateTime.now().plusSeconds(EXPIRES_IN_SECONDS)));
        return SendAuthCodeResponse.builder()
                .expiresInSeconds(EXPIRES_IN_SECONDS)
                .devCode(DEMO_CODE)
                .build();
    }

    public void verify(String phone, String scene, String code) {
        CodeRecord record = records.get(buildKey(phone, scene));
        if (record == null || record.expiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(BUSINESS_RULE_VIOLATION, "验证码已过期，请重新获取");
        }
        if (!record.code().equals(code)) {
            throw new BusinessException(BUSINESS_RULE_VIOLATION, "验证码不正确");
        }
    }

    private String buildKey(String phone, String scene) {
        return phone + ":" + scene;
    }

    private record CodeRecord(String code, LocalDateTime expiresAt) {
    }
}
