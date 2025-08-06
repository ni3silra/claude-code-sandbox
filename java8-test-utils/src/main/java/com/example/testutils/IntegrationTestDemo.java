package com.example.testutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IntegrationTestDemo {
    
    public static void main(String[] args) {
        IntegrationTestDemo demo = new IntegrationTestDemo();
        try {
            demo.runIntegrationTest();
            System.out.println("\n=== Integration Test Completed Successfully! ===");
        } catch (Exception e) {
            System.err.println("Integration test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void runIntegrationTest() throws Exception {
        System.out.println("=== Starting Integration Test Demo ===\n");
        
        DatabaseTestUtility dbUtil = new DatabaseTestUtility();
        MQTestUtility mqUtil = new MQTestUtility(true);
        SFTPTestUtility sftpUtil = new SFTPTestUtility();
        
        try {
            // Step 1: Database Setup and Initial Data
            System.out.println("Step 1: Setting up database and loading initial data...");
            dbUtil.setupFromSqlFile("test-data/init-database.sql");
            
            // Verify initial data was loaded
            List<Map<String, Object>> users = dbUtil.executeQuery("SELECT * FROM users");
            System.out.println("Loaded " + users.size() + " users into database");
            for (Map<String, Object> user : users) {
                System.out.println("  - User: " + user.get("USERNAME") + " (" + user.get("EMAIL") + ")");
            }
            System.out.println("✓ Database initialization completed\n");
            
            // Step 2: MQ Operations
            System.out.println("Step 2: Testing MQ operations...");
            mqUtil.setup();
            
            // Send message from file
            mqUtil.createQueue("test.order.queue");
            mqUtil.sendMessageFromFile("test.order.queue", "test-data/test-message.txt");
            System.out.println("✓ Message sent to queue from file");
            
            // Verify message was sent
            int messageCount = mqUtil.getMessageCount("test.order.queue");
            System.out.println("✓ Queue contains " + messageCount + " message(s)");
            
            // Receive and process message
            String receivedMessage = mqUtil.receiveMessage("test.order.queue");
            System.out.println("✓ Received message: " + (receivedMessage != null ? "Success" : "Failed"));
            System.out.println("✓ MQ operations completed\n");
            
            // Step 3: SFTP Operations
            System.out.println("Step 3: Testing SFTP operations...");
            sftpUtil.setup();
            
            // Upload file from text file
            sftpUtil.uploadFileFromTextFile("test-data/test-file-content.txt", "/uploaded-test-file.txt");
            System.out.println("✓ File uploaded to SFTP server");
            
            // Verify file exists
            boolean fileExists = sftpUtil.fileExists("/uploaded-test-file.txt");
            System.out.println("✓ File exists on server: " + fileExists);
            
            // Step 4: Download and verify file content
            System.out.println("Step 4: Downloading and verifying file content...");
            String downloadedContent = sftpUtil.downloadFileContent("/uploaded-test-file.txt");
            System.out.println("✓ File downloaded successfully");
            
            // Verify content matches
            boolean contentMatches = downloadedContent.contains("Test File Content for SFTP Operations");
            System.out.println("✓ Content verification: " + (contentMatches ? "Passed" : "Failed"));
            System.out.println("✓ SFTP operations completed\n");
            
            // Step 5: Database verification and updates
            System.out.println("Step 5: Updating database based on processed data...");
            
            // Simulate processing by updating order status
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("order_status", "PROCESSED");
            dbUtil.updateRecord("orders", updateData, "order_number = ?", "ORD-002");
            System.out.println("✓ Updated order status to PROCESSED");
            
            // Verify the update
            List<Map<String, Object>> updatedOrders = dbUtil.executeQuery(
                "SELECT * FROM orders WHERE order_number = ?", "ORD-002");
            if (!updatedOrders.isEmpty()) {
                String status = (String) updatedOrders.get(0).get("ORDER_STATUS");
                System.out.println("✓ Verified order status: " + status);
            }
            
            // Add a new processed record
            Map<String, Object> newOrder = new HashMap<>();
            newOrder.put("user_id", 1);
            newOrder.put("order_number", "ORD-004");
            newOrder.put("total_amount", 79.97);
            newOrder.put("order_status", "COMPLETED");
            dbUtil.insertRecord("orders", newOrder);
            System.out.println("✓ Inserted new processed order");
            
            // Final verification
            List<Map<String, Object>> allOrders = dbUtil.executeQuery("SELECT COUNT(*) as total FROM orders");
            Object totalOrders = allOrders.get(0).get("TOTAL");
            System.out.println("✓ Total orders in database: " + totalOrders);
            System.out.println("✓ Database operations completed\n");
            
            // Step 6: Cleanup verification
            System.out.println("Step 6: Final verification...");
            System.out.println("✓ Database connected: " + dbUtil.isConnected());
            System.out.println("✓ MQ connected: " + mqUtil.isConnected());
            System.out.println("✓ SFTP connected: " + sftpUtil.isConnected());
            
        } finally {
            // Cleanup resources
            System.out.println("\nCleaning up resources...");
            dbUtil.teardown();
            mqUtil.teardown();
            sftpUtil.teardown();
            System.out.println("✓ All resources cleaned up");
        }
    }
}