package com.coparentassistant.ai;

import com.coparentassistant.model.AiMessageAnalysis;
import com.coparentassistant.ai.dto.OpenAiResponseRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Component 
public class OpenAiAnalysisClient implements AiAnalysisClient {

    private final WebClient webClient;
    private final String model;
    private final ObjectMapper objectMapper;

    public OpenAiAnalysisClient(@Value("${openai.api.key}") String apiKey,
                               @Value("${openai.model}") String model,
                               WebClient.Builder webClientBuilder,
                               ObjectMapper objectMapper) {
        this.model = model;
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder
            .baseUrl("https://api.openai.com") // Reverted to standard api.openai.com
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

    @Override 
    public Mono<AiMessageAnalysis> analyzeText(String text) {
        OpenAiResponseRequest request = new OpenAiResponseRequest(model, text, 3);
        return getAnalysisFromOpenAi(request);
    }

    private Mono<AiMessageAnalysis> getAnalysisFromOpenAi(OpenAiResponseRequest request) {
        return this.webClient.post()
            .uri("/v1/chat/completions") 
            .bodyValue(request)
            .retrieve()
            .bodyToMono(OpenAiRawResponse.class)
            // FIX: Explicitly pass <AiMessageAnalysis> target type type-witness hint to the map call
            .<AiMessageAnalysis>map(raw -> {
                String jsonTextContent = raw.choices().get(0).message().content();
                try {
                    return objectMapper.readValue(jsonTextContent, AiMessageAnalysis.class);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to map OpenAI text string response to AiMessageAnalysis model", e);
                }
            })
            .retryWhen(Retry.backoff(5, Duration.ofSeconds(3))
                .jitter(0.75)
                .filter(this::isRetryableException)
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> 
                    new RuntimeException("Failed to get analysis from OpenAI after retries", retrySignal.failure())));
    }

    private boolean isRetryableException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            HttpStatusCode status = ex.getStatusCode();
            return status.value() == 429 || status.is5xxServerError();
        }
        return throwable instanceof java.io.IOException;
    }

    // FIX: Made record explicitly public static to guarantee flawless compilation type visibility
    public static record OpenAiRawResponse(List<Choice> choices) {
        public static record Choice(Message message) {}
        public static record Message(String content) {}
    }
}
