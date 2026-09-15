package com.clothingretail.order.service;

import com.clothingretail.order.Order;
import com.clothingretail.order.OrderStatus;
import com.clothingretail.order.OrderStatusHistory;
import com.clothingretail.order.repository.OrderStatusHistoryRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code @Transactional} here simply joins whatever transaction is already open at each call
 * site (all of them are themselves {@code @Transactional}) - it never starts a second one.
 */
@Service
@Log4j2
public class OrderStatusHistoryServiceImpl implements OrderStatusHistoryService {

    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    public OrderStatusHistoryServiceImpl(OrderStatusHistoryRepository orderStatusHistoryRepository) {
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
    }

    @Override
    @Transactional
    public void record(Order order, OrderStatus previousStatus, OrderStatus newStatus, Long changedBy, String reason) {
        log.info("[1656] Recording status history for order {}: {} -> {}, changedBy={}, reason={}",
                order.getId(), previousStatus, newStatus, changedBy, reason);
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setChangedBy(changedBy);
        history.setReason(reason);
        orderStatusHistoryRepository.save(history);
    }
}
