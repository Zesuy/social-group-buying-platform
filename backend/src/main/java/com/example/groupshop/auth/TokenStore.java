package com.example.groupshop.auth;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MVP in-memory token store.
 *
 * <p>Tokens are UUID-based strings mapped to user IDs.
 * This is stateless across restarts but sufficient for MVP mock-login.
 */
@Component
public class TokenStore {

    private final Map<String, Long> tokenToUser = new ConcurrentHashMap<>();

    /**
     * Generate a new token for the given user ID.
     */
    public String createToken(Long userId) {
        String token = "mock_token_" + UUID.randomUUID().toString().replace("-", "");
        tokenToUser.put(token, userId);
        return token;
    }

    /**
     * Resolve the user ID from a token, or {@code null} if the token is invalid.
     */
    public Long resolveUserId(String token) {
        return tokenToUser.get(token);
    }
}
