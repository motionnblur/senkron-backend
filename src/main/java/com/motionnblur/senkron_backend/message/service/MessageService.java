package com.motionnblur.senkron_backend.message.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.motionnblur.senkron_backend.channel.domain.ChannelEntity;
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

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final UserRepository userRepository;

    public MessageService(
            MessageRepository messageRepository,
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public MessageResponse sendMessage(Long userId, Long channelId, SendMessageRequest request) {
        validateContent(request);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        ChannelEntity channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ChannelNotFoundException(channelId));

        if (!channelMemberRepository.existsByUserIdAndChannelId(userId, channelId)) {
            throw new IllegalStateException("Only channel members can send messages");
        }

        MessageEntity message = new MessageEntity();
        message.setContent(request.content());
        message.setUser(user);
        message.setChannel(channel);
        message.setCreatedAt(LocalDateTime.now());

        MessageEntity saved = messageRepository.save(message);
        return MessageResponse.from(saved);
    }

    private void validateContent(SendMessageRequest request) {
        if (request.content() == null || request.content().isBlank()) {
            throw new IllegalArgumentException("Message content must not be blank");
        }
    }

}
