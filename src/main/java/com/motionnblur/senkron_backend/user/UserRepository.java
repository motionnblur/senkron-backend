package com.motionnblur.senkron_backend.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByGoogleId(String googleId);

    boolean existsByEmail(String email);

}
