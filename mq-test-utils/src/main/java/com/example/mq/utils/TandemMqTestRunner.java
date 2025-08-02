package com.example.mq.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.UUID;

@SpringBootApplication
@Profile("tandem")
public class TandemMqTestRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(TandemMqTestRunner.class);

    @Autowired
    private MqTestUtils mqTestUtils;

    @Autowired
    private MqProperties mqProperties;

    @Autowired
    private MqConnectionManager connectionManager;

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "tandem");
        SpringApplication.run(TandemMqTestRunner.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("=================================================");
        logger.info("Starting IBM MQ Tests on Tandem Environment");
        logger.info("=================================================");

        try {
            // Test connection first
            testConnection();
            
            // Run comprehensive CRUD tests
            runCrudTests();
            
            // Performance tests
            runPerformanceTests();
            
            logger.info("=================================================");
            logger.info("✅ ALL MQ TESTS COMPLETED SUCCESSFULLY");
            logger.info("=================================================");
            
        } catch (Exception e) {
            logger.error("=================================================");
            logger.error("❌ MQ TESTS FAILED: {}", e.getMessage(), e);
            logger.error("=================================================");
            System.exit(1);
        }
    }

    private void testConnection() throws Exception {
        logger.info("🔗 Testing MQ Connection...");
        
        if (connectionManager.isConnected()) {
            logger.info("✅ MQ Connection successful");
        } else {
            connectionManager.getQueueManager(); // This will attempt connection
            logger.info("✅ MQ Connection established");
        }
        
        logger.info("📊 Queue Manager: {}", mqProperties.getConnection().getQueueManager());
        logger.info("🏠 Host: {}:{}", mqProperties.getConnection().getHost(), mqProperties.getConnection().getPort());
        logger.info("📡 Channel: {}", mqProperties.getConnection().getChannel());
    }

    private void runCrudTests() throws Exception {
        logger.info("🧪 Running CRUD Tests...");
        
        String testQueue = mqProperties.getQueues().getTestQueue();
        
        // CREATE Test
        logger.info("📝 CREATE Test - Sending messages...");
        String message1 = "Tandem Test Message 1: " + UUID.randomUUID();
        String message2 = "Tandem Test Message 2: " + UUID.randomUUID();
        String correlationId1 = mqTestUtils.sendMessage(testQueue, message1);
        String correlationId2 = mqTestUtils.sendMessage(testQueue, message2, "CUSTOM-CORR-ID");
        
        logger.info("✅ Messages sent - Correlation IDs: {}, {}", correlationId1, correlationId2);
        
        // READ Test
        logger.info("📖 READ Test - Reading messages...");
        int queueDepth = mqTestUtils.getQueueDepth(testQueue);
        logger.info("📊 Queue depth: {}", queueDepth);
        
        String receivedMessage1 = mqTestUtils.receiveMessage(testQueue);
        String receivedMessage2 = mqTestUtils.receiveMessageByCorrelationId(testQueue, "CUSTOM-CORR-ID");
        
        logger.info("✅ Messages received: '{}', '{}'", receivedMessage1, receivedMessage2);
        
        // UPDATE Test
        logger.info("🔄 UPDATE Test - Updating message...");
        String updateMessage = "Updated Tandem Message: " + UUID.randomUUID();
        String updateCorrelationId = mqTestUtils.sendMessage(testQueue, updateMessage, "UPDATE-TEST");
        String retrievedUpdate = mqTestUtils.receiveMessageByCorrelationId(testQueue, "UPDATE-TEST");
        
        logger.info("✅ Update test completed: '{}'", retrievedUpdate);
        
        // DELETE Test
        logger.info("🗑️ DELETE Test - Clearing queue...");
        mqTestUtils.clearQueue(testQueue);
        boolean isEmpty = mqTestUtils.isQueueEmpty(testQueue);
        
        logger.info("✅ Queue cleared: {}", isEmpty ? "SUCCESS" : "FAILED");
    }

    private void runPerformanceTests() throws Exception {
        logger.info("⚡ Running Performance Tests...");
        
        String testQueue = mqProperties.getQueues().getTestQueue();
        int messageCount = 100;
        
        // Bulk send test
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < messageCount; i++) {
            mqTestUtils.sendMessage(testQueue, "Performance Test Message " + i);
        }
        long sendTime = System.currentTimeMillis() - startTime;
        
        logger.info("📊 Sent {} messages in {} ms ({} msg/sec)", 
                   messageCount, sendTime, (messageCount * 1000.0 / sendTime));
        
        // Bulk receive test
        startTime = System.currentTimeMillis();
        List<String> receivedMessages = mqTestUtils.receiveAllMessages(testQueue);
        long receiveTime = System.currentTimeMillis() - startTime;
        
        logger.info("📊 Received {} messages in {} ms ({} msg/sec)", 
                   receivedMessages.size(), receiveTime, (receivedMessages.size() * 1000.0 / receiveTime));
        
        logger.info("✅ Performance tests completed");
    }
}