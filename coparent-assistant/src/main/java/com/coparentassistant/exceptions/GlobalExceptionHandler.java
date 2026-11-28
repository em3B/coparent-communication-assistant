
package com.coparentassistant.exceptions;

import com.coparentassistant.dto.ErrorResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.time.Instant;
import java.util.UUID;

import java.io.IOException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpHeaders;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j 
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidAiResponseException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAiResponse(
            InvalidAiResponseException e) {

        return buildErrorResponse(
            e.getMetadata().getCorrelationId(),
            e.getMetadata().getPersistedAt(),
            HttpStatus.BAD_GATEWAY,
            "INVALID_AI_RESPONSE",
            "Unable to analyse your message. Please try again."
        );
    }

    @ExceptionHandler(AiProviderException.class)
    public ResponseEntity<ErrorResponse> handleAiProviderException(
            AiProviderException e) {

        Throwable cause = e.getCause();

        HttpStatus status = HttpStatus.BAD_GATEWAY;
        String errorCode = "AI_PROVIDER_ERROR";
        String message = "Unable to communicate with the AI provider. Please try again.";

        if (cause instanceof WebClientResponseException ex) {

            if (ex.getStatusCode().value() == 429) {

                status = HttpStatus.TOO_MANY_REQUESTS;
                errorCode = "AI_PROVIDER_RATE_LIMITED";
                message = "The AI provider is currently experiencing high demand or usage limits. Please try again later.";

            } else if (ex.getStatusCode().is5xxServerError()) {

                status = HttpStatus.SERVICE_UNAVAILABLE;
                errorCode = "AI_PROVIDER_UNAVAILABLE";
                message = "The AI service is temporarily unavailable. Please try again later.";
            }

        } else if (cause instanceof WebClientRequestException
                || cause instanceof IOException) {

            status = HttpStatus.SERVICE_UNAVAILABLE;
            errorCode = "AI_PROVIDER_UNAVAILABLE";
            message = "The AI service is temporarily unavailable. Please try again later.";
        }

        return buildErrorResponse(
            e.getMetadata().getCorrelationId(),
            e.getMetadata().getPersistedAt(),
            status,
            errorCode,
            message
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e) {

        return buildErrorResponse(
            UUID.randomUUID(),
            Instant.now(),
            HttpStatus.BAD_REQUEST,
            "INVALID_INPUT",
            "One or more input parameters are invalid."
        );

    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception e) {

        if (e instanceof org.springframework.web.ErrorResponse springError) {

            return buildErrorResponse(
                UUID.randomUUID(),
                Instant.now(),
                springError.getStatusCode(),
                "HTTP_REQUEST_ERROR",
                "The request could not be processed.",
                springError.getHeaders()
            );
        }

        return buildErrorResponse(
            UUID.randomUUID(),
            Instant.now(),
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred. Please try again later."
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException e) {
        return buildErrorResponse(
            UUID.randomUUID(),
            Instant.now(),
            HttpStatus.BAD_REQUEST,
            "INVALID_INPUT",
            "The request body is not readable."
        );
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            UUID correlationId,
            Instant timestamp,
            HttpStatusCode status,
            String errorCode,
            String message) {

        return buildErrorResponse(
            correlationId,
            timestamp,
            status,
            errorCode,
            message,
            HttpHeaders.EMPTY
        );
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            UUID correlationId,
            Instant timestamp,
            HttpStatusCode status,
            String errorCode,
            String message,
            HttpHeaders headers) {

        var logEvent = status.is5xxServerError()
            ? log.atError()
            : log.atWarn();

        logEvent
            .addKeyValue("correlationId", correlationId)
            .addKeyValue("statusCode", status.value())
            .addKeyValue("errorCode", errorCode)
            .log("Request failed");

        return new ResponseEntity<>(
            new ErrorResponse(
                correlationId,
                timestamp,
                status.value(),
                errorCode,
                message
            ),
            headers,
            status
        );
    }
}
