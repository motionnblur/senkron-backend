package com.motionnblur.senkron_backend.message.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.motionnblur.senkron_backend.auth.domain.JwtUserPrincipal;
import com.motionnblur.senkron_backend.message.dto.request.SendMessageRequest;
import com.motionnblur.senkron_backend.message.dto.response.MessageResponse;
import com.motionnblur.senkron_backend.message.service.MessageService;

@RestController
@RequestMapping("/channels/{channelId}/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long channelId,
            @RequestBody SendMessageRequest request) {

        MessageResponse response = messageService.sendMessage(principal.userId(), channelId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
