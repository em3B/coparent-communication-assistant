package com.coparentassistant.ai;

import com.coparentassistant.model.AiMessageAnalysis;
import com.coparentassistant.model.Metadata;
import com.coparentassistant.model.MessageStatus;
import com.coparentassistant.exceptions.AiProviderException;
import com.coparentassistant.exceptions.InvalidAiResponseException;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.Duration;
import java.util.UUID;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OpenAiAnalysisClientTest {

    private static WireMockServer wireMockServer;

    private Metadata metadata;

    @Autowired
    private OpenAiAnalysisClient openAiAnalysisClient;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void resetStubs() {
        wireMockServer.resetAll();
        metadata = new Metadata(UUID.randomUUID(), Instant.now());
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("openai.api.key", () -> "test-key");
        registry.add("openai.model", () -> "gpt-4o");
        registry.add("openai.base-url", () -> wireMockServer.baseUrl());
    }

    @Test
    void analyzeText_ReadyMessage_WithComplexJsonPayload() {

        String message = "Please send Tom's school letter by 5pm.";

        stubAiResponse("""
            {
                "status": "READY",
                "corrections": []
            }
            """);

        Mono<AiMessageAnalysis> result =
            openAiAnalysisClient.analyzeText(message, metadata);

        StepVerifier.create(result)
            .assertNext(analysis -> {
                assertEquals(MessageStatus.READY, analysis.getStatus());
                assertTrue(analysis.getCorrections().isEmpty());
            })
            .verifyComplete();
    }

    @Test
    void analyzeText_ReviewMessage_WithComplexJsonPayload() {

        String message =
            "Why didn't you send me the school form? "
                + "You always keep me out of the loop on purpose!";

        stubAiResponse("""
            {
                "status": "REVIEW",
                "corrections": [
                    {
                        "originalText": "You always keep me out of the loop on purpose!",
                        "suggestedCorrections": [
                            "Please send me the school form.",
                            "I have not yet received the school form.",
                            "Please send me the school form at your earliest convenience."
                        ],
                        "explanation": "This assumes deliberate exclusion and assigns negative intent.",
                        "criteria": ["ACCUSATION"]
                    }
                ]
            }
            """);

        Mono<AiMessageAnalysis> result =
            openAiAnalysisClient.analyzeText(message, metadata);

        StepVerifier.create(result)
            .assertNext(analysis -> {

                assertEquals(MessageStatus.REVIEW, analysis.getStatus());
                assertEquals(1, analysis.getCorrections().size());

                var correction = analysis.getCorrections().get(0);

                assertEquals(
                    "You always keep me out of the loop on purpose!",
                    correction.getOriginalText()
                );

                assertEquals(
                    3,
                    correction.getSuggestedCorrections().size()
                );

                assertEquals(
                    "Please send me the school form.",
                    correction.getSuggestedCorrections().get(0)
                );

                assertEquals(
                    "This assumes deliberate exclusion and assigns negative intent.",
                    correction.getExplanation()
                );
            })
            .verifyComplete();
    }

    @Test
    void analyzeText_MissingStatus_ThrowsInvalidAiResponseException() {

        assertInvalidAiResponse(
            """
            {
                "corrections": []
            }
            """,
            "AI response is missing status"
        );
    }

    @Test
    void analyzeText_ReviewStatusWithNoCorrections_ThrowsInvalidAiResponseException() {

        assertInvalidAiResponse(
            """
            {
                "status": "REVIEW",
                "corrections": []
            }
            """,
            "AI analysis requiring review must include corrections"
        );
    }

    @Test
    void analyzeText_ReadyStatusWithCorrections_ThrowsInvalidAiResponseException() {

        assertInvalidAiResponse(
            """
            {
                "status": "READY",
                "corrections": [
                    {
                        "originalText": "You never listen to me!",
                        "suggestedCorrections": [
                            "Please consider my perspective.",
                            "I would appreciate your response.",
                            "Could we discuss this?"
                        ],
                        "explanation": "Accusatory wording.",
                        "criteria": ["ACCUSATION"]
                    }
                ]
            }
            """,
            "AI response must not contain corrections when status is READY or MORE_CONTEXT_NEEDED"
        );
    }

    @Test
    void analyzeText_MoreContextNeededWithNoCorrections_ReturnsSuccessfulAnalysis() {

        String message = "Can you confirm what we agreed yesterday?";

        stubAiResponse("""
            {
                "status": "MORE_CONTEXT_NEEDED",
                "corrections": []
            }
            """);

        StepVerifier.create(
                openAiAnalysisClient.analyzeText(message, metadata)
            )
            .assertNext(analysis -> {
                assertEquals(
                    MessageStatus.MORE_CONTEXT_NEEDED,
                    analysis.getStatus()
                );

                assertTrue(analysis.getCorrections().isEmpty());
            })
            .verifyComplete();
    }

    @Test
    void analyzeText_MoreContextNeededWithCorrections_ThrowsInvalidAiResponseException() {

        assertInvalidAiResponse(
            """
            {
                "status": "MORE_CONTEXT_NEEDED",
                "corrections": [
                    {
                        "originalText": "You know what happened yesterday!",
                        "suggestedCorrections": [
                            "Could we clarify what happened yesterday?",
                            "I'd like to discuss yesterday's events.",
                            "Can we review what happened yesterday?"
                        ],
                        "explanation": "The message assumes shared context.",
                        "criteria": ["ACCUSATION"]
                    }
                ]
            }
            """,
            "AI response must not contain corrections when status is READY or MORE_CONTEXT_NEEDED"
        );
    }

    @Test
    void analyzeText_NullCorrections_ThrowsInvalidAiResponseException() {

        assertInvalidAiResponse(
            """
            {
                "status": "REVIEW",
                "corrections": null
            }
            """,
            "AI response is missing corrections"
        );
    }

    @Test
    void analyzeText_MissingCorrections_ThrowsInvalidAiResponseException() {

        assertInvalidAiResponse(
            """
            {
                "status": "REVIEW"
            }
            """,
            "AI response is missing corrections"
        );
    }

    @Test
    void analyzeText_NullCorrectionEntry_ThrowsInvalidAiResponseException() {

        assertInvalidAiResponse(
            """
            {
                "status": "REVIEW",
                "corrections": [null]
            }
            """,
            "Corrections cannot contain null entries"
        );
    }

    @Test
    void analyzeText_NullSuggestedCorrections_ThrowsInvalidAiResponseException() {

        assertInvalidAiResponse(
            """
            {
                "status": "REVIEW",
                "corrections": [
                    {
                        "originalText": "You never listen!",
                        "suggestedCorrections": null,
                        "explanation": "Accusatory language.",
                        "criteria": ["ACCUSATION"]
                    }
                ]
            }
            """,
            "Suggestions cannot be null for a correction entry"
        );
    }

    @Test
    void analyzeText_IncorrectSuggestionCount_ThrowsInvalidAiResponseException() {

        assertInvalidAiResponse(
            """
            {
                "status": "REVIEW",
                "corrections": [
                    {
                        "originalText": "You never listen!",
                        "suggestedCorrections": [
                            "Please consider my perspective.",
                            "I would appreciate your response."
                        ],
                        "explanation": "Accusatory language.",
                        "criteria": ["ACCUSATION"]
                    }
                ]
            }
            """,
            "Each correction must contain exactly three suggestions"
        );
    }

    @Test
    void analyzeText_BlankSuggestion_ThrowsInvalidAiResponseException() {

        assertInvalidAiResponse(
            """
            {
                "status": "REVIEW",
                "corrections": [
                    {
                        "originalText": "You never listen!",
                        "suggestedCorrections": [
                            "Please consider my perspective.",
                            "   ",
                            "Could we discuss this?"
                        ],
                        "explanation": "Accusatory language.",
                        "criteria": ["ACCUSATION"]
                    }
                ]
            }
            """,
            "Suggested corrections cannot be null or blank"
        );
    }

    @Test
    void analyzeText_NullSuggestionEntry_ThrowsInvalidAiResponseException() {

        assertInvalidAiResponse(
            """
            {
                "status": "REVIEW",
                "corrections": [
                    {
                        "originalText": "You never listen!",
                        "suggestedCorrections": [
                            "Please consider my perspective.",
                            null,
                            "Could we discuss this?"
                        ],
                        "explanation": "Accusatory language.",
                        "criteria": ["ACCUSATION"]
                    }
                ]
            }
            """,
            "Suggested corrections cannot be null or blank"
        );
    }

    @Test
    void analyzeText_BlankOriginalText_ThrowsInvalidAiResponseException() {

        assertInvalidAiResponse(
            """
            {
                "status": "REVIEW",
                "corrections": [
                    {
                        "originalText": "   ",
                        "suggestedCorrections": [
                            "Please consider my perspective.",
                            "I would appreciate your response.",
                            "Could we discuss this?"
                        ],
                        "explanation": "Accusatory language.",
                        "criteria": ["ACCUSATION"]
                    }
                ]
            }
            """,
            "Original text cannot be null or blank"
        );
    }

    @Test
    void analyzeText_NullOriginalText_ThrowsInvalidAiResponseException() {

        assertInvalidAiResponse(
            """
            {
                "status": "REVIEW",
                "corrections": [
                    {
                        "originalText": null,
                        "suggestedCorrections": [
                            "Please consider my perspective.",
                            "I would appreciate your response.",
                            "Could we discuss this?"
                        ],
                        "explanation": "Accusatory language.",
                        "criteria": ["ACCUSATION"]
                    }
                ]
            }
            """,
            "Original text cannot be null or blank"
        );
    }

    @Test
    void analyzeText_BlankExplanation_ThrowsInvalidAiResponseException() {

        assertInvalidAiResponse(
            """
            {
                "status": "REVIEW",
                "corrections": [
                    {
                        "originalText": "You never listen!",
                        "suggestedCorrections": [
                            "Please consider my perspective.",
                            "I would appreciate your response.",
                            "Could we discuss this?"
                        ],
                        "explanation": "   ",
                        "criteria": ["ACCUSATION"]
                    }
                ]
            }
            """,
            "Correction explanation cannot be null or blank"
        );
    }

    @Test
    void analyzeText_NullExplanation_ThrowsInvalidAiResponseException() {

        assertInvalidAiResponse(
            """
            {
                "status": "REVIEW",
                "corrections": [
                    {
                        "originalText": "You never listen!",
                        "suggestedCorrections": [
                            "Please consider my perspective.",
                            "I would appreciate your response.",
                            "Could we discuss this?"
                        ],
                        "explanation": null,
                        "criteria": ["ACCUSATION"]
                    }
                ]
            }
            """,
            "Correction explanation cannot be null or blank"
        );
    }

    @Test
    void analyzeText_EmptyCriteria_ThrowsInvalidAiResponseException() {

        assertInvalidAiResponse(
            """
            {
                "status": "REVIEW",
                "corrections": [
                    {
                        "originalText": "You never listen!",
                        "suggestedCorrections": [
                            "Please consider my perspective.",
                            "I would appreciate your response.",
                            "Could we discuss this?"
                        ],
                        "explanation": "Accusatory language.",
                        "criteria": []
                    }
                ]
            }
            """,
            "Each correction must contain at least one valid criterion"
        );
    }

    @Test
    void analyzeText_NullCriteria_ThrowsInvalidAiResponseException() {

        assertInvalidAiResponse(
            """
            {
                "status": "REVIEW",
                "corrections": [
                    {
                        "originalText": "You never listen!",
                        "suggestedCorrections": [
                            "Please consider my perspective.",
                            "I would appreciate your response.",
                            "Could we discuss this?"
                        ],
                        "explanation": "Accusatory language.",
                        "criteria": null
                    }
                ]
            }
            """,
            "Each correction must contain at least one valid criterion"
        );
    }

    @Test
    void analyzeText_NullCriterionEntry_ThrowsInvalidAiResponseException() {

        assertInvalidAiResponse(
            """
            {
                "status": "REVIEW",
                "corrections": [
                    {
                        "originalText": "You never listen!",
                        "suggestedCorrections": [
                            "Please consider my perspective.",
                            "I would appreciate your response.",
                            "Could we discuss this?"
                        ],
                        "explanation": "Accusatory language.",
                        "criteria": [null]
                    }
                ]
            }
            """,
            "Each correction must contain at least one valid criterion"
        );
    }

    @Test
    void analyzeText_EmptyResponse_ThrowsInvalidAiResponseException() {

        stubFor(post(urlEqualTo("/v1/responses"))
            .willReturn(aResponse().withStatus(200)));

        StepVerifier.create(
                openAiAnalysisClient.analyzeText("Test message", metadata)
            )
            .expectErrorSatisfies(error -> {

                var exception = assertInstanceOf(
                    InvalidAiResponseException.class,
                    error
                );

                assertEquals(
                    "OpenAI returned an empty response",
                    exception.getMessage()
                );

                assertSame(metadata, exception.getMetadata());
            })
            .verify(Duration.ofSeconds(10));

        wireMockServer.verify(
            1,
            postRequestedFor(urlEqualTo("/v1/responses"))
        );
    }

    @Test
    void analyzeText_MalformedAnalysisJson_ThrowsInvalidAiResponseException() {

        stubAiResponse("not valid JSON");

        StepVerifier.create(
                openAiAnalysisClient.analyzeText("Test message", metadata)
            )
            .expectErrorSatisfies(error -> {

                var exception = assertInstanceOf(
                    InvalidAiResponseException.class,
                    error
                );

                assertSame(metadata, exception.getMetadata());
                assertNotNull(exception.getCause());
            })
            .verify(Duration.ofSeconds(10));

        wireMockServer.verify(
            1,
            postRequestedFor(urlEqualTo("/v1/responses"))
        );
    }

    @Test
    void analyzeText_EmptyOutput_ThrowsInvalidAiResponseException() {

        stubFor(post(urlEqualTo("/v1/responses"))
            .willReturn(okJson("{\"output\":[]}")));

        StepVerifier.create(
                openAiAnalysisClient.analyzeText("Test message", metadata)
            )
            .expectErrorSatisfies(error -> {

                var exception = assertInstanceOf(
                    InvalidAiResponseException.class,
                    error
                );

                assertSame(metadata, exception.getMetadata());
                assertNotNull(exception.getCause());
            })
            .verify(Duration.ofSeconds(10));

        wireMockServer.verify(
            1,
            postRequestedFor(urlEqualTo("/v1/responses"))
        );
    }

    @Test
    void analyzeText_Unauthorized_ThrowsAiProviderExceptionWithoutRetry() {

        stubFor(post(urlEqualTo("/v1/responses"))
            .willReturn(aResponse().withStatus(401)));

        StepVerifier.create(
                openAiAnalysisClient.analyzeText("Test message", metadata)
            )
            .expectErrorSatisfies(error -> {

                var exception = assertInstanceOf(
                    AiProviderException.class,
                    error
                );

                assertSame(metadata, exception.getMetadata());

                var cause = assertInstanceOf(
                    WebClientResponseException.class,
                    exception.getCause()
                );

                assertEquals(401, cause.getStatusCode().value());
            })
            .verify(Duration.ofSeconds(10));

        wireMockServer.verify(
            1,
            postRequestedFor(urlEqualTo("/v1/responses"))
        );
    }

    @Test
    void analyzeText_RetryableFailure_Recovers() {

        String scenario = "temporary provider failure";

        stubFor(post(urlEqualTo("/v1/responses"))
            .inScenario(scenario)
            .whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("recovered")
            .willReturn(aResponse().withStatus(429)));

        String body = new ObjectMapper().writeValueAsString(
            Map.of(
                "output", List.of(
                    Map.of(
                        "content", List.of(
                            Map.of(
                                "type", "output_text",
                                "text", "{\"status\":\"READY\",\"corrections\":[]}"
                            )
                        )
                    )
                )
            )
        );

        stubFor(post(urlEqualTo("/v1/responses"))
            .inScenario(scenario)
            .whenScenarioStateIs("recovered")
            .willReturn(okJson(body)));

        StepVerifier.create(
                openAiAnalysisClient.analyzeText("Test message", metadata)
            )
            .assertNext(analysis -> {
                assertEquals(MessageStatus.READY, analysis.getStatus());
            })
            .expectComplete()
            .verify(Duration.ofSeconds(20));

        wireMockServer.verify(
            2,
            postRequestedFor(urlEqualTo("/v1/responses"))
        );
    }

    @Test
    void analyzeText_RetriesExhausted_ThrowsAiProviderException() {

        stubFor(post(urlEqualTo("/v1/responses"))
            .willReturn(aResponse().withStatus(503)));

        StepVerifier.create(
                openAiAnalysisClient.analyzeText("Test message", metadata)
            )
            .expectErrorSatisfies(error -> {

                var exception = assertInstanceOf(
                    AiProviderException.class,
                    error
                );

                assertSame(metadata, exception.getMetadata());

                assertEquals(
                    "Failed to get analysis from OpenAI after retries",
                    exception.getMessage()
                );

                var cause = assertInstanceOf(
                    WebClientResponseException.class,
                    exception.getCause()
                );

                assertEquals(503, cause.getStatusCode().value());
            })
            .verify(Duration.ofMinutes(4));

        wireMockServer.verify(
            6,
            postRequestedFor(urlEqualTo("/v1/responses"))
        );
    }

    private void stubAiResponse(String analysisJson) {

        ObjectMapper mapper = new ObjectMapper();

        String openAiResponse = mapper.writeValueAsString(
            Map.of(
                "output", List.of(
                    Map.of(
                        "content", List.of(
                            Map.of(
                                "type", "output_text",
                                "text", analysisJson
                            )
                        )
                    )
                )
            )
        );

        stubFor(post(urlEqualTo("/v1/responses"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(openAiResponse)));
    }

    private void assertInvalidAiResponse(
            String analysisJson,
            String expectedMessage) {

        stubAiResponse(analysisJson);

        StepVerifier.create(
                openAiAnalysisClient.analyzeText(
                    "You never listen to me!",
                    metadata
                )
            )
            .expectErrorSatisfies(error -> {

                InvalidAiResponseException exception =
                    assertInstanceOf(
                        InvalidAiResponseException.class,
                        error
                    );

                assertEquals(
                    expectedMessage,
                    exception.getMessage()
                );

                assertSame(metadata, exception.getMetadata());
            })
            .verify();
    }
}
