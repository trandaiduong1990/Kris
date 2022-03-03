package com.cdg.krispay.exception;

public class MissingIdempotencyKeyException extends RuntimeException {

    public MissingIdempotencyKeyException(String string) {

        super("Missing idempotency key in " + string);

    }

}
