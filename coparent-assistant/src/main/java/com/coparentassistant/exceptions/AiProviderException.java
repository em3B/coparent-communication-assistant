package com.coparentassistant.exceptions;

import lombok.Getter;
import com.coparentassistant.model.Metadata;

@Getter
public class AiProviderException extends RuntimeException {

    private final Metadata metadata;

    public AiProviderException(String message, Metadata metadata, Throwable cause) {
        super(message, cause);
        this.metadata = metadata;
    }
}