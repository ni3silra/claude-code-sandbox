package com.example.mq.utils;

import com.ibm.mq.MQEnvironment;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.MQException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

@Component
public class MqConnectionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(MqConnectionManager.class);
    
    private final MqProperties mqProperties;
    private MQQueueManager queueManager;
    
    @Autowired
    public MqConnectionManager(MqProperties mqProperties) {
        this.mqProperties = mqProperties;
    }
    
    public synchronized MQQueueManager getQueueManager() throws MQException {
        if (queueManager == null || !queueManager.isConnected()) {
            connect();
        }
        return queueManager;
    }
    
    private void connect() throws MQException {
        try {
            logger.info("Connecting to MQ Queue Manager: {}", mqProperties.getConnection().getQueueManager());
            
            MQEnvironment.hostname = mqProperties.getConnection().getHost();
            MQEnvironment.port = mqProperties.getConnection().getPort();
            MQEnvironment.channel = mqProperties.getConnection().getChannel();
            MQEnvironment.userID = mqProperties.getConnection().getUser();
            MQEnvironment.password = mqProperties.getConnection().getPassword();
            
            queueManager = new MQQueueManager(mqProperties.getConnection().getQueueManager());
            logger.info("Successfully connected to MQ Queue Manager");
            
        } catch (MQException e) {
            logger.error("Failed to connect to MQ Queue Manager: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    public synchronized void disconnect() {
        if (queueManager != null) {
            try {
                if (queueManager.isConnected()) {
                    queueManager.disconnect();
                    logger.info("Disconnected from MQ Queue Manager");
                }
            } catch (MQException e) {
                logger.warn("Error disconnecting from MQ Queue Manager: {}", e.getMessage());
            } finally {
                queueManager = null;
            }
        }
    }
    
    public boolean isConnected() {
        try {
            return queueManager != null && queueManager.isConnected();
        } catch (Exception e) {
            return false;
        }
    }
    
    @PreDestroy
    public void cleanup() {
        disconnect();
    }
}