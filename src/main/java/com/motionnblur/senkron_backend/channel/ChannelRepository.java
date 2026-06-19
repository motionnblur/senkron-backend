package com.motionnblur.senkron_backend.channel;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.motionnblur.senkron_backend.user.UserEntity;

public interface ChannelRepository extends JpaRepository<ChannelEntity, Long> {

    List<ChannelEntity> findByType(ChannelType type);

    List<ChannelEntity> findByCreatedBy(UserEntity createdBy);

    List<ChannelEntity> findByCreatedById(Long createdById);

}
