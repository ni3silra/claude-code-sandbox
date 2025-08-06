package com.example.testutils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class AllUtilitiesTest {
    
    private DatabaseTestUtility dbUtil;
    private MQTestUtility mqUtil;
    private SFTPTestUtility sftpUtil;
    
    @Before
    public void setUp() throws Exception {
        dbUtil = new DatabaseTestUtility();
        mqUtil = new MQTestUtility(true);
        sftpUtil = new SFTPTestUtility();
        
        dbUtil.setup();
        mqUtil.setup();
        sftpUtil.setup();
    }
    
    @After
    public void tearDown() {
        if (dbUtil != null) dbUtil.teardown();
        if (mqUtil != null) mqUtil.teardown();
        if (sftpUtil != null) sftpUtil.teardown();
    }
    
    @Test
    public void testDatabaseUtility() throws Exception {
        // Test table creation and data insertion
        dbUtil.executeUpdate("CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(50))");
        dbUtil.executeUpdate("INSERT INTO test_table (id, name) VALUES (?, ?)", 1, "Test User");
        
        // Test data retrieval
        List<Map<String, Object>> results = dbUtil.executeQuery("SELECT * FROM test_table WHERE id = ?", 1);
        assertFalse("Should have results", results.isEmpty());
        assertEquals("Test User", results.get(0).get("NAME"));
        
        // Test record existence
        assertTrue("Record should exist", dbUtil.recordExists("test_table", "id = ?", 1));
        assertFalse("Record should not exist", dbUtil.recordExists("test_table", "id = ?", 999));
        
        // Test update operation
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("name", "Updated User");
        dbUtil.updateRecord("test_table", updateData, "id = ?", 1);
        
        List<Map<String, Object>> updatedResults = dbUtil.executeQuery("SELECT * FROM test_table WHERE id = ?", 1);
        assertEquals("Updated User", updatedResults.get(0).get("NAME"));
        
        // Test delete operation
        dbUtil.deleteRecord("test_table", "id = ?", 1);
        assertFalse("Record should be deleted", dbUtil.recordExists("test_table", "id = ?", 1));
    }
    
    @Test
    public void testMQUtility() throws Exception {
        String queueName = "test.queue";
        String testMessage = "Test message content";
        
        // Test queue creation and message sending
        mqUtil.createQueue(queueName);
        mqUtil.sendMessage(queueName, testMessage);
        
        // Test message count
        assertEquals("Should have 1 message", 1, mqUtil.getMessageCount(queueName));
        assertFalse("Queue should not be empty", mqUtil.isQueueEmpty(queueName));
        
        // Test message receiving
        String receivedMessage = mqUtil.receiveMessage(queueName);
        assertEquals("Message content should match", testMessage, receivedMessage);
        
        // Test queue is empty after receiving
        assertTrue("Queue should be empty", mqUtil.isQueueEmpty(queueName));
        
        // Test multiple messages
        mqUtil.sendMessage(queueName, "Message 1");
        mqUtil.sendMessage(queueName, "Message 2");
        assertEquals("Should have 2 messages", 2, mqUtil.getMessageCount(queueName));
        
        List<String> allMessages = mqUtil.receiveAllMessages(queueName);
        assertEquals("Should receive 2 messages", 2, allMessages.size());
        assertTrue("Should contain Message 1", allMessages.contains("Message 1"));
        assertTrue("Should contain Message 2", allMessages.contains("Message 2"));
    }
    
    @Test
    public void testSFTPUtility() throws Exception {
        String testContent = "This is test file content\nLine 2\nLine 3";
        String remoteFilePath = "/test-upload.txt";
        
        // Test file upload
        sftpUtil.uploadFileContent(testContent, remoteFilePath);
        assertTrue("File should exist after upload", sftpUtil.fileExists(remoteFilePath));
        
        // Test file download
        String downloadedContent = sftpUtil.downloadFileContent(remoteFilePath);
        // Normalize line endings for comparison
        String normalizedTestContent = testContent.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
        String normalizedDownloaded = downloadedContent.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
        assertEquals("Downloaded content should match", normalizedTestContent, normalizedDownloaded);
        
        // Test file size
        long fileSize = sftpUtil.getFileSize(remoteFilePath);
        assertTrue("File size should be greater than 0", fileSize > 0);
        
        // Test directory operations
        String testDir = "/testdir";
        sftpUtil.createDirectory(testDir);
        
        // Upload file to directory
        String dirFilePath = testDir + "/dir-file.txt";
        sftpUtil.uploadFileContent("Directory file content", dirFilePath);
        
        // Test file listing
        List<String> files = sftpUtil.listFiles(testDir);
        assertTrue("Directory should contain the uploaded file", files.contains("dir-file.txt"));
        
        // Test file deletion
        sftpUtil.deleteFile(remoteFilePath);
        assertFalse("File should not exist after deletion", sftpUtil.fileExists(remoteFilePath));
        
        // Cleanup directory
        sftpUtil.deleteFile(dirFilePath);
        sftpUtil.removeDirectory(testDir);
    }
    
    @Test
    public void testIntegratedWorkflow() throws Exception {
        // Step 1: Initialize database
        dbUtil.executeUpdate("CREATE TABLE workflow_test (id INT PRIMARY KEY, status VARCHAR(20), data TEXT)");
        
        Map<String, Object> initialData = new HashMap<>();
        initialData.put("id", 1);
        initialData.put("status", "PENDING");
        initialData.put("data", "Initial data");
        dbUtil.insertRecord("workflow_test", initialData);
        
        // Step 2: Send message via MQ
        String queueName = "workflow.queue";
        mqUtil.createQueue(queueName);
        mqUtil.sendMessage(queueName, "Process record ID: 1");
        
        // Step 3: Upload file via SFTP
        String fileContent = "Workflow test file\nRecord ID: 1\nStatus: Processing";
        sftpUtil.uploadFileContent(fileContent, "/workflow-file.txt");
        
        // Step 4: Simulate processing - receive message
        String message = mqUtil.receiveMessage(queueName);
        assertNotNull("Should receive processing message", message);
        assertTrue("Message should contain record ID", message.contains("ID: 1"));
        
        // Step 5: Download and verify file
        String downloadedFile = sftpUtil.downloadFileContent("/workflow-file.txt");
        assertTrue("File should contain record ID", downloadedFile.contains("Record ID: 1"));
        
        // Step 6: Update database based on processing
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("status", "COMPLETED");
        updateData.put("data", "Processed data from workflow");
        dbUtil.updateRecord("workflow_test", updateData, "id = ?", 1);
        
        // Step 7: Verify final state
        List<Map<String, Object>> results = dbUtil.executeQuery("SELECT * FROM workflow_test WHERE id = ?", 1);
        assertEquals("Status should be updated", "COMPLETED", results.get(0).get("STATUS"));
        
        assertTrue("Queue should be empty", mqUtil.isQueueEmpty(queueName));
        assertTrue("File should exist", sftpUtil.fileExists("/workflow-file.txt"));
        
        // Cleanup
        sftpUtil.deleteFile("/workflow-file.txt");
    }
    
    @Test
    public void testUtilityConnections() {
        assertTrue("Database should be connected", dbUtil.isConnected());
        assertTrue("MQ should be connected", mqUtil.isConnected());
        assertTrue("SFTP should be connected", sftpUtil.isConnected());
    }
}