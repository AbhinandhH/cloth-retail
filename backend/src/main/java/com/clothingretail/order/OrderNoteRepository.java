package com.clothingretail.order;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderNoteRepository extends JpaRepository<OrderNote, Long> {
    List<OrderNote> findByOrderIdOrderByIdAsc(Long orderId);
}
