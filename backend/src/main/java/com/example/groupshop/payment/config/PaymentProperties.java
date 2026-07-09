package com.example.groupshop.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "groupshop.payment")
public class PaymentProperties {

    private Sandbox sandbox = new Sandbox();
    private Alipay alipay = new Alipay();

    @Getter
    @Setter
    public static class Sandbox {
        private boolean enabled;
    }

    @Getter
    @Setter
    public static class Alipay {
        private String appId;
        private String appPrivateKey;
        private String publicKey;
        private String gateway;
        private String backendPublicBaseUrl;
        private String frontendPublicBaseUrl;
    }
}
