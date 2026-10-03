package com.coparentassistant.model;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import com.coparentassistant.model.AiMessageAnalysis;
import com.coparentassistant.model.MessageStatus;
import com.coparentassistant.model.SingleMessageCriteria;
import com.coparentassistant.model.TextCorrection;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AiMessageAnalysisTest {
    
    @Test
    void aiMessageInReviewStatus_shouldContainAllRelevantFields() {
        AiMessageAnalysis analysis = new AiMessageAnalysis();
        String originalText = "You should have sent it yesterday.";
        TextCorrection textCorrection = new TextCorrection(originalText);
        analysis.setStatus(MessageStatus.REVIEW);
        String correctedText = "Please send it today.";
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(SingleMessageCriteria.ACCUSATION);
        criteria.add(SingleMessageCriteria.IRRELEVANT_OR_UNNECESSARY_CONTENT);
        textCorrection.setCorrectedText(correctedText);
        textCorrection.getCriteria().addAll(criteria);
        analysis.getCorrections().add(textCorrection);
        
        assertEquals(MessageStatus.REVIEW, analysis.getStatus());
        assertEquals(originalText, analysis.getCorrections().get(0).getOriginalText());
        assertEquals(correctedText, analysis.getCorrections().get(0).getCorrectedText());
        assertEquals(criteria, analysis.getCorrections().get(0).getCriteria());
        assertEquals(2, analysis.getCorrections().get(0).getCriteria().size());
        assertEquals(1, analysis.getCorrections().size());
    }
}
