package com.coparentassistant.service;

import org.springframework.stereotype.Service;

import com.coparentassistant.model.AiMessageAnalysis;
import com.coparentassistant.ai.AiAnalysisClient;

@Service 
public class MessageAnalysisService {

    private final AiAnalysisClient aiAnalysisClient;
    
    public MessageAnalysisService(AiAnalysisClient aiAnalysisClient) {
        this.aiAnalysisClient = aiAnalysisClient;
    }

    public AiMessageAnalysis analyzeMessage(String message) {
        return aiAnalysisClient.analyzeText(message);
    }

}
