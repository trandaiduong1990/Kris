package com.cdg.krispay.dto;

import lombok.Data;

@Data
public class CaptureRequest {
    String partnerOrderId;
    Payment payment;
    String orderMetaData;

    public CaptureRequest(String partnerOrderId, String orderMetaData, double amount) {
        this.payment = new Payment(amount);
        this.orderMetaData = orderMetaData;
        this.partnerOrderId = partnerOrderId;
    }
}
