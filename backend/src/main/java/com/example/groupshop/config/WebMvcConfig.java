package com.example.groupshop.config;

import com.example.groupshop.auth.AuthInterceptor;
import com.example.groupshop.upload.config.UploadProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final UploadProperties uploadProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/v1/**")
                .allowedOriginPatterns(
                        "http://localhost",
                        "http://localhost:*",
                        "http://127.0.0.1",
                        "http://127.0.0.1:*",
                        "https://localhost",
                        "https://localhost:*",
                        "http://192.168.*:*",
                        "http://10.0.2.2:*",
                        "http://shop.zesuy.top",
                        "https://shop.zesuy.top",
                        "capacitor://localhost"
                )
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Idempotency-Key")
                .exposedHeaders("X-Trace-Id")
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(
                        "/api/v1/health",
                        "/api/v1/auth/mock-login",
                        "/api/v1/auth/codes",
                        "/api/v1/auth/login",
                        "/api/v1/auth/register",
                        "/api/v1/payments/alipay/sandbox/notify",
                        "/api/v1/_test/**",
                        "/api/v1/group-buys",
                        "/api/v1/group-buys/**",
                        "/api/v1/categories",
                        "/api/v1/leaders/*/homepage",
                        "/api/v1/leaders/*/coupons",
                        "/api/v1/share/**"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = uploadProperties.getLocalDir().toAbsolutePath().normalize();
        String location = uploadDir.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
