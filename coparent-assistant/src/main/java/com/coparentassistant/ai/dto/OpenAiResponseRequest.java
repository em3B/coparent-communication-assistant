package com.coparentassistant.ai.dto;

import java.util.List;
import java.util.Map;

public record OpenAiResponseRequest(
    String model,
    List<Message> messages,
    Map<String, String> response_format,
    int n // Instructs OpenAI how many alternative options to generate
) {
    public OpenAiResponseRequest(String model, String text, int alternativesCount) {
        this(
            model, 
            List.of(
                new Message("system", "You are an AI assistant. Analyze the text for co-parenting tone. You must respond with a JSON object containing a 'status' field and a 'corrections' array. Do not use any other key names."),
                new Message("user", text)
            ),
            Map.of("type", "json_object"), alternativesCount
        );
    }

    public record Message(String role, String content) {}
}
