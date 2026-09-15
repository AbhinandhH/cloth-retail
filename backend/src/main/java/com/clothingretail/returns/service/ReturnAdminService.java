package com.clothingretail.returns.service;

import com.clothingretail.common.PageResponse;
import com.clothingretail.returns.RequestType;
import com.clothingretail.returns.ReturnRequestStatus;
import com.clothingretail.returns.dto.AdminInitiateRefundRequest;
import com.clothingretail.returns.dto.AdminReturnDecisionRequest;
import com.clothingretail.returns.dto.AdminReturnDetailResponse;
import com.clothingretail.returns.dto.AdminReturnRow;
import org.springframework.web.multipart.MultipartFile;

/** Admin-facing half of the return/exchange module - reads and writes both live here, same one-service shape most of this codebase's admin modules use. */
public interface ReturnAdminService {

    PageResponse<AdminReturnRow> list(RequestType requestType, ReturnRequestStatus status, String q, int page, int size);

    AdminReturnDetailResponse detail(Long id);

    AdminReturnDetailResponse approveExchange(Long id, AdminReturnDecisionRequest request, Long adminUserId);

    AdminReturnDetailResponse approveDamageClaim(Long id, AdminReturnDecisionRequest request, Long adminUserId);

    AdminReturnDetailResponse reject(Long id, AdminReturnDecisionRequest request, Long adminUserId);

    AdminReturnDetailResponse uploadEvidence(Long id, MultipartFile file);

    AdminReturnDetailResponse initiateRefund(Long id, AdminInitiateRefundRequest request, Long adminUserId);

    EvidenceFile loadEvidence(Long id);
}
