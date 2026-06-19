package com.motionnblur.senkron_backend.user;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long userId) {
        super("User not found: " + userId);
    }

}
