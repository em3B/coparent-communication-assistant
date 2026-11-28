package com.coparentassistant.dto;

import java.time.Instant;
import java.util.UUID;

public record ErrorResponse(UUID correlationId, Instant timestamp, int statusCode, String errorCode, String message) {}