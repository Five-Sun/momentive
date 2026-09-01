package com.momentive.backend.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.momentive.backend.auth.domain.User;
import com.momentive.backend.auth.repository.RefreshTokenRepository;
import com.momentive.backend.auth.repository.UserRepository;
import com.momentive.backend.common.exception.CustomException;
import com.momentive.backend.common.exception.ErrorCode;
import com.momentive.backend.coupon.domain.Coupon;
import com.momentive.backend.coupon.domain.DiscountType;
import com.momentive.backend.coupon.domain.UserCoupon;
import com.momentive.backend.coupon.dto.UserCouponResponse;
import com.momentive.backend.coupon.repository.CouponRepository;
import com.momentive.backend.coupon.repository.UserCouponRepository;
import com.momentive.backend.coupon.service.CouponService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CouponServiceTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        cleanUp();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        userCouponRepository.deleteAll();
        couponRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createUser(String email) {
        return userRepository.save(User.createUser(email, "hash", "몽이보호자"));
    }

    private Coupon createCoupon(String code, LocalDateTime expiresAt) {
        return couponRepository.save(Coupon.create(
                code, code + " 쿠폰", DiscountType.FIXED, 3000, null, 0, expiresAt));
    }

    @Test
    void register_succeeds_with_valid_code() {
        User user = createUser("coupon1@momentive.com");
        createCoupon("WELCOME", LocalDateTime.now().plusDays(30));

        UserCouponResponse response = couponService.register(user.getId(), "welcome");

        assertThat(response.couponName()).isEqualTo("WELCOME 쿠폰");
        assertThat(response.status().name()).isEqualTo("AVAILABLE");
    }

    @Test
    void register_fails_when_code_does_not_exist() {
        User user = createUser("coupon2@momentive.com");

        assertThatThrownBy(() -> couponService.register(user.getId(), "NOTEXIST"))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.COUPON_NOT_FOUND);
    }

    @Test
    void register_fails_when_coupon_expired() {
        User user = createUser("coupon3@momentive.com");
        createCoupon("EXPIRED", LocalDateTime.now().minusDays(1));

        assertThatThrownBy(() -> couponService.register(user.getId(), "EXPIRED"))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.COUPON_EXPIRED);
    }

    @Test
    void register_fails_when_already_registered() {
        User user = createUser("coupon4@momentive.com");
        createCoupon("DUP", LocalDateTime.now().plusDays(30));
        couponService.register(user.getId(), "DUP");

        assertThatThrownBy(() -> couponService.register(user.getId(), "DUP"))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.COUPON_ALREADY_REGISTERED);
    }

    @Test
    void findMyCoupons_returns_available_before_used_and_expired() {
        User user = createUser("coupon5@momentive.com");
        Coupon usedCoupon = createCoupon("USEDONE", LocalDateTime.now().plusDays(30));
        Coupon expiredCoupon = createCoupon("EXPIREDONE", LocalDateTime.now().minusDays(1));
        Coupon availableCoupon = createCoupon("AVAILONE", LocalDateTime.now().plusDays(30));

        UserCoupon usedUserCoupon = userCouponRepository.save(UserCoupon.register(user, usedCoupon));
        usedUserCoupon.use(999L);
        userCouponRepository.save(usedUserCoupon);
        userCouponRepository.save(UserCoupon.register(user, expiredCoupon));
        userCouponRepository.save(UserCoupon.register(user, availableCoupon));

        List<UserCouponResponse> responses = couponService.findMyCoupons(user.getId());

        assertThat(responses).hasSize(3);
        assertThat(responses.get(0).couponName()).isEqualTo(availableCoupon.getName());
        assertThat(responses.get(0).status().name()).isEqualTo("AVAILABLE");
        List<String> trailingNames = List.of(responses.get(1).couponName(), responses.get(2).couponName());
        assertThat(trailingNames).containsExactlyInAnyOrder(usedCoupon.getName(), expiredCoupon.getName());
    }

    @Test
    void findMyCoupons_only_returns_own_coupons() {
        User owner = createUser("coupon6-owner@momentive.com");
        User other = createUser("coupon6-other@momentive.com");
        Coupon coupon = createCoupon("MINE", LocalDateTime.now().plusDays(30));
        userCouponRepository.save(UserCoupon.register(owner, coupon));

        assertThat(couponService.findMyCoupons(other.getId())).isEmpty();
        assertThat(couponService.findMyCoupons(owner.getId())).hasSize(1);
    }
}
