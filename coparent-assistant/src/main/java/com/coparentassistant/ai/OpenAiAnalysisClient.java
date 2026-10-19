package com.coparentassistant.ai;

import com.coparentassistant.model.AiMessageAnalysis;
import com.coparentassistant.model.Criteria;
import com.coparentassistant.model.MessageStatus;
import com.coparentassistant.ai.dto.AiAnalysisResponse;
import com.coparentassistant.ai.dto.OpenAiRawResponse;
import com.coparentassistant.ai.dto.OpenAiResponseRequest;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.coparentassistant.model.SingleMessageCriteria;
import com.coparentassistant.model.TextCorrection;
import com.coparentassistant.ai.dto.OpenAiResponseRequest.Text;
import com.coparentassistant.exceptions.InvalidAiResponseException;

@Component 
public class OpenAiAnalysisClient implements AiAnalysisClient {

    private final WebClient webClient;
    private final String model;
    private final ObjectMapper objectMapper;

    private static final String ANALYSIS_INSTRUCTIONS = """
    Analyse the supplied co-parenting message against the following criteria:

    - ACCUSATION
    - THREAT_OR_COERCION
    - IRRELEVANT_OR_UNNECESSARY_CONTENT
    - UNNECESSARY_INTERROGATION
    - DEGRADING_OR_BELITTLING_LANGUAGE
    - OBSCENE_LANGUAGE
    - INTIMIDATION
    - PUNITIVE_OR_RETALIATORY_LANGUAGE

    A clear, firm, or practical boundary does not require correction unless it
    violates one or more of these criteria. A neutral practical consequence is
    not automatically a threat or coercion.

    Do not invent missing context or make assumptions about previous
    communication between the parties.

    If a portion of the message violates one or more criteria, identify only
    the problematic portion rather than unnecessarily rewriting the entire
    message.

    For each problematic portion, provide three alternative corrections.
    Each correction should remove the problematic language while preserving
    the sender's intended practical meaning.

    Apply only criteria that are clearly supported by the supplied text.
    """;

    public OpenAiAnalysisClient(@Value("${openai.api.key}") String apiKey,
                            @Value("${openai.model}") String model,
                            @Value("${openai.base-url:https://api.openai.com}") String baseUrl) {
        this.model = model;
        this.objectMapper = new ObjectMapper();
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl) 
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

@Override
public Mono<AiMessageAnalysis> analyzeText(String text) {
    Map<String, Object> schema = Map.of(
        "type", "object",
        "required", List.of("status", "corrections"),
        "additionalProperties", false,
        "properties", Map.of(
            "status", Map.of(
                "type", "string",
                "enum", List.of(
                    MessageStatus.READY.name(),
                    MessageStatus.REVIEW.name(),
                    MessageStatus.MORE_CONTEXT_NEEDED.name()
                )
            ),
            "corrections", Map.of(
                "type", "array",
                "items", Map.of(
                    "type", "object",
                    "additionalProperties", false,
                    "required", List.of(
                        "originalText",
                        "suggestedCorrections",
                        "explanation",
                        "criteria"
                    ),
                    "properties", Map.of(
                        "originalText", Map.of("type", "string"),
                        "suggestedCorrections", Map.of(
                            "type", "array",
                            "items", Map.of("type", "string")
                        ),
                        "explanation", Map.of("type", "string"),
                        "criteria", Map.of(
                            "type", "array",
                            "items", Map.of(
                                "type", "string",
                                "enum", List.of(
                                    SingleMessageCriteria.ACCUSATION.name(),
                                    SingleMessageCriteria.THREAT_OR_COERCION.name(),
                                    SingleMessageCriteria.IRRELEVANT_OR_UNNECESSARY_CONTENT.name(),
                                    SingleMessageCriteria.UNNECESSARY_INTERROGATION.name(),
                                    SingleMessageCriteria.DEGRADING_OR_BELITTLING_LANGUAGE.name(),
                                    SingleMessageCriteria.OBSCENE_LANGUAGE.name(),
                                    SingleMessageCriteria.INTIMIDATION.name(),
                                    SingleMessageCriteria.PUNITIVE_OR_RETALIATORY_LANGUAGE.name()
                                )
                            )
                        )
                    )
                )
            )
        )
    );

    Text inputText = new OpenAiResponseRequest.Text(
        new OpenAiResponseRequest.Format(
            "json_schema", "message_analysis", schema, true
        )
    );

    OpenAiResponseRequest request = new OpenAiResponseRequest(
        model, ANALYSIS_INSTRUCTIONS, text, inputText
    );

    UUID analysisId = UUID.randomUUID();

    return getAnalysisFromOpenAi(request)
        .map(response -> {
            validateResponse(response, analysisId);

            return AiMessageAnalysis.builder()
                .status(response.status())
                .corrections(response.corrections().stream()
                    .map(correction -> TextCorrection.builder()
                        .criteria(
                            correction.criteria().stream()
                                .map(criteria -> (Criteria) criteria)
                                .toList()
                        )
                        .explanation(correction.explanation())
                        .suggestedCorrections(correction.suggestedCorrections())
                        .originalText(correction.originalText())
                        .build())
                    .toList())
                .build();
        });
}

