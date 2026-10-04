package com.coparentassistant.ai.dto;

import java.util.List;

public record OpenAiRawResponse(List<Choice> choices) {
    public record Choice(Message message) {}
    public record Message(String content) {}
}

