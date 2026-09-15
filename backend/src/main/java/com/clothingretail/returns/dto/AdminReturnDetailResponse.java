package com.clothingretail.returns.dto;

import com.clothingretail.order.dto.AdminRefundResponse;
import com.clothingretail.returns.EvidenceStatus;
import com.clothingretail.returns.RequestType;
import com.clothingretail.returns.ReturnRequestStatus;
import java.time.Instant;

public record AdminReturnDetailResponse(
        Long id,
        Long orderId,
        String orderNumber,
        String customerName,
        String customerEmail,
        String customerPhone,
        String productName,
        String sku,
        String originalSizeName,
        String requestedSizeName,
        RequestType requestType,
        String reason,
        ReturnRequestStatus status,
        EvidenceStatus evidenceStatus,
        boolean evidenceVideoAvailable,
        String evidenceReferenceCode,
        String adminNote,
        String reviewedByName,
        Instant reviewedAt,
        AdminRefundResponse refund,
        Instant createdAt) {}
