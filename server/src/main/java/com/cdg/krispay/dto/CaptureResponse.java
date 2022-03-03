package com.cdg.krispay.dto;

import lombok.Data;

@Data
public class CaptureResponse {
    String partnerOrderId;
    String transactionId;
    String paymentStatus;
    String paymentAt;
    String status;
    String code;
    String message;
}