    private Mono<AiAnalysisResponse> getAnalysisFromOpenAi(OpenAiResponseRequest request) {
        return this.webClient.post()
            .uri("/v1/responses")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(OpenAiRawResponse.class)
            .<AiAnalysisResponse>map(raw -> {
                String jsonTextContent = raw.output().get(0).content().get(0).text();
                try {
                    return objectMapper.readValue(jsonTextContent, AiAnalysisResponse.class);
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

    private void validateResponse(AiAnalysisResponse response, UUID analysisId) {

        if (response == null) {
            throw new InvalidAiResponseException(
                "AI response cannot be null",
                analysisId
            );
        }

        if (response.status() == null) {
            throw new InvalidAiResponseException(
                "AI response is missing status",
                analysisId
            );
        }

        if (response.corrections() == null) {
            throw new InvalidAiResponseException(
                "AI response is missing corrections",
                analysisId
            );
        }

        if ((response.status() == MessageStatus.READY
                || response.status() == MessageStatus.MORE_CONTEXT_NEEDED)
                && !response.corrections().isEmpty()) {
            throw new InvalidAiResponseException(
                "AI response must not contain corrections when status is READY or MORE_CONTEXT_NEEDED",
                analysisId
            );
        }

        if (response.status() == MessageStatus.REVIEW
                && response.corrections().isEmpty()) {
            throw new InvalidAiResponseException(
                "AI analysis requiring review must include corrections",
                analysisId
            );
        }

        if (response.corrections().stream().anyMatch(Objects::isNull)) {
            throw new InvalidAiResponseException(
                "Corrections cannot contain null entries",
                analysisId
            );
        }

        if (response.corrections().stream()
                .anyMatch(correction -> correction.suggestedCorrections() == null)) {
            throw new InvalidAiResponseException(
                "Suggestions cannot be null for a correction entry",
                analysisId
            );
        }

        if (response.corrections().stream()
                .anyMatch(correction -> correction.suggestedCorrections().size() != 3)) {
            throw new InvalidAiResponseException(
                "Each correction must contain exactly three suggestions",
                analysisId
            );
        }

        if (response.corrections().stream()
                .flatMap(correction -> correction.suggestedCorrections().stream())
                .anyMatch(suggestion -> suggestion == null || suggestion.isBlank())) {
            throw new InvalidAiResponseException(
                "Suggested corrections cannot be null or blank",
                analysisId
            );
        }

        if (response.corrections().stream()
                .anyMatch(correction -> correction.originalText() == null
                        || correction.originalText().isBlank())) {
            throw new InvalidAiResponseException(
                "Original text cannot be null or blank",
                analysisId
            );
        }

        if (response.corrections().stream()
                .anyMatch(correction -> correction.explanation() == null
                        || correction.explanation().isBlank())) {
            throw new InvalidAiResponseException(
                "Correction explanation cannot be null or blank",
                analysisId
            );
        }

        if (response.corrections().stream()
                .anyMatch(correction -> correction.criteria() == null
                        || correction.criteria().isEmpty()
                        || correction.criteria().stream().anyMatch(Objects::isNull))) {
            throw new InvalidAiResponseException(
                "Each correction must contain at least one valid criterion",
                analysisId
            );
        }
    }

}
