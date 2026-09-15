package com.clothingretail.returns.service;

import org.springframework.core.io.Resource;

/** Shared between the customer- and admin-facing services - both stream the same underlying stored file. */
public record EvidenceFile(Resource resource, String contentType) {}
