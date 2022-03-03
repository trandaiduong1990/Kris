package com.cdg.krispay.domain;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

@Data
public class Authorisation {

    @NotEmpty(message = "requestId is required")
    String requestId;

    @NotEmpty(message = "bookingRef is required")
    String bookingRef;

    @PositiveOrZero(message = "amount cannot be negative")
    double amount;

    @NotNull(message = "sessionId is required")
    String sessionId;

    String platform;


}
