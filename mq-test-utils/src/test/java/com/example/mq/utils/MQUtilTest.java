package com.example.mq.utils;

import com.ibm.mq.MQException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest(classes = MqTestConfiguration.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("MQ Integration Tests (Requires MQ Server)")
class MQUtilTest {

    @Autowired
    private MqTestUtils mqTestUtils;

    @Autowired
    private MqProperties mqProperties;

    @Autowired
    private MqConnectionManager connectionManager;

    private String testQueueName;
    private String testMessage;
    private String correlationId;
    private static boolean mqServerAvailable = false;

    @BeforeAll
    static void checkMqServerAvailability(@Autowired MqConnectionManager connectionManager) {
        try {
            connectionManager.getQueueManager();
            mqServerAvailable = true;
            System.out.println("✅ IBM MQ Server is available - Integration tests will run");
        } catch (Exception e) {
            mqServerAvailable = false;
            System.out.println("⚠️  IBM MQ Server is not available - Integration tests will be skipped");
            System.out.println("   Reason: " + e.getMessage());
        }
    }

    @BeforeEach
    void setUp() {
        assumeTrue(mqServerAvailable, "IBM MQ Server is not available. Skipping integration test.");
        
        testQueueName = mqProperties.getQueues().getTestQueue();
        testMessage = "Test message: " + UUID.randomUUID().toString();
        correlationId = UUID.randomUUID().toString();
    }

    @AfterEach
    void tearDown() {
        if (mqServerAvailable) {
            try {
                mqTestUtils.clearQueue(testQueueName);
            } catch (MQException e) {
                System.err.println("Failed to clear queue after test: " + e.getMessage());
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("Create - Send Message to Queue")
    void testSendMessage() {
        assertDoesNotThrow(() -> {
            String returnedCorrelationId = mqTestUtils.sendMessage(testQueueName, testMessage);
            
            assertNotNull(returnedCorrelationId);
            assertFalse(returnedCorrelationId.isEmpty());
            
            int queueDepth = mqTestUtils.getQueueDepth(testQueueName);
            assertEquals(1, queueDepth, "Queue should contain exactly 1 message after sending");
        });
    }

    @Test
    @Order(2)
    @DisplayName("Create - Send Message with Custom Correlation ID")
    void testSendMessageWithCorrelationId() {
        assertDoesNotThrow(() -> {
            String returnedCorrelationId = mqTestUtils.sendMessage(testQueueName, testMessage, correlationId);
            
            assertEquals(correlationId, returnedCorrelationId);
            
            int queueDepth = mqTestUtils.getQueueDepth(testQueueName);
            assertEquals(1, queueDepth, "Queue should contain exactly 1 message after sending");
        });
    }

    @Test
    @Order(3)
    @DisplayName("Read - Receive Message from Queue")
    void testReceiveMessage() {
        assertDoesNotThrow(() -> {
            mqTestUtils.sendMessage(testQueueName, testMessage);
            
            String receivedMessage = mqTestUtils.receiveMessage(testQueueName);
            
            assertNotNull(receivedMessage);
            assertEquals(testMessage, receivedMessage);
            
            int queueDepth = mqTestUtils.getQueueDepth(testQueueName);
            assertEquals(0, queueDepth, "Queue should be empty after receiving message");
        });
    }

    @Test
    @Order(4)
    @DisplayName("Read - Receive Message by Correlation ID")
    void testReceiveMessageByCorrelationId() {
        assertDoesNotThrow(() -> {
            mqTestUtils.sendMessage(testQueueName, "Message 1", "corr1");
            mqTestUtils.sendMessage(testQueueName, testMessage, correlationId);
            mqTestUtils.sendMessage(testQueueName, "Message 3", "corr3");
            
            String receivedMessage = mqTestUtils.receiveMessageByCorrelationId(testQueueName, correlationId);
            
            assertNotNull(receivedMessage);
            assertEquals(testMessage, receivedMessage);
            
            int queueDepth = mqTestUtils.getQueueDepth(testQueueName);
            assertEquals(2, queueDepth, "Queue should contain 2 remaining messages");
        });
    }

    @Test
    @Order(5)
    @DisplayName("Read - Receive All Messages from Queue")
    void testReceiveAllMessages() {
        assertDoesNotThrow(() -> {
            String message1 = "Test message 1";
            String message2 = "Test message 2";
            String message3 = "Test message 3";
            
            mqTestUtils.sendMessage(testQueueName, message1);
            mqTestUtils.sendMessage(testQueueName, message2);
            mqTestUtils.sendMessage(testQueueName, message3);
            
            List<String> receivedMessages = mqTestUtils.receiveAllMessages(testQueueName);
            
            assertNotNull(receivedMessages);
            assertEquals(3, receivedMessages.size());
            assertTrue(receivedMessages.contains(message1));
            assertTrue(receivedMessages.contains(message2));
            assertTrue(receivedMessages.contains(message3));
            
            assertTrue(mqTestUtils.isQueueEmpty(testQueueName), "Queue should be empty after receiving all messages");
        });
    }

    @Test
    @Order(6)
    @DisplayName("Update - Send and Update Message Content")
    void testUpdateMessageContent() {
        assertDoesNotThrow(() -> {
            String originalMessage = "Original message";
            String updatedMessage = "Updated message";
            
            String correlationId = mqTestUtils.sendMessage(testQueueName, originalMessage);
            
            String receivedOriginal = mqTestUtils.receiveMessageByCorrelationId(testQueueName, correlationId);
            assertEquals(originalMessage, receivedOriginal);
            
            String newCorrelationId = mqTestUtils.sendMessage(testQueueName, updatedMessage, correlationId);
            assertEquals(correlationId, newCorrelationId);
            
            String receivedUpdated = mqTestUtils.receiveMessageByCorrelationId(testQueueName, correlationId);
            assertEquals(updatedMessage, receivedUpdated);
        });
    }

    @Test
    @Order(7)
    @DisplayName("Delete - Clear Specific Message by Correlation ID")
    void testDeleteMessageByCorrelationId() {
        assertDoesNotThrow(() -> {
            String message1 = "Keep this message";
            String message2 = "Delete this message";
            
            String corr1 = mqTestUtils.sendMessage(testQueueName, message1);
            String corr2 = mqTestUtils.sendMessage(testQueueName, message2);
            
            assertEquals(2, mqTestUtils.getQueueDepth(testQueueName));
            
            String deletedMessage = mqTestUtils.receiveMessageByCorrelationId(testQueueName, corr2);
            assertEquals(message2, deletedMessage);
            
            assertEquals(1, mqTestUtils.getQueueDepth(testQueueName));
            
            String remainingMessage = mqTestUtils.receiveMessageByCorrelationId(testQueueName, corr1);
            assertEquals(message1, remainingMessage);
        });
    }

    @Test
    @Order(8)
    @DisplayName("Delete - Clear All Messages from Queue")
    void testClearQueue() {
        assertDoesNotThrow(() -> {
            mqTestUtils.sendMessage(testQueueName, "Message 1");
            mqTestUtils.sendMessage(testQueueName, "Message 2");
            mqTestUtils.sendMessage(testQueueName, "Message 3");
            
            assertEquals(3, mqTestUtils.getQueueDepth(testQueueName));
            
            mqTestUtils.clearQueue(testQueueName);
            
            assertTrue(mqTestUtils.isQueueEmpty(testQueueName), "Queue should be empty after clearing");
        });
    }

    @Test
    @Order(9)
    @DisplayName("Queue Management - Get Queue Depth")
    void testGetQueueDepth() {
        assertDoesNotThrow(() -> {
            assertEquals(0, mqTestUtils.getQueueDepth(testQueueName), "Queue should be empty initially");
            
            mqTestUtils.sendMessage(testQueueName, "Message 1");
            assertEquals(1, mqTestUtils.getQueueDepth(testQueueName));
            
            mqTestUtils.sendMessage(testQueueName, "Message 2");
            assertEquals(2, mqTestUtils.getQueueDepth(testQueueName));
            
            mqTestUtils.receiveMessage(testQueueName);
            assertEquals(1, mqTestUtils.getQueueDepth(testQueueName));
        });
    }

    @Test
    @Order(10)
    @DisplayName("Queue Management - Check if Queue is Empty")
    void testIsQueueEmpty() {
        assertDoesNotThrow(() -> {
            assertTrue(mqTestUtils.isQueueEmpty(testQueueName), "Queue should be empty initially");
            
            mqTestUtils.sendMessage(testQueueName, testMessage);
            assertFalse(mqTestUtils.isQueueEmpty(testQueueName), "Queue should not be empty after sending message");
            
            mqTestUtils.receiveMessage(testQueueName);
            assertTrue(mqTestUtils.isQueueEmpty(testQueueName), "Queue should be empty after receiving message");
        });
    }

    @Test
    @Order(11)
    @DisplayName("Timeout Handling - Receive Message with Timeout")
    void testReceiveMessageWithTimeout() {
        assertDoesNotThrow(() -> {
            String receivedMessage = mqTestUtils.receiveMessage(testQueueName, 1000);
            assertNull(receivedMessage, "Should return null when no message is available within timeout");
        });
    }

    @Test
    @Order(12)
    @DisplayName("Concurrency - Wait for Message")
    void testWaitForMessage() {
        assertDoesNotThrow(() -> {
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    mqTestUtils.sendMessage(testQueueName, testMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
            
            assertDoesNotThrow(() -> {
                mqTestUtils.waitForMessage(testQueueName, 5000);
                
                String receivedMessage = mqTestUtils.receiveMessage(testQueueName);
                assertEquals(testMessage, receivedMessage);
            });
        });
    }

    @Test
    @Order(13)
    @DisplayName("Error Handling - Receive from Non-existent Queue")
    void testReceiveFromNonExistentQueue() {
        String nonExistentQueue = "NON.EXISTENT.QUEUE";
        
        assertThrows(MQException.class, () -> {
            mqTestUtils.receiveMessage(nonExistentQueue);
        });
    }

    @Test
    @Order(14)
    @DisplayName("Bulk Operations - Send Multiple Messages")
    void testSendMultipleMessages() {
        assertDoesNotThrow(() -> {
            int messageCount = 10;
            
            for (int i = 0; i < messageCount; i++) {
                mqTestUtils.sendMessage(testQueueName, "Bulk message " + i);
            }
            
            assertEquals(messageCount, mqTestUtils.getQueueDepth(testQueueName));
            
            List<String> receivedMessages = mqTestUtils.receiveAllMessages(testQueueName);
            assertEquals(messageCount, receivedMessages.size());
            
            for (int i = 0; i < messageCount; i++) {
                assertTrue(receivedMessages.contains("Bulk message " + i));
            }
        });
    }

    @Test
    @Order(15)
    @DisplayName("Integration - Complete CRUD Workflow")
    void testCompleteCrudWorkflow() {
        assertDoesNotThrow(() -> {
            String originalMessage = "Original workflow message";
            String updatedMessage = "Updated workflow message";
            
            String correlationId = mqTestUtils.sendMessage(testQueueName, originalMessage);
            assertNotNull(correlationId);
            assertEquals(1, mqTestUtils.getQueueDepth(testQueueName));
            
            String readMessage = mqTestUtils.receiveMessageByCorrelationId(testQueueName, correlationId);
            assertEquals(originalMessage, readMessage);
            assertTrue(mqTestUtils.isQueueEmpty(testQueueName));
            
            mqTestUtils.sendMessage(testQueueName, updatedMessage, correlationId);
            assertEquals(1, mqTestUtils.getQueueDepth(testQueueName));
            
            String updatedReadMessage = mqTestUtils.receiveMessageByCorrelationId(testQueueName, correlationId);
            assertEquals(updatedMessage, updatedReadMessage);
            
            assertTrue(mqTestUtils.isQueueEmpty(testQueueName));
        });
    }
}