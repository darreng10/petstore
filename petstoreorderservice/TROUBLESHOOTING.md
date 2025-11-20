# Order Service - Troubleshooting Guide

## Common Issues and Solutions

### Issue 1: Application Failed to Start - "CosmosClient bean could not be found"

#### Error Message

```
APPLICATION FAILED TO START

Description:
Parameter 0 of method cosmosContainer in com.chtrembl.petstore.order.config.CosmosDbConfig 
required a bean of type 'com.azure.cosmos.CosmosClient' that could not be found.

The following candidates were found but could not be injected:
  - User-defined bean method 'cosmosClient' in 'CosmosDbConfig' ignored as the bean value is null
```

#### Root Cause

The Order Service **requires Cosmos DB to be configured** to run. If `AZURE_COSMOS_ENABLED` is not set to `true`, or if the connection details are missing, the application will fail to start.

#### Solution

You must configure Cosmos DB for the Order Service to work. You have two options:

##### Option 1: Use Local Cosmos DB Emulator (Development)

**Step 1**: Start Cosmos DB Emulator

```bash
# Using Docker
docker run -d \
  --name cosmosdb-emulator \
  -p 8084:8081 \
  -p 10251:10251 \
  -p 10252:10252 \
  -p 10253:10253 \
  -p 10254:10254 \
  mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator:latest

# Or use the main docker-compose.yml
cd C:\Users\dargr\AzureCourse\cloudx-java-azure-dev\petstore
docker-compose up cosmosdb
```

**Step 2**: Set Environment Variables

PowerShell:
```powershell
$env:AZURE_COSMOS_ENABLED="true"
$env:AZURE_COSMOS_URI="https://localhost:8084"
$env:AZURE_COSMOS_KEY="C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIw/Jw=="
$env:AZURE_COSMOS_DATABASE="petstore"
$env:AZURE_COSMOS_CONTAINER="orders"
```

Bash/Linux:
```bash
export AZURE_COSMOS_ENABLED=true
export AZURE_COSMOS_URI=https://localhost:8084
export AZURE_COSMOS_KEY=C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIw/Jw==
export AZURE_COSMOS_DATABASE=petstore
export AZURE_COSMOS_CONTAINER=orders
```

**Step 3**: Run the Application

```bash
mvn spring-boot:run
```

##### Option 2: Use Azure Cosmos DB (Production)

**Step 1**: Create Cosmos DB Resources

```bash
# Create Cosmos DB account
az cosmosdb create \
  --name petstore-cosmos-db \
  --resource-group <your-resource-group> \
  --locations regionName=eastus

# Create database
az cosmosdb sql database create \
  --account-name petstore-cosmos-db \
  --resource-group <your-resource-group> \
  --name petstore

# Create container
az cosmosdb sql container create \
  --account-name petstore-cosmos-db \
  --resource-group <your-resource-group> \
  --database-name petstore \
  --name orders \
  --partition-key-path "/id" \
  --throughput 400
```

**Step 2**: Get Connection Details

```bash
# Get URI
COSMOS_URI=$(az cosmosdb show \
  --name petstore-cosmos-db \
  --resource-group <your-resource-group> \
  --query "documentEndpoint" \
  --output tsv)

# Get Key
COSMOS_KEY=$(az cosmosdb keys list \
  --name petstore-cosmos-db \
  --resource-group <your-resource-group> \
  --query "primaryMasterKey" \
  --output tsv)

echo "URI: $COSMOS_URI"
echo "Key: $COSMOS_KEY"
```

**Step 3**: Configure App Service

```bash
az webapp config appsettings set \
  --resource-group <your-resource-group> \
  --name <order-service-name> \
  --settings \
    AZURE_COSMOS_ENABLED=true \
    AZURE_COSMOS_URI="$COSMOS_URI" \
    AZURE_COSMOS_KEY="$COSMOS_KEY" \
    AZURE_COSMOS_DATABASE=petstore \
    AZURE_COSMOS_CONTAINER=orders

az webapp restart \
  --resource-group <your-resource-group> \
  --name <order-service-name>
```

---

### Issue 2: "Cosmos DB is not enabled or not configured"

#### Error Message (in logs)

```
WARN c.c.p.order.config.CosmosDbConfig - Cosmos DB is not enabled or not configured. Running in fallback mode.
```

#### Root Cause

The `AZURE_COSMOS_ENABLED` environment variable is not set to `true`.

#### Solution

Set the environment variable:

```bash
# PowerShell
$env:AZURE_COSMOS_ENABLED="true"

# Bash/Linux
export AZURE_COSMOS_ENABLED=true

# Or in application.yml / .env file
AZURE_COSMOS_ENABLED=true
```

Then restart the application.

---

### Issue 3: "Failed to connect to Cosmos DB"

#### Error Message

```
ERROR c.c.p.order.config.CosmosDbConfig - Failed to initialize Cosmos DB container
RuntimeException: Failed to connect to Cosmos DB
```

#### Possible Causes

1. **Incorrect URI** - Check the endpoint format
2. **Incorrect Key** - Verify the key hasn't been regenerated
3. **Database doesn't exist** - Create the database first
4. **Container doesn't exist** - Create the container with partition key `/id`
5. **Firewall rules** (Azure only) - Allow connections from your IP

#### Solution Checklist

✅ **Verify URI Format**
```bash
# Local emulator
AZURE_COSMOS_URI=https://localhost:8084

# Azure (must include :443/)
AZURE_COSMOS_URI=https://petstore-cosmos-db.documents.azure.com:443/
```

