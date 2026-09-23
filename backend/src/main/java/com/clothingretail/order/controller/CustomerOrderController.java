package com.clothingretail.order.controller;

import com.clothingretail.common.PageResponse;
import com.clothingretail.order.dto.CreateOrderRequest;
import com.clothingretail.order.dto.OrderDetailResponse;
import com.clothingretail.order.dto.OrderSummaryResponse;
import com.clothingretail.order.service.OrderInvoiceService;
import com.clothingretail.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerOrderController {

    private final OrderService orderService;
    private final OrderInvoiceService orderInvoiceService;

    public CustomerOrderController(OrderService orderService, OrderInvoiceService orderInvoiceService) {
        this.orderService = orderService;
        this.orderInvoiceService = orderInvoiceService;
    }

    @PostMapping
    public OrderDetailResponse createOrder(Authentication authentication, @Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(userId(authentication), request);
    }

    @GetMapping
    public PageResponse<OrderSummaryResponse> listOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return orderService.listOrders(userId(authentication), page, size);
    }

    @GetMapping("/{id}")
    public OrderDetailResponse getOrder(Authentication authentication, @PathVariable Long id) {
        return orderService.getOrder(userId(authentication), id);
    }

    @GetMapping("/{id}/invoice")
    public ResponseEntity<byte[]> invoice(Authentication authentication, @PathVariable Long id) {
        byte[] pdf = orderInvoiceService.renderCustomerInvoice(userId(authentication), id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice-" + id + ".pdf\"")
                .body(pdf);
    }

    private Long userId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
