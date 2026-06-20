package com.motionnblur.senkron_backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.motionnblur.senkron_backend.config.AppProperties;
import com.motionnblur.senkron_backend.user.UserEntity;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CookieUtils cookieUtils;

    @Mock
    private AuthService authService;

    private AppProperties appProperties;

    private OAuth2LoginSuccessHandler handler;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties(
                new AppProperties.Jwt("secret", 168),
                new AppProperties.Cookie(false, "Lax"),
                new AppProperties.OAuth2("http://localhost:3000/success"),
                new AppProperties.Cors("http://localhost:3000")
        );
        handler = new OAuth2LoginSuccessHandler(jwtService, cookieUtils, authService, appProperties);
    }

    @Test
    void onAuthenticationSuccess_setsAccessTokenCookieAndRedirects() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        OAuth2User oauthUser = mock(OAuth2User.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oauthUser);
        
        when(oauthUser.getAttributes()).thenReturn(Map.of(
                "sub", "google-123",
                "email", "ada@example.com",
                "given_name", "Ada",
                "family_name", "Lovelace",
                "name", "Ada Lovelace"
        ));
        
        UserEntity userEntity = new UserEntity();
        userEntity.setId(42L);
        userEntity.setEmail("ada@example.com");
        
        when(authService.findOrCreateGoogleUser(any(GoogleUserProfile.class))).thenReturn(userEntity);
        when(jwtService.generateToken(42L, "ada@example.com")).thenReturn("mock-token");
        
        when(cookieUtils.createAccessTokenCookie("mock-token", Duration.ofHours(168)))
                .thenReturn(ResponseCookie.from("access_token", "mock-token").build());
                
        handler.onAuthenticationSuccess(request, response, authentication);
        
        verify(authService).findOrCreateGoogleUser(any(GoogleUserProfile.class));
        verify(jwtService).generateToken(42L, "ada@example.com");
        verify(cookieUtils).createAccessTokenCookie("mock-token", Duration.ofHours(168));
        
        String setCookieHeader = response.getHeader("Set-Cookie");
        assertThat(setCookieHeader).isNotNull();
        assertThat(setCookieHeader).contains("access_token=mock-token");
        
        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000/success");
    }

    @Test
    void onAuthenticationSuccess_invalidatesSessionWhenPresent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        OAuth2User oauthUser = mock(OAuth2User.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oauthUser);
        
        when(oauthUser.getAttributes()).thenReturn(Map.of("sub", "google-123", "email", "ada@example.com"));
        
        UserEntity userEntity = new UserEntity();
        userEntity.setId(42L);
        userEntity.setEmail("ada@example.com");
        
        when(authService.findOrCreateGoogleUser(any(GoogleUserProfile.class))).thenReturn(userEntity);
        when(jwtService.generateToken(any(), any())).thenReturn("mock-token");
        when(cookieUtils.createAccessTokenCookie(any(), any()))
                .thenReturn(ResponseCookie.from("access_token", "mock-token").build());
                
        handler.onAuthenticationSuccess(request, response, authentication);
        
        assertThat(session.isInvalid()).isTrue();
    }
}
