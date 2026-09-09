package com.momentive.backend.order.repository;

import com.momentive.backend.order.domain.Order;
import com.momentive.backend.order.domain.OrderStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findAllByStatus(OrderStatus status);

    /**
     * 관리자용 주문 목록. 목록 응답에 주문자 정보가 필요하므로 {@code JOIN FETCH}로 N+1을 피한다.
     */
    @Query("""
            SELECT o FROM Order o
            JOIN FETCH o.user
            WHERE o.status IN :statuses
            ORDER BY o.createdAt DESC
            """)
    Page<Order> findForAdmin(@Param("statuses") Collection<OrderStatus> statuses, Pageable pageable);
}
