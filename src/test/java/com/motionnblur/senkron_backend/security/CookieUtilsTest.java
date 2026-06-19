package com.motionnblur.senkron_backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import com.motionnblur.senkron_backend.config.AppProperties;

class CookieUtilsTest {

    private CookieUtils cookieUtils(boolean secure, String sameSite) {
        AppProperties properties = new AppProperties(
                new AppProperties.Jwt("test-jwt-secret-key-min-32-chars!!", 168),
                new AppProperties.Cookie(secure, sameSite),
                new AppProperties.OAuth2("http://localhost:3000/auth/callback"),
                new AppProperties.Cors("http://localhost:3000")
        );
        return new CookieUtils(properties);
    }

    @Test
    void createAccessTokenCookie_setsSecurityAttributesFromProperties() {
        ResponseCookie cookie = cookieUtils(true, "Strict")
                .createAccessTokenCookie("token-value", Duration.ofHours(168));

        assertThat(cookie.getName()).isEqualTo(CookieUtils.ACCESS_TOKEN_COOKIE);
        assertThat(cookie.getValue()).isEqualTo("token-value");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofHours(168));
    }

    @Test
    void createAccessTokenCookie_honorsInsecureConfiguration() {
        ResponseCookie cookie = cookieUtils(false, "Lax")
                .createAccessTokenCookie("token-value", Duration.ofHours(1));

        assertThat(cookie.isSecure()).isFalse();
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
    }

    @Test
    void deleteAccessTokenCookie_clearsValueAndExpiresImmediately() {
        ResponseCookie cookie = cookieUtils(true, "Lax").deleteAccessTokenCookie();

        assertThat(cookie.getName()).isEqualTo(CookieUtils.ACCESS_TOKEN_COOKIE);
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isZero();
    }
}
