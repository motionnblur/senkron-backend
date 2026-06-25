package com.motionnblur.senkron_backend.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.motionnblur.senkron_backend.user.domain.UserEntity;
import com.motionnblur.senkron_backend.user.dto.request.UpdateProfileRequest;
import com.motionnblur.senkron_backend.user.exception.UserNotFoundException;
import com.motionnblur.senkron_backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void updateProfile_successfullyUpdatesFields() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setName("OldName");
        user.setLastName("OldLastName");
        user.setDisplayName("OldDisplayName");
        user.setEmail("test@example.com");
        user.setCreatedAt(LocalDateTime.now());

        UpdateProfileRequest request = new UpdateProfileRequest("NewName", "NewLastName", "NewDisplayName", null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserEntity result = userService.updateProfile(1L, request);

        assertThat(result.getName()).isEqualTo("NewName");
        assertThat(result.getLastName()).isEqualTo("NewLastName");
        assertThat(result.getDisplayName()).isEqualTo("NewDisplayName");
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_throwsUserNotFoundExceptionWhenUserMissing() {
        UpdateProfileRequest request = new UpdateProfileRequest("NewName", "NewLastName", "NewDisplayName", null);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.updateProfile(99L, request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_throwsIllegalArgumentExceptionWhenNameIsBlank() {
        UpdateProfileRequest request = new UpdateProfileRequest(" ", "LastName", "DisplayName", null);

        assertThrows(IllegalArgumentException.class, () -> userService.updateProfile(1L, request));
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_allowsNullLastNameAndConvertsToEmptyString() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setName("Name");
        user.setLastName("OldLastName");
        user.setDisplayName("DisplayName");
        user.setEmail("test@example.com");
        user.setCreatedAt(LocalDateTime.now());

        UpdateProfileRequest request = new UpdateProfileRequest("Name", null, "DisplayName", null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserEntity result = userService.updateProfile(1L, request);

        assertThat(result.getLastName()).isEqualTo("");
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_throwsIllegalArgumentExceptionWhenDisplayNameIsBlank() {
        UpdateProfileRequest request = new UpdateProfileRequest("Name", "LastName", "", null);

        assertThrows(IllegalArgumentException.class, () -> userService.updateProfile(1L, request));
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
    }

}
