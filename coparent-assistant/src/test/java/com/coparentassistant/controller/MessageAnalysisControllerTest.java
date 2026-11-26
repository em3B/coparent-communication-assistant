package com.coparentassistant.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import com.coparentassistant.model.Metadata;

import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

import java.util.List;

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

        MessageAnalysisRequest request =
            new MessageAnalysisRequest(message);

        AiMessageAnalysis analysis = AiMessageAnalysis.builder()
            .status(MessageStatus.READY)
            .corrections(List.of())
            .build();

        when(messageAnalysisService.analyzeMessage(
            eq(message),
            any(Metadata.class)
        )).thenReturn(Mono.just(analysis));

        MvcResult result = mockMvc.perform(post("/api/message_analysis")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.corrections").isEmpty());

        ArgumentCaptor<Metadata> metadataCaptor =
            ArgumentCaptor.forClass(Metadata.class);

        verify(messageAnalysisService).analyzeMessage(
            eq(message),
            metadataCaptor.capture()
        );

        Metadata metadata = metadataCaptor.getValue();

        assertNotNull(metadata);
        assertNotNull(metadata.getCorrelationId());
        assertNotNull(metadata.getPersistedAt());
    }

    @Test
    void testBlankMessageReturnsBadRequest() throws Exception {

        String message = "";

        MessageAnalysisRequest request =
            new MessageAnalysisRequest(message);

        mockMvc.perform(post("/api/message_analysis")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(messageAnalysisService);
    }

    @Test
    void testMessageExceedingMaxLengthReturnsBadRequest() throws Exception {

        String message = "a".repeat(501);

        MessageAnalysisRequest request =
            new MessageAnalysisRequest(message);

        mockMvc.perform(post("/api/message_analysis")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(messageAnalysisService);
    }

    @Test
    void testNullMessageReturnsBadRequest() throws Exception {

        mockMvc.perform(post("/api/message_analysis")
                .contentType("application/json")
                .content("{\"message\": null}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(messageAnalysisService);
    }

    @Test
    void testWhitespaceOnlyMessageReturnsBadRequest() throws Exception {

        mockMvc.perform(post("/api/message_analysis")
                .contentType("application/json")
                .content("{\"message\": \"   \"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(messageAnalysisService);
    }
}
