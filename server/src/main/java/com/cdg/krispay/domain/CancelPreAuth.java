package com.cdg.krispay.domain;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

@Data
public class CancelPreAuth {

    @NotEmpty(message = "requestId is required")
    String requestId;

    @NotEmpty(message = "Reason code is required")
    String reason;

}
