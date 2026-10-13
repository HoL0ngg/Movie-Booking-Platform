package com.cinema.ai.controller;

import jakarta.validation.Valid;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cinema.ai.dto.ChatRequest;
import com.cinema.ai.dto.ChatResponse;
import com.cinema.ai.service.ChatService;

@RestController
@RequestMapping("/api/v1/ai")
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "google-genai")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(value = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return chatService.chat(request);
    }
}
