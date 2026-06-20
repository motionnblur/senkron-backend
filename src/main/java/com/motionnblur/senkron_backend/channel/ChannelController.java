package com.motionnblur.senkron_backend.channel;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.motionnblur.senkron_backend.auth.JwtUserPrincipal;

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

}
