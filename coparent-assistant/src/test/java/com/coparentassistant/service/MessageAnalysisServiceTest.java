package com.coparentassistant.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.coparentassistant.ai.AiAnalysisClient;
import com.coparentassistant.model.AiMessageAnalysis;
import com.coparentassistant.model.MessageStatus;

import reactor.core.publisher.Mono;

public class MessageAnalysisServiceTest {
    private AiAnalysisClient aiAnalysisClient;
    private MessageAnalysisService messageAnalysisService;  

    @BeforeEach 
    public void setUp() {
        aiAnalysisClient = Mockito.mock(AiAnalysisClient.class);
        messageAnalysisService = new MessageAnalysisService(aiAnalysisClient);
    }

    @Test
    void analyzeMessageReturnsAiAnalysis() {
        String message = "Please send the school letter.";

        AiMessageAnalysis expectedAnalysis = AiMessageAnalysis.builder().status(MessageStatus.READY).build();

        Mockito.when(aiAnalysisClient.analyzeText(message)).thenReturn(Mono.just(expectedAnalysis));

        Mono<AiMessageAnalysis> actualAnalysis = messageAnalysisService.analyzeMessage(message);

        reactor.test.StepVerifier.create(actualAnalysis)
                .assertNext(analysis -> {
                    org.junit.jupiter.api.Assertions.assertEquals(MessageStatus.READY, analysis.getStatus());
                })
                .verifyComplete();

        Mockito.verify(aiAnalysisClient).analyzeText(message);
    }

}
