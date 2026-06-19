package com.motionnblur.senkron_backend.auth;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AuthService authService;

    public CustomOAuth2UserService(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = fetchOAuth2User(userRequest);

        authService.findOrCreateGoogleUser(GoogleUserProfile.from(oauthUser));
        return oauthUser;
    }

    protected OAuth2User fetchOAuth2User(OAuth2UserRequest userRequest) {
        return super.loadUser(userRequest);
    }

}
