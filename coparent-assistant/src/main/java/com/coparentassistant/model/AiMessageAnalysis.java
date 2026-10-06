package com.coparentassistant.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder 
public class AiMessageAnalysis {
    @Setter
    private MessageStatus status;

    @Builder.Default
    private final List<TextCorrection> corrections = new ArrayList<>();

}
