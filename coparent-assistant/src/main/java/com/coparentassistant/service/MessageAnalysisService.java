package com.coparentassistant.service;

import org.springframework.stereotype.Service;

import com.coparentassistant.model.AiMessageAnalysis;
import com.coparentassistant.model.MessageStatus;

@Service 
public class MessageAnalysisService {
    
    public AiMessageAnalysis analyzeMessage(String message) {
        AiMessageAnalysis analysis = new AiMessageAnalysis();
        analysis.setStatus(MessageStatus.READY);
//    leverage LLM here to analyze the message and populate the analysis object and then set finding to entity 
        return analysis;
    }
}
