package com.momentive.backend.order.controller;

import com.momentive.backend.auth.security.CurrentUser;
import com.momentive.backend.order.dto.OrderConfirmRequest;
import com.momentive.backend.order.dto.OrderCreateRequest;
import com.momentive.backend.order.dto.OrderResponse;
import com.momentive.backend.order.dto.OrderStatusResponse;
import com.momentive.backend.order.dto.OrderSummaryResponse;
import com.momentive.backend.order.service.OrderService;
import com.momentive.backend.payment.service.PaymentService;
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
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@CurrentUser Long userId, @Valid @RequestBody OrderCreateRequest request) {
        return orderService.createOrder(userId, request);
    }

    @GetMapping
    public List<OrderSummaryResponse> getOrders(@CurrentUser Long userId) {
        return orderService.getOrders(userId);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@CurrentUser Long userId, @PathVariable Long orderId) {
        return orderService.getOrder(userId, orderId);
    }

    @PostMapping("/{orderId}/confirm")
    public OrderStatusResponse confirmOrder(
            @CurrentUser Long userId, @PathVariable Long orderId, @Valid @RequestBody OrderConfirmRequest request) {
        return paymentService.confirmOrder(userId, orderId, request);
    }

    @PostMapping("/{orderId}/cancel")
    public OrderStatusResponse cancelOrder(@CurrentUser Long userId, @PathVariable Long orderId) {
        return paymentService.cancelOrder(userId, orderId);
    }
}
