package com.coparentassistant.model;

import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

import com.coparentassistant.model.Criteria;

@Getter
public class TextCorrection {
    private final String originalText;

    @Setter
    private String correctedText;

    @Setter
    private String explanation;

    private List<Criteria> criteria = new ArrayList<>();

    public TextCorrection(String originalText) {
        this.originalText = originalText;
    }
}
