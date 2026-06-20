package com.motionnblur.senkron_backend.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.motionnblur.senkron_backend.channel.ChannelMemberNotFoundException;
import com.motionnblur.senkron_backend.channel.ChannelNotFoundException;
import com.motionnblur.senkron_backend.user.UserNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            UserNotFoundException.class,
            ChannelNotFoundException.class,
            ChannelMemberNotFoundException.class
    })
    public ResponseEntity<ProblemDetail> handleNotFound(RuntimeException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleBadRequest(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleForbidden(IllegalStateException ex) {
        return problem(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        return ResponseEntity.status(status).body(body);
    }

}
