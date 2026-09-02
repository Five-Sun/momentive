package com.momentive.backend.coupon.controller;

import com.momentive.backend.auth.security.CurrentUser;
import com.momentive.backend.common.config.OpenApiConfig;
import com.momentive.backend.coupon.dto.CouponRegisterRequest;
import com.momentive.backend.coupon.dto.UserCouponResponse;
import com.momentive.backend.coupon.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_SECURITY_SCHEME)
public class CouponController {

    private final CouponService couponService;

    @Operation(summary = "쿠폰 코드 등록")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserCouponResponse register(
            @Parameter(hidden = true) @CurrentUser Long userId, @Valid @RequestBody CouponRegisterRequest request) {
        return couponService.register(userId, request.code());
    }

    @Operation(summary = "보유 쿠폰 목록 조회")
    @GetMapping
    public List<UserCouponResponse> getMyCoupons(@Parameter(hidden = true) @CurrentUser Long userId) {
        return couponService.findMyCoupons(userId);
    }
}
