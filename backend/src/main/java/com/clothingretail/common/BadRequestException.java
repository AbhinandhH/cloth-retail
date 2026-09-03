package com.clothingretail.common;

/** Thrown for a semantically invalid request that isn't a Bean Validation failure. Mapped to HTTP 400. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
