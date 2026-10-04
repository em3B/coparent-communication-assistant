package com.coparentassistant.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coparentassistant.model.AiMessageAnalysis;
import com.coparentassistant.dto.MessageAnalysisRequest;
import com.coparentassistant.service.MessageAnalysisService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;

import reactor.core.publisher.Mono;

@RestController 
@RequestMapping("/api/message-analysis") 
public class MessageAnalysisController {
    
    private final MessageAnalysisService messageAnalysisService;

    public MessageAnalysisController(MessageAnalysisService messageAnalysisService) {
        this.messageAnalysisService = messageAnalysisService;
    }
    
    @PostMapping
    public ResponseEntity<Mono<AiMessageAnalysis>> postMessageAnalysis(@RequestBody MessageAnalysisRequest request) {
        Mono<AiMessageAnalysis> analysisMono = messageAnalysisService.analyzeMessage(request.getMessage());
        return ResponseEntity.ok(analysisMono);
    }
    

}
