package com.momentive.backend.order.controller;

import com.momentive.backend.auth.security.CurrentUser;
import com.momentive.backend.common.config.OpenApiConfig;
import com.momentive.backend.order.dto.OrderConfirmRequest;
import com.momentive.backend.order.dto.OrderCreateRequest;
import com.momentive.backend.order.dto.OrderResponse;
import com.momentive.backend.order.dto.OrderStatusResponse;
import com.momentive.backend.order.dto.OrderSummaryResponse;
import com.momentive.backend.order.service.OrderService;
import com.momentive.backend.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_SECURITY_SCHEME)
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    @Operation(summary = "주문 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(
            @Parameter(hidden = true) @CurrentUser Long userId, @Valid @RequestBody OrderCreateRequest request) {
        return orderService.createOrder(userId, request);
    }

    @Operation(summary = "주문 목록 조회")
    @GetMapping
    public List<OrderSummaryResponse> getOrders(@Parameter(hidden = true) @CurrentUser Long userId) {
        return orderService.getOrders(userId);
    }

    @Operation(summary = "주문 상세 조회")
    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@Parameter(hidden = true) @CurrentUser Long userId, @PathVariable Long orderId) {
        return orderService.getOrder(userId, orderId);
    }

    @Operation(summary = "결제 승인(주문 확정)")
    @PostMapping("/{orderId}/confirm")
    public OrderStatusResponse confirmOrder(
            @Parameter(hidden = true) @CurrentUser Long userId,
            @PathVariable Long orderId,
            @Valid @RequestBody OrderConfirmRequest request) {
        return paymentService.confirmOrder(userId, orderId, request);
    }

    @Operation(summary = "주문 취소")
    @PostMapping("/{orderId}/cancel")
    public OrderStatusResponse cancelOrder(@Parameter(hidden = true) @CurrentUser Long userId, @PathVariable Long orderId) {
        return paymentService.cancelOrder(userId, orderId);
    }
}
