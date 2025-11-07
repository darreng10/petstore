# Order Items Reserver - Azure Functions Service

This Azure Functions service handles order item reservations from the PetStore application. It receives cart update requests via HTTP and stores them as JSON files in Azure Blob Storage.

## Features

- **HTTP Trigger**: Receives order reservation requests via POST endpoint
- **Blob Storage Integration**: Stores order data as JSON files in Azure Blob Storage
- **Session-Based File Naming**: Uses session ID for unique file identification
- **Automatic File Overwrite**: Updates existing files when cart is modified in the same session
- **Container Deployment**: Dockerized for flexible deployment options
- **Health Check Endpoint**: Monitoring endpoint to verify service status

## Architecture

```
PetStoreApp (Shopping Cart Update) 
    → HTTP POST /api/reserve 
    → OrderItemsReserver Function 
    → Azure Blob Storage (JSON files named by session ID)
```

## API Endpoints

### Reserve Order Items
- **Endpoint**: `POST /api/reserve`
- **Auth Level**: Function
- **Content-Type**: `application/json`

**Request Body**:
```json
{
  "sessionId": "session-123",
  "email": "user@example.com",
  "userName": "Guest",
  "products": [
    {
      "id": 1,
      "name": "Dog Food",
      "category": {
        "id": 1,
        "name": "Food"
      },
      "photoURL": "https://...",
      "quantity": 2
    }
  ],
  "timestamp": "2025-11-05T10:30:00",
  "totalItems": 1,
  "status": "active"
}
```

**Response**:
```json
{
  "success": true,
  "message": "Order reservation stored successfully",
  "sessionId": "session-123",
  "blobFileName": "order-reservation-session-123.json",
  "timestamp": "2025-11-05T10:30:00"
}
```

### Health Check
- **Endpoint**: `GET /api/health`
- **Auth Level**: Anonymous

**Response**:
```json
{
  "status": "healthy",
  "service": "OrderItemsReserver"
}
```

## Configuration

### Environment Variables

- `AzureWebJobsStorage`: Azure Storage connection string for blob storage
- `FUNCTIONS_WORKER_RUNTIME`: Set to `java`
- `FUNCTIONS_EXTENSION_VERSION`: Set to `~4`

### Blob Storage

- **Container Name**: `order-reservations`
- **File Naming Pattern**: `order-reservation-{sessionId}.json`
- **Content Type**: `application/json`

## Local Development

### Prerequisites
- Java 21
- Maven 3.6+
- Azure Functions Core Tools v4
- Azure Storage Emulator or Azurite

### Run Locally

1. Start Azure Storage Emulator or Azurite:
```bash
azurite --silent --location ./azurite --debug ./azurite/debug.log
```

2. Build the project:
```bash
mvn clean package
```

3. Run the function locally:
```bash
mvn azure-functions:run
```

The function will be available at: `http://localhost:7071/api/reserve`

## Container Deployment

### Build Docker Image

```bash
docker build -t orderitemsreserver:latest .
```

### Run Container Locally

```bash
docker run -p 8080:80 \
  -e AzureWebJobsStorage="<your-connection-string>" \
  orderitemsreserver:latest
```

### Deploy to Azure Container Apps

```bash
# Login to Azure
az login

# Create resource group (if not exists)
az group create --name petstore-rg --location eastus

# Create container registry
az acr create --resource-group petstore-rg \
  --name petstoreacr --sku Basic

# Build and push image
az acr build --registry petstoreacr \
  --image orderitemsreserver:latest .

# Create container app
az containerapp create \
  --name orderitemsreserver \
  --resource-group petstore-rg \
  --image petstoreacr.azurecr.io/orderitemsreserver:latest \
  --environment <your-container-app-env> \
  --ingress external --target-port 80 \
  --env-vars AzureWebJobsStorage="<your-connection-string>"
```

## Testing

### Test with curl

```bash
curl -X POST http://localhost:7071/api/reserve \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "test-session-123",
    "email": "test@example.com",
    "userName": "Test User",
    "products": [{
      "id": 1,
      "name": "Test Product",
      "quantity": 1
    }],
    "status": "active"
  }'
```

### Health Check

```bash
curl http://localhost:7071/api/health
```

## Integration with PetStore Application

The PetStore application calls this service whenever a shopping cart is updated:

1. User adds/removes items from cart
2. PetStoreApp sends order reservation request to this function
3. Function stores/updates the JSON file in blob storage
4. Response is returned to PetStoreApp

See the PetStoreApp integration code in:
- `petstoreapp/src/main/java/com/chtrembl/petstoreapp/client/OrderItemsReserverClient.java`
- `petstoreapp/src/main/java/com/chtrembl/petstoreapp/service/PetStoreFacadeService.java`

## Monitoring

The function logs important events:
- Request reception
- Validation errors
- Blob storage operations
- Success/failure responses

Logs can be viewed in:
- Azure Application Insights (when deployed)
- Console output (local development)

## Error Handling

The function handles various error scenarios:
- Empty request body → `400 Bad Request`
- Missing session ID → `400 Bad Request`
- Empty products list → `400 Bad Request`
- Storage connection issues → `500 Internal Server Error`
- Blob upload failures → `500 Internal Server Error`

## License

Copyright © 2025 PetStore Application




