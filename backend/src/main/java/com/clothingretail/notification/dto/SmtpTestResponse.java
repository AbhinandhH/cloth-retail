package com.clothingretail.notification.dto;

/**
 * Always 200 OK regardless of success - the point is to surface the REAL underlying error (e.g.
 * "Authentication failed", "Unknown host") to the admin UI, which GlobalExceptionHandler's
 * catch-all would otherwise flatten into a generic "An unexpected error occurred".
 */
public record SmtpTestResponse(boolean success, String message) {}