✅ **Verify Key**
```bash
# Get fresh key from Azure
az cosmosdb keys list \
  --name petstore-cosmos-db \
  --resource-group <your-resource-group> \
  --query "primaryMasterKey" \
  --output tsv
```

✅ **Verify Database Exists**
```bash
az cosmosdb sql database show \
  --account-name petstore-cosmos-db \
  --resource-group <your-resource-group> \
  --name petstore
```

✅ **Verify Container Exists with Correct Partition Key**
```bash
az cosmosdb sql container show \
  --account-name petstore-cosmos-db \
  --resource-group <your-resource-group> \
  --database-name petstore \
  --name orders \
  --query "resource.partitionKey.paths"
# Should return: ["/id"]
```

✅ **Check Firewall Rules** (Azure Portal)
1. Go to Cosmos DB account
2. Click "Firewall and virtual networks"
3. Add your IP or enable "Allow access from Azure Portal"

---

### Issue 4: OrderRepository Required Error

#### Error Message

```
ERROR c.c.p.o.service.OrderService - =============================================================
ERROR c.c.p.o.service.OrderService - CRITICAL: OrderRepository is not available!
ERROR c.c.p.o.service.OrderService - Cosmos DB is not enabled or not configured properly.
java.lang.IllegalStateException: Order Service requires Cosmos DB to be configured.
```

#### Root Cause

The Order Service was refactored to use Cosmos DB and **no longer supports in-memory storage**. Cosmos DB must be configured.

#### Solution

Follow the steps in **Issue 1** to configure either:
- Local Cosmos DB Emulator, or
- Azure Cosmos DB

The Order Service **cannot run** without Cosmos DB configuration.

---

### Issue 5: Partition Key Mismatch

#### Error Message

```
CosmosException: Partition key provided doesn't match the definition
```

#### Root Cause

The container was created with a different partition key than `/id`.

#### Solution

**Delete and recreate the container** with the correct partition key:

```bash
# Delete existing container
az cosmosdb sql container delete \
  --account-name petstore-cosmos-db \
  --resource-group <your-resource-group> \
  --database-name petstore \
  --name orders \
  --yes

# Create with correct partition key
az cosmosdb sql container create \
  --account-name petstore-cosmos-db \
  --resource-group <your-resource-group> \
  --database-name petstore \
  --name orders \
  --partition-key-path "/id" \
  --throughput 400
```

---

### Issue 6: SSL Certificate Error (Local Emulator)

#### Error Message

```
javax.net.ssl.SSLHandshakeException: PKIX path building failed
```

#### Root Cause

The Cosmos DB Emulator uses a self-signed certificate that Java doesn't trust by default.

#### Solution

**Option 1**: Accept the certificate (development only)

Add JVM option:
```bash
mvn spring-boot:run -Djavax.net.ssl.trustStore=path/to/cosmos/cert
```

**Option 2**: Use Docker (certificate handled automatically)

```bash
docker-compose up
```

**Option 3**: Trust the certificate in Windows

1. Open `https://localhost:8084/_explorer/index.html` in browser
2. Export the certificate
3. Import to Java truststore

---

## Quick Diagnostic Commands

### Check if Service is Running

```bash
# Health check
curl http://localhost:8083/petstoreorderservice/v2/health

# Service info (shows order count)
curl http://localhost:8083/petstoreorderservice/v2/store/info
```

### Check Environment Variables

PowerShell:
```powershell
Get-ChildItem Env: | Where-Object { $_.Name -like "AZURE_COSMOS*" }
```

Bash:
```bash
env | grep AZURE_COSMOS
```

### View Application Logs

```bash
# Local
tail -f logs/application.log

# Azure
az webapp log tail \
  --resource-group <your-resource-group> \
  --name <order-service-name>
```

### Test Cosmos DB Connection

```bash
# Test creating an order
curl -X POST http://localhost:8083/petstoreorderservice/v2/store/order \
  -H "Content-Type: application/json" \
  -d '{
    "id": "68FAE9B1D86B794F0AE0ADD35A437428",
    "email": "test@example.com",
    "status": "placed",
    "complete": false,
    "products": []
  }'

# Test retrieving the order
curl http://localhost:8083/petstoreorderservice/v2/store/order/68FAE9B1D86B794F0AE0ADD35A437428
```

---

## Required Configuration Summary

### Minimum Required Environment Variables

```bash
AZURE_COSMOS_ENABLED=true                    # MUST be true
AZURE_COSMOS_URI=<cosmos-endpoint>           # MUST be set
AZURE_COSMOS_KEY=<cosmos-key>                # MUST be set
AZURE_COSMOS_DATABASE=petstore               # Default: petstore
AZURE_COSMOS_CONTAINER=orders                # Default: orders
```

### Required Azure Resources

1. ✅ Cosmos DB Account (NoSQL API)
2. ✅ Database named `petstore`
3. ✅ Container named `orders` with partition key `/id`

---

## Success Indicators

When everything is configured correctly, you should see:

```
✅ Initializing Cosmos DB client for URI: https://..., Database: petstore, Container: orders
✅ Successfully connected to Cosmos DB container: orders
✅ OrderRepository initialized with Cosmos DB support
✅ Application started successfully
```

---

## Getting Help

If you're still having issues after trying these solutions:

1. **Check the logs** for specific error messages
2. **Verify all environment variables** are set correctly
3. **Test Cosmos DB connection** separately
4. **Review documentation**:
   - `COSMOS_DB_CONFIGURATION.md` - Configuration reference
   - `AZURE_COSMOS_DB_SETUP.md` - Detailed setup guide
   - `README.md` - Service overview

---

**Document Version**: 1.0  
**Last Updated**: 2024  
**Applies To**: Order Service with Cosmos DB Integration

