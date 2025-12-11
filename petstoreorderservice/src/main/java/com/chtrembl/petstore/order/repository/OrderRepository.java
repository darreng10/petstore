package com.chtrembl.petstore.order.repository;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.util.CosmosPagedIterable;
import com.chtrembl.petstore.order.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Slf4j
@ConditionalOnProperty(name = "azure.cosmos.enabled", havingValue = "true")
public class OrderRepository {

    private final CosmosContainer cosmosContainer;

    @Autowired
    public OrderRepository(CosmosContainer cosmosContainer) {
        this.cosmosContainer = cosmosContainer;
        log.info("OrderRepository initialized with Cosmos DB support");
    }

    /**
     * Save or update an order in Cosmos DB
     */
    public Order save(Order order) {
        if (cosmosContainer == null) {
            log.error("Cosmos DB container is not available");
            throw new IllegalStateException("Cosmos DB is not configured");
        }

        try {
            log.info("Saving order to Cosmos DB: {}", order.getId());
            
            CosmosItemResponse<Order> response = cosmosContainer.upsertItem(
                    order,
                    new PartitionKey(order.getId()),
                    new CosmosItemRequestOptions()
            );
            
            Order savedOrder = response.getItem();
            
            // Handle case where response.getItem() returns null
            if (savedOrder == null) {
                log.warn("Cosmos DB upsert response returned null item, returning original order");
                return order;
            }
            
            log.info("Successfully saved order {} to Cosmos DB. Request charge: {} RUs",
                    savedOrder.getId(), response.getRequestCharge());
            
            return savedOrder;
        } catch (Exception e) {
            log.error("Error saving order {} to Cosmos DB: {}", order.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to save order to Cosmos DB", e);
        }
    }

    /**
     * Find an order by ID
     */
    public Optional<Order> findById(String orderId) {
        if (cosmosContainer == null) {
            log.error("Cosmos DB container is not available");
            return Optional.empty();
        }

        try {
            log.info("Retrieving order from Cosmos DB: {}", orderId);
            
            CosmosItemResponse<Order> response = cosmosContainer.readItem(
                    orderId,
                    new PartitionKey(orderId),
                    Order.class
            );
            
            Order order = response.getItem();
            log.info("Successfully retrieved order {} from Cosmos DB. Request charge: {} RUs",
                    orderId, response.getRequestCharge());
            
            return Optional.of(order);
        } catch (com.azure.cosmos.CosmosException e) {
            if (e.getStatusCode() == 404) {
                log.info("Order {} not found in Cosmos DB", orderId);
                return Optional.empty();
            }
            log.error("Error retrieving order {} from Cosmos DB: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve order from Cosmos DB", e);
        } catch (Exception e) {
            log.error("Unexpected error retrieving order {} from Cosmos DB: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve order from Cosmos DB", e);
        }
    }

    /**
     * Delete an order by ID
     */
    public void deleteById(String orderId) {
        if (cosmosContainer == null) {
            log.error("Cosmos DB container is not available");
            throw new IllegalStateException("Cosmos DB is not configured");
        }

        try {
            log.info("Deleting order from Cosmos DB: {}", orderId);
            
            CosmosItemResponse<Object> response = cosmosContainer.deleteItem(
                    orderId,
                    new PartitionKey(orderId),
                    new CosmosItemRequestOptions()
            );
            
            log.info("Successfully deleted order {} from Cosmos DB. Request charge: {} RUs",
                    orderId, response.getRequestCharge());
        } catch (com.azure.cosmos.CosmosException e) {
            if (e.getStatusCode() == 404) {
                log.warn("Order {} not found in Cosmos DB for deletion", orderId);
                return;
            }
            log.error("Error deleting order {} from Cosmos DB: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete order from Cosmos DB", e);
        } catch (Exception e) {
            log.error("Unexpected error deleting order {} from Cosmos DB: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete order from Cosmos DB", e);
        }
    }

    /**
     * Check if an order exists
     */
    public boolean existsById(String orderId) {
        return findById(orderId).isPresent();
    }

    /**
     * Count all orders (useful for monitoring)
     */
    public long count() {
        if (cosmosContainer == null) {
            log.error("Cosmos DB container is not available");
            return 0;
        }

        try {
            String query = "SELECT VALUE COUNT(1) FROM c";
            CosmosPagedIterable<Long> response = cosmosContainer.queryItems(
                    query,
                    new CosmosQueryRequestOptions(),
                    Long.class
            );
            
            return response.stream().findFirst().orElse(0L);
        } catch (Exception e) {
            log.error("Error counting orders in Cosmos DB: {}", e.getMessage(), e);
            return 0;
        }
    }
}

