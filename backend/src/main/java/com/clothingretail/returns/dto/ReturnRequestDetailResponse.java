package com.clothingretail.returns.dto;

import com.clothingretail.returns.EvidenceStatus;
import com.clothingretail.returns.RequestType;
import com.clothingretail.returns.ReturnRequestStatus;
import java.time.Instant;

/** Customer-facing detail view of their own request - never includes anything the customer shouldn't see (no other customer's data, no internal admin-only fields beyond the note explaining a decision). */
public record ReturnRequestDetailResponse(
        Long id,
        Long orderId,
        Long orderItemId,
        String productName,
        RequestType requestType,
        ReturnRequestStatus status,
        String reason,
        String requestedSizeName,
        EvidenceStatus evidenceStatus,
        String evidenceReferenceCode,
        String whatsappLink,
        String adminNote,
        Instant createdAt) {}
