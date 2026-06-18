package com.motionnblur.senkron_backend.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.motionnblur.senkron_backend.entity.UserEntity;
import com.motionnblur.senkron_backend.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserEntity findOrCreateGoogleUser(GoogleUserProfile profile) {
        return userRepository.findByGoogleId(profile.googleId())
                .or(() -> userRepository.findByEmail(profile.email())
                        .map(existing -> linkGoogleAccount(existing, profile.googleId())))
                .orElseGet(() -> createGoogleUser(profile));
    }

    public UserEntity getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private UserEntity linkGoogleAccount(UserEntity existing, String googleId) {
        existing.setGoogleId(googleId);
        return userRepository.save(existing);
    }

    private UserEntity createGoogleUser(GoogleUserProfile profile) {
        UserEntity user = new UserEntity();
        user.setGoogleId(profile.googleId());
        user.setEmail(profile.email());
        user.setName(fallback(profile.givenName(), ""));
        user.setLastName(fallback(profile.familyName(), ""));
        user.setDisplayName(fallback(profile.fullName(), profile.email()));
        user.setPassword(null);
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    private String fallback(String value, String defaultValue) {
        return value != null && !value.isBlank() ? value : defaultValue;
    }

}
