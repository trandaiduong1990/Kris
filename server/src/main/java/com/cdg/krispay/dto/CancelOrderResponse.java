package com.cdg.krispay.dto;

import lombok.Data;

@Data
public class CancelOrderResponse {
    String partnerOrderId;
    String paymentStatus;
    String status;
    String code;
    String message;
}
