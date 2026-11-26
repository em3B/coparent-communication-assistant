package com.coparentassistant.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.coparentassistant.ai.AiAnalysisClient;
import com.coparentassistant.model.AiMessageAnalysis;
import com.coparentassistant.model.MessageStatus;
import com.coparentassistant.model.Metadata;

import java.time.Instant;
import java.util.UUID;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class MessageAnalysisServiceTest {

    private AiAnalysisClient aiAnalysisClient;
    private MessageAnalysisService messageAnalysisService;
    private Metadata metadata;

    @BeforeEach
    public void setUp() {
        aiAnalysisClient = Mockito.mock(AiAnalysisClient.class);
        messageAnalysisService = new MessageAnalysisService(aiAnalysisClient);
        metadata = new Metadata(UUID.randomUUID(), Instant.now());
    }

    @Test
    void analyzeMessageReturnsAiAnalysis() {

        String message = "Please send the school letter.";

        AiMessageAnalysis expectedAnalysis = AiMessageAnalysis.builder()
            .status(MessageStatus.READY)
            .build();

        Mockito.when(aiAnalysisClient.analyzeText(message, metadata))
            .thenReturn(Mono.just(expectedAnalysis));

        Mono<AiMessageAnalysis> actualAnalysis =
            messageAnalysisService.analyzeMessage(message, metadata);

        StepVerifier.create(actualAnalysis)
            .assertNext(analysis -> {
                assertEquals(MessageStatus.READY, analysis.getStatus());
                assertSame(expectedAnalysis, analysis);
            })
            .verifyComplete();

        Mockito.verify(aiAnalysisClient).analyzeText(
            Mockito.eq(message),
            Mockito.same(metadata)
        );
    }
}
