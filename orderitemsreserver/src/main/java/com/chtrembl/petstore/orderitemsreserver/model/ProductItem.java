package com.chtrembl.petstore.orderitemsreserver.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Product Item model representing a product in the order.
 * Ignores unknown properties to remain compatible with Product model changes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductItem {
    
    /**
     * Product ID
     */
    private Long id;
    
    /**
     * Product name
     */
    private String name;
    
    /**
     * Product category
     */
    private CategoryItem category;
    
    /**
     * Product photo URL
     */
    private String photoURL;
    
    /**
     * Quantity of this product in the cart
     */
    private Integer quantity;
}

