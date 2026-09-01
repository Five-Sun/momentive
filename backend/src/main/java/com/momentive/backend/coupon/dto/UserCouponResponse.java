package com.momentive.backend.coupon.dto;

import com.momentive.backend.coupon.domain.DiscountType;
import com.momentive.backend.coupon.domain.UserCoupon;
import com.momentive.backend.coupon.domain.UserCouponStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record UserCouponResponse(
        @Schema(description = "보유 쿠폰 ID") Long id,
        @Schema(description = "쿠폰 이름") String couponName,
        @Schema(description = "할인 종류") DiscountType discountType,
        @Schema(description = "할인 값(정액이면 원, 정률이면 %)") Integer discountValue,
        @Schema(description = "최대 할인 금액(정률 쿠폰만 존재)") Integer maxDiscountAmount,
        @Schema(description = "최소 주문 금액") Integer minOrderAmount,
        @Schema(description = "유효기간") LocalDateTime expiresAt,
        @Schema(description = "보유 쿠폰 상태") UserCouponStatus status,
        @Schema(description = "사용한 주문 ID (미사용 시 null)") Long usedOrderId
) {

    public static UserCouponResponse from(UserCoupon userCoupon) {
        return new UserCouponResponse(
                userCoupon.getId(),
                userCoupon.getCoupon().getName(),
                userCoupon.getCoupon().getDiscountType(),
                userCoupon.getCoupon().getDiscountValue(),
                userCoupon.getCoupon().getMaxDiscountAmount(),
                userCoupon.getCoupon().getMinOrderAmount(),
                userCoupon.getCoupon().getExpiresAt(),
                userCoupon.getStatus(),
                userCoupon.getUsedOrderId()
        );
    }
}
