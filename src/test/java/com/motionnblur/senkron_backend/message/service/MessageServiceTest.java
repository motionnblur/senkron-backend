package com.motionnblur.senkron_backend.message.service;

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

import com.motionnblur.senkron_backend.channel.domain.ChannelEntity;
import com.motionnblur.senkron_backend.channel.domain.ChannelType;
import com.motionnblur.senkron_backend.channel.exception.ChannelNotFoundException;
import com.motionnblur.senkron_backend.channel.repository.ChannelMemberRepository;
import com.motionnblur.senkron_backend.channel.repository.ChannelRepository;
import com.motionnblur.senkron_backend.message.domain.MessageEntity;
import com.motionnblur.senkron_backend.message.dto.request.SendMessageRequest;
import com.motionnblur.senkron_backend.message.dto.response.MessageResponse;
import com.motionnblur.senkron_backend.message.repository.MessageRepository;
import com.motionnblur.senkron_backend.user.domain.UserEntity;
import com.motionnblur.senkron_backend.user.exception.UserNotFoundException;
import com.motionnblur.senkron_backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private ChannelMemberRepository channelMemberRepository;

    @Mock
    private UserRepository userRepository;

    private MessageService messageService;

    @BeforeEach
    void setUp() {
        messageService = new MessageService(
                messageRepository, channelRepository, channelMemberRepository, userRepository);
    }

    @Test
    void sendMessage_persistsMessageAndReturnsResponse() {
        UserEntity author = user(1L, "ada@example.com");
        ChannelEntity channel = channel(10L, "general", ChannelType.PUBLIC);
        SendMessageRequest request = new SendMessageRequest("hello team");

        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(channelRepository.findById(10L)).thenReturn(Optional.of(channel));
        when(channelMemberRepository.existsByUserIdAndChannelId(1L, 10L)).thenReturn(true);
        when(messageRepository.save(any(MessageEntity.class))).thenAnswer(invocation -> {
            MessageEntity message = invocation.getArgument(0);
            message.setId(100L);
            return message;
        });

        MessageResponse result = messageService.sendMessage(1L, 10L, request);

        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.content()).isEqualTo("hello team");
        assertThat(result.channelId()).isEqualTo(10L);
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.authorDisplayName()).isEqualTo("Ada Lovelace");
        assertThat(result.createdAt()).isNotNull();

        ArgumentCaptor<MessageEntity> messageCaptor = ArgumentCaptor.forClass(MessageEntity.class);
        verify(messageRepository).save(messageCaptor.capture());

        MessageEntity saved = messageCaptor.getValue();
        assertThat(saved.getContent()).isEqualTo("hello team");
        assertThat(saved.getUser()).isSameAs(author);
        assertThat(saved.getChannel()).isSameAs(channel);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void sendMessage_throwsWhenContentBlank() {
        SendMessageRequest request = new SendMessageRequest("   ");

        assertThatThrownBy(() -> messageService.sendMessage(1L, 10L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Message content must not be blank");

        verify(messageRepository, never()).save(any(MessageEntity.class));
    }

    @Test
    void sendMessage_throwsWhenContentNull() {
        SendMessageRequest request = new SendMessageRequest(null);

        assertThatThrownBy(() -> messageService.sendMessage(1L, 10L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Message content must not be blank");

        verify(messageRepository, never()).save(any(MessageEntity.class));
    }

    @Test
    void sendMessage_throwsWhenUserNotFound() {
        SendMessageRequest request = new SendMessageRequest("hello");

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.sendMessage(99L, 10L, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found: 99");

        verify(messageRepository, never()).save(any(MessageEntity.class));
    }

    @Test
    void sendMessage_throwsWhenChannelNotFound() {
        UserEntity author = user(1L, "ada@example.com");
        SendMessageRequest request = new SendMessageRequest("hello");

        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(channelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.sendMessage(1L, 99L, request))
                .isInstanceOf(ChannelNotFoundException.class)
                .hasMessage("Channel not found: 99");

        verify(messageRepository, never()).save(any(MessageEntity.class));
    }

    @Test
    void sendMessage_throwsWhenNotMember() {
        UserEntity author = user(1L, "ada@example.com");
        ChannelEntity channel = channel(10L, "general", ChannelType.PUBLIC);
        SendMessageRequest request = new SendMessageRequest("hello");

        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(channelRepository.findById(10L)).thenReturn(Optional.of(channel));
        when(channelMemberRepository.existsByUserIdAndChannelId(1L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> messageService.sendMessage(1L, 10L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only channel members can send messages");

        verify(messageRepository, never()).save(any(MessageEntity.class));
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
