package com.coparentassistant.controller;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.coparentassistant.service.MessageAnalysisService;
import com.coparentassistant.dto.MessageAnalysisRequest;
import com.coparentassistant.model.AiMessageAnalysis;
import com.coparentassistant.model.MessageStatus;

import tools.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(MessageAnalysisController.class)
public class MessageAnalysisControllerTest {

    @MockitoBean 
    private MessageAnalysisService messageAnalysisService;

    @Autowired 
    private ObjectMapper objectMapper;

    private final MockMvc mockMvc;

    @Autowired 
    public MessageAnalysisControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }
    
    @Test 
    void testPostMessageAnalysis() throws Exception {
        String message = "Please send the school letter.";
        MessageAnalysisRequest request = new MessageAnalysisRequest(message);

        AiMessageAnalysis analysis = new AiMessageAnalysis();
        analysis.setStatus(MessageStatus.READY);

        when(messageAnalysisService.analyzeMessage(message))
        .thenReturn(analysis);

        mockMvc.perform(post("/api/message-analysis")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));
    }
}
