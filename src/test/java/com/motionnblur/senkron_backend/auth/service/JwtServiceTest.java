package com.motionnblur.senkron_backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.motionnblur.senkron_backend.auth.domain.JwtUserPrincipal;
import com.motionnblur.senkron_backend.config.properties.AppProperties;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtServiceTest {

    private static final String SECRET = "test-jwt-secret-key-min-32-chars!!";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(propertiesWithExpiration(168));
    }

    private AppProperties propertiesWithExpiration(int expirationHours) {
        return new AppProperties(
                new AppProperties.Jwt(SECRET, expirationHours),
                new AppProperties.Cookie(false, "Lax"),
                new AppProperties.OAuth2("http://localhost:3000/auth/callback"),
                new AppProperties.Cors("http://localhost:3000")
        );
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

    @Test
    void parseToken_returnsNullForExpiredToken() {
        JwtService expiringService = new JwtService(propertiesWithExpiration(-1));
        String token = expiringService.generateToken(42L, "ada@example.com");

        assertThat(expiringService.parseToken(token)).isNull();
    }

    @Test
    void parseToken_returnsNullWhenSubjectNotNumeric() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        Date now = new Date();
        String token = Jwts.builder()
                .subject("not-a-number")
                .claim("email", "ada@example.com")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 3_600_000L))
                .signWith(key)
                .compact();

        assertThat(jwtService.parseToken(token)).isNull();
    }

}
