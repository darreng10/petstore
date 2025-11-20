# OrderService

A back-end Java Spring Boot microservice dedicated to managing Pet Store orders.

## Database

This service uses **Azure Cosmos DB** for persistent storage of order data. Previously used in-memory cache has been replaced with Cosmos DB NoSQL database.

### Features
- Persistent order storage with Cosmos DB
- NoSQL document-based data model
- Global distribution capabilities
- Automatic indexing and low-latency access
- Scalable throughput

## Configuration

See [AZURE_COSMOS_DB_SETUP.md](AZURE_COSMOS_DB_SETUP.md) for detailed configuration instructions.

### Quick Start with Azure Cosmos DB Emulator

Run the service locally with Cosmos DB Emulator using Docker Compose:

```bash
docker-compose up
```

This will start:
- Azure Cosmos DB Emulator on port 8081
- Order Service on port 8083

### Environment Variables

**Required for Azure Deployment:**
- `AZURE_COSMOS_ENABLED` - Enable Cosmos DB (set to `true`)
- `AZURE_COSMOS_URI` - Cosmos DB endpoint URI
- `AZURE_COSMOS_KEY` - Cosmos DB access key
- `AZURE_COSMOS_DATABASE` - Database name (default: `petstore`)
- `AZURE_COSMOS_CONTAINER` - Container name (default: `orders`)

**Optional:**
- `PETSTOREORDERSERVICE_SERVER_PORT` - Server port (default: 8080)
- `PETSTOREPRODUCTSERVICE_URL` - Product service URL (default: `http://localhost:8082`)

## Building

```bash
mvn clean package
```

## Running Locally

### With Cosmos DB Emulator

```bash
# Set environment variables
export AZURE_COSMOS_ENABLED=true
export AZURE_COSMOS_URI=https://localhost:8081
export AZURE_COSMOS_KEY=C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIw/Jw==
export AZURE_COSMOS_DATABASE=petstore
export AZURE_COSMOS_CONTAINER=orders
export PETSTOREPRODUCTSERVICE_URL=http://localhost:8082

# Run the application
mvn spring-boot:run
```

### With Azure Cosmos DB

```bash
# Set environment variables with your Azure Cosmos DB credentials
export AZURE_COSMOS_ENABLED=true
export AZURE_COSMOS_URI=https://your-cosmos-account.documents.azure.com:443/
export AZURE_COSMOS_KEY=your-cosmos-key
export AZURE_COSMOS_DATABASE=petstore
export AZURE_COSMOS_CONTAINER=orders
export PETSTOREPRODUCTSERVICE_URL=http://localhost:8082

# Run the application
mvn spring-boot:run
```

## API Endpoints

- **Swagger UI**: http://localhost:8083/swagger-ui.html
- **Health Check**: http://localhost:8083/petstoreorderservice/v2/health
- **Service Info**: http://localhost:8083/petstoreorderservice/v2/store/info
- **Place Order**: POST http://localhost:8083/petstoreorderservice/v2/store/order
- **Get Order**: GET http://localhost:8083/petstoreorderservice/v2/store/order/{orderId}

## Data Model

Orders are stored as JSON documents in Cosmos DB:

```json
{
  "id": "68FAE9B1D86B794F0AE0ADD35A437428",
  "email": "customer@example.com",
  "status": "placed",
  "complete": false,
  "products": [
    {
      "id": 1,
      "name": "Product Name",
      "quantity": 2,
      "photoURL": "https://example.com/photo.jpg"
    }
  ]
}
```

## Deployment to Azure

1. Create Azure Cosmos DB account and container (see [AZURE_COSMOS_DB_SETUP.md](AZURE_COSMOS_DB_SETUP.md))
2. Configure App Service application settings with Cosmos DB credentials
3. Deploy the application
4. Verify health endpoint shows successful connection