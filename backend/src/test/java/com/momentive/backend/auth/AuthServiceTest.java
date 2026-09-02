package com.momentive.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.momentive.backend.auth.dto.AuthResult;
import com.momentive.backend.auth.dto.LoginRequest;
import com.momentive.backend.auth.dto.SignupRequest;
import com.momentive.backend.auth.repository.RefreshTokenRepository;
import com.momentive.backend.auth.repository.UserRepository;
import com.momentive.backend.auth.service.AuthService;
import com.momentive.backend.common.exception.CustomException;
import com.momentive.backend.common.exception.ErrorCode;
import com.momentive.backend.coupon.repository.UserCouponRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    // user_coupon이 users를 참조하므로, 남아 있으면 userRepository.deleteAll()이 FK 제약에 걸린다.
    @Autowired
    private UserCouponRepository userCouponRepository;

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
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void signup_creates_user_and_issues_tokens() {
        SignupRequest request = new SignupRequest("new@momentive.com", "password1", "몽이");

        AuthResult result = authService.signup(request);

        assertThat(result.user().email()).isEqualTo("new@momentive.com");
        assertThat(result.user().nickname()).isEqualTo("몽이");
        assertThat(result.tokens().accessToken()).isNotBlank();
        assertThat(result.tokens().refreshToken()).isNotBlank();
    }

    @Test
    void signup_fails_when_email_already_exists() {
        authService.signup(new SignupRequest("dup@momentive.com", "password1", "몽이"));

        assertThatThrownBy(() -> authService.signup(new SignupRequest("dup@momentive.com", "password2", "another")))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    @Test
    void login_succeeds_with_correct_credentials() {
        authService.signup(new SignupRequest("login@momentive.com", "password1", "몽이"));

        AuthResult result = authService.login(new LoginRequest("login@momentive.com", "password1"));

        assertThat(result.user().email()).isEqualTo("login@momentive.com");
        assertThat(result.tokens().accessToken()).isNotBlank();
    }

    @Test
    void login_fails_with_same_error_when_email_missing() {
        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@momentive.com", "password1")))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void login_fails_with_same_error_when_password_wrong() {
        authService.signup(new SignupRequest("wrongpw@momentive.com", "password1", "몽이"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("wrongpw@momentive.com", "wrongpassword1")))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void refresh_rotates_token_and_invalidates_previous_one() {
        AuthResult signupResult = authService.signup(new SignupRequest("refresh@momentive.com", "password1", "몽이"));
        String originalRefreshToken = signupResult.tokens().refreshToken();

        AuthResult refreshed = authService.refresh(originalRefreshToken);

        assertThat(refreshed.tokens().refreshToken()).isNotEqualTo(originalRefreshToken);
        assertThatThrownBy(() -> authService.refresh(originalRefreshToken))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void refresh_fails_with_invalid_token() {
        assertThatThrownBy(() -> authService.refresh("not-a-real-token"))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void logout_revokes_refresh_token_so_subsequent_refresh_fails() {
        AuthResult signupResult = authService.signup(new SignupRequest("logout@momentive.com", "password1", "몽이"));
        String refreshToken = signupResult.tokens().refreshToken();

        authService.logout(refreshToken);

        assertThatThrownBy(() -> authService.refresh(refreshToken))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void me_returns_user_when_id_is_valid() {
        AuthResult signupResult = authService.signup(new SignupRequest("me@momentive.com", "password1", "몽이"));

        var response = authService.me(signupResult.user().id());

        assertThat(response.email()).isEqualTo("me@momentive.com");
        assertThat(response.nickname()).isEqualTo("몽이");
    }
}
