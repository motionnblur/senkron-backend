package com.motionnblur.senkron_backend.security;

import java.io.IOException;
import java.time.Duration;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.motionnblur.senkron_backend.config.AppProperties;
import com.motionnblur.senkron_backend.entity.UserEntity;
import com.motionnblur.senkron_backend.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final CookieUtils cookieUtils;
    private final UserRepository userRepository;
    private final AppProperties appProperties;

    public OAuth2LoginSuccessHandler(
            JwtService jwtService,
            CookieUtils cookieUtils,
            UserRepository userRepository,
            AppProperties appProperties) {
        this.jwtService = jwtService;
        this.cookieUtils = cookieUtils;
        this.userRepository = userRepository;
        this.appProperties = appProperties;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String email = oauthUser.getAttribute("email");

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("OAuth user not found after login: " + email));

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
