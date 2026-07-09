package com.example.groupshop.payment;

import com.alipay.api.internal.util.AlipaySignature;
import com.example.groupshop.model.entity.Order;
import com.example.groupshop.order.dto.OrderResponse;
import com.example.groupshop.order.service.OrderService;
import com.example.groupshop.payment.config.PaymentProperties;
import com.example.groupshop.payment.dto.PaymentStartResponse;
import com.example.groupshop.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final String APP_ID = "2021000000000000";
    private static final String CHARSET = "UTF-8";
    private static final String SIGN_TYPE = "RSA2";

    @Mock
    private OrderService orderService;

    private PaymentProperties properties;
    private PaymentService paymentService;
    private String privateKey;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        properties = new PaymentProperties();
        properties.getAlipay().setAppId(APP_ID);
        properties.getAlipay().setAppPrivateKey(privateKey);
        properties.getAlipay().setPublicKey(publicKey);
        properties.getAlipay().setGateway("https://openapi-sandbox.dl.alipaydev.com/gateway.do");
        properties.getAlipay().setBackendPublicBaseUrl("http://localhost:8080");
        properties.getAlipay().setFrontendPublicBaseUrl("http://localhost:5173");
        paymentService = new PaymentService(properties, orderService);
    }

    @Test
    void startPayment_shouldUseSimulateModeWhenSandboxDisabled() {
        Order order = order();
        OrderResponse paid = OrderResponse.builder()
                .id(order.getId())
                .payStatus("paid")
                .orderStatus("paid")
                .build();
        when(orderService.requirePayableOrderForUser(1L, order.getId(), true)).thenReturn(order);
        when(orderService.simulatePay(1L, order.getId())).thenReturn(paid);

        PaymentStartResponse response = paymentService.startPayment(1L, order.getId());

        assertThat(response.getMode()).isEqualTo("simulate");
        assertThat(response.getOrder()).isSameAs(paid);
    }

    @Test
    void startPayment_shouldReturnAlipayFormWhenSandboxEnabled() {
        properties.getSandbox().setEnabled(true);
        Order order = order();
        when(orderService.requirePayableOrderForUser(1L, order.getId(), true)).thenReturn(order);

        PaymentStartResponse response = paymentService.startPayment(1L, order.getId());

        assertThat(response.getMode()).isEqualTo("sandboxAlipay");
        assertThat(response.getFormHtml()).contains("alipay");
        assertThat(response.getOrder()).isNull();
    }

    @Test
    void startPayment_shouldCompleteZeroAmountBeforeSandboxRedirect() {
        properties.getSandbox().setEnabled(true);
        Order order = order();
        order.setPayAmount(0L);
        OrderResponse paid = OrderResponse.builder()
                .id(order.getId())
                .payStatus("paid")
                .orderStatus("paid")
                .build();
        when(orderService.requirePayableOrderForUser(1L, order.getId(), true)).thenReturn(order);
        when(orderService.completeZeroAmountPayment(1L, order.getId())).thenReturn(paid);

        PaymentStartResponse response = paymentService.startPayment(1L, order.getId());

        assertThat(response.getMode()).isEqualTo("zeroAmount");
        assertThat(response.getOrder()).isSameAs(paid);
        assertThat(response.getFormHtml()).isNull();
    }

    @Test
    void handleAlipayNotify_shouldCompleteOrderWhenSignatureAndAmountValid() throws Exception {
        properties.getSandbox().setEnabled(true);
        Order order = order();
        when(orderService.findOrderByOrderNo(order.getOrderNo())).thenReturn(order);
        when(orderService.completePaidOrderFromCallback(order.getOrderNo())).thenReturn(OrderResponse.builder().build());

        boolean handled = paymentService.handleAlipayNotify(signedNotifyParams("3.98", "TRADE_SUCCESS"));

        assertThat(handled).isTrue();
        verify(orderService).completePaidOrderFromCallback(order.getOrderNo());
    }

    @Test
    void handleAlipayNotify_shouldRejectAmountMismatch() throws Exception {
        properties.getSandbox().setEnabled(true);
        Order order = order();
        when(orderService.findOrderByOrderNo(order.getOrderNo())).thenReturn(order);

        boolean handled = paymentService.handleAlipayNotify(signedNotifyParams("3.99", "TRADE_SUCCESS"));

        assertThat(handled).isFalse();
        verify(orderService).findOrderByOrderNo(order.getOrderNo());
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void handleAlipayNotify_shouldIgnoreNonSuccessTradeStatus() throws Exception {
        properties.getSandbox().setEnabled(true);

        boolean handled = paymentService.handleAlipayNotify(signedNotifyParams("3.98", "WAIT_BUYER_PAY"));

        assertThat(handled).isFalse();
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void handleAlipayNotify_shouldRejectInvalidSignature() {
        properties.getSandbox().setEnabled(true);
        Map<String, String> params = new HashMap<>();
        params.put("app_id", APP_ID);
        params.put("out_trade_no", "202607090001");
        params.put("total_amount", "3.98");
        params.put("trade_status", "TRADE_SUCCESS");
        params.put("sign_type", SIGN_TYPE);
        params.put("sign", "invalid");

        boolean handled = paymentService.handleAlipayNotify(params);

        assertThat(handled).isFalse();
        verifyNoMoreInteractions(orderService);
    }

    private Map<String, String> signedNotifyParams(String totalAmount, String tradeStatus) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("app_id", APP_ID);
        params.put("out_trade_no", "202607090001");
        params.put("total_amount", totalAmount);
        params.put("trade_status", tradeStatus);
        params.put("sign_type", SIGN_TYPE);
        String content = AlipaySignature.getSignCheckContentV1(params);
        params.put("sign", AlipaySignature.rsaSign(content, privateKey, CHARSET, SIGN_TYPE));
        return params;
    }

    private Order order() {
        Order order = new Order();
        order.setId(9001L);
        order.setOrderNo("202607090001");
        order.setUserId(1L);
        order.setPayAmount(398L);
        order.setPayStatus("unpaid");
        order.setOrderStatus("pending_pay");
        return order;
    }
}
