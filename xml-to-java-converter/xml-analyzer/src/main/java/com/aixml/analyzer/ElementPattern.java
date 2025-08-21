package com.aixml.analyzer;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ElementPattern {
    private String patternName;
    private List<String> elementNames;
    private double similarityScore;
    private String parentContext;
    private boolean isRepeating;
}