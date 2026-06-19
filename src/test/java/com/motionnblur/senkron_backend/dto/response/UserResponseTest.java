package com.motionnblur.senkron_backend.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.motionnblur.senkron_backend.user.UserEntity;
import com.motionnblur.senkron_backend.user.UserResponse;

class UserResponseTest {

    @Test
    void from_mapsAllFieldsFromEntity() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 15, 10, 0);
        UserEntity entity = new UserEntity();
        entity.setId(7L);
        entity.setName("Ada");
        entity.setLastName("Lovelace");
        entity.setDisplayName("Ada Lovelace");
        entity.setEmail("ada@example.com");
        entity.setPassword("hashed-secret");
        entity.setCreatedAt(createdAt);

        UserResponse response = UserResponse.from(entity);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.name()).isEqualTo("Ada");
        assertThat(response.lastName()).isEqualTo("Lovelace");
        assertThat(response.displayName()).isEqualTo("Ada Lovelace");
        assertThat(response.email()).isEqualTo("ada@example.com");
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }
}
