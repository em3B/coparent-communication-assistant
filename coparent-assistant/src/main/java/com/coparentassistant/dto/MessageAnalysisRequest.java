package com.coparentassistant.dto;

import lombok.Getter;

@Getter 
public class MessageAnalysisRequest {
    private final String message;

    public MessageAnalysisRequest(String message) {
        this.message = message;
    }
}