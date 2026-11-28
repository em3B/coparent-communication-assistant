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
import lombok.extern.slf4j.Slf4j;

import com.coparentassistant.model.Metadata;

@RestController 
@RequestMapping("/api/message_analysis") 
@Slf4j
public class MessageAnalysisController {
    
    private final MessageAnalysisService messageAnalysisService;

    public MessageAnalysisController(MessageAnalysisService messageAnalysisService) {
        this.messageAnalysisService = messageAnalysisService;
    }
    
    @PostMapping
    public Mono<AiMessageAnalysis> postMessageAnalysis(@RequestBody @Valid  MessageAnalysisRequest request) {

        Metadata metadata = new Metadata(UUID.randomUUID(), Instant.now());

        log.atInfo()
            .addKeyValue("correlationId", metadata.getCorrelationId())
            .log("Message analysis request received");
            
        return messageAnalysisService.analyzeMessage(request.getMessage(), metadata);
    }
    

}
