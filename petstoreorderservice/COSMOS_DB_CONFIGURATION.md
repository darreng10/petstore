# Order Service - Cosmos DB Configuration Requirements

## Quick Reference

This document outlines all variables and settings required for the Order Service to work with Azure Cosmos DB.

## ⚠️ IMPORTANT

**The Order Service REQUIRES Cosmos DB to be configured.** The application will NOT start without proper Cosmos DB configuration. If you see startup errors, see the [TROUBLESHOOTING.md](TROUBLESHOOTING.md) guide.

---

## Required Environment Variables

| Variable | Required | Description | Example |
|----------|----------|-------------|---------|
| `AZURE_COSMOS_ENABLED` | ✅ Yes | Enable/disable Cosmos DB | `true` |
| `AZURE_COSMOS_URI` | ✅ Yes | Cosmos DB endpoint URI | `https://petstore-cosmos.documents.azure.com:443/` |
| `AZURE_COSMOS_KEY` | ✅ Yes | Cosmos DB primary or secondary key | `your-cosmos-db-key-here` |
| `AZURE_COSMOS_DATABASE` | ✅ Yes | Database name | `petstore` |
| `AZURE_COSMOS_CONTAINER` | ✅ Yes | Container name for orders | `orders` |

### Default Values

If not specified, these defaults are used (configured in `application.yml`):

```yaml
AZURE_COSMOS_ENABLED: false      # ⚠️ Must be set to 'true' for Cosmos DB
AZURE_COSMOS_URI: ""             # ⚠️ Must be provided
AZURE_COSMOS_KEY: ""             # ⚠️ Must be provided
AZURE_COSMOS_DATABASE: petstore  # ✅ Default is 'petstore'
AZURE_COSMOS_CONTAINER: orders   # ✅ Default is 'orders'
```

---

## Optional Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PETSTOREORDERSERVICE_SERVER_PORT` | `8080` | Port the service runs on |
| `PETSTOREPRODUCTSERVICE_URL` | `http://localhost:8082` | Product service URL for validation |

---

## Azure Cosmos DB Resources Required

### 1. Cosmos DB Account

**Requirements:**
- API: **NoSQL (Core SQL)**
- Consistency Level: Session (recommended)
- Region: Same as your App Services

**Creation:**
```bash
az cosmosdb create \
  --name petstore-cosmos-db \
  --resource-group <your-resource-group> \
  --locations regionName=eastus \
  --default-consistency-level Session
```

### 2. Database

**Requirements:**
- Name: `petstore` (or match `AZURE_COSMOS_DATABASE` variable)

**Creation:**
```bash
az cosmosdb sql database create \
  --account-name petstore-cosmos-db \
  --resource-group <your-resource-group> \
  --name petstore
```

### 3. Container

**Requirements:**
- Name: `orders` (or match `AZURE_COSMOS_CONTAINER` variable)
- **Partition Key**: `/id` (CRITICAL - must be exactly this)
- Throughput: 400 RU/s minimum (or Serverless)

**Creation:**
```bash
az cosmosdb sql container create \
  --account-name petstore-cosmos-db \
  --resource-group <your-resource-group> \
  --database-name petstore \
  --name orders \
  --partition-key-path "/id" \
  --throughput 400
```

⚠️ **IMPORTANT**: The partition key `/id` must match the Order model's `id` field.

---

## Configuration by Environment

### 🔧 Local Development (with Cosmos DB Emulator)

#### Docker Compose (Recommended)

**File**: `docker-compose.yml`

```yaml
environment:
  AZURE_COSMOS_ENABLED: "true"
  AZURE_COSMOS_URI: "https://cosmosdb:8081"  # or "https://localhost:8084" from host
  AZURE_COSMOS_KEY: "C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIw/Jw=="
  AZURE_COSMOS_DATABASE: "petstore"
  AZURE_COSMOS_CONTAINER: "orders"
```

#### Manual Setup (PowerShell)

```powershell
$env:AZURE_COSMOS_ENABLED="true"
$env:AZURE_COSMOS_URI="https://localhost:8084"
$env:AZURE_COSMOS_KEY="C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIw/Jw=="
$env:AZURE_COSMOS_DATABASE="petstore"
$env:AZURE_COSMOS_CONTAINER="orders"

mvn spring-boot:run
```

#### Manual Setup (Bash/Linux)

```bash
export AZURE_COSMOS_ENABLED=true
export AZURE_COSMOS_URI=https://localhost:8084
export AZURE_COSMOS_KEY=C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIw/Jw==
export AZURE_COSMOS_DATABASE=petstore
export AZURE_COSMOS_CONTAINER=orders

mvn spring-boot:run
```

**Note**: The Cosmos DB Emulator key is a well-known development key. It's the same for all local installations.

---

### ☁️ Azure App Service

#### Using Azure Portal

1. Navigate to your **Order Service** App Service
2. Go to **Configuration** → **Application settings**
3. Click **+ New application setting** and add:

