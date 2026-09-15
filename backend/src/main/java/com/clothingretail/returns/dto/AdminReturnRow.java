package com.clothingretail.returns.dto;

import com.clothingretail.returns.EvidenceStatus;
import com.clothingretail.returns.RequestType;
import com.clothingretail.returns.ReturnRequestStatus;
import java.time.Instant;

/** One row of the admin return/exchange list - every column the admin portal's list table needs, no per-row extra query. */
public record AdminReturnRow(
        Long id,
        Long orderId,
        String orderNumber,
        String customerName,
        String productName,
        String sku,
        String originalSizeName,
        String requestedSizeName,
        RequestType requestType,
        String reason,
        ReturnRequestStatus status,
        EvidenceStatus evidenceStatus,
        Instant createdAt) {}
