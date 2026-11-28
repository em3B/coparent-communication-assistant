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
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.http.HttpHeaders;

import com.coparentassistant.service.MessageAnalysisService;
import com.coparentassistant.dto.MessageAnalysisRequest;
import com.coparentassistant.model.AiMessageAnalysis;
import com.coparentassistant.model.MessageStatus;
import com.coparentassistant.model.Metadata;
import com.coparentassistant.exceptions.InvalidAiResponseException;
import com.coparentassistant.exceptions.AiProviderException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

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
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.correlationId").isNotEmpty())
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.statusCode").value(400))
            .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
            .andExpect(jsonPath("$.message")
                .value("One or more input parameters are invalid."));

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
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.correlationId").isNotEmpty())
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.statusCode").value(400))
            .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
            .andExpect(jsonPath("$.message")
                .value("One or more input parameters are invalid."));

        verifyNoInteractions(messageAnalysisService);
    }

    @Test
    void testNullMessageReturnsBadRequest() throws Exception {

        mockMvc.perform(post("/api/message_analysis")
            .contentType("application/json")
            .content("{\"message\": null}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.correlationId").isNotEmpty())
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.statusCode").value(400))
            .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
            .andExpect(jsonPath("$.message")
                .value("One or more input parameters are invalid."));

        verifyNoInteractions(messageAnalysisService);
    }

    @Test
    void testWhitespaceOnlyMessageReturnsBadRequest() throws Exception {

        mockMvc.perform(post("/api/message_analysis")
            .contentType("application/json")
            .content("{\"message\": \"   \"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.correlationId").isNotEmpty())
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.statusCode").value(400))
            .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
            .andExpect(jsonPath("$.message")
                .value("One or more input parameters are invalid."));

        verifyNoInteractions(messageAnalysisService);
    }

    @Test
    void testInvalidAiResponseReturnsBadGateway() throws Exception {

        String message = "Please send the school letter.";

        MessageAnalysisRequest request =
            new MessageAnalysisRequest(message);

        when(messageAnalysisService.analyzeMessage(
            eq(message),
            any(Metadata.class)
        )).thenAnswer(invocation -> {

            Metadata metadata = invocation.getArgument(1);

            return Mono.error(
                new InvalidAiResponseException(
                    "OpenAI response contains no analysis text",
                    metadata
                )
            );
        });

        MvcResult result = mockMvc.perform(post("/api/message_analysis")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(request().asyncStarted())
            .andReturn();

        ArgumentCaptor<Metadata> metadataCaptor =
            ArgumentCaptor.forClass(Metadata.class);

        verify(messageAnalysisService).analyzeMessage(
            eq(message),
            metadataCaptor.capture()
        );

        Metadata capturedMetadata = metadataCaptor.getValue();

        assertNotNull(capturedMetadata);
        assertNotNull(capturedMetadata.getCorrelationId());
        assertNotNull(capturedMetadata.getPersistedAt());

        mockMvc.perform(asyncDispatch(result))
            .andDo(print())
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.message")
                .value("Unable to analyse your message. Please try again."))
            .andExpect(jsonPath("$.correlationId")
                .value(capturedMetadata.getCorrelationId().toString()))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.statusCode").value(502))
            .andExpect(jsonPath("$.errorCode").value("INVALID_AI_RESPONSE"));
    }

    @Test
    void testAiProviderRateLimitReturnsTooManyRequests() throws Exception {

        String message = "Please send the school letter.";

        MessageAnalysisRequest request =
            new MessageAnalysisRequest(message);

        WebClientResponseException cause =
            WebClientResponseException.create(
                429,
                "Too Many Requests",
                HttpHeaders.EMPTY,
                new byte[0],
                null
            );

        when(messageAnalysisService.analyzeMessage(
            eq(message),
            any(Metadata.class)
        )).thenAnswer(invocation -> {

            Metadata metadata = invocation.getArgument(1);

            return Mono.error(
                new AiProviderException(
                    "Failed to get analysis from OpenAI after retries",
                    metadata,
                    cause
                )
            );
        });

        MvcResult result = mockMvc.perform(post("/api/message_analysis")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(request().asyncStarted())
            .andReturn();

        ArgumentCaptor<Metadata> metadataCaptor =
            ArgumentCaptor.forClass(Metadata.class);

        verify(messageAnalysisService).analyzeMessage(
            eq(message),
            metadataCaptor.capture()
        );

        Metadata capturedMetadata = metadataCaptor.getValue();

        assertNotNull(capturedMetadata);
        assertNotNull(capturedMetadata.getCorrelationId());
        assertNotNull(capturedMetadata.getPersistedAt());

        mockMvc.perform(asyncDispatch(result))
            .andDo(print())
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.message")
                .value("The AI provider is currently experiencing high demand or usage limits. Please try again later."))
            .andExpect(jsonPath("$.correlationId")
                .value(capturedMetadata.getCorrelationId().toString()))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.statusCode").value(429))
            .andExpect(jsonPath("$.errorCode").value("AI_PROVIDER_RATE_LIMITED"));
    }

    @Test
    void testAiProviderUnavailableReturnsServiceUnavailable() throws Exception {

        String message = "Please send the school letter.";

        MessageAnalysisRequest request =
            new MessageAnalysisRequest(message);

        WebClientResponseException cause =
            WebClientResponseException.create(
                500,
                "Internal Server Error",
                HttpHeaders.EMPTY,
                new byte[0],
                null
            );

        when(messageAnalysisService.analyzeMessage(
            eq(message),
            any(Metadata.class)
        )).thenAnswer(invocation -> {

            Metadata metadata = invocation.getArgument(1);

            return Mono.error(
                new AiProviderException(
                    "Failed to get analysis from OpenAI after retries",
                    metadata,
                    cause
                )
            );
        });

        MvcResult result = mockMvc.perform(post("/api/message_analysis")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(request().asyncStarted())
            .andReturn();

        ArgumentCaptor<Metadata> metadataCaptor =
            ArgumentCaptor.forClass(Metadata.class);

        verify(messageAnalysisService).analyzeMessage(
            eq(message),
            metadataCaptor.capture()
        );

        Metadata capturedMetadata = metadataCaptor.getValue();

        assertNotNull(capturedMetadata);
        assertNotNull(capturedMetadata.getCorrelationId());
        assertNotNull(capturedMetadata.getPersistedAt());

        mockMvc.perform(asyncDispatch(result))
            .andDo(print())
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.message")
                .value("The AI service is temporarily unavailable. Please try again later."))
            .andExpect(jsonPath("$.correlationId")
                .value(capturedMetadata.getCorrelationId().toString()))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.statusCode").value(503))
            .andExpect(jsonPath("$.errorCode").value("AI_PROVIDER_UNAVAILABLE"));
    }

    @Test
    void testMalformedJsonReturnsBadRequest() throws Exception {

        mockMvc.perform(post("/api/message_analysis")
            .contentType("application/json")
            .content("{\"message\":"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.correlationId").isNotEmpty())
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.statusCode").value(400))
            .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"))
            .andExpect(jsonPath("$.message")
                .value("The request body is not readable."));

        verifyNoInteractions(messageAnalysisService);
    }

    @Test
    void testUnsupportedHttpMethodReturnsMethodNotAllowed() throws Exception {

        mockMvc.perform(get("/api/message_analysis"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(header().string("Allow", "POST"))
            .andExpect(jsonPath("$.correlationId").isNotEmpty())
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.statusCode").value(405))
            .andExpect(jsonPath("$.errorCode").value("HTTP_REQUEST_ERROR"))
            .andExpect(jsonPath("$.message")
                .value("The request could not be processed."));

        verifyNoInteractions(messageAnalysisService);
    }

    @Test
    void testUnexpectedExceptionReturnsInternalServerError() throws Exception {

        String message = "Please send the school letter.";

        MessageAnalysisRequest request =
            new MessageAnalysisRequest(message);

        when(messageAnalysisService.analyzeMessage(
            eq(message),
            any(Metadata.class)
        )).thenReturn(Mono.error(
            new RuntimeException("Sensitive internal failure")
        ));

        MvcResult result = mockMvc.perform(post("/api/message_analysis")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(result))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message")
                .value("An unexpected error occurred. Please try again later."))
            .andExpect(jsonPath("$.correlationId").isNotEmpty())
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.statusCode").value(500))
            .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"));
    }

}