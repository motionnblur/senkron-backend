package com.motionnblur.senkron_backend.auth.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.motionnblur.senkron_backend.auth.domain.JwtUserPrincipal;
import com.motionnblur.senkron_backend.auth.service.AuthService;
import com.motionnblur.senkron_backend.auth.service.CookieUtils;
import com.motionnblur.senkron_backend.user.domain.UserEntity;
import com.motionnblur.senkron_backend.user.dto.response.UserResponse;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final CookieUtils cookieUtils;
    private final AuthService authService;

    public AuthController(CookieUtils cookieUtils, AuthService authService) {
        this.cookieUtils = cookieUtils;
        this.authService = authService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal JwtUserPrincipal principal) {
        UserEntity user = authService.getUserById(principal.userId());
        UserResponse response = UserResponse.from(user);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        response.addHeader("Set-Cookie", cookieUtils.deleteAccessTokenCookie().toString());
        return ResponseEntity.noContent().build();
    }

}
