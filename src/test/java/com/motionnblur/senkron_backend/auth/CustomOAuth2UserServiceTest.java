package com.motionnblur.senkron_backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.motionnblur.senkron_backend.user.UserEntity;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private AuthService authService;

    private OAuth2User stubbedUser;
    private CustomOAuth2UserService service;

    @BeforeEach
    void setUp() {
        stubbedUser = mock(OAuth2User.class);
        service = new CustomOAuth2UserService(authService) {
            @Override
            protected OAuth2User fetchOAuth2User(OAuth2UserRequest userRequest) {
                return stubbedUser;
            }
        };
    }

    @Test
    void loadUser_provisionsUserAndReturnsOriginalPrincipal() {
        when(stubbedUser.getAttributes()).thenReturn(Map.of(
                "sub", "google-123",
                "email", "ada@example.com",
                "given_name", "Ada",
                "family_name", "Lovelace",
                "name", "Ada Lovelace"
        ));
        when(authService.findOrCreateGoogleUser(any(GoogleUserProfile.class))).thenReturn(new UserEntity());

        OAuth2User result = service.loadUser(mock(OAuth2UserRequest.class));

        assertThat(result).isSameAs(stubbedUser);

        ArgumentCaptor<GoogleUserProfile> captor = ArgumentCaptor.forClass(GoogleUserProfile.class);
        verify(authService).findOrCreateGoogleUser(captor.capture());
        assertThat(captor.getValue().googleId()).isEqualTo("google-123");
        assertThat(captor.getValue().email()).isEqualTo("ada@example.com");
    }
}
