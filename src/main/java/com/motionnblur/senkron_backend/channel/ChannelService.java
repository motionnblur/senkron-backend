package com.motionnblur.senkron_backend.channel;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.motionnblur.senkron_backend.user.UserEntity;
import com.motionnblur.senkron_backend.user.UserNotFoundException;
import com.motionnblur.senkron_backend.user.UserRepository;

@Service
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final UserRepository userRepository;

    public ChannelService(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            UserRepository userRepository) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ChannelEntity createChannel(Long creatorId, CreateChannelRequest request) {
        validateRequest(request);

        UserEntity creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new UserNotFoundException(creatorId));

        LocalDateTime now = LocalDateTime.now();

        ChannelEntity channel = new ChannelEntity();
        channel.setName(request.name());
        channel.setDescription(request.description());
        channel.setType(request.type());
        channel.setCreatedBy(creator);
        channel.setCreatedAt(now);

        ChannelEntity saved = channelRepository.save(channel);

        ChannelMemberEntity membership = new ChannelMemberEntity();
        membership.setUser(creator);
        membership.setChannel(saved);
        membership.setJoinedAt(now);
        channelMemberRepository.save(membership);

        return saved;
    }

    private void validateRequest(CreateChannelRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Channel name must not be blank");
        }
        if (request.description() == null || request.description().isBlank()) {
            throw new IllegalArgumentException("Channel description must not be blank");
        }
        if (request.type() == null) {
            throw new IllegalArgumentException("Channel type must not be null");
        }
        if (request.type() == ChannelType.DM) {
            throw new IllegalArgumentException("DM channels cannot be created via this endpoint");
        }
    }

}
