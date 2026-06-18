package com.motionnblur.senkron_backend.service;

public record GoogleUserProfile(
        String googleId,
        String email,
        String givenName,
        String familyName,
        String fullName
) {
}