```
Name: AZURE_COSMOS_ENABLED
Value: true

Name: AZURE_COSMOS_URI
Value: https://petstore-cosmos-db.documents.azure.com:443/

Name: AZURE_COSMOS_KEY
Value: <your-primary-key-from-cosmos-db>

Name: AZURE_COSMOS_DATABASE
Value: petstore

Name: AZURE_COSMOS_CONTAINER
Value: orders
```

4. Click **Save**
5. Click **Restart** (required for changes to take effect)

#### Using Azure CLI

```bash
# Set variables
RESOURCE_GROUP="your-resource-group"
ORDER_SERVICE_NAME="your-orderservice-app-name"
COSMOS_ACCOUNT="petstore-cosmos-db"

# Get Cosmos DB URI
COSMOS_URI=$(az cosmosdb show \
  --name $COSMOS_ACCOUNT \
  --resource-group $RESOURCE_GROUP \
  --query "documentEndpoint" \
  --output tsv)

# Get Cosmos DB key
COSMOS_KEY=$(az cosmosdb keys list \
  --name $COSMOS_ACCOUNT \
  --resource-group $RESOURCE_GROUP \
  --query "primaryMasterKey" \
  --output tsv)

# Configure App Service
az webapp config appsettings set \
  --resource-group $RESOURCE_GROUP \
  --name $ORDER_SERVICE_NAME \
  --settings \
    AZURE_COSMOS_ENABLED=true \
    AZURE_COSMOS_URI="$COSMOS_URI" \
    AZURE_COSMOS_KEY="$COSMOS_KEY" \
    AZURE_COSMOS_DATABASE=petstore \
    AZURE_COSMOS_CONTAINER=orders

# Restart the service
az webapp restart \
  --resource-group $RESOURCE_GROUP \
  --name $ORDER_SERVICE_NAME
```

---

## Quick Setup Checklist

### ☑️ Azure Resources

- [ ] Create Cosmos DB account
- [ ] Create database named `petstore`
- [ ] Create container named `orders` with partition key `/id`
- [ ] Note down the URI (endpoint)
- [ ] Note down the primary key

### ☑️ Local Development

- [ ] Install/run Cosmos DB Emulator (or Docker container)
- [ ] Set `AZURE_COSMOS_ENABLED=true`
- [ ] Set `AZURE_COSMOS_URI` (emulator: `https://localhost:8084`)
- [ ] Set `AZURE_COSMOS_KEY` (emulator well-known key)
- [ ] Create database and container (automatic with emulator)

### ☑️ Azure Deployment

- [ ] Add all 5 environment variables to App Service
- [ ] Verify URI includes `:443/` at the end
- [ ] Restart App Service after configuration
- [ ] Check logs for successful connection
- [ ] Test order creation via API

---

## How to Get Cosmos DB Connection Information

### Method 1: Azure Portal

1. Go to your **Cosmos DB account**
2. Click **Keys** (under Settings in left menu)
3. Copy:
   - **URI**: The endpoint URL
   - **PRIMARY KEY**: The primary key value

### Method 2: Azure CLI

```bash
# Get URI
az cosmosdb show \
  --name petstore-cosmos-db \
  --resource-group <your-resource-group> \
  --query "documentEndpoint" \
  --output tsv

# Output: https://petstore-cosmos-db.documents.azure.com:443/

# Get Primary Key
az cosmosdb keys list \
  --name petstore-cosmos-db \
  --resource-group <your-resource-group> \
  --query "primaryMasterKey" \
  --output tsv

# Output: <your-key-here>
```

---

## Configuration Validation

### Verify Configuration is Correct

#### Check Application Logs

**Success messages to look for:**

```
✅ Initializing Cosmos DB client for URI: https://..., Database: petstore, Container: orders
✅ Successfully connected to Cosmos DB container: orders
✅ Successfully saved order ... Request charge: X RUs
✅ Successfully retrieved order ... Request charge: X RUs
```

**Error messages indicate issues:**

```
❌ Cosmos DB is not enabled or not configured
❌ Failed to connect to Cosmos DB
❌ Cosmos DB container is not available
```

#### Test Endpoints

```bash
# Health check
curl http://localhost:8083/petstoreorderservice/v2/health

# Service info (shows order count from Cosmos DB)
curl http://localhost:8083/petstoreorderservice/v2/store/info

# Create order
curl -X POST http://localhost:8083/petstoreorderservice/v2/store/order \
  -H "Content-Type: application/json" \
  -d '{
    "id": "68FAE9B1D86B794F0AE0ADD35A437428",
    "email": "test@example.com",
    "status": "placed",
    "products": []
  }'

# Get order
curl http://localhost:8083/petstoreorderservice/v2/store/order/68FAE9B1D86B794F0AE0ADD35A437428
```

---

## Common Configuration Issues

### Issue 1: Service Won't Start

**Symptom**: Application fails to start

**Check:**
- [ ] `AZURE_COSMOS_ENABLED` is set to `true`
- [ ] `AZURE_COSMOS_URI` is not empty
- [ ] `AZURE_COSMOS_KEY` is not empty

### Issue 2: Connection Failed

**Symptom**: "Failed to connect to Cosmos DB"

