package com.cdg.krispay.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;


@ControllerAdvice
public class ControllerAdvisor extends ResponseEntityExceptionHandler {

    @ExceptionHandler(MissingIdempotencyKeyException.class)
    public ResponseEntity<Object> handleCityNotFoundException(
            MissingIdempotencyKeyException ex, WebRequest request) {
        return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IdempotentKeyMessageTypeMismatchException.class)
    public ResponseEntity<Object> IdempotentKeyMessageTypeMismatchException(
            IdempotentKeyMessageTypeMismatchException ex, WebRequest request) {
        return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
    }

}
