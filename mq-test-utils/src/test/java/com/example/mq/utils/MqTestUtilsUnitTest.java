package com.example.mq.utils;

import com.ibm.mq.MQException;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MqTestUtils Unit Tests (Mocked)")
class MqTestUtilsUnitTest {

    @Mock
    private MqConnectionManager connectionManager;

    @Mock
    private MQQueueManager queueManager;

    @Mock
    private MQQueue queue;

    private MqTestUtils mqTestUtils;
    private MqProperties mqProperties;

    @BeforeEach
    void setUp() throws MQException {
        mqProperties = new MqProperties();
        mqProperties.getQueues().setTestQueue("TEST.QUEUE");
        mqProperties.getTimeout().setReceive(5000);
        mqProperties.getTimeout().setSend(3000);

        mqTestUtils = new MqTestUtils(connectionManager, mqProperties);
    }

    @Test
    @DisplayName("Unit Test - Send Message Success")
    void testSendMessageSuccess() throws Exception {
        String queueName = "TEST.QUEUE";
        String message = "Test message";

        when(connectionManager.getQueueManager()).thenReturn(queueManager);
        when(queueManager.accessQueue(anyString(), anyInt())).thenReturn(queue);
        doNothing().when(queue).put(any(), any());
        doNothing().when(queue).close();

        String correlationId = mqTestUtils.sendMessage(queueName, message);

        assertNotNull(correlationId);
        assertFalse(correlationId.isEmpty());
        verify(queueManager).accessQueue(eq(queueName), anyInt());
        verify(queue).put(any(), any());
        verify(queue).close();
    }

    @Test
    @DisplayName("Unit Test - Send Message with Custom Correlation ID")
    void testSendMessageWithCorrelationId() throws Exception {
        String queueName = "TEST.QUEUE";
        String message = "Test message";
        String customCorrelationId = "CUSTOM-123";

        when(connectionManager.getQueueManager()).thenReturn(queueManager);
        when(queueManager.accessQueue(anyString(), anyInt())).thenReturn(queue);
        doNothing().when(queue).put(any(), any());
        doNothing().when(queue).close();

        String returnedCorrelationId = mqTestUtils.sendMessage(queueName, message, customCorrelationId);

        assertEquals(customCorrelationId, returnedCorrelationId);
        verify(queueManager).accessQueue(eq(queueName), anyInt());
        verify(queue).put(any(), any());
        verify(queue).close();
    }

    @Test
    @DisplayName("Unit Test - Get Queue Depth")
    void testGetQueueDepth() throws Exception {
        String queueName = "TEST.QUEUE";
        int expectedDepth = 5;

        when(connectionManager.getQueueManager()).thenReturn(queueManager);
        when(queueManager.accessQueue(anyString(), anyInt())).thenReturn(queue);
        when(queue.getCurrentDepth()).thenReturn(expectedDepth);
        doNothing().when(queue).close();

        int actualDepth = mqTestUtils.getQueueDepth(queueName);

        assertEquals(expectedDepth, actualDepth);
        verify(queueManager).accessQueue(eq(queueName), anyInt());
        verify(queue).getCurrentDepth();
        verify(queue).close();
    }

    @Test
    @DisplayName("Unit Test - Check if Queue is Empty")
    void testIsQueueEmpty() throws Exception {
        String queueName = "TEST.QUEUE";

        when(connectionManager.getQueueManager()).thenReturn(queueManager);
        when(queueManager.accessQueue(anyString(), anyInt())).thenReturn(queue);
        when(queue.getCurrentDepth()).thenReturn(0);
        doNothing().when(queue).close();

        boolean isEmpty = mqTestUtils.isQueueEmpty(queueName);

        assertTrue(isEmpty);
        verify(queueManager).accessQueue(eq(queueName), anyInt());
        verify(queue).getCurrentDepth();
        verify(queue).close();
    }

    @Test
    @DisplayName("Unit Test - Check if Queue is Not Empty")
    void testIsQueueNotEmpty() throws Exception {
        String queueName = "TEST.QUEUE";

        when(connectionManager.getQueueManager()).thenReturn(queueManager);
        when(queueManager.accessQueue(anyString(), anyInt())).thenReturn(queue);
        when(queue.getCurrentDepth()).thenReturn(3);
        doNothing().when(queue).close();

        boolean isEmpty = mqTestUtils.isQueueEmpty(queueName);

        assertFalse(isEmpty);
        verify(queueManager).accessQueue(eq(queueName), anyInt());
        verify(queue).getCurrentDepth();
        verify(queue).close();
    }

    @Test
    @DisplayName("Unit Test - MQ Exception Handling")
    void testMqExceptionHandling() throws Exception {
        String queueName = "INVALID.QUEUE";
        MQException mockException = mock(MQException.class);

        when(connectionManager.getQueueManager()).thenReturn(queueManager);
        when(queueManager.accessQueue(eq(queueName), anyInt())).thenThrow(mockException);

        assertThrows(MQException.class, () -> {
            mqTestUtils.getQueueDepth(queueName);
        });

        verify(connectionManager).getQueueManager();
        verify(queueManager).accessQueue(eq(queueName), anyInt());
    }

    @Test
    @DisplayName("Unit Test - Connection Manager Integration")
    void testConnectionManagerIntegration() throws Exception {
        String queueName = "TEST.QUEUE";

        when(connectionManager.getQueueManager()).thenReturn(queueManager);
        when(queueManager.accessQueue(anyString(), anyInt())).thenReturn(queue);
        when(queue.getCurrentDepth()).thenReturn(10);
        doNothing().when(queue).close();

        int depth = mqTestUtils.getQueueDepth(queueName);

        assertEquals(10, depth);
        verify(connectionManager).getQueueManager();
    }

    @Test
    @DisplayName("Unit Test - Properties Configuration")
    void testPropertiesConfiguration() {
        assertEquals("TEST.QUEUE", mqProperties.getQueues().getTestQueue());
        assertEquals(5000, mqProperties.getTimeout().getReceive());
        assertEquals(3000, mqProperties.getTimeout().getSend());
    }

    @Test
    @DisplayName("Unit Test - Multiple Operations Workflow")
    void testMultipleOperationsWorkflow() throws Exception {
        String queueName = "TEST.QUEUE";

        when(connectionManager.getQueueManager()).thenReturn(queueManager);
        when(queueManager.accessQueue(anyString(), anyInt())).thenReturn(queue);
        when(queue.getCurrentDepth()).thenReturn(0, 1, 0);
        doNothing().when(queue).put(any(), any());
        doNothing().when(queue).close();

        assertTrue(mqTestUtils.isQueueEmpty(queueName));

        String correlationId = mqTestUtils.sendMessage(queueName, "Test message");
        assertNotNull(correlationId);

        when(queue.getCurrentDepth()).thenReturn(1);
        assertFalse(mqTestUtils.isQueueEmpty(queueName));

        verify(queueManager, times(3)).accessQueue(eq(queueName), anyInt());
        verify(queue, times(3)).close();
    }
}