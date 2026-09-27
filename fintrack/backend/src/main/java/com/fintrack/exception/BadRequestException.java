package com.fintrack.exception;

/** Thrown for business-rule violations that validation annotations cannot express. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
