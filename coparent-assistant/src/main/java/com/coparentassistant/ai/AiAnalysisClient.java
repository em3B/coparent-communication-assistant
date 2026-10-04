package com.coparentassistant.ai;

import com.coparentassistant.model.AiMessageAnalysis;

import reactor.core.publisher.Mono;

public interface AiAnalysisClient {
    public Mono<AiMessageAnalysis> analyzeText(String text);
}
