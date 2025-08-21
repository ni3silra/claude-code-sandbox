package com.aixml.mapping;

import com.aixml.analyzer.XmlSchema;
import com.aixml.detector.OneToManyRelation;
import com.aixml.detector.ManyToManyRelation;
import com.aixml.detector.ParentChildRelation;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ConversionResult {
    private Object rootObject;
    private List<Class<?>> generatedClasses;
    private XmlSchema xmlSchema;
    private List<OneToManyRelation> oneToManyRelations;
    private List<ManyToManyRelation> manyToManyRelations;
    private List<ParentChildRelation> parentChildRelations;
    
    public List<Object> getRelationships() {
        List<Object> allRelations = new java.util.ArrayList<>();
        if (oneToManyRelations != null) allRelations.addAll(oneToManyRelations);
        if (manyToManyRelations != null) allRelations.addAll(manyToManyRelations);
        if (parentChildRelations != null) allRelations.addAll(parentChildRelations);
        return allRelations;
    }
}