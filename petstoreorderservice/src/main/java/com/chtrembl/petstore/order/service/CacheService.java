package com.chtrembl.petstore.order.service;

import com.chtrembl.petstore.order.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CacheService {

    private final OrderRepository orderRepository;

    @Autowired
    public CacheService(@Autowired(required = false) OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Get the total number of orders in Cosmos DB
     * This method is kept for backwards compatibility with existing endpoints
     */
    public long getOrdersCacheSize() {
        if (orderRepository == null) {
            log.warn("OrderRepository is not available - Cosmos DB not configured");
            return 0;
        }
        
        try {
            long count = orderRepository.count();
            log.debug("Total orders in Cosmos DB: {}", count);
            return count;
        } catch (Exception e) {
            log.warn("Could not get orders count from Cosmos DB: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Scheduled cleanup task - can be used for maintenance operations
     * Currently logs order count for monitoring purposes
     */
    @Scheduled(fixedRate = 43200000) // Every 12 hours
    public void performMaintenanceTasks() {
        if (orderRepository == null) {
            log.debug("Skipping maintenance tasks - Cosmos DB not configured");
            return;
        }
        
        try {
            long orderCount = orderRepository.count();
            log.info("Cosmos DB maintenance check - Total orders: {}", orderCount);
            
            // Additional maintenance tasks can be added here
            // e.g., cleanup old completed orders, archive data, etc.
            
        } catch (Exception e) {
            log.error("Error during maintenance tasks: {}", e.getMessage(), e);
        }
    }
}