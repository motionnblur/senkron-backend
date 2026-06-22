package com.motionnblur.senkron_backend.channel.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.motionnblur.senkron_backend.auth.domain.JwtUserPrincipal;
import com.motionnblur.senkron_backend.channel.domain.ChannelEntity;
import com.motionnblur.senkron_backend.channel.dto.request.AddChannelMemberRequest;
import com.motionnblur.senkron_backend.channel.dto.request.CreateChannelRequest;
import com.motionnblur.senkron_backend.channel.dto.response.ChannelResponse;
import com.motionnblur.senkron_backend.channel.service.ChannelService;

@RestController
@RequestMapping("/channels")
public class ChannelController {

    private final ChannelService channelService;

    public ChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    @PostMapping
    public ResponseEntity<ChannelResponse> createChannel(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody CreateChannelRequest request) {

        ChannelEntity channel = channelService.createChannel(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ChannelResponse.from(channel));
    }

    @PostMapping("/{channelId}/join")
    public ResponseEntity<Void> joinChannel(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long channelId) {
        channelService.joinChannel(principal.userId(), channelId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{channelId}/leave")
    public ResponseEntity<Void> leaveChannel(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long channelId) {
        channelService.leaveChannel(principal.userId(), channelId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{channelId}/add-member")
    public ResponseEntity<Void> addMember(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long channelId,
            @RequestBody AddChannelMemberRequest request) {
        channelService.addMember(principal.userId(), channelId, request.userId());
        return ResponseEntity.noContent().build();
    }

}
