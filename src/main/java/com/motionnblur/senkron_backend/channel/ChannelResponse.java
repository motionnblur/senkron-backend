package com.motionnblur.senkron_backend.channel;

import java.time.LocalDateTime;

public record ChannelResponse(
        Long id,
        String name,
        String description,
        ChannelType type,
        Long createdById,
        LocalDateTime createdAt
) {

    public static ChannelResponse from(ChannelEntity entity) {
        return new ChannelResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getType(),
                entity.getCreatedBy().getId(),
                entity.getCreatedAt()
        );
    }

}
