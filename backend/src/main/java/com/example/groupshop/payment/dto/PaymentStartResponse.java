package com.example.groupshop.payment.dto;

import com.example.groupshop.order.dto.OrderResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentStartResponse {

    private String mode;
    private OrderResponse order;
    private String formHtml;
}
