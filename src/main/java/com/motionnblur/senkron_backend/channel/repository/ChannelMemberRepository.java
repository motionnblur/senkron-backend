package com.motionnblur.senkron_backend.channel.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.motionnblur.senkron_backend.channel.domain.ChannelEntity;
import com.motionnblur.senkron_backend.channel.domain.ChannelMemberEntity;
import com.motionnblur.senkron_backend.user.domain.UserEntity;

public interface ChannelMemberRepository extends JpaRepository<ChannelMemberEntity, Long> {

    List<ChannelMemberEntity> findByUser(UserEntity user);

    List<ChannelMemberEntity> findByUserId(Long userId);

    List<ChannelMemberEntity> findByChannel(ChannelEntity channel);

    List<ChannelMemberEntity> findByChannelId(Long channelId);

    Optional<ChannelMemberEntity> findByUserAndChannel(UserEntity user, ChannelEntity channel);

    boolean existsByUserIdAndChannelId(Long userId, Long channelId);

}
