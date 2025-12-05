package com.chtrembl.petstoreapp.service;

import com.chtrembl.petstoreapp.client.OrderServiceClient;
import com.chtrembl.petstoreapp.exception.OrderServiceException;
import com.chtrembl.petstoreapp.model.Order;
import com.chtrembl.petstoreapp.model.OrderReservationRequest;
import com.chtrembl.petstoreapp.model.Product;
import com.chtrembl.petstoreapp.model.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.chtrembl.petstoreapp.config.Constants.COMPLETE_ORDER;
import static com.chtrembl.petstoreapp.config.Constants.OPERATION;
import static com.chtrembl.petstoreapp.config.Constants.ORDER_ID;
import static com.chtrembl.petstoreapp.config.Constants.PRODUCT_ID;
import static com.chtrembl.petstoreapp.config.Constants.QUANTITY;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderManagementService {

    private final User sessionUser;
    private final OrderServiceClient orderServiceClient;
    
    @Autowired(required = false)
    private ServiceBusSenderService serviceBusSenderService;

    public void updateOrder(long productId, int quantity, boolean completeOrder) {
        MDC.put(OPERATION, "updateOrder");
        MDC.put(PRODUCT_ID, String.valueOf(productId));
        MDC.put(QUANTITY, String.valueOf(quantity));
        MDC.put(COMPLETE_ORDER, String.valueOf(completeOrder));

        this.sessionUser.getTelemetryClient()
                .trackEvent(String.format(
                        "PetStoreApp user %s is trying to update an order",
                        this.sessionUser.getName()), this.sessionUser.getCustomEventProperties(), null);

        try {
            Order updatedOrder = buildOrderUpdate(productId, quantity, completeOrder);
            String orderJSON = serializeOrder(updatedOrder);

            Order resultOrder = orderServiceClient.createOrUpdateOrder(orderJSON);
            log.info("Successfully updated order: {}", resultOrder);

            // Reserve order items in blob storage after cart update (only if not completing order)
            if (!completeOrder) {
                reserveOrderItems(resultOrder);
            }

        } catch (FeignException fe) {
            log.error("Unable to update order via Feign client: HTTP {} - {}", fe.status(), fe.getMessage(), fe);
            this.sessionUser.getTelemetryClient().trackException(fe);
            throw new OrderServiceException("Unable to update order via order service", fe);
        } catch (Exception e) {
            log.error("Unexpected error updating order", e);
            this.sessionUser.getTelemetryClient().trackException(e);
            throw new OrderServiceException("Unable to update order via order service", e);
        } finally {
            cleanupMDC();
        }
    }

    public Order retrieveOrder(String orderId) {
        MDC.put(OPERATION, "retrieveOrder");
        MDC.put(ORDER_ID, orderId);

        this.sessionUser.getTelemetryClient()
                .trackEvent(String.format(
                        "PetStoreApp user %s is requesting to retrieve an order from the PetStoreOrderService",
                        this.sessionUser.getName()), this.sessionUser.getCustomEventProperties(), null);

        try {
            Order order = orderServiceClient.getOrder(orderId);
            log.info("Successfully retrieved order: {}", order);
            return order;

        } catch (FeignException.NotFound e) {
            log.debug("Order not found: {}", orderId);
            return null;
        } catch (FeignException fe) {
            log.error("Unable to retrieve order via Feign client: HTTP {} - {}", fe.status(), fe.getMessage(), fe);
            this.sessionUser.getTelemetryClient().trackException(fe);
            throw new OrderServiceException("Unable to retrieve order from order service", fe);
        } catch (Exception e) {
            log.error("Unexpected error retrieving order: {}", orderId, e);
            this.sessionUser.getTelemetryClient().trackException(e);
            throw new OrderServiceException("Unable to retrieve order from order service", e);
        } finally {
            MDC.remove(OPERATION);
            MDC.remove(ORDER_ID);
        }
    }

    private Order buildOrderUpdate(long productId, int quantity, boolean completeOrder) {
        Order updatedOrder = new Order();
        updatedOrder.setId(this.sessionUser.getSessionId());

        String userEmail = this.sessionUser.getEmail();
        if (userEmail != null && !userEmail.trim().isEmpty()) {
            updatedOrder.setEmail(userEmail);
            log.info("Setting order email to: {}", userEmail);
        } else {
            log.warn("User email is not available for session: {}", this.sessionUser.getSessionId());
        }

        if (completeOrder) {
            updatedOrder.setComplete(true);
            log.info("Completing order for session: {}", this.sessionUser.getSessionId());
        } else {
            List<Product> products = new ArrayList<>();
            Product product = new Product();
            product.setId(productId);
            product.setQuantity(quantity);
            products.add(product);
            updatedOrder.setProducts(products);
            log.info("Adding/updating product {} with quantity {} to order", productId, quantity);
        }

        return updatedOrder;
    }

    private String serializeOrder(Order order) throws Exception {
        return new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false)
                .writeValueAsString(order);
    }

    /**
     * Reserves order items by sending a message to Azure Service Bus Queue.
     * The OrderItemsReserver Azure Function (with Service Bus trigger) will process the message
     * and store the order data in blob storage using session ID as file name.
     * This method is called after each cart update to keep the reservation up-to-date.
     * 
     * @param order The order to reserve
     */
    private void reserveOrderItems(Order order) {
        // Skip if serviceBusSenderService is not configured
        if (serviceBusSenderService == null || !serviceBusSenderService.isConfigured()) {
            log.debug("ServiceBusSenderService not configured, skipping order reservation");
            return;
        }

        // Skip if order has no products
        if (order == null || order.getProducts() == null || order.getProducts().isEmpty()) {
            log.debug("Order has no products, skipping order reservation");
            return;
        }

        try {
            log.info("Sending order reservation message to Service Bus for session: {} with {} products", 
                    sessionUser.getSessionId(), order.getProducts().size());

            // Build reservation request
            OrderReservationRequest reservationRequest = new OrderReservationRequest();
            reservationRequest.setSessionId(sessionUser.getSessionId());
            reservationRequest.setEmail(sessionUser.getEmail());
            reservationRequest.setUserName(sessionUser.getName());
            reservationRequest.setProducts(order.getProducts());
            reservationRequest.setTimestamp(LocalDateTime.now());
            reservationRequest.setTotalItems(order.getProducts().size());
            reservationRequest.setStatus(order.isComplete() ? "completed" : "active");

            // Send message to Service Bus
            serviceBusSenderService.sendMessage(reservationRequest);

            log.info("Successfully sent order reservation message to Service Bus");
            
            // Track telemetry
            this.sessionUser.getTelemetryClient()
                    .trackEvent(String.format(
                            "PetStoreApp user %s sent order reservation to Service Bus",
                            this.sessionUser.getName()), 
                            this.sessionUser.getCustomEventProperties(), 
                            null);

        } catch (Exception e) {
            // Log but don't throw - order reservation failure shouldn't break the cart update
            log.error("Failed to send order reservation message to Service Bus: {}", e.getMessage(), e);
            this.sessionUser.getTelemetryClient().trackException(e);
        }
    }

    private void cleanupMDC() {
        MDC.remove(OPERATION);
        MDC.remove(PRODUCT_ID);
        MDC.remove(QUANTITY);
        MDC.remove(COMPLETE_ORDER);
    }
}
