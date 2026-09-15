package com.clothingretail.order.service;

import com.clothingretail.order.dto.AdminOrderCancelRequest;
import com.clothingretail.order.dto.AdminOrderDetailResponse;
import com.clothingretail.order.dto.AdminOrderNoteRequest;
import com.clothingretail.order.dto.AdminOrderNoteResponse;
import com.clothingretail.order.dto.AdminOrderStatusUpdateRequest;
import com.clothingretail.order.dto.AdminRefundRequest;
import com.clothingretail.order.dto.AdminRefundResponse;
import com.clothingretail.order.dto.AdminShipmentRequest;
import com.clothingretail.order.dto.AdminShipmentResponse;

/**
 * Write-side of the admin order module: status transitions, cancellation (with its stock-restore
 * or reservation-release branching), notes, shipment upsert, and refund. Kept separate from
 * AdminOrderQueryService (the read side), same split as StockService/InventoryQueryService.
 */
public interface AdminOrderService {

    AdminOrderDetailResponse updateStatus(Long orderId, AdminOrderStatusUpdateRequest request, Long adminUserId);

    AdminOrderDetailResponse cancel(Long orderId, AdminOrderCancelRequest request, Long adminUserId);

    AdminOrderNoteResponse addNote(Long orderId, AdminOrderNoteRequest request, Long adminUserId);

    AdminShipmentResponse upsertShipment(Long orderId, AdminShipmentRequest request);

    AdminRefundResponse refund(Long orderId, AdminRefundRequest request, Long adminUserId);
}
