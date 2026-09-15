package com.clothingretail.order.repository;

import com.clothingretail.order.OrderNote;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderNoteRepository extends JpaRepository<OrderNote, Long> {
    List<OrderNote> findByOrderIdOrderByIdAsc(Long orderId);
}
