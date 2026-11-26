package com.coparentassistant.exceptions;

import lombok.Getter;
import com.coparentassistant.model.Metadata;

@Getter
public class InvalidAiResponseException extends RuntimeException {

    private final Metadata metadata;

    public InvalidAiResponseException(String message, Metadata metadata) {
        super(message);
        this.metadata = metadata;
    }

    public InvalidAiResponseException(String message, Metadata metadata, Throwable cause) {
        super(message, cause);
        this.metadata = metadata;
    }
}