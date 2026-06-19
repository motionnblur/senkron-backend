package com.motionnblur.senkron_backend.auth;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.motionnblur.senkron_backend.config.AppProperties;

@Component
public class CookieUtils {

    public static final String ACCESS_TOKEN_COOKIE = "access_token";

    private final AppProperties appProperties;

    public CookieUtils(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public ResponseCookie createAccessTokenCookie(String token, Duration maxAge) {
        return baseCookie(token, maxAge);
    }

    public ResponseCookie deleteAccessTokenCookie() {
        return baseCookie("", Duration.ZERO);
    }

    private ResponseCookie baseCookie(String value, Duration maxAge) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, value)
                .httpOnly(true)
                .secure(appProperties.cookie().secure())
                .sameSite(appProperties.cookie().sameSite())
                .path("/")
                .maxAge(maxAge)
                .build();
    }

}
