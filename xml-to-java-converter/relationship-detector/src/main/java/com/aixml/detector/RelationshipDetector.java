package com.aixml.detector;

import com.aixml.analyzer.XmlSchema;
import com.aixml.analyzer.ElementDefinition;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RelationshipDetector {
    
    public List<OneToManyRelation> detectOneToMany(XmlSchema schema) {
        List<OneToManyRelation> relations = new ArrayList<>();
        
        for (ElementDefinition element : schema.getElements()) {
            if (element.getChildren() != null && !element.getChildren().isEmpty()) {
                Map<String, Long> childCounts = element.getChildren().stream()
                        .collect(Collectors.groupingBy(ElementDefinition::getName, Collectors.counting()));
                
                for (Map.Entry<String, Long> entry : childCounts.entrySet()) {
                    if (entry.getValue() > 1) {
                        OneToManyRelation relation = OneToManyRelation.builder()
                                .parentElement(element.getName())
                                .childElement(entry.getKey())
                                .cardinality(entry.getValue().intValue())
                                .confidence(calculateOneToManyConfidence(element, entry.getKey()))
                                .build();
                        relations.add(relation);
                    }
                }
            }
        }
        
        return relations;
    }
    
    public List<ManyToManyRelation> detectManyToMany(XmlSchema schema) {
        List<ManyToManyRelation> relations = new ArrayList<>();
        Map<String, List<String>> elementContexts = new HashMap<>();
        
        for (ElementDefinition element : schema.getElements()) {
            elementContexts.computeIfAbsent(element.getName(), k -> new ArrayList<>())
                    .add(element.getParentElement());
        }
        
        for (Map.Entry<String, List<String>> entry : elementContexts.entrySet()) {
            if (entry.getValue().size() > 1) {
                Set<String> uniqueParents = new HashSet<>(entry.getValue());
                if (uniqueParents.size() > 1) {
                    for (String parent1 : uniqueParents) {
                        for (String parent2 : uniqueParents) {
                            if (!parent1.equals(parent2)) {
                                ManyToManyRelation relation = ManyToManyRelation.builder()
                                        .firstElement(parent1)
                                        .secondElement(parent2)
                                        .linkingElement(entry.getKey())
                                        .confidence(calculateManyToManyConfidence(schema, parent1, parent2, entry.getKey()))
                                        .build();
                                relations.add(relation);
                            }
                        }
                    }
                }
            }
        }
        
        return relations;
    }
    
    public List<ParentChildRelation> detectHierarchical(XmlSchema schema) {
        List<ParentChildRelation> relations = new ArrayList<>();
        
        for (ElementDefinition element : schema.getElements()) {
            if (element.getParentElement() != null) {
                ParentChildRelation relation = ParentChildRelation.builder()
                        .parentElement(element.getParentElement())
                        .childElement(element.getName())
                        .depth(calculateDepth(element, schema))
                        .isDirectChild(true)
                        .build();
                relations.add(relation);
            }
        }
        
        return relations;
    }
    
    private double calculateOneToManyConfidence(ElementDefinition parent, String childName) {
        if (parent.getChildren() == null) return 0.0;
        
        long childCount = parent.getChildren().stream()
                .filter(child -> child.getName().equals(childName))
                .count();
        
        if (childCount > 1) {
            return Math.min(0.9, 0.5 + (childCount * 0.1));
        }
        
        return 0.0;
    }
    
    private double calculateManyToManyConfidence(XmlSchema schema, String element1, String element2, String linkingElement) {
        long element1Count = schema.getElements().stream()
                .filter(e -> e.getName().equals(element1))
                .count();
        
        long element2Count = schema.getElements().stream()
                .filter(e -> e.getName().equals(element2))
                .count();
        
        if (element1Count > 1 && element2Count > 1) {
            return 0.8;
        } else if (element1Count > 1 || element2Count > 1) {
            return 0.6;
        }
        
        return 0.4;
    }
    
    private int calculateDepth(ElementDefinition element, XmlSchema schema) {
        int depth = 0;
        String currentParent = element.getParentElement();
        
        while (currentParent != null && !currentParent.equals(schema.getRootElementName())) {
            depth++;
            String finalCurrentParent = currentParent;
            currentParent = schema.getElements().stream()
                    .filter(e -> e.getName().equals(finalCurrentParent))
                    .findFirst()
                    .map(ElementDefinition::getParentElement)
                    .orElse(null);
        }
        
        return depth;
    }
}