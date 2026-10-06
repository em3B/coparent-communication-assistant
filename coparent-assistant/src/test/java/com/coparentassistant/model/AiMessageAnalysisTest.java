package com.coparentassistant.model;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AiMessageAnalysisTest {
    
    @Test
    void aiMessageInReviewStatus_shouldContainAllRelevantFields() {
        String originalText = "You should have sent it yesterday.";
        String correctedText = "Please send it today.";
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(SingleMessageCriteria.ACCUSATION);
        criteria.add(SingleMessageCriteria.IRRELEVANT_OR_UNNECESSARY_CONTENT);
        TextCorrection textCorrection = TextCorrection.builder()
            .criteria(criteria)
            .originalText(originalText)
            .suggestedCorrections(List.of(correctedText))
            .build();
        AiMessageAnalysis analysis = AiMessageAnalysis.builder()
            .status(MessageStatus.REVIEW)
            .corrections(List.of(textCorrection))
            .build();
        
        assertEquals(MessageStatus.REVIEW, analysis.getStatus());
        assertEquals(originalText, analysis.getCorrections().get(0).getOriginalText());
        assertEquals(correctedText, analysis.getCorrections().get(0).getSuggestedCorrections().get(0    ));
        assertEquals(criteria, analysis.getCorrections().get(0).getCriteria());
        assertEquals(2, analysis.getCorrections().get(0).getCriteria().size());
        assertEquals(1, analysis.getCorrections().size());
    }
}
