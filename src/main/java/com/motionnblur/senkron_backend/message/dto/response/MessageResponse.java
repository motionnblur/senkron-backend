package com.motionnblur.senkron_backend.message.dto.response;

import java.time.LocalDateTime;

import com.motionnblur.senkron_backend.message.domain.MessageEntity;

public record MessageResponse(
        Long id,
        String content,
        Long channelId,
        Long userId,
        String authorDisplayName,
        LocalDateTime createdAt
) {

    public static MessageResponse from(MessageEntity entity) {
        return new MessageResponse(
                entity.getId(),
                entity.getContent(),
                entity.getChannel().getId(),
                entity.getUser().getId(),
                entity.getUser().getDisplayName(),
                entity.getCreatedAt()
        );
    }

}
