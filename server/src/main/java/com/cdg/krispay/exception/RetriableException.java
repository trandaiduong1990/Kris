package com.cdg.krispay.exception;

public class RetriableException extends Exception {
    public RetriableException(String error) {
        super(error);
    }
}
