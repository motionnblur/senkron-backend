package com.motionnblur.senkron_backend.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Cookie cookie,
        OAuth2 oauth2,
        Cors cors
) {

    public record Jwt(String secret, int expirationHours) {
    }

    public record Cookie(boolean secure, String sameSite) {
    }

    public record OAuth2(String successUrl) {
    }

    public record Cors(String allowedOrigins) {
    }

}
