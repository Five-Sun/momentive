package com.momentive.backend.coupon.repository;

import com.momentive.backend.coupon.domain.Coupon;
import com.momentive.backend.coupon.domain.UserCoupon;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    /**
     * 목록 조회는 정렬 키(만료 여부)와 응답 변환 양쪽에서 coupon을 참조하므로,
     * LAZY 연관을 그대로 두면 행 수만큼 추가 SELECT가 발생한다. fetch join으로 한 번에 읽는다.
     */
    @Query("SELECT uc FROM UserCoupon uc JOIN FETCH uc.coupon"
            + " WHERE uc.user.id = :userId ORDER BY uc.registeredAt DESC")
    List<UserCoupon> findAllByUserIdWithCoupon(@Param("userId") Long userId);

    Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId);
}
