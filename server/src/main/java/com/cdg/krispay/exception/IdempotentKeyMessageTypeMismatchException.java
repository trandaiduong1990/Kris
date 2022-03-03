package com.cdg.krispay.exception;

public class IdempotentKeyMessageTypeMismatchException extends RuntimeException {

    public IdempotentKeyMessageTypeMismatchException(String string) {

        super("Idempotent record type and api call mismatch, expecting : " + string);

    }

}
