package com.chtrembl.petstoreapp.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Order Reservation Request sent to OrderItemsReserver Azure Function.
 * Contains order details and product list for blob storage.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderReservationRequest {
    
    /**
     * Session ID used for tracking user session and file naming
     */
    private String sessionId;
    
    /**
     * User email if available
     */
    private String email;
    
    /**
     * User name
     */
    private String userName;
    
    /**
     * List of products in the cart
     */
    private List<Product> products;
    
    /**
     * Timestamp when the reservation was created
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    /**
     * Total number of items in the cart
     */
    private int totalItems;
    
    /**
     * Order status
     */
    private String status;
}







