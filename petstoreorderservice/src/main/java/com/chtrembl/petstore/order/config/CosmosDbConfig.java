package com.chtrembl.petstore.order.config;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class CosmosDbConfig {

    @Value("${azure.cosmos.uri:}")
    private String cosmosUri;

    @Value("${azure.cosmos.key:}")
    private String cosmosKey;

    @Value("${azure.cosmos.database:petstore}")
    private String databaseName;

    @Value("${azure.cosmos.container:orders}")
    private String containerName;

    @Bean
    @ConditionalOnProperty(name = "azure.cosmos.enabled", havingValue = "true")
    public CosmosClient cosmosClient() {
        if (cosmosUri == null || cosmosUri.isEmpty()) {
            log.error("Cosmos DB is enabled but URI is not configured!");
            throw new IllegalStateException("AZURE_COSMOS_URI must be set when Cosmos DB is enabled");
        }

        if (cosmosKey == null || cosmosKey.isEmpty()) {
            log.error("Cosmos DB is enabled but key is not configured!");
            throw new IllegalStateException("AZURE_COSMOS_KEY must be set when Cosmos DB is enabled");
        }

        log.info("Initializing Cosmos DB client for URI: {}, Database: {}, Container: {}",
                cosmosUri, databaseName, containerName);

        return new CosmosClientBuilder()
                .endpoint(cosmosUri)
                .key(cosmosKey)
                .buildClient();
    }

    @Bean
    @ConditionalOnProperty(name = "azure.cosmos.enabled", havingValue = "true")
    public CosmosContainer cosmosContainer(CosmosClient cosmosClient) {
        try {
            CosmosDatabase database = cosmosClient.getDatabase(databaseName);
            CosmosContainer container = database.getContainer(containerName);
            
            log.info("Successfully connected to Cosmos DB container: {}", containerName);
            return container;
        } catch (Exception e) {
            log.error("Failed to initialize Cosmos DB container: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to connect to Cosmos DB", e);
        }
    }
}

