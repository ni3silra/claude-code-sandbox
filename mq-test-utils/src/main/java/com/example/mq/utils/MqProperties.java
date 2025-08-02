package com.example.mq.utils;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mq.test")
public class MqProperties {
    
    private Connection connection = new Connection();
    private Queues queues = new Queues();
    private Timeout timeout = new Timeout();
    private Retry retry = new Retry();
    
    public static class Connection {
        private String host = "localhost";
        private int port = 1414;
        private String queueManager = "QM1";
        private String channel = "DEV.ADMIN.SVRCONN";
        private String user = "admin";
        private String password = "passw0rd";
        private String clientId = "MQ_TEST_CLIENT";
        
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public String getQueueManager() { return queueManager; }
        public void setQueueManager(String queueManager) { this.queueManager = queueManager; }
        
        public String getChannel() { return channel; }
        public void setChannel(String channel) { this.channel = channel; }
        
        public String getUser() { return user; }
        public void setUser(String user) { this.user = user; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
    }
    
    public static class Queues {
        private String request = "DEV.QUEUE.1";
        private String response = "DEV.QUEUE.2";
        private String deadLetter = "DEV.DEAD.LETTER.QUEUE";
        private String testQueue = "TEST.QUEUE";
        
        public String getRequest() { return request; }
        public void setRequest(String request) { this.request = request; }
        
        public String getResponse() { return response; }
        public void setResponse(String response) { this.response = response; }
        
        public String getDeadLetter() { return deadLetter; }
        public void setDeadLetter(String deadLetter) { this.deadLetter = deadLetter; }
        
        public String getTestQueue() { return testQueue; }
        public void setTestQueue(String testQueue) { this.testQueue = testQueue; }
    }
    
    public static class Timeout {
        private long receive = 5000;
        private long send = 3000;
        
        public long getReceive() { return receive; }
        public void setReceive(long receive) { this.receive = receive; }
        
        public long getSend() { return send; }
        public void setSend(long send) { this.send = send; }
    }
    
    public static class Retry {
        private int maxAttempts = 3;
        private long delay = 1000;
        
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        
        public long getDelay() { return delay; }
        public void setDelay(long delay) { this.delay = delay; }
    }
    
    public Connection getConnection() { return connection; }
    public void setConnection(Connection connection) { this.connection = connection; }
    
    public Queues getQueues() { return queues; }
    public void setQueues(Queues queues) { this.queues = queues; }
    
    public Timeout getTimeout() { return timeout; }
    public void setTimeout(Timeout timeout) { this.timeout = timeout; }
    
    public Retry getRetry() { return retry; }
    public void setRetry(Retry retry) { this.retry = retry; }
}