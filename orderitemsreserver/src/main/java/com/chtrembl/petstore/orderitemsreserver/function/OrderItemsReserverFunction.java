package com.chtrembl.petstore.orderitemsreserver.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.functions.annotation.ServiceBusQueueTrigger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.chtrembl.petstore.orderitemsreserver.model.OrderReservationRequest;
import com.chtrembl.petstore.orderitemsreserver.model.ReservationResponse;
import com.chtrembl.petstore.orderitemsreserver.service.BlobStorageService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Azure Function with Service Bus Trigger for Order Items Reservation.
 * 
 * This function receives order reservation requests from the petstore application via Azure Service Bus
 * and stores them as JSON files in Azure Blob Storage using the session ID as the file name.
 * Files are overwritten for each session update.
 */
public class OrderItemsReserverFunction {

    private static final String STORAGE_CONNECTION_STRING_ENV = "AzureWebJobsStorage";
    private final ObjectMapper objectMapper;
    
    public OrderItemsReserverFunction() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Service Bus Queue trigger function to reserve order items.
     * 
     * This function is triggered by messages sent to the Azure Service Bus Queue and processes 
     * order reservation requests. It validates the request, stores it to blob storage.
     * 
     * @param message The message from Service Bus queue containing order reservation data (JSON string)
     * @param context The execution context
     */
    @FunctionName("ReserveOrderItems")
    public void run(
            @ServiceBusQueueTrigger(
                name = "message",
                queueName = "%ServiceBusQueueName%",
                connection = "ServiceBusConnection")
            String message,
            final ExecutionContext context) {

        context.getLogger().info("OrderItemsReserver Service Bus function triggered");
        context.getLogger().info("Message received: " + message);

        try {
            // Validate message
            if (message == null || message.isEmpty()) {
                context.getLogger().severe("Empty message received from Service Bus");
                throw new IllegalArgumentException("Message body is empty");
            }

            // Parse message to OrderReservationRequest
            OrderReservationRequest reservationRequest = objectMapper.readValue(
                    message, 
                    OrderReservationRequest.class);

            // Validate request
            if (reservationRequest.getSessionId() == null || reservationRequest.getSessionId().isEmpty()) {
                context.getLogger().severe("Session ID is missing in request");
                throw new IllegalArgumentException("Session ID is required");
            }

            if (reservationRequest.getProducts() == null || reservationRequest.getProducts().isEmpty()) {
                context.getLogger().severe("Products list is empty in request");
                throw new IllegalArgumentException("Products list is required");
            }

            // Set timestamp if not provided
            if (reservationRequest.getTimestamp() == null) {
                reservationRequest.setTimestamp(LocalDateTime.now());
            }

            // Set total items count
            reservationRequest.setTotalItems(reservationRequest.getProducts().size());

            context.getLogger().info("Processing order reservation for session: " + 
                    reservationRequest.getSessionId() + 
                    " with " + reservationRequest.getTotalItems() + " items");

            // Get storage connection string from environment
            String connectionString = System.getenv(STORAGE_CONNECTION_STRING_ENV);
            
            if (connectionString == null || connectionString.isEmpty()) {
                context.getLogger().severe("Azure Storage connection string not configured");
                throw new RuntimeException("Storage connection not configured");
            }

            // Initialize blob storage service and upload
            BlobStorageService blobService = new BlobStorageService(connectionString);
            String blobFileName = blobService.uploadOrderReservation(reservationRequest);

            context.getLogger().info("Order reservation successfully stored to blob: " + blobFileName);

        } catch (Exception e) {
            context.getLogger().severe("Error processing order reservation: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to process order reservation", e);
        }
    }

    /**
     * Health check endpoint to verify the function is running.
     * This HTTP endpoint is kept for monitoring purposes.
     */
    @FunctionName("HealthCheck")
    public HttpResponseMessage healthCheck(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET},
                authLevel = AuthorizationLevel.ANONYMOUS,
                route = "health")
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Health check endpoint called");
        
        return request.createResponseBuilder(HttpStatus.OK)
                .body("{\"status\":\"healthy\",\"service\":\"OrderItemsReserver\",\"trigger\":\"ServiceBus\"}")
                .build();
    }
}

