package com.momentive.backend.order.controller;

import com.momentive.backend.common.config.OpenApiConfig;
import com.momentive.backend.order.domain.OrderStatus;
import com.momentive.backend.order.dto.OrderStatusResponse;
import com.momentive.backend.order.dto.admin.AdminOrderDetailResponse;
import com.momentive.backend.order.dto.admin.AdminOrderListResponse;
import com.momentive.backend.order.dto.admin.AdminShippingUpdateRequest;
import com.momentive.backend.order.service.AdminOrderService;
import com.momentive.backend.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 주문 API. {@code /admin/**}은 {@code SecurityConfig}에서 {@code hasRole("ADMIN")}으로
 * 막혀 있어, 권한 없는 호출은 이 컨트롤러에 도달하기 전에 403으로 끊긴다.
 */
@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_SECURITY_SCHEME)
public class AdminOrderController {

    private final AdminOrderService adminOrderService;
    private final PaymentService paymentService;

    @Operation(summary = "[관리자] 주문 목록 조회")
    @GetMapping
    public AdminOrderListResponse getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "조회할 주문 상태(복수 지정 가능). 기본값은 PAID")
            @RequestParam(defaultValue = "PAID") List<OrderStatus> status
    ) {
        return adminOrderService.getOrders(page, size, status);
    }

    @Operation(summary = "[관리자] 주문 상세 조회")
    @GetMapping("/{orderId}")
    public AdminOrderDetailResponse getOrder(@PathVariable Long orderId) {
        return adminOrderService.getOrder(orderId);
    }

    @Operation(summary = "[관리자] 배송상태·송장 변경")
    @PatchMapping("/{orderId}/shipping")
    public AdminOrderDetailResponse updateShipping(
            @PathVariable Long orderId, @Valid @RequestBody AdminShippingUpdateRequest request) {
        return adminOrderService.updateShipping(orderId, request);
    }

    @Operation(summary = "[관리자] 주문 취소")
    @PostMapping("/{orderId}/cancel")
    public OrderStatusResponse cancelOrder(@PathVariable Long orderId) {
        return paymentService.cancelOrderAsAdmin(orderId);
    }
}
