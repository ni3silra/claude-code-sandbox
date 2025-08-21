package com.aixml.mapping;

import com.aixml.analyzer.XmlStructureAnalyzer;
import com.aixml.detector.RelationshipDetector;
import com.aixml.generator.DynamicObjectGenerator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfiguration.class})
public class XmlToJavaConverterTest {
    
    private XmlToJavaConverter converter;
    private XmlStructureAnalyzer analyzer;
    private RelationshipDetector relationshipDetector;
    private DynamicObjectGenerator objectGenerator;
    
    @Before
    public void setUp() {
        analyzer = new XmlStructureAnalyzer();
        relationshipDetector = new RelationshipDetector();
        objectGenerator = new DynamicObjectGenerator();
        
        converter = new XmlToJavaConverter();
        setField(converter, "analyzer", analyzer);
        setField(converter, "relationshipDetector", relationshipDetector);
        setField(converter, "objectGenerator", objectGenerator);
    }
    
    @Test
    public void testConvertSimpleXmlToObjects() {
        String simpleXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<book id=\"1\">" +
                "<title>Java Programming</title>" +
                "<author>John Doe</author>" +
                "</book>";
        
        try {
            ConversionResult result = converter.convertXmlToObjects(simpleXml);
            
            assertNotNull("Conversion result should not be null", result);
            assertNotNull("XML schema should not be null", result.getXmlSchema());
            assertEquals("Root element should be 'book'", "book", result.getXmlSchema().getRootElementName());
            assertNotNull("Generated classes should not be null", result.getGeneratedClasses());
            assertNotNull("Relationships should not be null", result.getRelationships());
            
            System.out.println("Simple XML conversion test passed!");
            System.out.println("Root element: " + result.getXmlSchema().getRootElementName());
            System.out.println("Generated classes count: " + result.getGeneratedClasses().size());
            System.out.println("Relationships count: " + result.getRelationships().size());
            
        } catch (Exception e) {
            fail("Conversion should not throw exception: " + e.getMessage());
        }
    }
    
    @Test
    public void testConvertComplexXmlToObjects() {
        String complexXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<library>" +
                "<book id=\"1\">" +
                "<title>Java Programming</title>" +
                "<author>John Doe</author>" +
                "</book>" +
                "<book id=\"2\">" +
                "<title>Spring Framework</title>" +
                "<author>Jane Smith</author>" +
                "</book>" +
                "</library>";
        
        try {
            ConversionResult result = converter.convertXmlToObjects(complexXml);
            
            assertNotNull("Conversion result should not be null", result);
            assertNotNull("XML schema should not be null", result.getXmlSchema());
            assertEquals("Root element should be 'library'", "library", result.getXmlSchema().getRootElementName());
            assertTrue("Should have generated classes", result.getGeneratedClasses().size() > 0);
            
            System.out.println("Complex XML conversion test passed!");
            System.out.println("Root element: " + result.getXmlSchema().getRootElementName());
            System.out.println("Generated classes count: " + result.getGeneratedClasses().size());
            System.out.println("One-to-many relations: " + result.getOneToManyRelations().size());
            System.out.println("Parent-child relations: " + result.getParentChildRelations().size());
            
        } catch (Exception e) {
            System.out.println("Exception details: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            fail("Complex XML conversion should not throw exception: " + e.getMessage());
        }
    }
    
    @Test
    public void testGenerateClassesFromXml() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<person id=\"1\">" +
                "<name>John Doe</name>" +
                "<age>30</age>" +
                "</person>";
        
        try {
            java.util.List<Class<?>> generatedClasses = converter.generateClassesFromXml(xml, "com.test.generated");
            
            assertNotNull("Generated classes should not be null", generatedClasses);
            assertTrue("Should generate at least one class", generatedClasses.size() > 0);
            
            System.out.println("Class generation test passed!");
            System.out.println("Generated classes count: " + generatedClasses.size());
            
        } catch (Exception e) {
            fail("Class generation should not throw exception: " + e.getMessage());
        }
    }
    
    @Test
    public void testInvalidXml() {
        String invalidXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<invalid><unclosed>";
        
        try {
            ConversionResult result = converter.convertXmlToObjects(invalidXml);
            fail("Should throw exception for invalid XML");
        } catch (RuntimeException e) {
            assertTrue("Should contain error message about XML parsing", 
                e.getMessage().contains("Conversion failed") || e.getMessage().contains("Failed to analyze"));
            System.out.println("Invalid XML test passed - correctly threw exception: " + e.getMessage());
        }
    }
    
    @Test
    public void testEmptyXml() {
        String emptyXml = "";
        
        try {
            ConversionResult result = converter.convertXmlToObjects(emptyXml);
            fail("Should throw exception for empty XML");
        } catch (RuntimeException e) {
            System.out.println("Empty XML test passed - correctly threw exception: " + e.getMessage());
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