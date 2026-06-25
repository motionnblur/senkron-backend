package com.motionnblur.senkron_backend.user.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.motionnblur.senkron_backend.auth.domain.JwtUserPrincipal;
import com.motionnblur.senkron_backend.user.domain.UserEntity;
import com.motionnblur.senkron_backend.user.dto.request.UpdateProfileRequest;
import com.motionnblur.senkron_backend.user.dto.response.UserResponse;
import com.motionnblur.senkron_backend.user.service.UserService;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/update-profile")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody UpdateProfileRequest request) {
        UserEntity updated = userService.updateProfile(principal.userId(), request);
        return ResponseEntity.ok(UserResponse.from(updated));
    }

}
