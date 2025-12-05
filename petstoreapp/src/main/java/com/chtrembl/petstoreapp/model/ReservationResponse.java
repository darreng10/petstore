package com.chtrembl.petstoreapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response from OrderItemsReserver Azure Function.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {
    
    /**
     * Operation success status
     */
    private boolean success;
    
    /**
     * Response message
     */
    private String message;
    
    /**
     * Session ID that was processed
     */
    private String sessionId;
    
    /**
     * Blob storage file name/path
     */
    private String blobFileName;
    
    /**
     * Timestamp of the operation
     */
    private String timestamp;
}







