package com.example.groupshop.payment.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.groupshop.common.enums.ErrorCode;
import com.example.groupshop.common.exception.BusinessException;
import com.example.groupshop.model.entity.Order;
import com.example.groupshop.order.dto.OrderResponse;
import com.example.groupshop.order.service.OrderService;
import com.example.groupshop.payment.config.PaymentProperties;
import com.example.groupshop.payment.dto.PaymentStartResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String MODE_SIMULATE = "simulate";
    private static final String MODE_ZERO_AMOUNT = "zeroAmount";
    private static final String MODE_SANDBOX_ALIPAY = "sandboxAlipay";
    private static final String CHARSET = StandardCharsets.UTF_8.name();
    private static final String SIGN_TYPE = "RSA2";

    private final PaymentProperties paymentProperties;
    private final OrderService orderService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaymentStartResponse startPayment(Long userId, Long orderId) {
        Order order = orderService.requirePayableOrderForUser(userId, orderId, true);
        if (order.getPayAmount() <= 0) {
            return PaymentStartResponse.builder()
                    .mode(MODE_ZERO_AMOUNT)
                    .order(orderService.completeZeroAmountPayment(userId, orderId))
                    .build();
        }
        if (!paymentProperties.getSandbox().isEnabled()) {
            return PaymentStartResponse.builder()
                    .mode(MODE_SIMULATE)
                    .order(orderService.simulatePay(userId, orderId))
                    .build();
        }

        validateAlipayConfig();
        try {
            return PaymentStartResponse.builder()
                    .mode(MODE_SANDBOX_ALIPAY)
                    .formHtml(buildWapPayForm(order))
                    .build();
        } catch (AlipayApiException | JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "支付宝沙箱支付发起失败");
        }
    }

    public boolean handleAlipayNotify(Map<String, String> params) {
        if (!paymentProperties.getSandbox().isEnabled()) {
            return false;
        }
        try {
            validateAlipayConfig();
        } catch (BusinessException e) {
            return false;
        }
        try {
            boolean verified = AlipaySignature.rsaCheckV1(
                    params,
                    paymentProperties.getAlipay().getPublicKey(),
                    CHARSET,
                    SIGN_TYPE);
            if (!verified) {
                return false;
            }
        } catch (AlipayApiException e) {
            return false;
        }

        if (!safeEquals(paymentProperties.getAlipay().getAppId(), params.get("app_id"))) {
            return false;
        }
        String tradeStatus = params.get("trade_status");
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            return false;
        }

        String orderNo = params.get("out_trade_no");
        String totalAmount = params.get("total_amount");
        if (orderNo == null || orderNo.isBlank() || totalAmount == null || totalAmount.isBlank()) {
            return false;
        }

        try {
            Order order = orderService.findOrderByOrderNo(orderNo);
            if (!amountOfFen(order.getPayAmount()).equals(totalAmount)) {
                return false;
            }
            orderService.completePaidOrderFromCallback(orderNo);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    private String buildWapPayForm(Order order) throws AlipayApiException, JsonProcessingException {
        AlipayClient client = new DefaultAlipayClient(
                paymentProperties.getAlipay().getGateway(),
                paymentProperties.getAlipay().getAppId(),
                paymentProperties.getAlipay().getAppPrivateKey(),
                "json",
                CHARSET,
                paymentProperties.getAlipay().getPublicKey(),
                SIGN_TYPE);

        AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
        request.setNotifyUrl(trimTrailingSlash(paymentProperties.getAlipay().getBackendPublicBaseUrl())
                + "/api/v1/payments/alipay/sandbox/notify");
        request.setReturnUrl(buildReturnUrl(order.getId()));
        request.setBizContent(buildBizContent(order));

        return client.pageExecute(request).getBody();
    }

    private String buildBizContent(Order order) throws JsonProcessingException {
        Map<String, String> bizContent = new LinkedHashMap<>();
        bizContent.put("out_trade_no", order.getOrderNo());
        bizContent.put("total_amount", amountOfFen(order.getPayAmount()));
        bizContent.put("subject", "邻鲜团订单 " + order.getOrderNo());
        bizContent.put("product_code", "QUICK_WAP_WAY");
        bizContent.put("quit_url", trimTrailingSlash(paymentProperties.getAlipay().getFrontendPublicBaseUrl())
                + "/#/orders/" + order.getId());
        return objectMapper.writeValueAsString(bizContent);
    }

    private String buildReturnUrl(Long orderId) {
        return trimTrailingSlash(paymentProperties.getAlipay().getFrontendPublicBaseUrl())
                + "/#/orders/" + orderId + "?paymentReturn=alipay";
    }

    private String amountOfFen(Long amount) {
        return BigDecimal.valueOf(amount, 2).setScale(2).toPlainString();
    }

    private void validateAlipayConfig() {
        PaymentProperties.Alipay alipay = paymentProperties.getAlipay();
        if (isBlank(alipay.getAppId())
                || isBlank(alipay.getAppPrivateKey())
                || isBlank(alipay.getPublicKey())
                || isBlank(alipay.getGateway())
                || isBlank(alipay.getBackendPublicBaseUrl())
                || isBlank(alipay.getFrontendPublicBaseUrl())) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "支付宝沙箱配置不完整");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("/+$", "");
    }

    private boolean safeEquals(String expected, String actual) {
        return expected != null && expected.equals(actual);
    }
}
