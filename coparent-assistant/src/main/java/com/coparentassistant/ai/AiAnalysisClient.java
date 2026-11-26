package com.coparentassistant.ai;

import com.coparentassistant.model.AiMessageAnalysis;

import reactor.core.publisher.Mono;

import com.coparentassistant.model.Metadata;

public interface AiAnalysisClient {
    public Mono<AiMessageAnalysis> analyzeText(String text, Metadata metadata);
}
