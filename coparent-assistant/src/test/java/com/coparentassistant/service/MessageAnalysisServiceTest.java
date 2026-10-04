package com.coparentassistant.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.coparentassistant.ai.AiAnalysisClient;
import com.coparentassistant.model.AiMessageAnalysis;
import com.coparentassistant.model.MessageStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        AiMessageAnalysis expectedAnalysis = new AiMessageAnalysis();
        expectedAnalysis.setStatus(MessageStatus.READY);

        Mockito.when(aiAnalysisClient.analyzeText(message)).thenReturn(expectedAnalysis);

        AiMessageAnalysis actualAnalysis = messageAnalysisService.analyzeMessage(message);
        assertEquals(expectedAnalysis, actualAnalysis);
        Mockito.verify(aiAnalysisClient).analyzeText(message);
    }
}
