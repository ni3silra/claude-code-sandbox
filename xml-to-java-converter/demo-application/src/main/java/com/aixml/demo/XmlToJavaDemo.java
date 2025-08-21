package com.aixml.demo;

import com.aixml.mapping.XmlToJavaConverter;
import com.aixml.mapping.ConversionResult;
import com.aixml.mapping.ConversionResultWithFiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
@ComponentScan(basePackages = {"com.aixml"})
@Slf4j
public class XmlToJavaDemo {
    
    @Autowired
    private XmlToJavaConverter converter;
    
    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(XmlToJavaDemo.class);
        XmlToJavaDemo demo = context.getBean(XmlToJavaDemo.class);
        demo.demonstrateConversion();
        context.close();
    }
    
    public void demonstrateConversion() {
        try {
            log.info("Starting XML to Java conversion demonstration...");
            
            String xmlContent = loadXmlFromResource("sample-library.xml");
            
            ConversionResult result = converter.convertXmlToObjects(xmlContent);
            
            displayConversionResults(result);
            
            demonstrateRelationships(result);
            
            demonstrateFileGeneration(xmlContent);
            
            log.info("Demonstration completed successfully!");
            
        } catch (Exception e) {
            log.error("Demo execution failed", e);
        }
    }
    
    private String loadXmlFromResource(String filename) throws IOException {
        try {
            return new String(Files.readAllBytes(
                Paths.get(getClass().getClassLoader().getResource(filename).toURI())
            ));
        } catch (Exception e) {
            log.error("Could not load XML file: " + filename, e);
            throw new RuntimeException("Failed to load XML resource", e);
        }
    }
    
    private void displayConversionResults(ConversionResult result) {
        System.out.println("\n=== XML to Java Conversion Results ===");
        
        System.out.println("\nGenerated Classes:");
        result.getGeneratedClasses().forEach(clazz -> 
            System.out.println("- " + clazz.getSimpleName())
        );
        
        System.out.println("\nDetected Relationships:");
        result.getRelationships().forEach(relation -> 
            System.out.println("- " + relation.toString())
        );
        
        System.out.println("\nRoot Object: " + 
            (result.getRootObject() != null ? result.getRootObject().getClass().getSimpleName() : "null"));
        
        System.out.println("\nXML Schema Analysis:");
        System.out.println("- Root Element: " + result.getXmlSchema().getRootElementName());
        System.out.println("- Total Elements: " + result.getXmlSchema().getElements().size());
        System.out.println("- Detected Patterns: " + result.getXmlSchema().getPatterns().size());
    }
    
    private void demonstrateRelationships(ConversionResult result) {
        System.out.println("\n=== Relationship Analysis ===");
        
        System.out.println("\nOne-to-Many Relationships:");
        result.getOneToManyRelations().forEach(relation -> 
            System.out.println("- " + relation.getDescription())
        );
        
        System.out.println("\nMany-to-Many Relationships:");
        result.getManyToManyRelations().forEach(relation -> 
            System.out.println("- " + relation.getDescription())
        );
        
        System.out.println("\nParent-Child Relationships:");
        result.getParentChildRelations().forEach(relation -> 
            System.out.println("- " + relation.getDescription())
        );
        
        if (result.getRootObject() != null) {
            displayObjectHierarchy(result.getRootObject(), 0);
        }
    }
    
    private void displayObjectHierarchy(Object obj, int depth) {
        if (depth > 3) return;
        
        String indent = "  ".repeat(depth);
        System.out.println(indent + "Object: " + obj.getClass().getSimpleName());
        
        try {
            java.lang.reflect.Field[] fields = obj.getClass().getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value != null) {
                    System.out.println(indent + "  " + field.getName() + ": " + 
                        (value instanceof String ? value : value.getClass().getSimpleName()));
                    
                    if (value instanceof java.util.Collection) {
                        ((java.util.Collection<?>) value).forEach(item -> 
                            displayObjectHierarchy(item, depth + 1)
                        );
                    } else if (!isPrimitiveOrWrapper(value.getClass())) {
                        displayObjectHierarchy(value, depth + 1);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not inspect object hierarchy for: " + obj.getClass().getSimpleName());
        }
    }
    
    private boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() || 
               clazz == String.class || 
               clazz == Integer.class || 
               clazz == Double.class || 
               clazz == Boolean.class ||
               clazz == Long.class ||
               clazz == Float.class;
    }
    
    private void demonstrateFileGeneration(String xmlContent) {
        System.out.println("\n=== File Generation Demonstration ===");
        
        try {
            String outputDir = "generated";
            String packageName = "com.generated.model";
            
            ConversionResultWithFiles fileResult = converter.convertXmlToObjectsAndFiles(
                xmlContent, packageName, outputDir);
            
            System.out.println("\nGenerated Class Files:");
            System.out.println("Package: " + fileResult.getPackageName());
            System.out.println("Output Directory: " + fileResult.getOutputDirectory());
            System.out.println("Files Generated: " + fileResult.getGeneratedFileCount());
            
            for (String filePath : fileResult.getGeneratedFiles()) {
                System.out.println("  - " + filePath);
            }
            
            // Also demonstrate simple file generation
            System.out.println("\n=== Simple File Generation ===");
            java.util.List<String> simpleFiles = converter.generateClassFilesFromXml(
                xmlContent, "com.simple.generated", "generated-simple");
            
            System.out.println("Simple generation created " + simpleFiles.size() + " files:");
            for (String file : simpleFiles) {
                System.out.println("  - " + file);
            }
            
            // Try to read and display one of the generated files
            if (!fileResult.getGeneratedFiles().isEmpty()) {
                displayGeneratedFileContent(fileResult.getGeneratedFiles().get(0));
            }
            
        } catch (Exception e) {
            System.out.println("File generation demonstration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void displayGeneratedFileContent(String filePath) {
        System.out.println("\n=== Generated File Content ===");
        System.out.println("File: " + filePath);
        System.out.println("Content:");
        
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(filePath);
            if (java.nio.file.Files.exists(path)) {
                java.util.List<String> lines = java.nio.file.Files.readAllLines(path);
                for (int i = 0; i < Math.min(lines.size(), 30); i++) {
                    System.out.println((i + 1) + ": " + lines.get(i));
                }
                if (lines.size() > 30) {
                    System.out.println("... (" + (lines.size() - 30) + " more lines)");
                }
            } else {
                System.out.println("File not found: " + filePath);
            }
        } catch (Exception e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }
}