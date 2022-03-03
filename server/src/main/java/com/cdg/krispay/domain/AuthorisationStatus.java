package com.cdg.krispay.domain;

import lombok.Data;

@Data
public class AuthorisationStatus {

    String requestId;
    String bookingRef;
    String status;

}
