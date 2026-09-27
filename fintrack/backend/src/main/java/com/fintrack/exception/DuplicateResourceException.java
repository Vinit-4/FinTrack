package com.fintrack.exception;

/** Thrown when registering with an email that already exists. */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
