package com.motionnblur.senkron_backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.motionnblur.senkron_backend.config.AppProperties;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties(
                new AppProperties.Jwt("test-jwt-secret-key-min-32-chars!!", 168),
                new AppProperties.Cookie(false, "Lax"),
                new AppProperties.OAuth2("http://localhost:3000/auth/callback"),
                new AppProperties.Cors("http://localhost:3000")
        );
        jwtService = new JwtService(properties);
    }

    @Test
    void generateTokenAndParseToken_roundTrip() {
        String token = jwtService.generateToken(42L, "ada@example.com");

        JwtUserPrincipal principal = jwtService.parseToken(token);

        assertThat(principal).isNotNull();
        assertThat(principal.userId()).isEqualTo(42L);
        assertThat(principal.email()).isEqualTo("ada@example.com");
    }

    @Test
    void parseToken_returnsNullForInvalidToken() {
        assertThat(jwtService.parseToken("not-a-valid-token")).isNull();
    }

    @Test
    void parseToken_returnsNullForTamperedToken() {
        String token = jwtService.generateToken(1L, "ada@example.com");
        String tampered = token.substring(0, token.length() - 1) + "x";

        assertThat(jwtService.parseToken(tampered)).isNull();
    }

}
