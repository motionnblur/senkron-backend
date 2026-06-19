package com.motionnblur.senkron_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.motionnblur.senkron_backend.entity.UserEntity;
import com.motionnblur.senkron_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository);
    }

    @Test
    void findOrCreateGoogleUser_returnsExistingUserWhenGoogleIdMatches() {
        UserEntity existing = googleUser(1L, "google-123", "ada@example.com");
        GoogleUserProfile profile = profile("google-123", "ada@example.com");

        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.of(existing));

        UserEntity result = authService.findOrCreateGoogleUser(profile);

        assertThat(result).isSameAs(existing);
        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void findOrCreateGoogleUser_linksGoogleIdWhenEmailExists() {
        UserEntity existing = passwordUser(2L, "ada@example.com");
        GoogleUserProfile profile = profile("google-456", "ada@example.com");

        when(userRepository.findByGoogleId("google-456")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        UserEntity result = authService.findOrCreateGoogleUser(profile);

        assertThat(result.getGoogleId()).isEqualTo("google-456");
        verify(userRepository).save(existing);
    }

    @Test
    void findOrCreateGoogleUser_createsUserWhenNotFound() {
        GoogleUserProfile profile = profile("google-789", "new@example.com");

        when(userRepository.findByGoogleId("google-789")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity saved = invocation.getArgument(0);
            saved.setId(3L);
            return saved;
        });

        UserEntity result = authService.findOrCreateGoogleUser(profile);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());

        UserEntity created = captor.getValue();
        assertThat(created.getGoogleId()).isEqualTo("google-789");
        assertThat(created.getEmail()).isEqualTo("new@example.com");
        assertThat(created.getPassword()).isNull();
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(result.getId()).isEqualTo(3L);
    }

    private GoogleUserProfile profile(String googleId, String email) {
        return new GoogleUserProfile(googleId, email, "Ada", "Lovelace", "Ada Lovelace");
    }

    private UserEntity googleUser(Long id, String googleId, String email) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setGoogleId(googleId);
        user.setEmail(email);
        user.setName("Ada");
        user.setLastName("Lovelace");
        user.setDisplayName("Ada Lovelace");
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    private UserEntity passwordUser(Long id, String email) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail(email);
        user.setName("Ada");
        user.setLastName("Lovelace");
        user.setDisplayName("Ada Lovelace");
        user.setPassword("hashed");
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    @Test
    void getUserById_returnsUserWhenFound() {
        UserEntity existing = googleUser(42L, "google-123", "ada@example.com");
        when(userRepository.findById(42L)).thenReturn(Optional.of(existing));

        UserEntity result = authService.getUserById(42L);

        assertThat(result).isSameAs(existing);
    }

    @Test
    void getUserById_throwsUserNotFoundExceptionWhenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        UserNotFoundException ex = org.junit.jupiter.api.Assertions.assertThrows(
                UserNotFoundException.class,
                () -> authService.getUserById(99L));
        
        assertThat(ex.getMessage()).contains("99");
    }

}
