package com.coparentassistant.ai;

import com.coparentassistant.model.AiMessageAnalysis;

public interface AiAnalysisClient {
    public AiMessageAnalysis analyzeText(String text);
}
