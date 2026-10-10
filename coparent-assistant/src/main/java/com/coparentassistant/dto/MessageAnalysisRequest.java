package com.coparentassistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter 
public class MessageAnalysisRequest {
    @NotBlank 
    @Size(max = 500) 
    private final String message;

    public MessageAnalysisRequest(String message) {
        this.message = message;
    }
}