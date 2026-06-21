package com.motionnblur.senkron_backend.message.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.motionnblur.senkron_backend.channel.domain.ChannelEntity;
import com.motionnblur.senkron_backend.message.domain.MessageEntity;
import com.motionnblur.senkron_backend.user.domain.UserEntity;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    List<MessageEntity> findByChannelOrderByCreatedAtDesc(ChannelEntity channel);

    Page<MessageEntity> findByChannelIdOrderByCreatedAtDesc(Long channelId, Pageable pageable);

    List<MessageEntity> findByUser(UserEntity user);

}
