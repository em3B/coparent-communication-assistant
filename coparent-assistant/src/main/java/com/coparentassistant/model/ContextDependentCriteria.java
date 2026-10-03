package com.coparentassistant.model;

import com.coparentassistant.model.Criteria;

public enum ContextDependentCriteria implements Criteria {
    PRIOR_BOUNDARY_IGNORED,
    REPEATED_COMMANDS,
    ISOLATION_TACTIC,
    BLAME_SHIFTING
}