package com.momentive.backend.coupon.repository;

import com.momentive.backend.coupon.domain.Coupon;
import com.momentive.backend.coupon.domain.UserCoupon;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    List<UserCoupon> findAllByUserIdOrderByRegisteredAtDesc(Long userId);

    Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId);
}
