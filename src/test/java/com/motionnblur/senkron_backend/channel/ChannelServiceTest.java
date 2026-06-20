package com.motionnblur.senkron_backend.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

}
