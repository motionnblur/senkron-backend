package com.motionnblur.senkron_backend.user;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.motionnblur.senkron_backend.auth.AuthService;
import com.motionnblur.senkron_backend.auth.JwtUserPrincipal;

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
