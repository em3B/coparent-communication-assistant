package com.coparentassistant.exceptions;

import com.coparentassistant.dto.ErrorResponse;
import com.coparentassistant.model.Metadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.http.HttpMethod;
import java.net.URI;
import java.net.ConnectException;
import java.io.IOException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;
    private Metadata metadata;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
        metadata = new Metadata(
            UUID.randomUUID(),
            Instant.now()
        );
    }

    @Test
    void shouldHandleInvalidAiResponseException() {
        InvalidAiResponseException exception =
            new InvalidAiResponseException(
                "OpenAI response contains no analysis text",
                metadata
            );

        ResponseEntity<ErrorResponse> responseEntity =
            globalExceptionHandler.handleInvalidAiResponse(exception);

        ErrorResponse body = responseEntity.getBody();

        assertEquals(HttpStatus.BAD_GATEWAY, responseEntity.getStatusCode());
        assertNotNull(body);
        assertEquals(metadata.getCorrelationId(), body.correlationId());
        assertEquals(metadata.getPersistedAt(), body.timestamp());
        assertEquals(502, body.statusCode());
        assertEquals("INVALID_AI_RESPONSE", body.errorCode());
        assertEquals(
            "Unable to analyse your message. Please try again.",
            body.message()
        );
    }

    @Test
    void shouldHandleAiProviderRateLimitException() {

        WebClientResponseException cause =
            WebClientResponseException.create(
                429,
                "Too Many Requests",
                HttpHeaders.EMPTY,
                new byte[0],
                null
            );

        AiProviderException exception = new AiProviderException(
            "Failed to get analysis from OpenAI after retries",
            metadata,
            cause
        );

        ResponseEntity<ErrorResponse> responseEntity =
            globalExceptionHandler.handleAiProviderException(exception);

        ErrorResponse body = responseEntity.getBody();

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, responseEntity.getStatusCode());
        assertNotNull(body);
        assertEquals(metadata.getCorrelationId(), body.correlationId());
        assertEquals(metadata.getPersistedAt(), body.timestamp());
        assertEquals(429, body.statusCode());
        assertEquals("AI_PROVIDER_RATE_LIMITED", body.errorCode());
        assertEquals(
            "The AI provider is currently experiencing high demand or usage limits. Please try again later.",
            body.message()
        );

    }

    @Test
    void shouldHandleAiProviderServerException() {

        WebClientResponseException cause =
            WebClientResponseException.create(
                500,
                "Internal Server Error",
                HttpHeaders.EMPTY,
                new byte[0],
                null
            );

        AiProviderException exception = new AiProviderException(
            "Failed to get analysis from OpenAI after retries",
            metadata,
            cause
        );

        ResponseEntity<ErrorResponse> responseEntity =
            globalExceptionHandler.handleAiProviderException(exception);

        ErrorResponse body = responseEntity.getBody();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, responseEntity.getStatusCode());
        assertNotNull(body);
        assertEquals(metadata.getCorrelationId(), body.correlationId());
        assertEquals(metadata.getPersistedAt(), body.timestamp());
        assertEquals(503, body.statusCode());
        assertEquals("AI_PROVIDER_UNAVAILABLE", body.errorCode());
        assertEquals(
            "The AI service is temporarily unavailable. Please try again later.",
            body.message()
        );

    }

    @Test
    void shouldHandleAiProviderConnectionException() {

        WebClientRequestException cause =
            new WebClientRequestException(
                new ConnectException("Connection refused"),
                HttpMethod.POST,
                URI.create("https://api.openai.com/v1/responses"),
                HttpHeaders.EMPTY
            );

        AiProviderException exception = new AiProviderException(
            "Failed to communicate with OpenAI",
            metadata,
            cause
        );

        ResponseEntity<ErrorResponse> responseEntity =
            globalExceptionHandler.handleAiProviderException(exception);

        ErrorResponse body = responseEntity.getBody();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, responseEntity.getStatusCode());
        assertNotNull(body);
        assertEquals(metadata.getCorrelationId(), body.correlationId());
        assertEquals(metadata.getPersistedAt(), body.timestamp());
        assertEquals(503, body.statusCode());
        assertEquals("AI_PROVIDER_UNAVAILABLE", body.errorCode());
        assertEquals(
            "The AI service is temporarily unavailable. Please try again later.",
            body.message()
        );

    }

    @Test
    void shouldHandleAiProviderIOException() {

        IOException cause = new IOException("Connection reset");

        AiProviderException exception = new AiProviderException(
            "I/O failure while communicating with OpenAI",
            metadata,
            cause
        );

        ResponseEntity<ErrorResponse> responseEntity =
            globalExceptionHandler.handleAiProviderException(exception);

        ErrorResponse body = responseEntity.getBody();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, responseEntity.getStatusCode());
        assertNotNull(body);
        assertEquals(metadata.getCorrelationId(), body.correlationId());
        assertEquals(metadata.getPersistedAt(), body.timestamp());
        assertEquals(503, body.statusCode());
        assertEquals("AI_PROVIDER_UNAVAILABLE", body.errorCode());
        assertEquals(
            "The AI service is temporarily unavailable. Please try again later.",
            body.message()
        );

    }

    @Test
    void shouldHandleAiProviderAuthenticationException() {

        WebClientResponseException cause =
            WebClientResponseException.create(
                401,
                "Unauthorized",
                HttpHeaders.EMPTY,
                new byte[0],
                null
            );

        AiProviderException exception = new AiProviderException(
            "OpenAI returned HTTP status 401",
            metadata,
            cause
        );

        ResponseEntity<ErrorResponse> responseEntity =
            globalExceptionHandler.handleAiProviderException(exception);

        ErrorResponse body = responseEntity.getBody();

        assertEquals(HttpStatus.BAD_GATEWAY, responseEntity.getStatusCode());
        assertNotNull(body);
        assertEquals(metadata.getCorrelationId(), body.correlationId());
        assertEquals(metadata.getPersistedAt(), body.timestamp());
        assertEquals(502, body.statusCode());
        assertEquals("AI_PROVIDER_ERROR", body.errorCode());
        assertEquals(
            "Unable to communicate with the AI provider. Please try again.",
            body.message()
        );

    }

    @Test
    void shouldHandleUnknownAiProviderException() {

        RuntimeException cause =
            new RuntimeException("Unexpected provider failure");

        AiProviderException exception = new AiProviderException(
            "Unexpected AI provider error",
            metadata,
            cause
        );

        ResponseEntity<ErrorResponse> responseEntity =
            globalExceptionHandler.handleAiProviderException(exception);

        ErrorResponse body = responseEntity.getBody();

        assertEquals(HttpStatus.BAD_GATEWAY, responseEntity.getStatusCode());
        assertNotNull(body);
        assertEquals(metadata.getCorrelationId(), body.correlationId());
        assertEquals(metadata.getPersistedAt(), body.timestamp());
        assertEquals(502, body.statusCode());
        assertEquals("AI_PROVIDER_ERROR", body.errorCode());
        assertEquals(
            "Unable to communicate with the AI provider. Please try again.",
            body.message()
        );

    }

    @Test
    void shouldHandleMethodArgumentNotValidException() {

        MethodArgumentNotValidException exception =
            mock(MethodArgumentNotValidException.class);

        ResponseEntity<ErrorResponse> responseEntity =
            globalExceptionHandler.handleMethodArgumentNotValid(exception);

        ErrorResponse body = responseEntity.getBody();

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertNotNull(body);
        assertNotNull(body.correlationId());
        assertNotNull(body.timestamp());
        assertEquals(400, body.statusCode());
        assertEquals("INVALID_INPUT", body.errorCode());
        assertEquals(
            "One or more input parameters are invalid.",
            body.message()
        );

    }

    @Test
    void shouldHandleHttpMessageNotReadableException() {

        HttpMessageNotReadableException exception =
            mock(HttpMessageNotReadableException.class);

        ResponseEntity<ErrorResponse> responseEntity =
            globalExceptionHandler.handleHttpMessageNotReadable(exception);
        
        ErrorResponse body = responseEntity.getBody();

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertNotNull(body);
        assertNotNull(body.correlationId());
        assertNotNull(body.timestamp());
        assertEquals(400, body.statusCode());
        assertEquals("INVALID_INPUT", body.errorCode());
        assertEquals(
            "The request body is not readable.",
            body.message()
        );

    }

    @Test
    void shouldHandleSpringHttpException() {

        HttpRequestMethodNotSupportedException exception =
            new HttpRequestMethodNotSupportedException(
                "GET",
                List.of("POST")
            );

        ResponseEntity<ErrorResponse> responseEntity =
            globalExceptionHandler.handleGenericException(exception);

        ErrorResponse body = responseEntity.getBody();

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, responseEntity.getStatusCode());
        assertNotNull(body);
        assertNotNull(body.correlationId());
        assertNotNull(body.timestamp());
        assertEquals(405, body.statusCode());
        assertEquals("HTTP_REQUEST_ERROR", body.errorCode());
        assertEquals(
            "The request could not be processed.",
            body.message()
        );
        assertTrue(
            responseEntity.getHeaders().getAllow().contains(HttpMethod.POST)
        );

    }

    @Test
    void shouldHandleUnexpectedException() {

        RuntimeException exception =
            new RuntimeException("Sensitive internal failure");

        ResponseEntity<ErrorResponse> responseEntity =
            globalExceptionHandler.handleGenericException(exception);

        ErrorResponse body = responseEntity.getBody();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertNotNull(body);
        assertNotNull(body.correlationId());
        assertNotNull(body.timestamp());
        assertEquals(500, body.statusCode());
        assertEquals("INTERNAL_SERVER_ERROR", body.errorCode());
        assertEquals(
            "An unexpected error occurred. Please try again later.",
            body.message()
        );

    }


}