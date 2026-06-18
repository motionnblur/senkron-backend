package com.motionnblur.senkron_backend.repository;

import java.time.LocalDateTime;

import com.motionnblur.senkron_backend.entity.ChannelEntity;
import com.motionnblur.senkron_backend.entity.ChannelMemberEntity;
import com.motionnblur.senkron_backend.entity.MessageEntity;
import com.motionnblur.senkron_backend.entity.UserEntity;
import com.motionnblur.senkron_backend.enums.ChannelType;

final class RepositoryTestFixtures {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 1, 15, 10, 0);

    private RepositoryTestFixtures() {
    }

    static UserEntity user(String email) {
        return user("Ada", "Lovelace", "ada", email, "password");
    }

    static UserEntity user(String name, String lastName, String displayName, String email, String password) {
        UserEntity user = new UserEntity();
        user.setName(name);
        user.setLastName(lastName);
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setPassword(password);
        user.setCreatedAt(BASE_TIME);
        return user;
    }

    static UserEntity oauthUser(String googleId, String email) {
        UserEntity user = new UserEntity();
        user.setGoogleId(googleId);
        user.setName("Ada");
        user.setLastName("Lovelace");
        user.setDisplayName("Ada Lovelace");
        user.setEmail(email);
        user.setPassword(null);
        user.setCreatedAt(BASE_TIME);
        return user;
    }

    static ChannelEntity channel(UserEntity createdBy, ChannelType type, String name) {
        ChannelEntity channel = new ChannelEntity();
        channel.setName(name);
        channel.setDescription(name + " description");
        channel.setType(type);
        channel.setCreatedBy(createdBy);
        channel.setCreatedAt(BASE_TIME);
        return channel;
    }

    static ChannelMemberEntity membership(UserEntity user, ChannelEntity channel) {
        return membership(user, channel, BASE_TIME);
    }

    static ChannelMemberEntity membership(UserEntity user, ChannelEntity channel, LocalDateTime joinedAt) {
        ChannelMemberEntity membership = new ChannelMemberEntity();
        membership.setUser(user);
        membership.setChannel(channel);
        membership.setJoinedAt(joinedAt);
        return membership;
    }

    static MessageEntity message(UserEntity user, ChannelEntity channel, String content, LocalDateTime createdAt) {
        MessageEntity message = new MessageEntity();
        message.setUser(user);
        message.setChannel(channel);
        message.setContent(content);
        message.setCreatedAt(createdAt);
        return message;
    }

    static LocalDateTime baseTime() {
        return BASE_TIME;
    }

    static LocalDateTime timeAfterMinutes(int minutes) {
        return BASE_TIME.plusMinutes(minutes);
    }

}
