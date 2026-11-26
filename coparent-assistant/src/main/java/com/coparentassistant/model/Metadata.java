package com.coparentassistant.model;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Metadata {
    private final UUID correlationId;
    private final Instant persistedAt;
}
