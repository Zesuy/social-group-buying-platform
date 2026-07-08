package com.example.groupshop.groupbuy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the group-buy copy polish provider.
 */
@Data
@Component
@ConfigurationProperties(prefix = "groupshop.ai.polish")
public class AiPolishProperties {

    /**
     * Provider name. Supported values: local, openai.
     */
    private String provider = "local";

    private OpenAi openai = new OpenAi();

    public boolean useOpenAi() {
        return "openai".equalsIgnoreCase(provider)
                && openai.apiKey != null
                && !openai.apiKey.isBlank();
    }

    @Data
    public static class OpenAi {
        private String apiKey;
        private String baseUrl = "https://api.openai.com/v1";
        private String model = "gpt-4.1-mini";
        private int timeoutSeconds = 20;
    }
}
