package com.motionnblur.senkron_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.motionnblur.senkron_backend.auth.GoogleUserProfile;

class GoogleUserProfileTest {

    @Test
    void from_mapsAttributesCorrectly() {
        OAuth2User oauthUser = mock(OAuth2User.class);
        when(oauthUser.getAttributes()).thenReturn(Map.of(
                "sub", "google-123",
                "email", "ada@example.com",
                "given_name", "Ada",
                "family_name", "Lovelace",
                "name", "Ada Lovelace"
        ));

        GoogleUserProfile profile = GoogleUserProfile.from(oauthUser);

        assertThat(profile.googleId()).isEqualTo("google-123");
        assertThat(profile.email()).isEqualTo("ada@example.com");
        assertThat(profile.givenName()).isEqualTo("Ada");
        assertThat(profile.familyName()).isEqualTo("Lovelace");
        assertThat(profile.fullName()).isEqualTo("Ada Lovelace");
    }

    @Test
    void from_yieldsNullFieldsWhenAttributesMissing() {
        OAuth2User oauthUser = mock(OAuth2User.class);
        when(oauthUser.getAttributes()).thenReturn(Map.of(
                "sub", "google-123",
                "email", "ada@example.com"
        ));

        GoogleUserProfile profile = GoogleUserProfile.from(oauthUser);

        assertThat(profile.googleId()).isEqualTo("google-123");
        assertThat(profile.email()).isEqualTo("ada@example.com");
        assertThat(profile.givenName()).isNull();
        assertThat(profile.familyName()).isNull();
        assertThat(profile.fullName()).isNull();
    }
}
