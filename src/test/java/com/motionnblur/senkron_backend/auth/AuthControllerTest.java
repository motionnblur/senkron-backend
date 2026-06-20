package com.motionnblur.senkron_backend.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.motionnblur.senkron_backend.user.UserEntity;
import com.motionnblur.senkron_backend.user.UserRepository;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private UserEntity user;
    private Cookie accessTokenCookie;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        user = userRepository.save(buildUser());
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        accessTokenCookie = new Cookie(CookieUtils.ACCESS_TOKEN_COOKIE, token);
    }

    @Test
    void me_returnsUnauthorizedWithoutCookie() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_returnsUserWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/auth/me").cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.displayName").value("Ada Lovelace"));
    }

    @Test
    void me_failsWhenAuthenticatedUserNoLongerExists() throws Exception {
        Long deletedUserId = user.getId();
        userRepository.deleteAll();

        mockMvc.perform(get("/auth/me").cookie(accessTokenCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("User not found: " + deletedUserId));
    }

    @Test
    void logout_clearsAccessTokenCookie() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .with(csrf())
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(CookieUtils.ACCESS_TOKEN_COOKIE, 0));
    }

    private UserEntity buildUser() {
        UserEntity entity = new UserEntity();
        entity.setGoogleId("google-test-1");
        entity.setName("Ada");
        entity.setLastName("Lovelace");
        entity.setDisplayName("Ada Lovelace");
        entity.setEmail("ada@example.com");
        entity.setPassword(null);
        entity.setCreatedAt(LocalDateTime.of(2026, 1, 15, 10, 0));
        return entity;
    }

}
