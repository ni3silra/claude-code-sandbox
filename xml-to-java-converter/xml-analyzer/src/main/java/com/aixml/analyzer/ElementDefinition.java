package com.aixml.analyzer;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ElementDefinition {
    private String name;
    private String type;
    private boolean isCollection;
    private List<ElementDefinition> children;
    private Map<String, String> attributes;
    private String parentElement;
    private int occurrenceCount;
}