package com.chtrembl.petstore.orderitemsreserver.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Order Reservation Request containing order details and product list.
 * This model is used to capture cart updates during a user session.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
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
    private List<ProductItem> products;
    
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

