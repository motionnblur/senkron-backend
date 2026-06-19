package com.motionnblur.senkron_backend.channel;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

import com.motionnblur.senkron_backend.user.UserEntity;

@Entity
@Data
@Table(
    name = "channel_members", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "channel_id"})
)
public class ChannelMemberEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private ChannelEntity channel;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;
    
}
