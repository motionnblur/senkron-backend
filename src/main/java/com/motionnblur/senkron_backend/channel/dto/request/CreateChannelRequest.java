package com.motionnblur.senkron_backend.channel.dto.request;

import com.motionnblur.senkron_backend.channel.domain.ChannelType;

public record CreateChannelRequest(
        String name,
        String description,
        ChannelType type
) {
}
