package com.coparentassistant.ai.dto;

import java.util.Map;

public record OpenAiResponseRequest(String model, String instructions, String input, Text text) {
    public record Text(Format format) {}

    public record Format(String type, String name, Map<String, Object> schema, boolean strict) {}
}
