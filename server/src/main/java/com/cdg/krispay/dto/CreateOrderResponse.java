package com.cdg.krispay.dto;

import lombok.Data;

@Data
public class CreateOrderResponse {
    
    String status;
    String code;
    String message;
    OrderData data;
    
    @Data
	public class OrderData {
    	String partnerOrderId;
    	String orderExpiry;
    	String createdAt;
    	String paymentStatus;
    }
}
