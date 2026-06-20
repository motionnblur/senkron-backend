package com.motionnblur.senkron_backend.channel;

public record CreateChannelRequest(
        String name,
        String description,
        ChannelType type
) {
}
