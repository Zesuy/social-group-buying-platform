package com.example.groupshop.payment;

import com.example.groupshop.base.MockMvcTestBase;
import com.example.groupshop.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentControllerTest extends MockMvcTestBase {

    private static final String ALIPAY_NOTIFY_URL = "/api/v1/payments/alipay/sandbox/notify";

    @MockBean
    private PaymentService paymentService;

    @Test
    void alipaySandboxNotify_shouldAllowAnonymousAndReturnSuccessText() throws Exception {
        when(paymentService.handleAlipayNotify(argThat(params ->
                "202607090001".equals(params.get("out_trade_no"))))).thenReturn(true);

        mockMvc.perform(post(ALIPAY_NOTIFY_URL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("app_id", "2021000000000000")
                        .param("out_trade_no", "202607090001")
                        .param("total_amount", "3.98")
                        .param("trade_status", "TRADE_SUCCESS")
                        .param("sign_type", "RSA2")
                        .param("sign", "signed"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("success"));

        verify(paymentService).handleAlipayNotify(argThat(params ->
                "TRADE_SUCCESS".equals(params.get("trade_status"))
                        && "signed".equals(params.get("sign"))));
    }

    @Test
    void alipaySandboxNotify_shouldReturnFailureTextWhenRejected() throws Exception {
        when(paymentService.handleAlipayNotify(argThat(Map::isEmpty))).thenReturn(false);

        mockMvc.perform(post(ALIPAY_NOTIFY_URL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("failure"));
    }
}
