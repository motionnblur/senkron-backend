package com.motionnblur.senkron_backend.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.motionnblur.senkron_backend.auth.CookieUtils;
import com.motionnblur.senkron_backend.auth.JwtService;
import com.motionnblur.senkron_backend.user.UserEntity;
import com.motionnblur.senkron_backend.user.UserRepository;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
class ChannelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private ChannelMemberRepository channelMemberRepository;

    @Autowired
    private JwtService jwtService;

    private UserEntity user;
    private Cookie accessTokenCookie;

    @BeforeEach
    void setUp() {
        channelMemberRepository.deleteAll();
        channelRepository.deleteAll();
        userRepository.deleteAll();
        user = userRepository.save(buildUser());
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        accessTokenCookie = new Cookie(CookieUtils.ACCESS_TOKEN_COOKIE, token);
    }

    @Test
    void createChannel_returnsUnauthorizedWithoutCookie() throws Exception {
        mockMvc.perform(post("/channels")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"general","description":"Main channel","type":"PUBLIC"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createChannel_returnsCreatedChannelAndAddsCreatorAsMember() throws Exception {
        mockMvc.perform(post("/channels")
                        .with(csrf())
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"general","description":"Main channel","type":"PUBLIC"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("general"))
                .andExpect(jsonPath("$.description").value("Main channel"))
                .andExpect(jsonPath("$.type").value("PUBLIC"))
                .andExpect(jsonPath("$.createdById").value(user.getId()))
                .andExpect(jsonPath("$.createdAt").exists());

        List<ChannelEntity> channels = channelRepository.findByCreatedById(user.getId());
        assertThat(channels).hasSize(1);
        assertThat(channels.get(0).getName()).isEqualTo("general");
        assertThat(channelMemberRepository.existsByUserIdAndChannelId(user.getId(), channels.get(0).getId()))
                .isTrue();
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
