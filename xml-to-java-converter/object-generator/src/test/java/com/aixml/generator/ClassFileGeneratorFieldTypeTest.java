package com.aixml.generator;

public class ClassFileGeneratorFieldTypeTest {
    
    public static void main(String[] args) {
        ClassFileGenerator generator = new ClassFileGenerator();
        
        // Use reflection to access private method
        try {
            java.lang.reflect.Method method = ClassFileGenerator.class.getDeclaredMethod("getFieldType", String.class);
            method.setAccessible(true);
            
            String result = (String) method.invoke(generator, "author");
            System.out.println("getFieldType('author') returns: " + result);
            System.out.println("Expected: Author, Actual: " + result + ", Match: " + result.equals("Author"));
            
            String result2 = (String) method.invoke(generator, "book");
            System.out.println("getFieldType('book') returns: " + result2);
            System.out.println("Expected: Book, Actual: " + result2 + ", Match: " + result2.equals("Book"));
            
            String result3 = (String) method.invoke(generator, "category");
            System.out.println("getFieldType('category') returns: " + result3);
            System.out.println("Expected: Category, Actual: " + result3 + ", Match: " + result3.equals("Category"));
            
        } catch (Exception e) {
            System.err.println("Failed to test getFieldType: " + e.getMessage());
            e.printStackTrace();
        }
    }
}