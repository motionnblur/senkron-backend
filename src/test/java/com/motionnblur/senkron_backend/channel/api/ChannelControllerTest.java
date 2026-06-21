package com.motionnblur.senkron_backend.channel.api;

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

import com.motionnblur.senkron_backend.auth.service.CookieUtils;
import com.motionnblur.senkron_backend.auth.service.JwtService;
import com.motionnblur.senkron_backend.channel.domain.ChannelEntity;
import com.motionnblur.senkron_backend.channel.domain.ChannelMemberEntity;
import com.motionnblur.senkron_backend.channel.domain.ChannelType;
import com.motionnblur.senkron_backend.channel.repository.ChannelMemberRepository;
import com.motionnblur.senkron_backend.channel.repository.ChannelRepository;
import com.motionnblur.senkron_backend.user.domain.UserEntity;
import com.motionnblur.senkron_backend.user.repository.UserRepository;

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

    @Test
    void joinChannel_returnsUnauthorizedWithoutCookie() throws Exception {
        mockMvc.perform(post("/channels/1/join")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void joinChannel_returnsNoContentAndAddsMember() throws Exception {
        ChannelEntity channel = saveChannel(user, ChannelType.PUBLIC);
        UserEntity joiner = userRepository.save(buildJoinerUser());
        Cookie joinerCookie = cookieFor(joiner);

        mockMvc.perform(post("/channels/{channelId}/join", channel.getId())
                        .with(csrf())
                        .cookie(joinerCookie))
                .andExpect(status().isNoContent());

        assertThat(channelMemberRepository.existsByUserIdAndChannelId(joiner.getId(), channel.getId()))
                .isTrue();
    }

    @Test
    void joinChannel_returnsNoContentWhenAlreadyMember() throws Exception {
        ChannelEntity channel = saveChannel(user, ChannelType.PUBLIC);
        UserEntity joiner = userRepository.save(buildJoinerUser());
        seedMember(joiner, channel);
        Cookie joinerCookie = cookieFor(joiner);

        mockMvc.perform(post("/channels/{channelId}/join", channel.getId())
                        .with(csrf())
                        .cookie(joinerCookie))
                .andExpect(status().isNoContent());

        assertThat(channelMemberRepository.findByUserId(joiner.getId())).hasSize(1);
    }

    @Test
    void joinChannel_failsWhenChannelIsPrivate() throws Exception {
        ChannelEntity channel = saveChannel(user, ChannelType.PRIVATE);
        UserEntity joiner = userRepository.save(buildJoinerUser());
        Cookie joinerCookie = cookieFor(joiner);

        mockMvc.perform(post("/channels/{channelId}/join", channel.getId())
                        .with(csrf())
                        .cookie(joinerCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("Only public channels can be joined directly"));

        assertThat(channelMemberRepository.existsByUserIdAndChannelId(joiner.getId(), channel.getId()))
                .isFalse();
    }

    @Test
    void joinChannel_failsWhenChannelNotFound() throws Exception {
        UserEntity joiner = userRepository.save(buildJoinerUser());
        Cookie joinerCookie = cookieFor(joiner);

        mockMvc.perform(post("/channels/{channelId}/join", 999L)
                        .with(csrf())
                        .cookie(joinerCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Channel not found: 999"));
    }

    @Test
    void leaveChannel_returnsUnauthorizedWithoutCookie() throws Exception {
        mockMvc.perform(post("/channels/1/leave")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void leaveChannel_returnsNoContentAndRemovesMember() throws Exception {
        ChannelEntity channel = saveChannel(user, ChannelType.PUBLIC);
        UserEntity member = userRepository.save(buildJoinerUser());
        seedMember(member, channel);
        Cookie memberCookie = cookieFor(member);

        mockMvc.perform(post("/channels/{channelId}/leave", channel.getId())
                        .with(csrf())
                        .cookie(memberCookie))
                .andExpect(status().isNoContent());

        assertThat(channelMemberRepository.existsByUserIdAndChannelId(member.getId(), channel.getId()))
                .isFalse();
    }

    @Test
    void addMember_returnsUnauthorizedWithoutCookie() throws Exception {
        mockMvc.perform(post("/channels/1/members")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":2}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addMember_returnsNoContentAndAddsMember() throws Exception {
        ChannelEntity channel = saveChannel(user, ChannelType.PRIVATE);
        seedMember(user, channel);
        UserEntity joiner = userRepository.save(buildJoinerUser());

        mockMvc.perform(post("/channels/{channelId}/members", channel.getId())
                        .with(csrf())
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d}
                                """.formatted(joiner.getId())))
                .andExpect(status().isNoContent());

        assertThat(channelMemberRepository.existsByUserIdAndChannelId(joiner.getId(), channel.getId()))
                .isTrue();
    }

    @Test
    void addMember_allowsExistingMemberToInvite() throws Exception {
        ChannelEntity channel = saveChannel(user, ChannelType.PRIVATE);
        seedMember(user, channel);
        UserEntity joiner = userRepository.save(buildJoinerUser());
        UserEntity invitee = userRepository.save(buildThirdUser());
        seedMember(joiner, channel);
        Cookie joinerCookie = cookieFor(joiner);

        mockMvc.perform(post("/channels/{channelId}/members", channel.getId())
                        .with(csrf())
                        .cookie(joinerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d}
                                """.formatted(invitee.getId())))
                .andExpect(status().isNoContent());

        assertThat(channelMemberRepository.existsByUserIdAndChannelId(invitee.getId(), channel.getId()))
                .isTrue();
    }

    @Test
    void addMember_returnsNoContentWhenAlreadyMember() throws Exception {
        ChannelEntity channel = saveChannel(user, ChannelType.PRIVATE);
        seedMember(user, channel);
        UserEntity joiner = userRepository.save(buildJoinerUser());
        seedMember(joiner, channel);

        mockMvc.perform(post("/channels/{channelId}/members", channel.getId())
                        .with(csrf())
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d}
                                """.formatted(joiner.getId())))
                .andExpect(status().isNoContent());

        assertThat(channelMemberRepository.findByUserId(joiner.getId())).hasSize(1);
    }

    @Test
    void addMember_returnsForbiddenWhenInviterNotMember() throws Exception {
        ChannelEntity channel = saveChannel(user, ChannelType.PRIVATE);
        seedMember(user, channel);
        UserEntity nonMember = userRepository.save(buildJoinerUser());
        Cookie nonMemberCookie = cookieFor(nonMember);

        mockMvc.perform(post("/channels/{channelId}/members", channel.getId())
                        .with(csrf())
                        .cookie(nonMemberCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d}
                                """.formatted(nonMember.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("Only channel members can add other members"));
    }

    @Test
    void addMember_returnsForbiddenForPublicChannel() throws Exception {
        ChannelEntity channel = saveChannel(user, ChannelType.PUBLIC);
        seedMember(user, channel);
        UserEntity joiner = userRepository.save(buildJoinerUser());

        mockMvc.perform(post("/channels/{channelId}/members", channel.getId())
                        .with(csrf())
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d}
                                """.formatted(joiner.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("Members can only be added to private channels"));
    }

    @Test
    void addMember_returnsNotFoundWhenChannelMissing() throws Exception {
        UserEntity joiner = userRepository.save(buildJoinerUser());

        mockMvc.perform(post("/channels/{channelId}/members", 999L)
                        .with(csrf())
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d}
                                """.formatted(joiner.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Channel not found: 999"));
    }

    @Test
    void addMember_returnsNotFoundWhenTargetUserMissing() throws Exception {
        ChannelEntity channel = saveChannel(user, ChannelType.PRIVATE);
        seedMember(user, channel);

        mockMvc.perform(post("/channels/{channelId}/members", channel.getId())
                        .with(csrf())
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":999}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("User not found: 999"));
    }

    @Test
    void leaveChannel_failsWhenNotMember() throws Exception {
        ChannelEntity channel = saveChannel(user, ChannelType.PUBLIC);
        UserEntity nonMember = userRepository.save(buildJoinerUser());
        Cookie nonMemberCookie = cookieFor(nonMember);

        mockMvc.perform(post("/channels/{channelId}/leave", channel.getId())
                        .with(csrf())
                        .cookie(nonMemberCookie))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value(
                        "Channel member not found for user " + nonMember.getId() + " in channel " + channel.getId()));
    }

    @Test
    void leaveChannel_failsWhenChannelNotFound() throws Exception {
        UserEntity member = userRepository.save(buildJoinerUser());
        Cookie memberCookie = cookieFor(member);

        mockMvc.perform(post("/channels/{channelId}/leave", 999L)
                        .with(csrf())
                        .cookie(memberCookie))
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

    private UserEntity buildThirdUser() {
        UserEntity entity = new UserEntity();
        entity.setGoogleId("google-test-3");
        entity.setName("Alan");
        entity.setLastName("Turing");
        entity.setDisplayName("Alan Turing");
        entity.setEmail("alan@example.com");
        entity.setPassword(null);
        entity.setCreatedAt(LocalDateTime.of(2026, 1, 17, 10, 0));
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
