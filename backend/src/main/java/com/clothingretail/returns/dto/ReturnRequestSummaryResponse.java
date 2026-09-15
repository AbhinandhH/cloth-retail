package com.clothingretail.returns.dto;

import com.clothingretail.returns.EvidenceStatus;
import com.clothingretail.returns.RequestType;
import com.clothingretail.returns.ReturnRequestStatus;
import java.time.Instant;

public record ReturnRequestSummaryResponse(
        Long id,
        Long orderId,
        String productName,
        RequestType requestType,
        ReturnRequestStatus status,
        EvidenceStatus evidenceStatus,
        Instant createdAt) {}
