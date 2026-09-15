package com.clothingretail.customer.service;

import com.clothingretail.auth.User;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.customer.CustomerProfile;
import com.clothingretail.customer.dto.AdminCustomerDetailResponse;
import com.clothingretail.customer.dto.AdminCustomerRow;
import com.clothingretail.customer.repository.CustomerProfileRepository;
import com.clothingretail.customer.repository.CustomerSpecifications;
import com.clothingretail.order.service.AdminOrderQueryService;
import com.clothingretail.order.Order;
import com.clothingretail.order.repository.OrderRepository;
import com.clothingretail.order.repository.OrderRepository.CustomerOrderStatsProjection;
import com.clothingretail.order.SaleOrderStatuses;
import com.clothingretail.order.dto.AdminOrderRow;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-side of the admin Customers module: the paginated/searchable list and the single-customer
 * detail view (contact info, account status, lifetime order stats, order history) - kept
 * separate from {@link AdminCustomerService} (the write side, just the enable/disable toggle),
 * same split as {@code AdminOrderQueryService}/{@code AdminOrderService}.
 */
@Service
@Transactional(readOnly = true)
@Log4j2
public class AdminCustomerQueryServiceImpl implements AdminCustomerQueryService {

    private final CustomerProfileRepository customerProfileRepository;
    private final OrderRepository orderRepository;
    private final AdminOrderQueryService adminOrderQueryService;

    public AdminCustomerQueryServiceImpl(
            CustomerProfileRepository customerProfileRepository,
            OrderRepository orderRepository,
            AdminOrderQueryService adminOrderQueryService) {
        this.customerProfileRepository = customerProfileRepository;
        this.orderRepository = orderRepository;
        this.adminOrderQueryService = adminOrderQueryService;
    }

    @Override
    public Page<AdminCustomerRow> list(String q, Boolean enabled, String sort, String dir, int page, int size) {
        log.info("[1503] Listing admin customers q={} enabled={} page={} size={}", q, enabled, page, size);
        Pageable pageable = PageRequest.of(page, size, sort(sort, dir));
        Specification<CustomerProfile> spec = CustomerSpecifications.filter(q, enabled);
        Page<CustomerProfile> profiles = customerProfileRepository.findAll(spec, pageable);

        Map<Long, CustomerOrderStatsProjection> statsByProfileId = statsFor(
                profiles.getContent().stream().map(CustomerProfile::getId).toList());

        List<AdminCustomerRow> rows =
                profiles.getContent().stream().map(p -> toRow(p, statsByProfileId.get(p.getId()))).toList();
        log.info("[1504] Admin customer list result: {} of {} total matched", rows.size(), profiles.getTotalElements());
        return new PageImpl<>(rows, pageable, profiles.getTotalElements());
    }

    @Override
    public AdminCustomerDetailResponse detail(Long id) {
        log.info("[1505] Fetching admin customer detail id={}", id);
        CustomerProfile profile = find(id);
        Map<Long, CustomerOrderStatsProjection> stats = statsFor(List.of(id));
        return toDetail(profile, stats.get(id));
    }

    @Override
    public Page<AdminOrderRow> orderHistory(Long id, int page, int size) {
        log.info("[1506] Fetching order history for customer id={} page={} size={}", id, page, size);
        find(id);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orders = orderRepository.findByCustomerProfileId(id, pageable);
        return new PageImpl<>(adminOrderQueryService.toRows(orders.getContent()), pageable, orders.getTotalElements());
    }

    @Override
    public CustomerProfile find(Long id) {
        return customerProfileRepository.findById(id).orElseThrow(() -> {
            log.error("[1507] Customer not found id={}", id);
            return new NotFoundException("Customer not found: " + id);
        });
    }

    private Map<Long, CustomerOrderStatsProjection> statsFor(List<Long> profileIds) {
        if (profileIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, CustomerOrderStatsProjection> byId = new HashMap<>();
        for (CustomerOrderStatsProjection p :
                orderRepository.sumStatsByCustomerProfileIds(profileIds, SaleOrderStatuses.SALE_STATUSES)) {
            byId.put(p.getCustomerProfileId(), p);
        }
        return byId;
    }

    private AdminCustomerRow toRow(CustomerProfile profile, CustomerOrderStatsProjection stats) {
        User user = profile.getUser();
        return new AdminCustomerRow(
                profile.getId(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getMobileNumber(),
                user.isEnabled(),
                stats != null ? stats.getOrderCount() : 0L,
                stats != null ? stats.getTotalSpent() : BigDecimal.ZERO,
                profile.getCreatedAt());
    }

    private AdminCustomerDetailResponse toDetail(CustomerProfile profile, CustomerOrderStatsProjection stats) {
        User user = profile.getUser();
        return new AdminCustomerDetailResponse(
                profile.getId(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getMobileNumber(),
                user.isEnabled(),
                profile.getDateOfBirth(),
                profile.getGender(),
                stats != null ? stats.getOrderCount() : 0L,
                stats != null ? stats.getTotalSpent() : BigDecimal.ZERO,
                profile.getCreatedAt());
    }

    private Sort sort(String sort, String dir) {
        String property = switch (sort == null ? "" : sort) {
            case "fullName" -> "user.fullName";
            case "email" -> "user.email";
            default -> "createdAt";
        };
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }
}