**Check:**
- [ ] URI is correct and includes `:443/` for Azure (`:8081` for emulator)
- [ ] Key is correct (not expired or regenerated)
- [ ] Database `petstore` exists
- [ ] Container `orders` exists
- [ ] Firewall allows connection (Azure only)

### Issue 3: Partition Key Error

**Symptom**: "Partition key is not specified"

**Check:**
- [ ] Container was created with partition key `/id`
- [ ] Order ID is not null when saving

### Issue 4: SSL/TLS Error (Local Emulator)

**Symptom**: SSL certificate errors with emulator

**Solution:**
```bash
# Windows: Trust the emulator certificate
# Or add this JVM option:
-Djavax.net.ssl.trustStore=path/to/emulator/cert
```

---

## Environment Variable Summary Table

### All Variables at a Glance

| Variable | Required | Default | Local Dev Value | Azure Value |
|----------|----------|---------|-----------------|-------------|
| `AZURE_COSMOS_ENABLED` | ✅ Yes | `false` | `true` | `true` |
| `AZURE_COSMOS_URI` | ✅ Yes | `""` | `https://localhost:8084` | `https://<account>.documents.azure.com:443/` |
| `AZURE_COSMOS_KEY` | ✅ Yes | `""` | `C2y6yDjf5...XIw/Jw==` | `<from-azure-portal>` |
| `AZURE_COSMOS_DATABASE` | No | `petstore` | `petstore` | `petstore` |
| `AZURE_COSMOS_CONTAINER` | No | `orders` | `orders` | `orders` |
| `PETSTOREORDERSERVICE_SERVER_PORT` | No | `8080` | `8083` | `8080` |
| `PETSTOREPRODUCTSERVICE_URL` | No | `http://localhost:8082` | varies | `https://<product-service>.azurewebsites.net` |

---

## Minimal Configuration Examples

### Absolute Minimum (Azure)

```bash
# These 3 are CRITICAL
AZURE_COSMOS_ENABLED=true
AZURE_COSMOS_URI=https://petstore-cosmos-db.documents.azure.com:443/
AZURE_COSMOS_KEY=your-key-here

# These use defaults (petstore/orders) - can be omitted
# AZURE_COSMOS_DATABASE=petstore
# AZURE_COSMOS_CONTAINER=orders
```

### Absolute Minimum (Local Emulator)

```bash
# These 3 are CRITICAL
AZURE_COSMOS_ENABLED=true
AZURE_COSMOS_URI=https://localhost:8084
AZURE_COSMOS_KEY=C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIw/Jw==

# These use defaults - can be omitted
# AZURE_COSMOS_DATABASE=petstore
# AZURE_COSMOS_CONTAINER=orders
```

---

## Next Steps

1. ✅ Set all required environment variables
2. ✅ Create Cosmos DB resources (account, database, container)
3. ✅ Start/restart the Order Service
4. ✅ Check logs for successful connection
5. ✅ Test order creation and retrieval
6. ✅ Monitor RU consumption in Azure Portal

## Additional Resources

- **Full Setup Guide**: See `AZURE_COSMOS_DB_SETUP.md`
- **Migration Details**: See `MIGRATION_SUMMARY.md`
- **Local Testing**: See `../LOCAL_TESTING_GUIDE.md`
- **Service README**: See `README.md`

---

## Quick Copy-Paste Templates

### For .env File (Local)

```bash
AZURE_COSMOS_ENABLED=true
AZURE_COSMOS_URI=https://localhost:8084
AZURE_COSMOS_KEY=C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIw/Jw==
AZURE_COSMOS_DATABASE=petstore
AZURE_COSMOS_CONTAINER=orders
PETSTOREORDERSERVICE_SERVER_PORT=8083
```

### For Azure CLI Script

```bash
#!/bin/bash

# Configuration
RESOURCE_GROUP="your-rg"
ORDER_SERVICE="your-order-service"
COSMOS_ACCOUNT="petstore-cosmos-db"

# Get Cosmos DB credentials
COSMOS_URI=$(az cosmosdb show --name $COSMOS_ACCOUNT --resource-group $RESOURCE_GROUP --query documentEndpoint -o tsv)
COSMOS_KEY=$(az cosmosdb keys list --name $COSMOS_ACCOUNT --resource-group $RESOURCE_GROUP --query primaryMasterKey -o tsv)

# Configure App Service
az webapp config appsettings set \
  --resource-group $RESOURCE_GROUP \
  --name $ORDER_SERVICE \
  --settings \
    AZURE_COSMOS_ENABLED=true \
    AZURE_COSMOS_URI="$COSMOS_URI" \
    AZURE_COSMOS_KEY="$COSMOS_KEY" \
    AZURE_COSMOS_DATABASE=petstore \
    AZURE_COSMOS_CONTAINER=orders

# Restart
az webapp restart --resource-group $RESOURCE_GROUP --name $ORDER_SERVICE

echo "Configuration complete!"
```

---

**Document Version**: 1.0  
**Last Updated**: 2024  
**Service**: PetStore Order Service  
**Cosmos DB SDK**: azure-cosmos v4.53.1

