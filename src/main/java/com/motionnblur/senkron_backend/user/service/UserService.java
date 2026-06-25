package com.motionnblur.senkron_backend.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.motionnblur.senkron_backend.user.domain.UserEntity;
import com.motionnblur.senkron_backend.user.dto.request.UpdateProfileRequest;
import com.motionnblur.senkron_backend.user.exception.UserNotFoundException;
import com.motionnblur.senkron_backend.user.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserEntity updateProfile(Long userId, UpdateProfileRequest request) {
        validateRequest(request);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.setName(request.name());
        user.setLastName(request.lastName() == null ? "" : request.lastName());
        user.setDisplayName(request.displayName());
        user.setTitle(request.title());

        return userRepository.save(user);
    }

    private void validateRequest(UpdateProfileRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Name must not be blank");
        }
        if (request.displayName() == null || request.displayName().isBlank()) {
            throw new IllegalArgumentException("Display name must not be blank");
        }
    }

}
