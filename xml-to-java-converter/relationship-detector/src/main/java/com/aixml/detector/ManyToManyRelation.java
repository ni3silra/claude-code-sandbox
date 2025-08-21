package com.aixml.detector;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ManyToManyRelation {
    private String firstElement;
    private String secondElement;
    private String linkingElement;
    private double confidence;
    
    public String getDescription() {
        return String.format("Many-to-Many: %s <-> %s via %s (confidence: %.2f)", 
                firstElement, secondElement, linkingElement, confidence);
    }
}