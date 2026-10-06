package com.coparentassistant.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder    
public class TextCorrection {
    
    private String originalText; 
 
    @Builder.Default
    private List<String> suggestedCorrections = new ArrayList<>();

    @Setter
    private String explanation;

    @Builder.Default
    private List<Criteria> criteria = new ArrayList<>();

    public TextCorrection(String originalText) {
        this.originalText = originalText;
    }
}
