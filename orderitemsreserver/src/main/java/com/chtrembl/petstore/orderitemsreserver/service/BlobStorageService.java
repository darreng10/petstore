package com.chtrembl.petstore.orderitemsreserver.service;

import com.azure.core.http.policy.ExponentialBackoffOptions;
import com.azure.core.http.policy.RetryOptions;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.chtrembl.petstore.orderitemsreserver.model.OrderReservationRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.logging.Logger;

/**
 * Service for handling blob storage operations.
 * Uploads order reservation requests as JSON files to Azure Blob Storage.
 */
public class BlobStorageService {
    
    private static final Logger logger = Logger.getLogger(BlobStorageService.class.getName());
    private static final String CONTAINER_NAME = "order-reservations";
    
    private final BlobServiceClient blobServiceClient;
    private final ObjectMapper objectMapper;
    
    /**
     * Constructor that initializes the blob service client with retry policy.
     * 
     * @param connectionString Azure Storage connection string
     */
    public BlobStorageService(String connectionString) {
        // Configure retry policy with exponential backoff
        ExponentialBackoffOptions backoffOptions = new ExponentialBackoffOptions()
                .setMaxRetries(5)                           // Max retry attempts
                .setBaseDelay(Duration.ofSeconds(2))        // Initial delay: 2 seconds
                .setMaxDelay(Duration.ofSeconds(30));       // Max delay: 30 seconds
        
        RetryOptions retryOptions = new RetryOptions(backoffOptions);
        
        this.blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .retryOptions(retryOptions)
                .buildClient();
        
        logger.info("BlobStorageService initialized with retry policy: max 5 retries, 2-30s backoff");
        
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        // Ensure container exists
        ensureContainerExists();
    }
    
    /**
     * Ensures that the blob container exists, creates it if not.
     */
    private void ensureContainerExists() {
        try {
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(CONTAINER_NAME);
            if (!containerClient.exists()) {
                containerClient.create();
                logger.info("Created blob container: " + CONTAINER_NAME);
            }
        } catch (Exception e) {
            logger.warning("Failed to create or verify blob container: " + e.getMessage());
        }
    }
    
    /**
     * Uploads an order reservation request to blob storage.
     * The file is named using the session ID and will be overwritten if it already exists.
     * 
     * @param request The order reservation request to upload
     * @return The blob file name
     * @throws Exception if upload fails
     */
    public String uploadOrderReservation(OrderReservationRequest request) throws Exception {
        String blobName = generateBlobName(request.getSessionId());
        
        logger.info("Uploading order reservation for session: " + request.getSessionId() + " to blob: " + blobName);
        
        try {
            // Convert request to JSON
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            objectMapper.writeValue(outputStream, request);
            byte[] jsonData = outputStream.toByteArray();
            
            // Get blob client
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(CONTAINER_NAME);
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            
            // Check if blob exists before upload
            boolean blobExists = blobClient.exists();
            logger.info("Blob " + blobName + " exists: " + blobExists + " - will " + (blobExists ? "overwrite" : "create new"));
            
            // Upload blob (overwrite if exists)
            // Using BinaryData for better compatibility with Azure SDK 12.x
            com.azure.core.util.BinaryData binaryData = com.azure.core.util.BinaryData.fromBytes(jsonData);
            
            // Set content type
            BlobHttpHeaders headers = new BlobHttpHeaders()
                    .setContentType("application/json");
            
            // Upload with overwrite=true to replace existing blob
            blobClient.upload(binaryData, true);
            blobClient.setHttpHeaders(headers);
            
            logger.info("Successfully uploaded order reservation to blob: " + blobName);
            return blobName;
            
        } catch (Exception e) {
            logger.severe("Failed to upload order reservation: " + e.getMessage());
            throw new Exception("Failed to upload order reservation to blob storage", e);
        }
    }
    
    /**
     * Generates a blob file name based on the session ID.
     * 
     * @param sessionId The user session ID
     * @return The blob file name
     */
    private String generateBlobName(String sessionId) {
        // Sanitize session ID to be safe for file names
        String sanitizedSessionId = sessionId.replaceAll("[^a-zA-Z0-9-_]", "_");
        return "order-reservation-" + sanitizedSessionId + ".json";
    }
    
    /**
     * Deletes an order reservation blob by session ID.
     * 
     * @param sessionId The user session ID
     * @return true if deleted successfully, false otherwise
     */
    public boolean deleteOrderReservation(String sessionId) {
        String blobName = generateBlobName(sessionId);
        
        try {
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(CONTAINER_NAME);
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            
            if (blobClient.exists()) {
                blobClient.delete();
                logger.info("Deleted order reservation blob: " + blobName);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            logger.warning("Failed to delete order reservation blob: " + e.getMessage());
            return false;
        }
    }
}


