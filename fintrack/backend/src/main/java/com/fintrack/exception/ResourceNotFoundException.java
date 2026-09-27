package com.fintrack.exception;

/** Thrown when a row does not exist, or does not belong to the calling user. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
