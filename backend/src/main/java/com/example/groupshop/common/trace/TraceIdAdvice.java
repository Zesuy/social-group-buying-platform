package com.example.groupshop.common.trace;

import com.example.groupshop.common.response.ApiResponse;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.UUID;

/**
 * Ensures every {@link ApiResponse} has a traceId.
 * In production the {@link TraceIdFilter} sets MDC first; this advice reads it.
 * In tests where the filter may not run, a fallback ID is generated.
 */
@ControllerAdvice
public class TraceIdAdvice implements ResponseBodyAdvice<ApiResponse<?>> {

    private static final String MDC_KEY = "traceId";

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return ApiResponse.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public ApiResponse<?> beforeBodyWrite(ApiResponse<?> body, MethodParameter returnType,
                                          MediaType selectedContentType,
                                          Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                          ServerHttpRequest request, ServerHttpResponse response) {
        if (body != null && body.getTraceId() == null) {
            String traceId = MDC.get(MDC_KEY);
            if (traceId == null || traceId.isBlank()) {
                traceId = "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            }
            body.setTraceId(traceId);
        }
        return body;
    }
}
