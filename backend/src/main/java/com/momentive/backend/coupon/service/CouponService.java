package com.momentive.backend.coupon.service;

import com.momentive.backend.auth.domain.User;
import com.momentive.backend.auth.repository.UserRepository;
import com.momentive.backend.common.exception.CustomException;
import com.momentive.backend.common.exception.ErrorCode;
import com.momentive.backend.coupon.domain.Coupon;
import com.momentive.backend.coupon.domain.UserCoupon;
import com.momentive.backend.coupon.domain.UserCouponStatus;
import com.momentive.backend.coupon.dto.UserCouponResponse;
import com.momentive.backend.coupon.repository.CouponRepository;
import com.momentive.backend.coupon.repository.UserCouponRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;

    @Transactional
    public UserCouponResponse register(Long userId, String code) {
        User user = getUser(userId);
        Coupon coupon = couponRepository.findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_FOUND,
                        Map.of("code", ErrorCode.COUPON_NOT_FOUND.getMessage())));

        if (coupon.isExpired()) {
            throw new CustomException(ErrorCode.COUPON_EXPIRED,
                    Map.of("code", ErrorCode.COUPON_EXPIRED.getMessage()));
        }

        userCouponRepository.findByUserIdAndCouponId(userId, coupon.getId())
                .ifPresent(existing -> {
                    throw new CustomException(ErrorCode.COUPON_ALREADY_REGISTERED,
                            Map.of("code", ErrorCode.COUPON_ALREADY_REGISTERED.getMessage()));
                });

        UserCoupon userCoupon = UserCoupon.register(user, coupon);
        userCouponRepository.save(userCoupon);
        return UserCouponResponse.from(userCoupon);
    }

    public List<UserCouponResponse> findMyCoupons(Long userId) {
        List<UserCoupon> userCoupons = userCouponRepository.findAllByUserIdOrderByRegisteredAtDesc(userId);
        return userCoupons.stream()
                .sorted(Comparator.comparing((UserCoupon uc) -> !uc.isAvailable()))
                .map(UserCouponResponse::from)
                .toList();
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHENTICATED));
    }
}
