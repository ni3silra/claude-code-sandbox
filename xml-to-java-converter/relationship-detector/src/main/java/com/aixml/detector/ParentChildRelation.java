package com.aixml.detector;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParentChildRelation {
    private String parentElement;
    private String childElement;
    private int depth;
    private boolean isDirectChild;
    
    public String getDescription() {
        return String.format("Parent-Child: %s -> %s (depth: %d, direct: %s)", 
                parentElement, childElement, depth, isDirectChild);
    }
}