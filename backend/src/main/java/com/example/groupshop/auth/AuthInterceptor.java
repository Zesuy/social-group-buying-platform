package com.example.groupshop.auth;

import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that extracts the Bearer token from the {@code Authorization} header,
 * resolves the user ID via {@link TokenStore}, and sets it as a request attribute.
 *
 * <p>Only applied to paths that require authentication (see {@link com.example.groupshop.config.WebMvcConfig}).
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    public static final String USER_ID_ATTR = "currentUserId";

    private final TokenStore tokenStore;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Token is empty");
        }

        Long userId = tokenStore.resolveUserId(token);
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid or expired token");
        }

        request.setAttribute(USER_ID_ATTR, userId);
        return true;
    }
}
