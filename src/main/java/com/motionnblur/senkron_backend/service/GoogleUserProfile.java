package com.motionnblur.senkron_backend.service;

import java.util.Map;

import org.springframework.security.oauth2.core.user.OAuth2User;

public record GoogleUserProfile(
        String googleId,
        String email,
        String givenName,
        String familyName,
        String fullName
) {

    public static GoogleUserProfile from(OAuth2User oauthUser) {
        Map<String, Object> attributes = oauthUser.getAttributes();
        return new GoogleUserProfile(
                (String) attributes.get("sub"),
                (String) attributes.get("email"),
                (String) attributes.get("given_name"),
                (String) attributes.get("family_name"),
                (String) attributes.get("name"));
    }

}
