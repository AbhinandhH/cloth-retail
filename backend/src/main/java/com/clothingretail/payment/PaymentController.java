package com.clothingretail.payment;

import com.clothingretail.payment.dto.InitiatePaymentRequest;
import com.clothingretail.payment.dto.PaymentInitiateResponse;
import com.clothingretail.payment.dto.SimulatePaymentRequest;
import com.clothingretail.payment.dto.SimulatePaymentResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentWebhookService paymentWebhookService;

    public PaymentController(PaymentService paymentService, PaymentWebhookService paymentWebhookService) {
        this.paymentService = paymentService;
        this.paymentWebhookService = paymentWebhookService;
    }

    @PostMapping("/initiate")
    @PreAuthorize("hasRole('CUSTOMER')")
    public PaymentInitiateResponse initiate(Authentication authentication, @Valid @RequestBody InitiatePaymentRequest request) {
        return paymentService.initiate(userId(authentication), request.orderId(), request.paymentMethod());
    }

    @PostMapping("/mock/simulate")
    @PreAuthorize("hasRole('CUSTOMER')")
    public SimulatePaymentResponse simulate(Authentication authentication, @Valid @RequestBody SimulatePaymentRequest request) {
        return paymentService.simulate(userId(authentication), request);
    }

    /**
     * What a real payment gateway would call. Deliberately NOT behind {@code @PreAuthorize}/JWT -
     * a real gateway calling this has no customer access token. Trust comes entirely from
     * {@link MockPaymentGateway#verifySignature}, checked first thing inside
     * {@link PaymentWebhookService#handleWebhook}; see SecurityConfig for why this path is in the
     * public allowlist and why that's safe here.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String payload, @RequestHeader("X-Signature") String signature) {
        paymentWebhookService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }

    private Long userId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
