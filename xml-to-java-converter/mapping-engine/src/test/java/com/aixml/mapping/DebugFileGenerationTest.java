package com.aixml.mapping;

import com.aixml.analyzer.XmlStructureAnalyzer;
import com.aixml.analyzer.XmlSchema;
import com.aixml.analyzer.ElementDefinition;
import com.aixml.detector.RelationshipDetector;
import com.aixml.generator.DynamicObjectGenerator;
import com.aixml.generator.ClassFileGenerator;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class DebugFileGenerationTest {
    
    private XmlToJavaConverter converter;
    private XmlStructureAnalyzer analyzer;
    
    @Before
    public void setUp() {
        analyzer = new XmlStructureAnalyzer();
        RelationshipDetector relationshipDetector = new RelationshipDetector();
        DynamicObjectGenerator objectGenerator = new DynamicObjectGenerator();
        ClassFileGenerator classFileGenerator = new ClassFileGenerator();
        
        converter = new XmlToJavaConverter();
        setField(converter, "analyzer", analyzer);
        setField(converter, "relationshipDetector", relationshipDetector);
        setField(converter, "objectGenerator", objectGenerator);
        setField(converter, "classFileGenerator", classFileGenerator);
    }
    
    @Test
    public void debugXmlParsing() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<person id=\"123\">" +
                "<name>John Doe</name>" +
                "<age>30</age>" +
                "</person>";
        
        try {
            System.out.println("=== Debug XML Parsing ===");
            System.out.println("Input XML: " + xml);
            
            XmlSchema schema = analyzer.analyzeStructure(xml);
            
            System.out.println("Root element: " + schema.getRootElementName());
            System.out.println("Elements count: " + schema.getElements().size());
            System.out.println("Element frequency: " + schema.getElementFrequency());
            
            for (int i = 0; i < schema.getElements().size(); i++) {
                ElementDefinition element = schema.getElements().get(i);
                System.out.println("Element " + i + ":");
                System.out.println("  Name: " + element.getName());
                System.out.println("  Type: " + element.getType());
                System.out.println("  Parent: " + element.getParentElement());
                System.out.println("  Attributes: " + element.getAttributes());
                System.out.println("  Children: " + (element.getChildren() != null ? element.getChildren().size() : 0));
                if (element.getChildren() != null) {
                    for (ElementDefinition child : element.getChildren()) {
                        System.out.println("    Child: " + child.getName() + " (type: " + child.getType() + ")");
                    }
                }
            }
            
            // Now test file generation
            System.out.println("\n=== File Generation ===");
            List<String> generatedFiles = converter.generateClassFilesFromXml(
                xml, "com.debug.test", "debug-output");
            
            System.out.println("Generated " + generatedFiles.size() + " files:");
            for (String filePath : generatedFiles) {
                System.out.println("  - " + filePath);
                
                Path path = Paths.get(filePath);
                if (Files.exists(path)) {
                    List<String> lines = Files.readAllLines(path);
                    System.out.println("File content:");
                    for (int i = 0; i < lines.size(); i++) {
                        System.out.println((i + 1) + ": " + lines.get(i));
                    }
                } else {
                    System.out.println("File not found: " + filePath);
                }
            }
            
        } catch (Exception e) {
            System.out.println("Debug test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Helper method to set private fields using reflection
    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}