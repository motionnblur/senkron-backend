package com.motionnblur.senkron_backend.message.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.motionnblur.senkron_backend.auth.service.CookieUtils;
import com.motionnblur.senkron_backend.auth.service.JwtService;
import com.motionnblur.senkron_backend.channel.domain.ChannelEntity;
import com.motionnblur.senkron_backend.channel.domain.ChannelMemberEntity;
import com.motionnblur.senkron_backend.channel.domain.ChannelType;
import com.motionnblur.senkron_backend.channel.repository.ChannelMemberRepository;
import com.motionnblur.senkron_backend.channel.repository.ChannelRepository;
import com.motionnblur.senkron_backend.message.domain.MessageEntity;
import com.motionnblur.senkron_backend.message.repository.MessageRepository;
import com.motionnblur.senkron_backend.user.domain.UserEntity;
import com.motionnblur.senkron_backend.user.repository.UserRepository;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private ChannelMemberRepository channelMemberRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private JwtService jwtService;

    private UserEntity user;
    private Cookie accessTokenCookie;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        channelMemberRepository.deleteAll();
        channelRepository.deleteAll();
        userRepository.deleteAll();
        user = userRepository.save(buildUser());
        accessTokenCookie = cookieFor(user);
    }

    @Test
    void sendMessage_returnsUnauthorizedWithoutCookie() throws Exception {
        mockMvc.perform(post("/channels/1/messages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"hello team"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sendMessage_returnsCreatedMessageAndPersists() throws Exception {
        ChannelEntity channel = saveChannel(user, ChannelType.PUBLIC);
        seedMember(user, channel);

        mockMvc.perform(post("/channels/{channelId}/messages", channel.getId())
                        .with(csrf())
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"hello team"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("hello team"))
                .andExpect(jsonPath("$.channelId").value(channel.getId()))
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.authorDisplayName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.id").exists());

        Page<MessageEntity> messages = messageRepository.findByChannelIdOrderByCreatedAtDesc(
                channel.getId(), PageRequest.of(0, 10));
        assertThat(messages.getTotalElements()).isEqualTo(1);
        assertThat(messages.getContent().get(0).getContent()).isEqualTo("hello team");
    }

    @Test
    void sendMessage_returnsBadRequestWhenContentBlank() throws Exception {
        ChannelEntity channel = saveChannel(user, ChannelType.PUBLIC);
        seedMember(user, channel);

        mockMvc.perform(post("/channels/{channelId}/messages", channel.getId())
                        .with(csrf())
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Message content must not be blank"));
    }

    @Test
    void sendMessage_returnsForbiddenWhenNotMember() throws Exception {
        ChannelEntity channel = saveChannel(user, ChannelType.PUBLIC);
        UserEntity nonMember = userRepository.save(buildJoinerUser());
        Cookie nonMemberCookie = cookieFor(nonMember);

        mockMvc.perform(post("/channels/{channelId}/messages", channel.getId())
                        .with(csrf())
                        .cookie(nonMemberCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"hello team"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("Only channel members can send messages"));
    }

    @Test
    void sendMessage_returnsNotFoundWhenChannelMissing() throws Exception {
        mockMvc.perform(post("/channels/{channelId}/messages", 999L)
                        .with(csrf())
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"hello team"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Channel not found: 999"));
    }

    @Test
    void listMessages_returnsUnauthorizedWithoutCookie() throws Exception {
        mockMvc.perform(get("/channels/1/messages"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listMessages_returnsPagedMessagesForMember() throws Exception {
        ChannelEntity channel = saveChannel(user, ChannelType.PUBLIC);
        seedMember(user, channel);
        seedMessage(user, channel, "first", LocalDateTime.of(2026, 1, 15, 10, 0));
        seedMessage(user, channel, "second", LocalDateTime.of(2026, 1, 15, 10, 5));
        seedMessage(user, channel, "third", LocalDateTime.of(2026, 1, 15, 10, 10));

        mockMvc.perform(get("/channels/{channelId}/messages", channel.getId())
                        .param("page", "0")
                        .param("size", "2")
                        .cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].content").value("third"))
                .andExpect(jsonPath("$.content[0].authorDisplayName").value("Ada Lovelace"));
    }

    @Test
    void listMessages_returnsForbiddenWhenNotMember() throws Exception {
        ChannelEntity channel = saveChannel(user, ChannelType.PUBLIC);
        UserEntity nonMember = userRepository.save(buildJoinerUser());
        Cookie nonMemberCookie = cookieFor(nonMember);

        mockMvc.perform(get("/channels/{channelId}/messages", channel.getId())
                        .cookie(nonMemberCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("Only channel members can read messages"));
    }

    @Test
    void listMessages_returnsNotFoundWhenChannelMissing() throws Exception {
        mockMvc.perform(get("/channels/{channelId}/messages", 999L)
                        .cookie(accessTokenCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Channel not found: 999"));
    }

    private Cookie cookieFor(UserEntity authenticatedUser) {
        String token = jwtService.generateToken(authenticatedUser.getId(), authenticatedUser.getEmail());
        return new Cookie(CookieUtils.ACCESS_TOKEN_COOKIE, token);
    }

    private void seedMember(UserEntity member, ChannelEntity channel) {
        ChannelMemberEntity membership = new ChannelMemberEntity();
        membership.setUser(member);
        membership.setChannel(channel);
        membership.setJoinedAt(LocalDateTime.of(2026, 1, 16, 10, 0));
        channelMemberRepository.save(membership);
    }

    private void seedMessage(UserEntity author, ChannelEntity channel, String content, LocalDateTime createdAt) {
        MessageEntity message = new MessageEntity();
        message.setUser(author);
        message.setChannel(channel);
        message.setContent(content);
        message.setCreatedAt(createdAt);
        messageRepository.save(message);
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

    private UserEntity buildJoinerUser() {
        UserEntity entity = new UserEntity();
        entity.setGoogleId("google-test-2");
        entity.setName("Grace");
        entity.setLastName("Hopper");
        entity.setDisplayName("Grace Hopper");
        entity.setEmail("grace@example.com");
        entity.setPassword(null);
        entity.setCreatedAt(LocalDateTime.of(2026, 1, 16, 10, 0));
        return entity;
    }

    private ChannelEntity saveChannel(UserEntity creator, ChannelType type) {
        ChannelEntity channel = new ChannelEntity();
        channel.setName("general");
        channel.setDescription("Main channel");
        channel.setType(type);
        channel.setCreatedBy(creator);
        channel.setCreatedAt(LocalDateTime.of(2026, 1, 15, 10, 0));
        return channelRepository.save(channel);
    }

}
