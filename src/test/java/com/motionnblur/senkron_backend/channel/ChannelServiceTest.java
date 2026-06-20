package com.motionnblur.senkron_backend.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.motionnblur.senkron_backend.user.UserEntity;
import com.motionnblur.senkron_backend.user.UserNotFoundException;
import com.motionnblur.senkron_backend.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class ChannelServiceTest {

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private ChannelMemberRepository channelMemberRepository;

    @Mock
    private UserRepository userRepository;

    private ChannelService channelService;

    @BeforeEach
    void setUp() {
        channelService = new ChannelService(channelRepository, channelMemberRepository, userRepository);
    }

    @Test
    void createChannel_persistsChannelAndCreatorMembership() {
        UserEntity creator = user(1L, "ada@example.com");
        CreateChannelRequest request = new CreateChannelRequest("general", "Main channel", ChannelType.PUBLIC);

        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(channelRepository.save(any(ChannelEntity.class))).thenAnswer(invocation -> {
            ChannelEntity channel = invocation.getArgument(0);
            channel.setId(10L);
            return channel;
        });

        ChannelEntity result = channelService.createChannel(1L, request);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("general");
        assertThat(result.getDescription()).isEqualTo("Main channel");
        assertThat(result.getType()).isEqualTo(ChannelType.PUBLIC);
        assertThat(result.getCreatedBy()).isSameAs(creator);
        assertThat(result.getCreatedAt()).isNotNull();

        ArgumentCaptor<ChannelMemberEntity> membershipCaptor = ArgumentCaptor.forClass(ChannelMemberEntity.class);
        verify(channelMemberRepository).save(membershipCaptor.capture());

        ChannelMemberEntity membership = membershipCaptor.getValue();
        assertThat(membership.getUser()).isSameAs(creator);
        assertThat(membership.getChannel()).isSameAs(result);
        assertThat(membership.getJoinedAt()).isNotNull();
    }

    @Test
    void createChannel_throwsWhenUserNotFound() {
        CreateChannelRequest request = new CreateChannelRequest("general", "Main channel", ChannelType.PUBLIC);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> channelService.createChannel(99L, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found: 99");
    }

    @Test
    void createChannel_rejectsDmType() {
        CreateChannelRequest request = new CreateChannelRequest("dm", "Direct message", ChannelType.DM);

        assertThatThrownBy(() -> channelService.createChannel(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("DM channels cannot be created via this endpoint");
    }

    @Test
    void joinChannel_persistsMembership() {
        UserEntity user = user(2L, "grace@example.com");
        ChannelEntity channel = channel(10L, "general", ChannelType.PUBLIC);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(channelRepository.findById(10L)).thenReturn(Optional.of(channel));
        when(channelMemberRepository.existsByUserIdAndChannelId(2L, 10L)).thenReturn(false);

        channelService.joinChannel(2L, 10L);

        ArgumentCaptor<ChannelMemberEntity> membershipCaptor = ArgumentCaptor.forClass(ChannelMemberEntity.class);
        verify(channelMemberRepository).save(membershipCaptor.capture());

        ChannelMemberEntity membership = membershipCaptor.getValue();
        assertThat(membership.getUser()).isSameAs(user);
        assertThat(membership.getChannel()).isSameAs(channel);
        assertThat(membership.getJoinedAt()).isNotNull();
    }

    @Test
    void joinChannel_skipsSaveWhenAlreadyMember() {
        UserEntity user = user(2L, "grace@example.com");
        ChannelEntity channel = channel(10L, "general", ChannelType.PUBLIC);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(channelRepository.findById(10L)).thenReturn(Optional.of(channel));
        when(channelMemberRepository.existsByUserIdAndChannelId(2L, 10L)).thenReturn(true);

        channelService.joinChannel(2L, 10L);

        verify(channelMemberRepository, never()).save(any(ChannelMemberEntity.class));
    }

    @Test
    void joinChannel_rejectsPrivateChannel() {
        UserEntity user = user(2L, "grace@example.com");
        ChannelEntity channel = channel(10L, "team", ChannelType.PRIVATE);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(channelRepository.findById(10L)).thenReturn(Optional.of(channel));

        assertThatThrownBy(() -> channelService.joinChannel(2L, 10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only public channels can be joined directly");

        verify(channelMemberRepository, never()).save(any(ChannelMemberEntity.class));
    }

    @Test
    void joinChannel_rejectsDmChannel() {
        UserEntity user = user(2L, "grace@example.com");
        ChannelEntity channel = channel(10L, "dm", ChannelType.DM);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(channelRepository.findById(10L)).thenReturn(Optional.of(channel));

        assertThatThrownBy(() -> channelService.joinChannel(2L, 10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only public channels can be joined directly");

        verify(channelMemberRepository, never()).save(any(ChannelMemberEntity.class));
    }

    @Test
    void joinChannel_throwsWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> channelService.joinChannel(99L, 10L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found: 99");
    }

    @Test
    void joinChannel_throwsWhenChannelNotFound() {
        UserEntity user = user(2L, "grace@example.com");

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(channelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> channelService.joinChannel(2L, 99L))
                .isInstanceOf(ChannelNotFoundException.class)
                .hasMessage("Channel not found: 99");
    }

    @Test
    void leaveChannel_deletesMembership() {
        UserEntity user = user(2L, "grace@example.com");
        ChannelEntity channel = channel(10L, "general", ChannelType.PUBLIC);
        ChannelMemberEntity membership = new ChannelMemberEntity();
        membership.setUser(user);
        membership.setChannel(channel);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(channelRepository.findById(10L)).thenReturn(Optional.of(channel));
        when(channelMemberRepository.findByUserAndChannel(user, channel)).thenReturn(Optional.of(membership));

        channelService.leaveChannel(2L, 10L);

        verify(channelMemberRepository).delete(membership);
    }

    @Test
    void leaveChannel_throwsWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> channelService.leaveChannel(99L, 10L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found: 99");
    }

    @Test
    void leaveChannel_throwsWhenChannelNotFound() {
        UserEntity user = user(2L, "grace@example.com");

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(channelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> channelService.leaveChannel(2L, 99L))
                .isInstanceOf(ChannelNotFoundException.class)
                .hasMessage("Channel not found: 99");
    }

    @Test
    void leaveChannel_throwsWhenNotMember() {
        UserEntity user = user(2L, "grace@example.com");
        ChannelEntity channel = channel(10L, "general", ChannelType.PUBLIC);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(channelRepository.findById(10L)).thenReturn(Optional.of(channel));
        when(channelMemberRepository.findByUserAndChannel(user, channel)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> channelService.leaveChannel(2L, 10L))
                .isInstanceOf(ChannelMemberNotFoundException.class)
                .hasMessage("Channel member not found for user 2 in channel 10");
    }

    @Test
    void addMember_persistsMembership() {
        UserEntity inviter = user(1L, "ada@example.com");
        UserEntity target = user(2L, "grace@example.com");
        ChannelEntity channel = channel(10L, "team", ChannelType.PRIVATE);

        when(channelRepository.findById(10L)).thenReturn(Optional.of(channel));
        when(channelMemberRepository.existsByUserIdAndChannelId(1L, 10L)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(channelMemberRepository.existsByUserIdAndChannelId(2L, 10L)).thenReturn(false);

        channelService.addMember(1L, 10L, 2L);

        ArgumentCaptor<ChannelMemberEntity> membershipCaptor = ArgumentCaptor.forClass(ChannelMemberEntity.class);
        verify(channelMemberRepository).save(membershipCaptor.capture());

        ChannelMemberEntity membership = membershipCaptor.getValue();
        assertThat(membership.getUser()).isSameAs(target);
        assertThat(membership.getChannel()).isSameAs(channel);
        assertThat(membership.getJoinedAt()).isNotNull();
    }

    @Test
    void addMember_skipsSaveWhenAlreadyMember() {
        UserEntity target = user(2L, "grace@example.com");
        ChannelEntity channel = channel(10L, "team", ChannelType.PRIVATE);

        when(channelRepository.findById(10L)).thenReturn(Optional.of(channel));
        when(channelMemberRepository.existsByUserIdAndChannelId(1L, 10L)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(channelMemberRepository.existsByUserIdAndChannelId(2L, 10L)).thenReturn(true);

        channelService.addMember(1L, 10L, 2L);

        verify(channelMemberRepository, never()).save(any(ChannelMemberEntity.class));
    }

    @Test
    void addMember_rejectsWhenInviterNotMember() {
        ChannelEntity channel = channel(10L, "team", ChannelType.PRIVATE);

        when(channelRepository.findById(10L)).thenReturn(Optional.of(channel));
        when(channelMemberRepository.existsByUserIdAndChannelId(1L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> channelService.addMember(1L, 10L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only channel members can add other members");

        verify(channelMemberRepository, never()).save(any(ChannelMemberEntity.class));
    }

    @Test
    void addMember_rejectsPublicChannel() {
        ChannelEntity channel = channel(10L, "general", ChannelType.PUBLIC);

        when(channelRepository.findById(10L)).thenReturn(Optional.of(channel));

        assertThatThrownBy(() -> channelService.addMember(1L, 10L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Members can only be added to private channels");

        verify(channelMemberRepository, never()).save(any(ChannelMemberEntity.class));
    }

    @Test
    void addMember_throwsWhenChannelNotFound() {
        when(channelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> channelService.addMember(1L, 99L, 2L))
                .isInstanceOf(ChannelNotFoundException.class)
                .hasMessage("Channel not found: 99");
    }

    @Test
    void addMember_throwsWhenTargetUserNotFound() {
        ChannelEntity channel = channel(10L, "team", ChannelType.PRIVATE);

        when(channelRepository.findById(10L)).thenReturn(Optional.of(channel));
        when(channelMemberRepository.existsByUserIdAndChannelId(1L, 10L)).thenReturn(true);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> channelService.addMember(1L, 10L, 99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found: 99");
    }

    private UserEntity user(Long id, String email) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setName("Ada");
        user.setLastName("Lovelace");
        user.setDisplayName("Ada Lovelace");
        user.setEmail(email);
        user.setCreatedAt(LocalDateTime.of(2026, 1, 15, 10, 0));
        return user;
    }

    private ChannelEntity channel(Long id, String name, ChannelType type) {
        ChannelEntity channel = new ChannelEntity();
        channel.setId(id);
        channel.setName(name);
        channel.setDescription("Main channel");
        channel.setType(type);
        channel.setCreatedBy(user(1L, "ada@example.com"));
        channel.setCreatedAt(LocalDateTime.of(2026, 1, 15, 10, 0));
        return channel;
    }

}
