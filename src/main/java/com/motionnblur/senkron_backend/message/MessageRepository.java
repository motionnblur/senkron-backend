package com.motionnblur.senkron_backend.message;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.motionnblur.senkron_backend.channel.ChannelEntity;
import com.motionnblur.senkron_backend.user.UserEntity;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    List<MessageEntity> findByChannelOrderByCreatedAtDesc(ChannelEntity channel);

    Page<MessageEntity> findByChannelIdOrderByCreatedAtDesc(Long channelId, Pageable pageable);

    List<MessageEntity> findByUser(UserEntity user);

}
