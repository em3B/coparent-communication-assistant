package com.coparentassistant.model;

import com.coparentassistant.model.Criteria;

public enum SingleMessageCriteria implements Criteria {
    ACCUSATION,
    THREAT_OR_COERCION,
    IRRELEVANT_OR_UNNECESSARY_CONTENT,
    UNNECESSARY_INTERROGATION,
    DEGRADING_OR_BELITTLING_LANGUAGE,
    OBSCENE_LANGUAGE,
    INTIMIDATION,
    PUNITIVE_OR_RETALIATORY_LANGUAGE
}