package com.motionnblur.senkron_backend.dto.response;

import java.time.LocalDateTime;

import com.motionnblur.senkron_backend.entity.UserEntity;

public record UserResponse(
        Long id,
        String name,
        String lastName,
        String displayName,
        String email,
        LocalDateTime createdAt
) {

    public static UserResponse from(UserEntity entity) {
        return new UserResponse(
                entity.getId(),
                entity.getName(),
                entity.getLastName(),
                entity.getDisplayName(),
                entity.getEmail(),
                entity.getCreatedAt()
        );
    }

}
