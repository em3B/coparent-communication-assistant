package com.coparentassistant.ai.dto;

import com.coparentassistant.model.SingleMessageCriteria;

import java.util.List;

public record AiCorrectionResponse(String originalText, List<String> suggestedCorrections, String explanation, List<SingleMessageCriteria> criteria) {
    
}
