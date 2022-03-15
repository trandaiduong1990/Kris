package com.cdg.krispay.domain;

public enum ResponseStatus {

    SUCCESS("SUCCESS"),
    FAILED("FAILED"),
    DECLINED("DECLINE"),
	ERROR("ERROR"),
	UNPAID("UNPAID"),
	AUTHORIZED("AUTHORIZED"),
	CAPTURED("CAPTURED"),
	CANCELLED("CANCELLED");

    public final String type;

    private ResponseStatus(String type) {
        this.type = type;
    }

}
