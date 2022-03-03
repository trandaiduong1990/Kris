package com.cdg.krispay.dto;

import lombok.Data;

@Data
public class CancelOrderRequest {
    String reason;

    public CancelOrderRequest(String reason) {
        this.reason = reason;
    }

}
