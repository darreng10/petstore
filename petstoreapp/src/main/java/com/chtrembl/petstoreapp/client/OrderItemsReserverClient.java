package com.chtrembl.petstoreapp.client;

import com.chtrembl.petstoreapp.config.FeignConfig;
import com.chtrembl.petstoreapp.model.OrderReservationRequest;
import com.chtrembl.petstoreapp.model.ReservationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for OrderItemsReserver Azure Function service.
 * Handles communication with the order items reservation service.
 */
@FeignClient(
        name = "orderitemsreserver",
        url = "${petstore.service.orderitemsreserver.url}",
        configuration = FeignConfig.class
)
public interface OrderItemsReserverClient {

    /**
     * Reserves order items by sending cart data to Azure Function.
     * The function stores the order data in blob storage.
     * 
     * @param request The order reservation request containing session ID and products
     * @return Reservation response with success status and blob file name
     */
    @PostMapping("/api/reserve")
    ReservationResponse reserveOrderItems(@RequestBody OrderReservationRequest request);

    /**
     * Health check endpoint to verify the service is available.
     * 
     * @return Health status response
     */
    @GetMapping("/api/health")
    String getHealth();
}







