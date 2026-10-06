package com.coparentassistant.ai;

import com.coparentassistant.model.AiMessageAnalysis;
import com.coparentassistant.model.MessageStatus;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OpenAiAnalysisClientTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private OpenAiAnalysisClient openAiAnalysisClient;

    @BeforeAll
    static void startWireMock() {
        // Spins up a local mock web server on a random free local port
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

        String openAiResponse = """
            {
              "output": [
                {
                  "content": [
                    {
                      "type": "output_text",
                      "text": "{\\"status\\":\\"READY\\",\\"corrections\\":[]}"
                    }
                  ]
                }
              ]
            }
            """;

        stubFor(post(urlEqualTo("/v1/responses"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(openAiResponse)));

        Mono<AiMessageAnalysis> result =
            openAiAnalysisClient.analyzeText(message);

        StepVerifier.create(result)
            .assertNext(analysis -> {
                assertEquals(
                    MessageStatus.READY,
                    analysis.getStatus()
                );

                assertTrue(
                    analysis.getCorrections().isEmpty()
                );
            })
            .verifyComplete();
    }

    @Test
    void analyzeText_ReviewMessage_WithComplexJsonPayload() {
        String message =
            "Why didn't you send me the school form? "
                + "You always keep me out of the loop on purpose!";

        String openAiResponse = """
            {
              "output": [
                {
                  "content": [
                    {
                      "type": "output_text",
                      "text": "{\\"status\\":\\"REVIEW\\",\\"corrections\\":[{\\"originalText\\":\\"You always keep me out of the loop on purpose!\\",\\"suggestedCorrections\\":[\\"Please send me the school form.\\",\\"I have not yet received the school form.\\",\\"Please send me the school form at your earliest convenience.\\"],\\"explanation\\":\\"This assumes deliberate exclusion and assigns negative intent.\\",\\"criteria\\":[\\"ACCUSATION\\"]}]}"
                    }
                  ]
                }
              ]
            }
            """;

        stubFor(post(urlEqualTo("/v1/responses"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(openAiResponse)));

        Mono<AiMessageAnalysis> result =
            openAiAnalysisClient.analyzeText(message);

        StepVerifier.create(result)
            .assertNext(analysis -> {
                assertEquals(
                    MessageStatus.REVIEW,
                    analysis.getStatus()
                );

                assertEquals(
                    1,
                    analysis.getCorrections().size()
                );

                var correction =
                    analysis.getCorrections().get(0);

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
}