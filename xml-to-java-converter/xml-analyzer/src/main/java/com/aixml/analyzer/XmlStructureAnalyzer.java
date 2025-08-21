package com.aixml.analyzer;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class XmlStructureAnalyzer {
    
    private final XmlMapper xmlMapper;
    
    public XmlStructureAnalyzer() {
        xmlMapper = new XmlMapper();
        // Configure to handle attributes properly
        xmlMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    
    public XmlSchema analyzeStructure(String xmlContent) {
        try {
            // Use DOM parsing for better attribute and element handling
            return analyzeStructureWithDOM(xmlContent);
                    
        } catch (Exception e) {
            log.error("Error analyzing XML structure", e);
            throw new RuntimeException("Failed to analyze XML structure", e);
        }
    }
    
    private XmlSchema analyzeStructureWithDOM(String xmlContent) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes()));
        
        Element rootElement = doc.getDocumentElement();
        String rootElementName = rootElement.getNodeName();
        
        Map<String, Integer> elementFrequency = new HashMap<>();
        List<ElementDefinition> elements = new ArrayList<>();
        
        // Analyze the root element
        analyzeDOMNode(rootElement, null, elements, elementFrequency);
        
        List<ElementPattern> patterns = detectRepeatingPatterns(elements, elementFrequency);
        
        return XmlSchema.builder()
                .rootElementName(rootElementName)
                .elements(elements)
                .elementFrequency(elementFrequency)
                .patterns(patterns)
                .build();
    }
    
    private void analyzeDOMNode(Element element, String parentName, 
                               List<ElementDefinition> elements, Map<String, Integer> frequency) {
        
        String elementName = element.getNodeName();
        frequency.merge(elementName, 1, Integer::sum);
        
        // Extract attributes
        Map<String, String> attributes = new HashMap<>();
        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            attributes.put(attr.getNodeName(), attr.getNodeValue());
        }
        
        // Extract direct child element names (not full definitions to avoid recursion issues)
        List<ElementDefinition> children = new ArrayList<>();
        NodeList childNodes = element.getChildNodes();
        
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) child;
                String childName = childElement.getNodeName();
                
                // Create simple child element definition
                ElementDefinition childDef = ElementDefinition.builder()
                        .name(childName)
                        .type(determineElementType(childElement))
                        .isCollection(false) // Will be updated later
                        .children(new ArrayList<>())
                        .attributes(new HashMap<>())
                        .parentElement(elementName)
                        .occurrenceCount(1)
                        .build();
                children.add(childDef);
                
                // Recursively analyze child elements
                analyzeDOMNode(childElement, elementName, elements, frequency);
            }
        }
        
        // Create element definition
        ElementDefinition element_def = ElementDefinition.builder()
                .name(elementName)
                .type(determineElementType(element))
                .isCollection(frequency.get(elementName) > 1)
                .children(children)
                .attributes(attributes)
                .parentElement(parentName)
                .occurrenceCount(frequency.get(elementName))
                .build();
                
        elements.add(element_def);
    }
    
    
    private String determineElementType(Element element) {
        // Check if element has child elements
        NodeList children = element.getChildNodes();
        boolean hasElementChildren = false;
        
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                hasElementChildren = true;
                break;
            }
        }
        
        if (hasElementChildren) {
            return "Object";
        } else {
            // Simple element with text content
            String textContent = element.getTextContent();
            if (textContent != null && !textContent.trim().isEmpty()) {
                // Try to determine if it's a number
                try {
                    Integer.parseInt(textContent.trim());
                    return "Integer";
                } catch (NumberFormatException e1) {
                    try {
                        Double.parseDouble(textContent.trim());
                        return "Double";
                    } catch (NumberFormatException e2) {
                        return "String";
                    }
                }
            }
            return "String";
        }
    }
    
    private String extractRootElementName(String xmlContent) {
        try {
            // Simple regex to extract root element name from XML
            String trimmed = xmlContent.trim();
            if (trimmed.startsWith("<?xml")) {
                // Skip XML declaration
                int declarationEnd = trimmed.indexOf("?>");
                if (declarationEnd != -1) {
                    trimmed = trimmed.substring(declarationEnd + 2).trim();
                }
            }
            
            if (trimmed.startsWith("<")) {
                int endOfTagName = trimmed.indexOf(' ');
                int endOfTag = trimmed.indexOf('>');
                
                if (endOfTagName == -1) endOfTagName = endOfTag;
                if (endOfTag == -1) return "unknown";
                
                int endIndex = Math.min(endOfTagName, endOfTag);
                return trimmed.substring(1, endIndex);
            }
            
            return "unknown";
        } catch (Exception e) {
            log.warn("Could not extract root element name, using fallback", e);
            return "unknown";
        }
    }
    
    public List<ElementPattern> detectRepeatingPatterns(XmlSchema schema) {
        return detectRepeatingPatterns(schema.getElements(), schema.getElementFrequency());
    }
    
    private List<ElementPattern> detectRepeatingPatterns(List<ElementDefinition> elements, Map<String, Integer> frequency) {
        List<ElementPattern> patterns = new ArrayList<>();
        
        for (Map.Entry<String, Integer> entry : frequency.entrySet()) {
            if (entry.getValue() > 1) {
                List<ElementDefinition> similarElements = elements.stream()
                        .filter(e -> e.getName().equals(entry.getKey()))
                        .collect(Collectors.toList());
                
                if (!similarElements.isEmpty()) {
                    ElementPattern pattern = ElementPattern.builder()
                            .patternName(entry.getKey() + "_pattern")
                            .elementNames(Arrays.asList(entry.getKey()))
                            .similarityScore(calculateSimilarity(similarElements))
                            .parentContext(similarElements.get(0).getParentElement())
                            .isRepeating(true)
                            .build();
                    patterns.add(pattern);
                }
            }
        }
        
        return patterns;
    }
    
    private void analyzeNode(JsonNode node, String elementName, String parentName, 
                           List<ElementDefinition> elements, Map<String, Integer> frequency) {
        
        frequency.merge(elementName, 1, Integer::sum);
        
        Map<String, String> attributes = new HashMap<>();
        List<ElementDefinition> children = new ArrayList<>();
        
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey();
                JsonNode fieldValue = field.getValue();
                
                if (fieldName.startsWith("@")) {
                    // Handle attributes
                    attributes.put(fieldName.substring(1), fieldValue.asText());
                } else if (fieldValue.isArray()) {
                    // Handle arrays
                    for (JsonNode arrayItem : fieldValue) {
                        analyzeNode(arrayItem, fieldName, elementName, elements, frequency);
                    }
                } else if (fieldValue.isObject()) {
                    // Handle nested objects
                    analyzeNode(fieldValue, fieldName, elementName, elements, frequency);
                } else {
                    // Handle simple values as child elements
                    ElementDefinition childElement = ElementDefinition.builder()
                            .name(fieldName)
                            .type(determineType(fieldValue))
                            .isCollection(false)
                            .children(new ArrayList<>())
                            .attributes(new HashMap<>())
                            .parentElement(elementName)
                            .occurrenceCount(1)
                            .build();
                    children.add(childElement);
                    frequency.merge(fieldName, 1, Integer::sum);
                }
            }
        } else if (node.isTextual()) {
            // Handle text content - this happens when we reach a leaf node
            // The parent element will handle this as part of its structure
        }
        
        ElementDefinition element = ElementDefinition.builder()
                .name(elementName)
                .type(determineType(node))
                .isCollection(frequency.get(elementName) > 1)
                .children(children)
                .attributes(attributes)
                .parentElement(parentName)
                .occurrenceCount(frequency.get(elementName))
                .build();
                
        elements.add(element);
    }
    
    private String determineType(JsonNode node) {
        if (node.isTextual()) {
            return "String";
        } else if (node.isNumber()) {
            return node.isInt() ? "Integer" : "Double";
        } else if (node.isBoolean()) {
            return "Boolean";
        } else if (node.isObject()) {
            return "Object";
        } else if (node.isArray()) {
            return "List";
        }
        return "String";
    }
    
    private double calculateSimilarity(List<ElementDefinition> elements) {
        if (elements.size() < 2) return 1.0;
        
        ElementDefinition first = elements.get(0);
        double totalSimilarity = 0.0;
        
        for (int i = 1; i < elements.size(); i++) {
            ElementDefinition current = elements.get(i);
            double similarity = compareElements(first, current);
            totalSimilarity += similarity;
        }
        
        return totalSimilarity / (elements.size() - 1);
    }
    
    private double compareElements(ElementDefinition elem1, ElementDefinition elem2) {
        double score = 0.0;
        double maxScore = 4.0;
        
        if (elem1.getName().equals(elem2.getName())) score += 1.0;
        if (elem1.getType().equals(elem2.getType())) score += 1.0;
        if (elem1.getChildren().size() == elem2.getChildren().size()) score += 1.0;
        if (elem1.getAttributes().size() == elem2.getAttributes().size()) score += 1.0;
        
        return score / maxScore;
    }
}