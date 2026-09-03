package com.clothingretail.common;

/** Thrown when a request conflicts with existing state (e.g. duplicate email/SKU). Mapped to HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
