package com.coparentassistant.controller;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.coparentassistant.service.MessageAnalysisService;
import com.coparentassistant.dto.MessageAnalysisRequest;
import com.coparentassistant.model.AiMessageAnalysis;
import com.coparentassistant.model.MessageStatus;
import com.coparentassistant.model.TextCorrection;

import tools.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

import reactor.core.publisher.Mono;

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
        // Ensure this property matches the key "message" used by your DTO 
        MessageAnalysisRequest request = new MessageAnalysisRequest(message); 
        TextCorrection correction = new TextCorrection("You should have sent it yesterday.");

        AiMessageAnalysis analysis = AiMessageAnalysis.builder()
            .status(MessageStatus.READY)
            .corrections(List.of(correction))
            .build();

        when(messageAnalysisService.analyzeMessage(message))
        .thenReturn(Mono.just(analysis));

        MvcResult result = mockMvc.perform(post("/api/message_analysis")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.corrections[0].originalText")
                        .value("You should have sent it yesterday."));
    }

}
