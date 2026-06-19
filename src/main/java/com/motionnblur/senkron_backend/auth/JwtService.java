package com.motionnblur.senkron_backend.auth;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.motionnblur.senkron_backend.config.AppProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMillis;

    public JwtService(AppProperties appProperties) {
        this.secretKey = Keys.hmacShaKeyFor(appProperties.jwt().secret().getBytes());
        this.expirationMillis = appProperties.jwt().expirationHours() * 3_600_000L;
    }

    public String generateToken(Long userId, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public JwtUserPrincipal parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = Long.parseLong(claims.getSubject());
            String email = claims.get("email", String.class);
            return new JwtUserPrincipal(userId, email);
        } catch (JwtException | NumberFormatException ex) {
            return null;
        }
    }

}
