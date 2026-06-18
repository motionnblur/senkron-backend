package com.motionnblur.senkron_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.motionnblur.senkron_backend.entity.ChannelEntity;
import com.motionnblur.senkron_backend.entity.UserEntity;
import com.motionnblur.senkron_backend.enums.ChannelType;

public interface ChannelRepository extends JpaRepository<ChannelEntity, Long> {

    List<ChannelEntity> findByType(ChannelType type);

    List<ChannelEntity> findByCreatedBy(UserEntity createdBy);

    List<ChannelEntity> findByCreatedById(Long createdById);

}
