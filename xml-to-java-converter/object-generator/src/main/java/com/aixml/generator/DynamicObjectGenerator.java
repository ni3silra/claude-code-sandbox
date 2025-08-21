package com.aixml.generator;

import com.aixml.analyzer.ElementDefinition;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FieldAccessor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAttribute;
import java.util.*;

@Service
@Slf4j
public class DynamicObjectGenerator {
    
    private final ByteBuddy byteBuddy = new ByteBuddy();
    private final Map<String, Class<?>> generatedClasses = new HashMap<>();
    
    public Class<?> generateClass(ElementDefinition definition, String packageName) {
        String className = packageName + "." + capitalize(definition.getName());
        
        if (generatedClasses.containsKey(className)) {
            return generatedClasses.get(className);
        }
        
        try {
            DynamicType.Builder<?> builder = byteBuddy
                    .subclass(Object.class)
                    .name(className);
            
            if (definition.getParentElement() == null) {
                builder = builder.annotateType(AnnotationDescription.Builder
                        .ofType(XmlRootElement.class)
                        .define("name", definition.getName())
                        .build());
            }
            
            builder = addFields(builder, definition);
            
            Class<?> generatedClass = builder
                    .make()
                    .load(getClass().getClassLoader())
                    .getLoaded();
            
            generatedClasses.put(className, generatedClass);
            return generatedClass;
            
        } catch (Exception e) {
            log.error("Error generating class for element: " + definition.getName(), e);
            throw new RuntimeException("Failed to generate class", e);
        }
    }
    
    public Object createInstance(Class<?> clazz, Map<String, Object> properties) {
        try {
            Object instance = clazz.getDeclaredConstructor().newInstance();
            
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                try {
                    String fieldName = entry.getKey();
                    Object value = entry.getValue();
                    
                    String setterName = "set" + capitalize(fieldName);
                    clazz.getMethod(setterName, value.getClass()).invoke(instance, value);
                    
                } catch (Exception e) {
                    log.warn("Could not set property {} on class {}", entry.getKey(), clazz.getSimpleName());
                }
            }
            
            return instance;
            
        } catch (Exception e) {
            log.error("Error creating instance of class: " + clazz.getSimpleName(), e);
            throw new RuntimeException("Failed to create instance", e);
        }
    }
    
    private DynamicType.Builder<?> addFields(DynamicType.Builder<?> builder, ElementDefinition definition) {
        if (definition.getAttributes() != null) {
            for (Map.Entry<String, String> attr : definition.getAttributes().entrySet()) {
                builder = builder
                        .defineField(attr.getKey(), String.class, Visibility.PRIVATE)
                        .annotateField(AnnotationDescription.Builder
                                .ofType(XmlAttribute.class)
                                .build())
                        .defineMethod("get" + capitalize(attr.getKey()), String.class, Visibility.PUBLIC)
                        .intercept(FieldAccessor.ofField(attr.getKey()))
                        .defineMethod("set" + capitalize(attr.getKey()), void.class, Visibility.PUBLIC)
                        .withParameters(String.class)
                        .intercept(FieldAccessor.ofField(attr.getKey()));
            }
        }
        
        if (definition.getChildren() != null) {
            for (ElementDefinition child : definition.getChildren()) {
                Class<?> fieldType = determineFieldType(child);
                
                builder = builder
                        .defineField(child.getName(), fieldType, Visibility.PRIVATE)
                        .annotateField(AnnotationDescription.Builder
                                .ofType(XmlElement.class)
                                .define("name", child.getName())
                                .build())
                        .defineMethod("get" + capitalize(child.getName()), fieldType, Visibility.PUBLIC)
                        .intercept(FieldAccessor.ofField(child.getName()))
                        .defineMethod("set" + capitalize(child.getName()), void.class, Visibility.PUBLIC)
                        .withParameters(fieldType)
                        .intercept(FieldAccessor.ofField(child.getName()));
            }
        }
        
        return builder;
    }
    
    private Class<?> determineFieldType(ElementDefinition definition) {
        switch (definition.getType()) {
            case "String":
                return String.class;
            case "Integer":
                return Integer.class;
            case "Double":
                return Double.class;
            case "Boolean":
                return Boolean.class;
            case "List":
                return List.class;
            default:
                return Object.class;
        }
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    public Map<String, Class<?>> getGeneratedClasses() {
        return new HashMap<>(generatedClasses);
    }
}