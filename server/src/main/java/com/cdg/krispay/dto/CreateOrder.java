package com.cdg.krispay.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateOrder {

    String partnerOrderId;
    String sessionId;
    Payment payment;

    String paymentStatus;
    String createdAt;
    String orderExpiry;
    String status;
    String code;
    String message;
    String reason;

    public CreateOrder() {}

    public CreateOrder(String partnerOrderId, double amount, String sessionId) {
        this.partnerOrderId = partnerOrderId;
        this.sessionId = sessionId;
        this.payment = new Payment(amount);
    }
}
