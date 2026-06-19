package com.motionnblur.senkron_backend.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final CookieUtils cookieUtils;

    public AuthController(CookieUtils cookieUtils) {
        this.cookieUtils = cookieUtils;
    }


    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        response.addHeader("Set-Cookie", cookieUtils.deleteAccessTokenCookie().toString());
        return ResponseEntity.noContent().build();
    }

}
