package com.cdg.krispay.domain;

public enum KrisPayRequestStatus {

    ERROR("ERROR"),
    SUCCESS("SUCCESS");

    public final String type;

    private KrisPayRequestStatus(String type) {
        this.type = type;
    }

}
