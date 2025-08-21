package com.aixml.analyzer;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class XmlSchema {
    private String rootElementName;
    private List<ElementDefinition> elements;
    private Map<String, Integer> elementFrequency;
    private List<ElementPattern> patterns;
}