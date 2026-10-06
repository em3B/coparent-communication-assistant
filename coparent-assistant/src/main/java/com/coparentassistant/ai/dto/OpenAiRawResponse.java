package com.coparentassistant.ai.dto;

import java.util.List;

public record OpenAiRawResponse(List<Output> output) {
    public record Output(List<Content> content) {};
    public record Content(String type, String text) {};
}

