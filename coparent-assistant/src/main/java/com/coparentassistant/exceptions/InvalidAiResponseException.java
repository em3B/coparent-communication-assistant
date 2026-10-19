package com.coparentassistant.exceptions;

import lombok.Getter;
import java.util.UUID;

@Getter
public class InvalidAiResponseException extends RuntimeException {

    private final UUID analysisId;

    public InvalidAiResponseException(String message, UUID analysisId) {
        super(message);
        this.analysisId = analysisId;
    }
}