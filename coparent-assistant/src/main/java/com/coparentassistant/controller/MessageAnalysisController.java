package com.coparentassistant.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coparentassistant.model.AiMessageAnalysis;
import com.coparentassistant.dto.MessageAnalysisRequest;
import com.coparentassistant.service.MessageAnalysisService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.UUID;

import com.coparentassistant.model.Metadata;

@RestController 
@RequestMapping("/api/message_analysis") 
public class MessageAnalysisController {
    
    private final MessageAnalysisService messageAnalysisService;

    public MessageAnalysisController(MessageAnalysisService messageAnalysisService) {
        this.messageAnalysisService = messageAnalysisService;
    }
    
    @PostMapping
    public Mono<AiMessageAnalysis> postMessageAnalysis(@RequestBody @Valid  MessageAnalysisRequest request) {

        Metadata metadata = new Metadata(UUID.randomUUID(), Instant.now());
        return messageAnalysisService.analyzeMessage(request.getMessage(), metadata);
    }
    

}
