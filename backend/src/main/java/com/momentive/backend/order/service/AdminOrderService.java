package com.momentive.backend.order.service;

import com.momentive.backend.common.exception.CustomException;
import com.momentive.backend.common.exception.ErrorCode;
import com.momentive.backend.order.domain.Order;
import com.momentive.backend.order.domain.OrderStatus;
import com.momentive.backend.order.dto.admin.AdminOrderDetailResponse;
import com.momentive.backend.order.dto.admin.AdminOrderListResponse;
import com.momentive.backend.order.dto.admin.AdminOrderSummaryResponse;
import com.momentive.backend.order.dto.admin.AdminShippingUpdateRequest;
import com.momentive.backend.order.repository.OrderRepository;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 주문 조회·배송상태 관리. 주문 취소는 고객 취소와 재고복원 로직을 공유하므로
 * 이 클래스가 아니라 {@link com.momentive.backend.payment.service.PaymentService}로 위임한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOrderService {

    /** 목록 기본 필터: 관리자가 우선 처리할 대상은 결제 완료 주문이다. */
    public static final List<OrderStatus> DEFAULT_LIST_STATUSES = List.of(OrderStatus.PAID);

    private final OrderRepository orderRepository;

    public AdminOrderListResponse getOrders(int page, int size, Collection<OrderStatus> statuses) {
        Collection<OrderStatus> effectiveStatuses =
                (statuses == null || statuses.isEmpty()) ? DEFAULT_LIST_STATUSES : statuses;
        Page<Order> orders = orderRepository.findForAdmin(effectiveStatuses, PageRequest.of(page, size));
        return AdminOrderListResponse.from(orders.map(AdminOrderSummaryResponse::from));
    }

    public AdminOrderDetailResponse getOrder(Long orderId) {
        return AdminOrderDetailResponse.from(findOrder(orderId));
    }

    @Transactional
    public AdminOrderDetailResponse updateShipping(Long orderId, AdminShippingUpdateRequest request) {
        Order order = findOrder(orderId);
        order.updateShipping(request.shippingStatus(), request.courier(), request.trackingNumber());
        return AdminOrderDetailResponse.from(order);
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
    }
}
