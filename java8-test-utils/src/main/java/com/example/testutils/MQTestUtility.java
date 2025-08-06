package com.example.testutils;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.region.policy.PolicyEntry;
import org.apache.activemq.broker.region.policy.PolicyMap;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MQTestUtility {
    private BrokerService broker;
    private ConnectionFactory connectionFactory;
    private Connection connection;
    private Session session;
    private final ConcurrentMap<String, List<String>> mockQueues = new ConcurrentHashMap<>();
    private final String brokerUrl = "vm://localhost?broker.persistent=false";
    private boolean useMockQueues = true;
    
    public MQTestUtility() {
        this(true);
    }
    
    public MQTestUtility(boolean useMockQueues) {
        this.useMockQueues = useMockQueues;
    }
    
    public void setup() throws Exception {
        if (useMockQueues) {
            setupMockQueues();
        } else {
            setupEmbeddedBroker();
        }
    }
    
    private void setupMockQueues() {
        mockQueues.clear();
    }
    
    private void setupEmbeddedBroker() throws Exception {
        broker = new BrokerService();
        broker.setUseJmx(false);
        broker.setPersistent(false);
        broker.setDeleteAllMessagesOnStartup(true);
        broker.addConnector(brokerUrl);
        
        PolicyMap policyMap = new PolicyMap();
        PolicyEntry defaultEntry = new PolicyEntry();
        defaultEntry.setMemoryLimit(1024 * 1024);
        policyMap.setDefaultEntry(defaultEntry);
        broker.setDestinationPolicy(policyMap);
        
        broker.start();
        broker.waitUntilStarted();
        
        // Create connection factory directly
        org.apache.activemq.ActiveMQConnectionFactory amqConnectionFactory = 
            new org.apache.activemq.ActiveMQConnectionFactory(brokerUrl);
        connectionFactory = amqConnectionFactory;
        connection = connectionFactory.createConnection();
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }
    
    public void createQueue(String queueName) throws Exception {
        if (useMockQueues) {
            mockQueues.putIfAbsent(queueName, new ArrayList<String>());
        } else {
            if (session == null) {
                throw new Exception("MQ session not initialized. Call setup() first.");
            }
            session.createQueue(queueName);
        }
    }
    
    public void sendMessage(String queueName, String message) throws Exception {
        if (useMockQueues) {
            List<String> queueMessages = mockQueues.get(queueName);
            if (queueMessages == null) {
                createQueue(queueName);
                queueMessages = mockQueues.get(queueName);
            }
            queueMessages.add(message);
        } else {
            if (session == null) {
                throw new Exception("MQ session not initialized. Call setup() first.");
            }
            
            Queue queue = session.createQueue(queueName);
            MessageProducer producer = session.createProducer(queue);
            TextMessage textMessage = session.createTextMessage(message);
            producer.send(textMessage);
            producer.close();
        }
    }
    
    public void sendMessageFromFile(String queueName, String messageFilePath) throws Exception {
        String message = readMessageFromFile(messageFilePath);
        sendMessage(queueName, message);
    }
    
    public String receiveMessage(String queueName) throws Exception {
        return receiveMessage(queueName, 1000);
    }
    
    public String receiveMessage(String queueName, long timeoutMs) throws Exception {
        if (useMockQueues) {
            List<String> queueMessages = mockQueues.get(queueName);
            if (queueMessages == null || queueMessages.isEmpty()) {
                return null;
            }
            return queueMessages.remove(0);
        } else {
            if (session == null) {
                throw new Exception("MQ session not initialized. Call setup() first.");
            }
            
            Queue queue = session.createQueue(queueName);
            MessageConsumer consumer = session.createConsumer(queue);
            
            try {
                Message message = consumer.receive(timeoutMs);
                if (message instanceof TextMessage) {
                    return ((TextMessage) message).getText();
                }
                return null;
            } finally {
                consumer.close();
            }
        }
    }
    
    public List<String> receiveAllMessages(String queueName) throws Exception {
        List<String> messages = new ArrayList<>();
        
        if (useMockQueues) {
            List<String> queueMessages = mockQueues.get(queueName);
            if (queueMessages != null) {
                messages.addAll(queueMessages);
                queueMessages.clear();
            }
        } else {
            String message;
            while ((message = receiveMessage(queueName, 100)) != null) {
                messages.add(message);
            }
        }
        
        return messages;
    }
    
    public int getMessageCount(String queueName) throws Exception {
        if (useMockQueues) {
            List<String> queueMessages = mockQueues.get(queueName);
            return queueMessages != null ? queueMessages.size() : 0;
        } else {
            if (session == null) {
                throw new Exception("MQ session not initialized. Call setup() first.");
            }
            
            Queue queue = session.createQueue(queueName);
            MessageConsumer consumer = session.createConsumer(queue);
            
            try {
                List<String> messages = new ArrayList<>();
                Message message;
                while ((message = consumer.receive(100)) != null) {
                    if (message instanceof TextMessage) {
                        messages.add(((TextMessage) message).getText());
                    }
                }
                
                for (String msg : messages) {
                    sendMessage(queueName, msg);
                }
                
                return messages.size();
            } finally {
                consumer.close();
            }
        }
    }
    
    public boolean isQueueEmpty(String queueName) throws Exception {
        return getMessageCount(queueName) == 0;
    }
    
    public void clearQueue(String queueName) throws Exception {
        if (useMockQueues) {
            List<String> queueMessages = mockQueues.get(queueName);
            if (queueMessages != null) {
                queueMessages.clear();
            }
        } else {
            receiveAllMessages(queueName);
        }
    }
    
    public void clearAllQueues() throws Exception {
        if (useMockQueues) {
            for (List<String> queueMessages : mockQueues.values()) {
                queueMessages.clear();
            }
        } else {
            for (String queueName : mockQueues.keySet()) {
                clearQueue(queueName);
            }
        }
    }
    
    private String readMessageFromFile(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        // Try to load as resource first, then as file
        java.io.InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);
        if (inputStream != null) {
            // Load from resource
            try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append(System.lineSeparator());
                }
            }
        } else {
            // If resource not found, try as file path
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append(System.lineSeparator());
                }
            }
        }
        return content.toString().trim();
    }
    
    public void teardown() {
        try {
            if (session != null) {
                session.close();
                session = null;
            }
            
            if (connection != null) {
                connection.close();
                connection = null;
            }
            
            if (broker != null) {
                broker.stop();
                broker.waitUntilStopped();
                broker = null;
            }
            
            mockQueues.clear();
        } catch (Exception e) {
            System.err.println("Error during MQ teardown: " + e.getMessage());
        }
    }
    
    public boolean isConnected() {
        if (useMockQueues) {
            return true;
        }
        
        try {
            return connection != null && session != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    public String getBrokerUrl() {
        return brokerUrl;
    }
    
    public boolean isUsingMockQueues() {
        return useMockQueues;
    }
}