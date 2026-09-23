package com.clothingretail.subscription.controller;

import com.clothingretail.subscription.dto.SubscriptionPayInitiationResponse;
import com.clothingretail.subscription.dto.SubscriptionPaymentRow;
import com.clothingretail.subscription.dto.SubscriptionSettingsRequest;
import com.clothingretail.subscription.dto.SubscriptionStatusResponse;
import com.clothingretail.subscription.service.SubscriptionBillingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Subscription billing between the software owner (SUPER_ADMIN) and the store owner (ADMIN) -
 * mixed per-method roles rather than one class-level check, since setting the terms and paying
 * them are deliberately different tiers: only SUPER_ADMIN can set the amount/due date or grant a
 * manual override, only ADMIN pays.
 */
@RestController
@RequestMapping("/api/admin/subscription")
public class AdminSubscriptionController {

    private final SubscriptionBillingService subscriptionBillingService;

    public AdminSubscriptionController(SubscriptionBillingService subscriptionBillingService) {
        this.subscriptionBillingService = subscriptionBillingService;
    }

    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public SubscriptionStatusResponse status() {
        return subscriptionBillingService.getStatus();
    }

    /** SUPER_ADMIN only, deliberately NOT hasRole('ADMIN') - the role hierarchy only extends SUPER_ADMIN's reach downward, never the other way. */
    @PutMapping("/settings")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public SubscriptionStatusResponse updateSettings(@Valid @RequestBody SubscriptionSettingsRequest request) {
        return subscriptionBillingService.updateSettings(request);
    }

    @PostMapping("/mark-paid")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public SubscriptionStatusResponse markPaid() {
        return subscriptionBillingService.markPaid();
    }

    @PostMapping("/pay")
    @PreAuthorize("hasRole('ADMIN')")
    public SubscriptionPayInitiationResponse pay() {
        return subscriptionBillingService.initiatePayment();
    }

    @GetMapping("/payments")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SubscriptionPaymentRow> payments() {
        return subscriptionBillingService.listPayments();
    }
}
