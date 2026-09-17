package com.momentive.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.momentive.backend.auth.security.AuthCookieProvider;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthCookieProviderTest {

    @Test
    void uses_configured_cross_site_cookie_attributes() {
        AuthCookieProvider cookieProvider = new AuthCookieProvider(true, "None");
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieProvider.setAccessTokenCookie(response, "access-token", 60);

        assertThat(response.getHeader("Set-Cookie"))
                .contains("Secure")
                .contains("SameSite=None");
    }
}
