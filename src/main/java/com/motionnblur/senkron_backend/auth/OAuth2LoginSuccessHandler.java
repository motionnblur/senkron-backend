package com.motionnblur.senkron_backend.auth;

import java.io.IOException;
import java.time.Duration;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.motionnblur.senkron_backend.config.AppProperties;
import com.motionnblur.senkron_backend.user.UserEntity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final CookieUtils cookieUtils;
    private final AuthService authService;
    private final AppProperties appProperties;

    public OAuth2LoginSuccessHandler(
            JwtService jwtService,
            CookieUtils cookieUtils,
            AuthService authService,
            AppProperties appProperties) {
        this.jwtService = jwtService;
        this.cookieUtils = cookieUtils;
        this.authService = authService;
        this.appProperties = appProperties;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        UserEntity user = authService.findOrCreateGoogleUser(GoogleUserProfile.from(oauthUser));

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        response.addHeader(
                "Set-Cookie",
                cookieUtils.createAccessTokenCookie(token, Duration.ofHours(appProperties.jwt().expirationHours()))
                        .toString());

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        response.sendRedirect(appProperties.oauth2().successUrl());
    }

}
