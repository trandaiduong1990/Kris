package com.cdg.krispay.domain;

public enum KrisPayMessageType {

    AUTHORISE("authorise"),
    CAPTURE("capture"),
    CREATE_ORDER("create-order"),
    ORDER_STATUS("order-status");

    public final String type;

    private KrisPayMessageType(String type) {
        this.type = type;
    }

}
