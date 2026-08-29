package com.momentive.backend.order.repository;

import com.momentive.backend.order.domain.Order;
import com.momentive.backend.order.domain.OrderStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findAllByStatus(OrderStatus status);
}
