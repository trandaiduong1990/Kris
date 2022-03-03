package com.cdg.krispay.domain;

public enum ResponseStatus {

    SUCCESS("SUCCESS"),
    FAILED("FAILED"),
    DECLINED("DECLINE");

    public final String type;

    private ResponseStatus(String type) {
        this.type = type;
    }

}
