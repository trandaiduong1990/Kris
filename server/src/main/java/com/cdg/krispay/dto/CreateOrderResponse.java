package com.cdg.krispay.dto;

import lombok.Data;

@Data
public class CreateOrderResponse {
    String partnerOrderId;
    String paymentStatus;
    String createdAt;
    String orderExpiry;
    String status;
    String code;
    String message;
}
