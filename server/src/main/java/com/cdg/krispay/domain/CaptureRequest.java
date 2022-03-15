package com.cdg.krispay.domain;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

@Data
public class CaptureRequest {

    @NotEmpty(message = "requestId is required")
    String requestId;

    @NotEmpty(message = "bookingRef is required")
    String bookingRef;

    @NotEmpty(message = "amount is required")
    @PositiveOrZero(message = "amount cannot be negative")
    Double amount;

    @NotEmpty(message = "jobNumber is required")
    Double jobNumber;
    
    @NotEmpty(message = "entryMode is required")
    String entryMode;

    @NotEmpty(message = "vehicleId is required")
    String vehicleId;

    @NotEmpty(message = "entity is required")
    String entity;
    
    @PositiveOrZero(message = "adminAmount cannot be negative")
    @NotEmpty(message = "adminAmount is required")
    Double adminAmount;
    
    @NotEmpty(message = "gstAmount is required")
    Double gstAmount;

    @NotEmpty(message = "fareAmount is required")
    Double fareAmount;
}
