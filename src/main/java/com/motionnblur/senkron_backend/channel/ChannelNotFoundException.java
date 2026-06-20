package com.motionnblur.senkron_backend.channel;

public class ChannelNotFoundException extends RuntimeException {

    public ChannelNotFoundException(Long channelId) {
        super("Channel not found: " + channelId);
    }

}
