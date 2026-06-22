package com.motionnblur.senkron_backend.message.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping
    public Page<MessageResponse> listMessages(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable Long channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return messageService.listMessages(principal.userId(), channelId, pageable);
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
