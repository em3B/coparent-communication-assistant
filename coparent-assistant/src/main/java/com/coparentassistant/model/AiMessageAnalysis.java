package com.coparentassistant.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
public class AiMessageAnalysis {
    @Setter
    private MessageStatus status;

    private final List<TextCorrection> corrections = new ArrayList<>();

}
