package com.cdg.krispay.dto;

import lombok.Data;

@Data
public class Payment {
    double amount;
    String currencyCode = "SGD";

    public Payment(double amount) {
        this.amount = amount;
    }
}
