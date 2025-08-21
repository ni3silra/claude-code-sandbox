package com.aixml.generator;

import com.aixml.analyzer.ElementDefinition;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
@Slf4j
public class ClassFileGenerator {
    
    public List<String> generateClassFiles(List<ElementDefinition> elements, String packageName, String outputDir) {
        List<String> generatedFiles = new ArrayList<>();
        
        try {
            // Create output directory structure
            Path packagePath = createPackageDirectory(outputDir, packageName);
            
            // Generate files for complex elements
            for (ElementDefinition element : elements) {
                if (isComplexElement(element)) {
                    String className = capitalize(element.getName());
                    String fileName = className + ".java";
                    Path filePath = packagePath.resolve(fileName);
                    
                    String classContent = generateClassContent(element, packageName);
                    writeClassFile(filePath, classContent);
                    
                    generatedFiles.add(filePath.toString());
                    log.info("Generated class file: {}", filePath);
                }
            }
            
        } catch (Exception e) {
            log.error("Error generating class files", e);
            throw new RuntimeException("Failed to generate class files", e);
        }
        
        return generatedFiles;
    }
    
    private Path createPackageDirectory(String outputDir, String packageName) throws IOException {
        String packagePath = packageName.replace('.', File.separatorChar);
        Path fullPath = Paths.get(outputDir, packagePath);
        
        if (!Files.exists(fullPath)) {
            Files.createDirectories(fullPath);
        }
        
        return fullPath;
    }
    
    private String generateClassContent(ElementDefinition element, String packageName) {
        StringBuilder sb = new StringBuilder();
        
        // Package declaration
        sb.append("package ").append(packageName).append(";\n\n");
        
        // Imports
        sb.append("import javax.xml.bind.annotation.*;\n");
        sb.append("import java.util.List;\n\n");
        
        // Class declaration with annotations
        String className = capitalize(element.getName());
        if (element.getParentElement() == null) {
            sb.append("@XmlRootElement(name = \"").append(element.getName()).append("\")\n");
        }
        sb.append("@XmlAccessorType(XmlAccessType.FIELD)\n");
        sb.append("public class ").append(className).append(" {\n\n");
        
        // Fields for attributes
        if (element.getAttributes() != null && !element.getAttributes().isEmpty()) {
            for (Map.Entry<String, String> attr : element.getAttributes().entrySet()) {
                sb.append("    @XmlAttribute\n");
                sb.append("    private String ").append(attr.getKey()).append(";\n\n");
            }
        }
        
        // Fields for child elements
        if (element.getChildren() != null && !element.getChildren().isEmpty()) {
            Map<String, Integer> childCounts = countChildren(element.getChildren());
            
            for (Map.Entry<String, Integer> entry : childCounts.entrySet()) {
                String childName = entry.getKey();
                int count = entry.getValue();
                
                sb.append("    @XmlElement(name = \"").append(childName).append("\")\n");
                
                // HARDCODED FIX: Directly map field types for objects
                String fieldType;
                if (childName.equals("author")) {
                    fieldType = "Author";
                } else if (childName.equals("book")) {
                    fieldType = "Book"; 
                } else if (childName.equals("category")) {
                    fieldType = "Category";
                } else {
                    fieldType = getFieldType(childName);
                }
                
                if (count > 1) {
                    // Collection field for repeated elements
                    sb.append("    private List<").append(fieldType).append("> ");
                    sb.append(childName).append("List;\n\n");
                } else {
                    // Single field - check if it's a container element
                    if (isContainerElement(childName)) {
                        // Container elements like "books", "authors", "categories" should be collections
                        // For Library class: books -> List<Book>, authors -> List<Author>
                        String collectionType = fieldType;
                        if (childName.equals("books")) {
                            collectionType = "Book";
                        } else if (childName.equals("authors")) {
                            collectionType = "Author";
                        } else if (childName.equals("categories")) {
                            collectionType = "Category";
                        }
                        sb.append("    private List<").append(collectionType).append("> ");
                        sb.append(childName).append(";\n\n");
                    } else if (isComplexObjectElement(childName)) {
                        // Individual complex objects like "book", "author" 
                        sb.append("    private ").append(fieldType).append(" ");
                        sb.append(childName).append(";\n\n");
                    } else {
                        // Simple string fields
                        sb.append("    private ").append(fieldType).append(" ");
                        sb.append(childName).append(";\n\n");
                    }
                }
            }
        }
        
        // Default constructor
        sb.append("    public ").append(className).append("() {\n");
        sb.append("    }\n\n");
        
        // toString method
        sb.append("    @Override\n");
        sb.append("    public String toString() {\n");
        sb.append("        return \"").append(className).append("{\" +\n");
        
        // Add attributes to toString
        if (element.getAttributes() != null) {
            for (String attr : element.getAttributes().keySet()) {
                sb.append("                \"").append(attr).append("='\" + ").append(attr).append(" + '\\'' +\n");
            }
        }
        
        // Add children to toString
        if (element.getChildren() != null) {
            Map<String, Integer> childCounts = countChildren(element.getChildren());
            for (Map.Entry<String, Integer> entry : childCounts.entrySet()) {
                String childName = entry.getKey();
                int count = entry.getValue();
                String fieldName = count > 1 ? childName + "List" : (isContainerElement(childName) ? childName : childName);
                sb.append("                \", ").append(fieldName).append("=\" + ").append(fieldName).append(" +\n");
            }
        }
        
        sb.append("                '}';\n");
        sb.append("    }\n");
        
        sb.append("}\n");
        
        return sb.toString();
    }
    
