package com.clothingretail.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clothingretail.subscription.repository.SubscriptionBillingRepository;
import com.clothingretail.support.CheckoutTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

/**
 * Covers SubscriptionAccessFilter - the site-wide lockout gate. The singleton subscription_billing
 * row (id=1) is shared state across this whole test class (and, since H2 persists for the JVM's
 * lifetime, across every other test class too), so every destructive test restores it to the safe
 * default (due_date=null, paid=true - the same state V36__subscription_billing.sql seeds) in
 * {@link #restoreSafeState()}, which runs after every test regardless of outcome.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubscriptionAccessFilterIntegrationTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired
    private SubscriptionBillingRepository subscriptionBillingRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void restoreSafeState() {
        SubscriptionBilling billing = subscriptionBillingRepository.findById(SubscriptionBilling.SINGLETON_ID).orElseThrow();
        billing.setDueDate(null);
        billing.setMonthlyAmount(null);
        billing.setPaid(true);
        billing.setPaidAt(null);
        billing.setLastPaymentReference(null);
        subscriptionBillingRepository.save(billing);
    }

    @Test
    void nullDueDateNeverLocksTheSite() throws Exception {
        mockMvc.perform(get("/api/products")).andExpect(status().isOk());
    }

    @Test
    void unpaidWithFutureDueDateAllowsAllTraffic() throws Exception {
        String superAdminToken = CheckoutTestSupport.superAdminAccessToken(mockMvc, objectMapper);
        String settingsBody = """
                {"monthlyAmount":999.00,"dueDate":"%s"}
                """.formatted(LocalDate.now().plusDays(5));
        mockMvc.perform(put("/api/admin/subscription/settings")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(settingsBody))
                .andExpect(status().isOk());

        String adminToken = CheckoutTestSupport.adminAccessToken(mockMvc, objectMapper);
        mockMvc.perform(get("/api/admin/brands").header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());

        CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "sub-lockout-future");
        mockMvc.perform(get("/api/products")).andExpect(status().isOk());
    }

    @Test
    void pastDueDateLocksOutEveryoneExceptSuperAdmin() throws Exception {
        String superAdminToken = CheckoutTestSupport.superAdminAccessToken(mockMvc, objectMapper);
        String adminToken = CheckoutTestSupport.adminAccessToken(mockMvc, objectMapper);
        var customer = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "sub-lockout-past");

        SubscriptionBilling billing = subscriptionBillingRepository.findById(SubscriptionBilling.SINGLETON_ID).orElseThrow();
        billing.setMonthlyAmount(new BigDecimal("999.00"));
        billing.setDueDate(LocalDate.now().minusDays(1));
        billing.setPaid(false);
        subscriptionBillingRepository.save(billing);

        // Anonymous storefront traffic is blocked too - "the site won't be accessible for anyone".
        mockMvc.perform(get("/api/products")).andExpect(status().is(402));

        // An authenticated customer is blocked.
        mockMvc.perform(get("/api/products").header("Authorization", "Bearer " + customer.accessToken()))
                .andExpect(status().is(402));

        // The store owner (plain ADMIN) is blocked too - no self-service way out.
        mockMvc.perform(get("/api/admin/brands").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().is(402));

        // Admin login itself stays reachable (allowlisted) even while locked - it just doesn't
        // grant access to anything else while the lock is still in effect.
        String loginBody = """
                {"email":"admin@clothingretail.local","password":"ChangeMe123!"}
                """;
        mockMvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk());

        // SUPER_ADMIN is fully exempt.
        mockMvc.perform(get("/api/admin/brands").header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/subscription/status").header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk());

        // "Mark as paid" unlocks the site immediately, without needing Razorpay reachable at all.
        mockMvc.perform(post("/api/admin/subscription/mark-paid").header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/products")).andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/brands").header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());
    }

    @Test
    void refreshEndpointStaysReachableEvenWhenLocked() throws Exception {
        SubscriptionBilling billing = subscriptionBillingRepository.findById(SubscriptionBilling.SINGLETON_ID).orElseThrow();
        billing.setMonthlyAmount(new BigDecimal("999.00"));
        billing.setDueDate(LocalDate.now().minusDays(1));
        billing.setPaid(false);
        subscriptionBillingRepository.save(billing);

        // No refresh cookie is sent, so this will fail for its own reason - the point is only that
        // the lockout filter itself never intercepts this path with a 402.
        int status = mockMvc.perform(post("/api/auth/refresh")).andReturn().getResponse().getStatus();
        assertThat(status).isNotEqualTo(402);
    }

    @Test
    void onlySuperAdminCanChangeSubscriptionSettings() throws Exception {
        String adminToken = CheckoutTestSupport.adminAccessToken(mockMvc, objectMapper);
        String settingsBody = """
                {"monthlyAmount":999.00,"dueDate":"%s"}
                """.formatted(LocalDate.now().plusDays(5));
        mockMvc.perform(put("/api/admin/subscription/settings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(settingsBody))
                .andExpect(status().isForbidden());
    }
}
