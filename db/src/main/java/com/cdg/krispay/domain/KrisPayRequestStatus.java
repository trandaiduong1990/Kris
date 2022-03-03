package com.cdg.krispay.domain;

public enum KrisPayRequestStatus {

    ERROR("error"),
    SUCCESS("success");

    public final String type;

    private KrisPayRequestStatus(String type) {
        this.type = type;
    }

}
