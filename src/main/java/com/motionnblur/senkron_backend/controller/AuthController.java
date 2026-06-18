package com.motionnblur.senkron_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.motionnblur.senkron_backend.dto.response.UserResponse;
import com.motionnblur.senkron_backend.security.CookieUtils;
import com.motionnblur.senkron_backend.security.JwtUserPrincipal;
import com.motionnblur.senkron_backend.service.AuthService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final CookieUtils cookieUtils;

    public AuthController(AuthService authService, CookieUtils cookieUtils) {
        this.authService = authService;
        this.cookieUtils = cookieUtils;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal JwtUserPrincipal principal) {
        return UserResponse.from(authService.getUserById(principal.userId()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        response.addHeader("Set-Cookie", cookieUtils.deleteAccessTokenCookie().toString());
        return ResponseEntity.noContent().build();
    }

}
