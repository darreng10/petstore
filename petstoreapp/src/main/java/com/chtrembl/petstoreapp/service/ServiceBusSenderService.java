package com.chtrembl.petstoreapp.service;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Service Bus Sender Service for sending messages to Azure Service Bus Queue.
 * This service is used to send order reservation requests asynchronously.
 */
@Service
@Slf4j
public class ServiceBusSenderService {

    @Value("${azure.servicebus.connection-string:}")
    private String connectionString;

    @Value("${azure.servicebus.queue-name:order-reservations}")
    private String queueName;

    private ServiceBusSenderClient senderClient;
    private final ObjectMapper objectMapper;

    public ServiceBusSenderService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Initialize Service Bus Sender Client after properties are set.
     */
    @PostConstruct
    public void initialize() {
        if (connectionString != null && !connectionString.isEmpty() && !connectionString.isBlank()) {
            try {
                log.info("Initializing Service Bus Sender for queue: {}", queueName);
                this.senderClient = new ServiceBusClientBuilder()
                        .connectionString(connectionString)
                        .sender()
                        .queueName(queueName)
                        .buildClient();
                log.info("Service Bus Sender initialized successfully");
            } catch (Exception e) {
                log.error("Failed to initialize Service Bus Sender: {}", e.getMessage(), e);
                this.senderClient = null;
            }
        } else {
            log.warn("Service Bus connection string not configured. Service Bus messaging will be disabled.");
            this.senderClient = null;
        }
    }

    /**
     * Close the Service Bus Sender Client on application shutdown.
     */
    @PreDestroy
    public void cleanup() {
        if (senderClient != null) {
            try {
                log.info("Closing Service Bus Sender");
                senderClient.close();
            } catch (Exception e) {
                log.error("Error closing Service Bus Sender: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Send a message to the Service Bus Queue.
     * 
     * @param message The object to send (will be serialized to JSON)
     * @throws RuntimeException if Service Bus is not configured or sending fails
     */
    public void sendMessage(Object message) {
        if (senderClient == null) {
            log.warn("Service Bus Sender not initialized. Cannot send message.");
            throw new RuntimeException("Service Bus Sender not configured");
        }

        try {
            // Serialize message to JSON
            String messageBody = objectMapper.writeValueAsString(message);
            
            log.info("Sending message to Service Bus queue '{}': {}", queueName, messageBody);
            
            // Create and send Service Bus message
            ServiceBusMessage serviceBusMessage = new ServiceBusMessage(messageBody);
            senderClient.sendMessage(serviceBusMessage);
            
            log.info("Message sent successfully to Service Bus queue '{}'", queueName);
            
        } catch (Exception e) {
            log.error("Failed to send message to Service Bus: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send message to Service Bus", e);
        }
    }

    /**
     * Check if Service Bus Sender is configured and ready.
     * 
     * @return true if sender is initialized, false otherwise
     */
    public boolean isConfigured() {
        return senderClient != null;
    }
}

