package com.clothingretail.subscription.repository;

import com.clothingretail.subscription.SubscriptionPayment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, Long> {

    Optional<SubscriptionPayment> findByGatewayOrderId(String gatewayOrderId);

    List<SubscriptionPayment> findAllByOrderByCreatedAtDesc();
}
