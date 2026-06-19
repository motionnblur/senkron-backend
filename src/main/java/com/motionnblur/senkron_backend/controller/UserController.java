package com.motionnblur.senkron_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.motionnblur.senkron_backend.dto.response.UserResponse;
import com.motionnblur.senkron_backend.entity.UserEntity;
import com.motionnblur.senkron_backend.security.JwtUserPrincipal;
import com.motionnblur.senkron_backend.service.AuthService;

@RestController
@RequestMapping("/user")
public class UserController {

    private final AuthService authService;
    
    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal JwtUserPrincipal principal) {
        UserEntity user = authService.getUserById(principal.userId());
        UserResponse response = UserResponse.from(user);
        
        return ResponseEntity.ok(response);
    }

}
