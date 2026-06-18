package com.motionnblur.senkron_backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.dao.DataIntegrityViolationException;

import com.motionnblur.senkron_backend.entity.UserEntity;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.ANY)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private UserEntity persistedUser;

    @BeforeEach
    void setUp() {
        persistedUser = userRepository.save(RepositoryTestFixtures.user("ada@example.com"));
    }

    @Nested
    @DisplayName("findByEmail")
    class FindByEmail {

        @Test
        void returnsUserWhenEmailExists() {
            assertThat(userRepository.findByEmail("ada@example.com"))
                    .isPresent()
                    .get()
                    .extracting(UserEntity::getId, UserEntity::getEmail)
                    .containsExactly(persistedUser.getId(), "ada@example.com");
        }

        @Test
        void returnsEmptyWhenEmailDoesNotExist() {
            assertThat(userRepository.findByEmail("missing@example.com")).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByEmail")
    class ExistsByEmail {

        @Test
        void returnsTrueWhenEmailExists() {
            assertThat(userRepository.existsByEmail("ada@example.com")).isTrue();
        }

        @Test
        void returnsFalseWhenEmailDoesNotExist() {
            assertThat(userRepository.existsByEmail("missing@example.com")).isFalse();
        }
    }

    @Nested
    @DisplayName("JpaRepository operations")
    class JpaRepositoryOperations {

        @Test
        void savesAndFindsById() {
            UserEntity saved = userRepository.save(RepositoryTestFixtures.user("grace@example.com"));

            assertThat(userRepository.findById(saved.getId()))
                    .isPresent()
                    .get()
                    .extracting(UserEntity::getEmail)
                    .isEqualTo("grace@example.com");
        }

        @Test
        void deletesUser() {
            userRepository.delete(persistedUser);

            assertThat(userRepository.findById(persistedUser.getId())).isEmpty();
            assertThat(userRepository.existsByEmail("ada@example.com")).isFalse();
        }

        @Test
        void enforcesUniqueEmailConstraint() {
            UserEntity duplicate = RepositoryTestFixtures.user("ada@example.com");

            org.junit.jupiter.api.Assertions.assertThrows(
                    DataIntegrityViolationException.class,
                    () -> userRepository.saveAndFlush(duplicate));
        }
    }

}
