package com.cdg.krispay.dto;

import lombok.Data;

@Data
public class GetPaymentStatusResponse {
    String partnerOrderId;
    String transactionId;
    String paymentStatus;
    String createdAt;
    String status;
    String code;
    String message;
}
