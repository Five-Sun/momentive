package com.momentive.backend.order.repository;

import com.momentive.backend.order.domain.OrderItem;
import com.momentive.backend.order.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * 구매 확인(verified purchase) 검증용: 특정 사용자가 특정 상태의 주문에
     * 해당 상품을 포함해 주문한 적이 있는지 확인한다. (리뷰 작성 자격 판단)
     */
    boolean existsByOrder_User_IdAndOrder_StatusAndProduct_Id(Long userId, OrderStatus status, Long productId);
}
