package com.aixml.mapping;

import com.aixml.analyzer.XmlStructureAnalyzer;
import com.aixml.detector.RelationshipDetector;
import com.aixml.generator.DynamicObjectGenerator;
import com.aixml.generator.ClassFileGenerator;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.*;

public class FileGenerationTest {
    
    private XmlToJavaConverter converter;
    private String testOutputDir = "test-generated";
    
    @Before
    public void setUp() {
        XmlStructureAnalyzer analyzer = new XmlStructureAnalyzer();
        RelationshipDetector relationshipDetector = new RelationshipDetector();
        DynamicObjectGenerator objectGenerator = new DynamicObjectGenerator();
        ClassFileGenerator classFileGenerator = new ClassFileGenerator();
        
        converter = new XmlToJavaConverter();
        setField(converter, "analyzer", analyzer);
        setField(converter, "relationshipDetector", relationshipDetector);
        setField(converter, "objectGenerator", objectGenerator);
        setField(converter, "classFileGenerator", classFileGenerator);
    }
    
    @After
    public void tearDown() {
        // Clean up test files
        try {
            Path testDir = Paths.get(testOutputDir);
            if (Files.exists(testDir)) {
                Files.walk(testDir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (Exception e) {
                            // Ignore cleanup errors
                        }
                    });
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
    
    @Test
    public void testGenerateClassFiles() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<book id=\"1\">" +
                "<title>Java Programming</title>" +
                "<author>John Doe</author>" +
                "</book>";
        
        try {
            List<String> generatedFiles = converter.generateClassFilesFromXml(
                xml, "com.test.generated", testOutputDir);
            
            assertNotNull("Generated files list should not be null", generatedFiles);
            assertTrue("Should generate at least one file", generatedFiles.size() > 0);
            
            // Verify file exists
            for (String filePath : generatedFiles) {
                Path path = Paths.get(filePath);
                assertTrue("Generated file should exist: " + filePath, Files.exists(path));
                assertTrue("Generated file should not be empty", Files.size(path) > 0);
                
                // Verify it's a Java file
                assertTrue("Should be a .java file", filePath.endsWith(".java"));
                
                System.out.println("Generated file: " + filePath);
            }
            
        } catch (Exception e) {
            fail("File generation should not throw exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Test
    public void testGenerateClassFileContent() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<person id=\"123\">" +
                "<name>John Doe</name>" +
                "<age>30</age>" +
                "</person>";
        
        try {
            List<String> generatedFiles = converter.generateClassFilesFromXml(
                xml, "com.example.model", testOutputDir);
            
            assertTrue("Should generate at least one file", generatedFiles.size() > 0);
            
            String filePath = generatedFiles.get(0);
            Path path = Paths.get(filePath);
            List<String> lines = Files.readAllLines(path);
            String content = String.join("\n", lines);
            
            // Verify package declaration
            assertTrue("Should contain package declaration", 
                content.contains("package com.example.model;"));
            
            // Verify class declaration
            assertTrue("Should contain class declaration", 
                content.contains("public class Person"));
            
            // Verify XML annotations
            assertTrue("Should contain XmlRootElement annotation", 
                content.contains("@XmlRootElement"));
            
            // Verify it doesn't contain getters/setters (as requested)
            assertFalse("Should not contain getter methods", 
                content.contains("public String get"));
            assertFalse("Should not contain setter methods", 
                content.contains("public void set"));
            
            // Verify fields exist
            assertTrue("Should contain id field", 
                content.contains("private String id;"));
            
            System.out.println("Generated file content validation passed!");
            System.out.println("File: " + filePath);
            
        } catch (Exception e) {
            fail("File content validation should not throw exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Test
    public void testConvertXmlToObjectsAndFiles() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<library>" +
                "<book>" +
                "<title>Java Programming</title>" +
                "<author>John Doe</author>" +
                "</book>" +
                "</library>";
        
        try {
            ConversionResultWithFiles result = converter.convertXmlToObjectsAndFiles(
                xml, "com.library.model", testOutputDir);
            
            assertNotNull("Conversion result should not be null", result);
            assertNotNull("Conversion result should contain base result", result.getConversionResult());
            assertNotNull("Should have generated files", result.getGeneratedFiles());
            assertTrue("Should generate at least one file", result.getGeneratedFileCount() > 0);
            assertEquals("Package name should match", "com.library.model", result.getPackageName());
            assertEquals("Output directory should match", testOutputDir, result.getOutputDirectory());
            
            // Verify files exist
            for (String filePath : result.getGeneratedFiles()) {
                assertTrue("Generated file should exist: " + filePath, 
                    Files.exists(Paths.get(filePath)));
            }
            
            System.out.println("Complete conversion test passed!");
            System.out.println(result.getGeneratedFilesSummary());
            
        } catch (Exception e) {
            fail("Complete conversion should not throw exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Test
    public void testComplexXmlFileGeneration() {
        String complexXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<company>" +
                "<name>Tech Corp</name>" +
                "<employees>" +
                "<employee id=\"1\">" +
                "<name>John Doe</name>" +
                "<position>Developer</position>" +
                "</employee>" +
                "<employee id=\"2\">" +
                "<name>Jane Smith</name>" +
                "<position>Manager</position>" +
                "</employee>" +
                "</employees>" +
                "<departments>" +
                "<department>" +
                "<name>Engineering</name>" +
                "<budget>100000</budget>" +
                "</department>" +
                "</departments>" +
                "</company>";
        
        try {
            List<String> generatedFiles = converter.generateClassFilesFromXml(
                complexXml, "com.company.model", testOutputDir);
            
            assertTrue("Should generate files for complex XML", generatedFiles.size() > 0);
            
            // Verify each generated file
            for (String filePath : generatedFiles) {
                Path path = Paths.get(filePath);
                assertTrue("File should exist: " + filePath, Files.exists(path));
                
                List<String> lines = Files.readAllLines(path);
                String content = String.join("\n", lines);
                
                // Basic validations
                assertTrue("Should contain package declaration", 
                    content.contains("package com.company.model;"));
                assertTrue("Should contain class declaration", 
                    content.contains("public class "));
                assertTrue("Should contain toString method", 
                    content.contains("public String toString()"));
                
                System.out.println("Complex XML file validated: " + filePath);
            }
            
        } catch (Exception e) {
            fail("Complex XML file generation should not throw exception: " + e.getMessage());
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