package com.aixml.mapping;

import com.aixml.analyzer.XmlStructureAnalyzer;
import com.aixml.analyzer.XmlSchema;
import com.aixml.analyzer.ElementDefinition;
import com.aixml.detector.RelationshipDetector;
import com.aixml.detector.OneToManyRelation;
import com.aixml.detector.ManyToManyRelation;
import com.aixml.detector.ParentChildRelation;
import com.aixml.generator.DynamicObjectGenerator;
import com.aixml.generator.ClassFileGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Component
@Slf4j
public class XmlToJavaConverter {
    
    @Autowired
    private XmlStructureAnalyzer analyzer;
    
    @Autowired
    private RelationshipDetector relationshipDetector;
    
    @Autowired
    private DynamicObjectGenerator objectGenerator;
    
    @Autowired
    private ClassFileGenerator classFileGenerator;
    
    public <T> T convertXmlToObject(String xmlContent, Class<T> targetClass) {
        try {
            XmlSchema schema = analyzer.analyzeStructure(xmlContent);
            
            Map<String, Object> properties = extractProperties(xmlContent, schema);
            
            return targetClass.cast(objectGenerator.createInstance(targetClass, properties));
            
        } catch (Exception e) {
            log.error("Error converting XML to object", e);
            throw new RuntimeException("Conversion failed", e);
        }
    }
    
    public ConversionResult convertXmlToObjects(String xmlContent) {
        try {
            XmlSchema schema = analyzer.analyzeStructure(xmlContent);
            
            List<Class<?>> generatedClasses = generateClassesFromXml(xmlContent, "com.generated.model");
            
            List<OneToManyRelation> oneToManyRelations = relationshipDetector.detectOneToMany(schema);
            List<ManyToManyRelation> manyToManyRelations = relationshipDetector.detectManyToMany(schema);
            List<ParentChildRelation> parentChildRelations = relationshipDetector.detectHierarchical(schema);
            
            Object rootObject = createRootObject(xmlContent, schema, generatedClasses);
            
            return ConversionResult.builder()
                    .rootObject(rootObject)
                    .generatedClasses(generatedClasses)
                    .xmlSchema(schema)
                    .oneToManyRelations(oneToManyRelations)
                    .manyToManyRelations(manyToManyRelations)
                    .parentChildRelations(parentChildRelations)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error converting XML to objects", e);
            throw new RuntimeException("Conversion failed", e);
        }
    }
    
    public List<Class<?>> generateClassesFromXml(String xmlContent, String packageName) {
        try {
            XmlSchema schema = analyzer.analyzeStructure(xmlContent);
            
            for (ElementDefinition element : schema.getElements()) {
                if (element.getParentElement() == null || isComplexType(element)) {
                    objectGenerator.generateClass(element, packageName);
                }
            }
            
            return List.copyOf(objectGenerator.getGeneratedClasses().values());
            
        } catch (Exception e) {
            log.error("Error generating classes from XML", e);
            throw new RuntimeException("Class generation failed", e);
        }
    }
    
    public List<String> generateClassFilesFromXml(String xmlContent, String packageName, String outputDir) {
        try {
            XmlSchema schema = analyzer.analyzeStructure(xmlContent);
            
            log.info("Generating class files for package: {} in directory: {}", packageName, outputDir);
            
            // Clean previous generated files
            classFileGenerator.cleanGeneratedDirectory(outputDir, packageName);
            
            // Generate new class files
            List<String> generatedFiles = classFileGenerator.generateClassFiles(
                schema.getElements(), packageName, outputDir);
            
            log.info("Generated {} class files", generatedFiles.size());
            return generatedFiles;
            
        } catch (Exception e) {
            log.error("Error generating class files from XML", e);
            throw new RuntimeException("Class file generation failed", e);
        }
    }
    
    public ConversionResultWithFiles convertXmlToObjectsAndFiles(String xmlContent, String packageName, String outputDir) {
        try {
            // Generate the schema and relationships as usual
            ConversionResult result = convertXmlToObjects(xmlContent);
            
            // Also generate physical class files
            List<String> generatedFiles = generateClassFilesFromXml(xmlContent, packageName, outputDir);
            
            return ConversionResultWithFiles.builder()
                    .conversionResult(result)
                    .generatedFiles(generatedFiles)
                    .outputDirectory(outputDir)
                    .packageName(packageName)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error in complete conversion with files", e);
            throw new RuntimeException("Complete conversion failed", e);
        }
    }
    
    private Object createRootObject(String xmlContent, XmlSchema schema, List<Class<?>> generatedClasses) {
        try {
            Class<?> rootClass = generatedClasses.stream()
                    .filter(clazz -> clazz.getSimpleName().equalsIgnoreCase(schema.getRootElementName()))
                    .findFirst()
                    .orElse(null);
            
            if (rootClass != null) {
                Map<String, Object> properties = extractProperties(xmlContent, schema);
                return objectGenerator.createInstance(rootClass, properties);
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("Error creating root object", e);
            return null;
        }
    }
    
    private Map<String, Object> extractProperties(String xmlContent, XmlSchema schema) {
        Map<String, Object> properties = new HashMap<>();
        
        for (ElementDefinition element : schema.getElements()) {
            if (element.getParentElement() == null) {
                properties.put(element.getName(), element.getName() + "_value");
            }
        }
        
        return properties;
    }
    
    private boolean isComplexType(ElementDefinition element) {
        return element.getChildren() != null && !element.getChildren().isEmpty() ||
               element.getAttributes() != null && !element.getAttributes().isEmpty();
    }
}