package com.clothingretail.returns.service;

import com.clothingretail.common.PageResponse;
import com.clothingretail.returns.dto.CreateDamageRequest;
import com.clothingretail.returns.dto.CreateExchangeRequest;
import com.clothingretail.returns.dto.EligibleOrderItemResponse;
import com.clothingretail.returns.dto.ReturnRequestDetailResponse;
import com.clothingretail.returns.dto.ReturnRequestSummaryResponse;
import java.util.List;

/**
 * Customer-facing half of the return/exchange module - every method resolves the acting
 * customer's {@code CustomerProfile} from {@code userId} (the JWT principal, supplied by the
 * controller) and re-validates ownership of any order/item id from there, exactly like
 * {@code OrderServiceImpl} - never from a client-supplied customer/order id.
 */
public interface ReturnRequestService {

    ReturnRequestDetailResponse createExchangeRequest(Long userId, Long orderId, Long orderItemId, CreateExchangeRequest request);

    ReturnRequestDetailResponse createDamageRequest(Long userId, Long orderId, Long orderItemId, CreateDamageRequest request);

    PageResponse<ReturnRequestSummaryResponse> listMyRequests(Long userId, int page, int size);

    ReturnRequestDetailResponse getMyRequest(Long userId, Long requestId);

    List<EligibleOrderItemResponse> listEligibleItems(Long userId, Long orderId);

    EvidenceFile loadMyEvidence(Long userId, Long requestId);
}