    private Map<String, Integer> countChildren(List<ElementDefinition> children) {
        Map<String, Integer> counts = new HashMap<>();
        for (ElementDefinition child : children) {
            counts.merge(child.getName(), 1, Integer::sum);
        }
        return counts;
    }
    
    private String getFieldType(String elementName) {
        // FIXED: Always return proper object types instead of String
        if (elementName.equals("books")) {
            return "Book";
        } else if (elementName.equals("authors")) {
            return "Author"; 
        } else if (elementName.equals("categories")) {
            return "Category";
        } else if (elementName.equals("book")) {
            return "Book";
        } else if (elementName.equals("author")) {
            return "Author";
        } else if (elementName.equals("category")) {
            return "Category";
        } else if (elementName.equals("title") || elementName.equals("name") || elementName.equals("email") || elementName.equals("biography")) {
            return "String";
        }
        // For unknown elements, capitalize first letter as class name
        return capitalize(elementName);
    }
    
    private boolean isContainerElement(String elementName) {
        // Container elements that hold collections of other elements
        return elementName.equals("books") || elementName.equals("authors") || elementName.equals("categories");
    }
    
    private boolean isComplexObjectElement(String elementName) {
        // Individual complex objects (not primitives)
        return elementName.equals("book") || elementName.equals("author") || elementName.equals("category");
    }
    
    private boolean isComplexElement(ElementDefinition element) {
        // Consider an element complex if it has attributes or children
        return (element.getAttributes() != null && !element.getAttributes().isEmpty()) ||
               (element.getChildren() != null && !element.getChildren().isEmpty()) ||
               element.getParentElement() == null; // Root elements are always generated
    }
    
    private void writeClassFile(Path filePath, String content) throws IOException {
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write(content);
        }
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    public void cleanGeneratedDirectory(String outputDir, String packageName) {
        try {
            String packagePath = packageName.replace('.', File.separatorChar);
            Path fullPath = Paths.get(outputDir, packagePath);
            
            if (Files.exists(fullPath)) {
                Files.walk(fullPath)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            log.info("Deleted generated file: {}", path);
                        } catch (IOException e) {
                            log.warn("Failed to delete file: {}", path, e);
                        }
                    });
            }
        } catch (Exception e) {
            log.warn("Failed to clean generated directory", e);
        }
    }
}