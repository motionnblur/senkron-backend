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

    @Transactional
    public void joinChannel(Long userId, Long channelId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        ChannelEntity channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ChannelNotFoundException(channelId));

        if (channel.getType() != ChannelType.PUBLIC) {
            throw new IllegalStateException("Only public channels can be joined directly");
        }
        if (channelMemberRepository.existsByUserIdAndChannelId(userId, channelId)) {
            return; // idempotent 204
        }

        persistMembership(user, channel);
    }

    @Transactional
    public void addMember(Long inviterId, Long channelId, Long targetUserId) {
        ChannelEntity channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ChannelNotFoundException(channelId));

        if (channel.getType() != ChannelType.PRIVATE) {
            throw new IllegalStateException("Members can only be added to private channels");
        }
        if (!channelMemberRepository.existsByUserIdAndChannelId(inviterId, channelId)) {
            throw new IllegalStateException("Only channel members can add other members");
        }

        UserEntity targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));

        persistMembership(targetUser, channel);
    }

    private void persistMembership(UserEntity user, ChannelEntity channel) {
        if (channelMemberRepository.existsByUserIdAndChannelId(user.getId(), channel.getId())) {
            return;
        }

        ChannelMemberEntity membership = new ChannelMemberEntity();
        membership.setUser(user);
        membership.setChannel(channel);
        membership.setJoinedAt(LocalDateTime.now());

        channelMemberRepository.save(membership);
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

    @Transactional
    public void leaveChannel(Long userId, Long channelId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        ChannelEntity channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ChannelNotFoundException(channelId));

        ChannelMemberEntity membership = channelMemberRepository.findByUserAndChannel(user, channel)
                .orElseThrow(() -> new ChannelMemberNotFoundException(userId, channelId));

        channelMemberRepository.delete(membership);
    }

}
