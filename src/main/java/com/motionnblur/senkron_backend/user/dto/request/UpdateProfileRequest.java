package com.motionnblur.senkron_backend.user.dto.request;

public record UpdateProfileRequest(
        String name,
        String lastName,
        String displayName
) {
}
