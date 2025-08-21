package com.aixml.detector;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OneToManyRelation {
    private String parentElement;
    private String childElement;
    private int cardinality;
    private double confidence;
    
    public String getDescription() {
        return String.format("One-to-Many: %s -> %s (cardinality: %d, confidence: %.2f)", 
                parentElement, childElement, cardinality, confidence);
    }
}