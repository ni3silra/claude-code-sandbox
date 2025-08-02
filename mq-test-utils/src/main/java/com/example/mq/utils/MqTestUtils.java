package com.example.mq.utils;

import com.ibm.mq.*;
import com.ibm.mq.constants.CMQC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class MqTestUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(MqTestUtils.class);
    
    private final MqConnectionManager connectionManager;
    private final MqProperties mqProperties;
    
    @Autowired
    public MqTestUtils(MqConnectionManager connectionManager, MqProperties mqProperties) {
        this.connectionManager = connectionManager;
        this.mqProperties = mqProperties;
    }
    
    public String sendMessage(String queueName, String message) throws MQException {
        return sendMessage(queueName, message, null);
    }
    
    public String sendMessage(String queueName, String message, String correlationId) throws MQException {
        MQQueue queue = null;
        try {
            MQQueueManager queueManager = connectionManager.getQueueManager();
            
            int openOptions = CMQC.MQOO_OUTPUT;
            queue = queueManager.accessQueue(queueName, openOptions);
            
            MQMessage mqMessage = new MQMessage();
            mqMessage.writeString(message);
            
            if (correlationId != null) {
                mqMessage.correlationId = correlationId.getBytes(StandardCharsets.UTF_8);
            } else {
                String generatedCorrelationId = UUID.randomUUID().toString();
                mqMessage.correlationId = generatedCorrelationId.getBytes(StandardCharsets.UTF_8);
            }
            
            MQPutMessageOptions putOptions = new MQPutMessageOptions();
            queue.put(mqMessage, putOptions);
            
            String resultCorrelationId = new String(mqMessage.correlationId, StandardCharsets.UTF_8);
            logger.info("Message sent to queue '{}' with correlation ID: {}", queueName, resultCorrelationId);
            
            return resultCorrelationId;
            
        } catch (IOException e) {
            logger.error("Failed to send message to queue '{}': {}", queueName, e.getMessage(), e);
            throw new RuntimeException("IO Error sending message", e);
        } catch (MQException e) {
            logger.error("Failed to send message to queue '{}': {}", queueName, e.getMessage(), e);
            throw e;
        } finally {
            closeQueue(queue);
        }
    }
    
    public String receiveMessage(String queueName) throws MQException {
        return receiveMessage(queueName, mqProperties.getTimeout().getReceive());
    }
    
    public String receiveMessage(String queueName, long timeoutMs) throws MQException {
        MQQueue queue = null;
        try {
            MQQueueManager queueManager = connectionManager.getQueueManager();
            
            int openOptions = CMQC.MQOO_INPUT_AS_Q_DEF;
            queue = queueManager.accessQueue(queueName, openOptions);
            
            MQMessage mqMessage = new MQMessage();
            MQGetMessageOptions getOptions = new MQGetMessageOptions();
            getOptions.options = CMQC.MQGMO_WAIT;
            getOptions.waitInterval = (int) timeoutMs;
            
            queue.get(mqMessage, getOptions);
            
            String message = mqMessage.readStringOfByteLength(mqMessage.getDataLength());
            logger.info("Message received from queue '{}': {}", queueName, message);
            
            return message;
            
        } catch (IOException e) {
            logger.error("Failed to receive message from queue '{}': {}", queueName, e.getMessage(), e);
            throw new RuntimeException("IO Error receiving message", e);
        } catch (MQException e) {
            if (e.reasonCode == CMQC.MQRC_NO_MSG_AVAILABLE) {
                logger.debug("No message available in queue '{}' within timeout", queueName);
                return null;
            }
            logger.error("Failed to receive message from queue '{}': {}", queueName, e.getMessage(), e);
            throw e;
        } finally {
            closeQueue(queue);
        }
    }
    
    public String receiveMessageByCorrelationId(String queueName, String correlationId) throws MQException {
        return receiveMessageByCorrelationId(queueName, correlationId, mqProperties.getTimeout().getReceive());
    }
    
    public String receiveMessageByCorrelationId(String queueName, String correlationId, long timeoutMs) throws MQException {
        MQQueue queue = null;
        try {
            MQQueueManager queueManager = connectionManager.getQueueManager();
            
            int openOptions = CMQC.MQOO_INPUT_AS_Q_DEF;
            queue = queueManager.accessQueue(queueName, openOptions);
            
            MQMessage mqMessage = new MQMessage();
            mqMessage.correlationId = correlationId.getBytes(StandardCharsets.UTF_8);
            
            MQGetMessageOptions getOptions = new MQGetMessageOptions();
            getOptions.options = CMQC.MQGMO_WAIT;
            getOptions.waitInterval = (int) timeoutMs;
            
            queue.get(mqMessage, getOptions);
            
            String message = mqMessage.readStringOfByteLength(mqMessage.getDataLength());
            logger.info("Message received from queue '{}' with correlation ID '{}': {}", queueName, correlationId, message);
            
            return message;
            
        } catch (IOException e) {
            logger.error("Failed to receive message from queue '{}' with correlation ID '{}': {}", queueName, correlationId, e.getMessage(), e);
            throw new RuntimeException("IO Error receiving message", e);
        } catch (MQException e) {
            if (e.reasonCode == CMQC.MQRC_NO_MSG_AVAILABLE) {
                logger.debug("No message available in queue '{}' with correlation ID '{}' within timeout", queueName, correlationId);
                return null;
            }
            logger.error("Failed to receive message from queue '{}' with correlation ID '{}': {}", queueName, correlationId, e.getMessage(), e);
            throw e;
        } finally {
            closeQueue(queue);
        }
    }
    
    public List<String> receiveAllMessages(String queueName) throws MQException {
        List<String> messages = new ArrayList<>();
        String message;
        
        while ((message = receiveMessage(queueName, 1000)) != null) {
            messages.add(message);
        }
        
        logger.info("Received {} messages from queue '{}'", messages.size(), queueName);
        return messages;
    }
    
    public int getQueueDepth(String queueName) throws MQException {
        MQQueue queue = null;
        try {
            MQQueueManager queueManager = connectionManager.getQueueManager();
            
            int openOptions = CMQC.MQOO_INQUIRE;
            queue = queueManager.accessQueue(queueName, openOptions);
            
            int depth = queue.getCurrentDepth();
            logger.debug("Queue '{}' depth: {}", queueName, depth);
            
            return depth;
            
        } finally {
            closeQueue(queue);
        }
    }
    
    public void clearQueue(String queueName) throws MQException {
        logger.info("Clearing all messages from queue '{}'", queueName);
        List<String> messages = receiveAllMessages(queueName);
        logger.info("Cleared {} messages from queue '{}'", messages.size(), queueName);
    }
    
    public boolean isQueueEmpty(String queueName) throws MQException {
        return getQueueDepth(queueName) == 0;
    }
    
    public void waitForMessage(String queueName, long timeoutMs) throws Exception {
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (getQueueDepth(queueName) > 0) {
                return;
            }
            Thread.sleep(100);
        }
        
        throw new RuntimeException("Timeout waiting for message in queue '" + queueName + "'");
    }
    
    private void closeQueue(MQQueue queue) {
        if (queue != null) {
            try {
                queue.close();
            } catch (MQException e) {
                logger.warn("Error closing queue: {}", e.getMessage());
            }
        }
    }
}