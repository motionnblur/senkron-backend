package com.motionnblur.senkron_backend.user.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.motionnblur.senkron_backend.auth.service.CookieUtils;
import com.motionnblur.senkron_backend.auth.service.JwtService;
import com.motionnblur.senkron_backend.user.domain.UserEntity;
import com.motionnblur.senkron_backend.user.repository.UserRepository;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

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
    void updateProfile_returnsUnauthorizedWithoutCookie() throws Exception {
        mockMvc.perform(post("/user/update-profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"UpdatedName","lastName":"UpdatedLastName","displayName":"UpdatedDisplayName"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfile_successfullyUpdatesUserProfile() throws Exception {
        mockMvc.perform(post("/user/update-profile")
                        .with(csrf())
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"UpdatedName","lastName":"UpdatedLastName","displayName":"UpdatedDisplayName"}
                                """))
                .andExpect(status().isOk());

        UserEntity updated = userRepository.findById(user.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getName()).isEqualTo("UpdatedName");
        org.assertj.core.api.Assertions.assertThat(updated.getLastName()).isEqualTo("UpdatedLastName");
        org.assertj.core.api.Assertions.assertThat(updated.getDisplayName()).isEqualTo("UpdatedDisplayName");
    }

    @Test
    void updateProfile_returnsBadRequestWhenFieldsAreBlank() throws Exception {
        mockMvc.perform(post("/user/update-profile")
                        .with(csrf())
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","lastName":"UpdatedLastName","displayName":"UpdatedDisplayName"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Name must not be blank"));
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
