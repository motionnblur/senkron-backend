package com.motionnblur.senkron_backend.channel;

public class ChannelMemberNotFoundException extends RuntimeException {

    public ChannelMemberNotFoundException(Long userId, Long channelId) {
        super("Channel member not found for user " + userId + " in channel " + channelId);
    }

}
