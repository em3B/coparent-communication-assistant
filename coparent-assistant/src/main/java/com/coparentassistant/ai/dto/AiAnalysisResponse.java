package com.coparentassistant.ai.dto;

import com.coparentassistant.model.MessageStatus;
import java.util.List;

public record AiAnalysisResponse(MessageStatus status, List<AiCorrectionResponse> corrections) {
    
}
