package com.coparentassistant.service;

import org.springframework.stereotype.Service;

import com.coparentassistant.model.AiMessageAnalysis;
import com.coparentassistant.ai.AiAnalysisClient;

import reactor.core.publisher.Mono;

import com.coparentassistant.model.Metadata;

import lombok.extern.slf4j.Slf4j;

@Service 
@Slf4j 
public class MessageAnalysisService {

    private final AiAnalysisClient aiAnalysisClient;
    
    public MessageAnalysisService(AiAnalysisClient aiAnalysisClient) {
        this.aiAnalysisClient = aiAnalysisClient;
    }

    public Mono<AiMessageAnalysis> analyzeMessage(String message, Metadata metadata) {

    return aiAnalysisClient.analyzeText(message, metadata)
        .doOnNext(analysis -> log.atInfo()
            .addKeyValue("correlationId", metadata.getCorrelationId())
            .addKeyValue("analysisStatus", analysis.getStatus())
            .log("Message analysis completed successfully"));
    }

}
