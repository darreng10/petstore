package com.chtrembl.petstore.orderitemsreserver.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.chtrembl.petstore.orderitemsreserver.model.OrderReservationRequest;
import com.chtrembl.petstore.orderitemsreserver.model.ReservationResponse;
import com.chtrembl.petstore.orderitemsreserver.service.BlobStorageService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Azure Function with HTTP Trigger for Order Items Reservation.
 * 
 * This function receives order reservation requests from the petstore application
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
     * HTTP trigger function to reserve order items.
     * 
     * This function is triggered by HTTP POST requests and processes order reservation requests.
     * It validates the request, stores it to blob storage, and returns a response.
     * 
     * @param request The HTTP request containing the order reservation data
     * @param context The execution context
     * @return HTTP response with reservation status
     */
    @FunctionName("ReserveOrderItems")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS,
                route = "reserve")
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("OrderItemsReserver function triggered");

        try {
            // Get request body
            Optional<String> requestBody = request.getBody();
            
            if (!requestBody.isPresent() || requestBody.get().isEmpty()) {
                context.getLogger().warning("Empty request body received");
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Request body is empty"))
                        .build();
            }

            // Parse request body to OrderReservationRequest
            OrderReservationRequest reservationRequest = objectMapper.readValue(
                    requestBody.get(), 
                    OrderReservationRequest.class);

            // Validate request
            if (reservationRequest.getSessionId() == null || reservationRequest.getSessionId().isEmpty()) {
                context.getLogger().warning("Session ID is missing in request");
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Session ID is required"))
                        .build();
            }

            if (reservationRequest.getProducts() == null || reservationRequest.getProducts().isEmpty()) {
                context.getLogger().warning("Products list is empty in request");
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("Products list cannot be empty"))
                        .build();
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
                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(createErrorResponse("Storage connection not configured"))
                        .build();
            }

            // Initialize blob storage service and upload
            BlobStorageService blobService = new BlobStorageService(connectionString);
            String blobFileName = blobService.uploadOrderReservation(reservationRequest);

            // Create success response
            ReservationResponse response = new ReservationResponse(
                    true,
                    "Order reservation stored successfully",
                    reservationRequest.getSessionId(),
                    blobFileName,
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );

            context.getLogger().info("Order reservation successfully stored to blob: " + blobFileName);

            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(response)
                    .build();

        } catch (Exception e) {
            context.getLogger().severe("Error processing order reservation: " + e.getMessage());
            e.printStackTrace();
            
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to process order reservation: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Health check endpoint to verify the function is running.
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
                .body("{\"status\":\"healthy\",\"service\":\"OrderItemsReserver\"}")
                .build();
    }

    /**
     * Creates an error response object.
     */
    private ReservationResponse createErrorResponse(String message) {
        return new ReservationResponse(
                false,
                message,
                null,
                null,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }
}

