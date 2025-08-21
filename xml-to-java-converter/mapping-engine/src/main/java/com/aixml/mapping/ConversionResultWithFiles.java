package com.aixml.mapping;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ConversionResultWithFiles {
    private ConversionResult conversionResult;
    private List<String> generatedFiles;
    private String outputDirectory;
    private String packageName;
    
    public int getGeneratedFileCount() {
        return generatedFiles != null ? generatedFiles.size() : 0;
    }
    
    public String getGeneratedFilesSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Generated ").append(getGeneratedFileCount()).append(" files in ")
          .append(outputDirectory).append(":\n");
        
        if (generatedFiles != null) {
            for (String file : generatedFiles) {
                sb.append("  - ").append(file).append("\n");
            }
        }
        
        return sb.toString();
    }
}