# Azure Cosmos DB Configuration Guide

This guide explains how to configure the Order Service to use Azure Cosmos DB for storing order information, replacing the previous in-memory cache.

## Overview

The Order Service has been updated to use **Azure Cosmos DB** for persistent storage of order data. This provides:
- ✅ Persistent storage across restarts
- ✅ Scalable NoSQL database
- ✅ Global distribution capabilities
- ✅ Automatic indexing
- ✅ Low latency access

## Environment Variables

### Required Variables (for Azure Deployment)

| Variable | Description | Example |
|----------|-------------|---------|
| `AZURE_COSMOS_ENABLED` | Enable Cosmos DB (set to `true` in Azure) | `true` |
| `AZURE_COSMOS_URI` | Cosmos DB account endpoint URI | `https://mypetstore.documents.azure.com:443/` |
| `AZURE_COSMOS_KEY` | Cosmos DB account primary/secondary key | `your-cosmos-key` |
| `AZURE_COSMOS_DATABASE` | Database name | `petstore` |
| `AZURE_COSMOS_CONTAINER` | Container name for orders | `orders` |

### Optional Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PETSTOREORDERSERVICE_SERVER_PORT` | `8080` | Service port |
| `PETSTOREPRODUCTSERVICE_URL` | `http://localhost:8082` | Product service URL |

## Azure Cosmos DB Setup

### Step 1: Create Cosmos DB Account

#### Using Azure Portal

1. Go to **Azure Portal** → **Create a resource** → **Azure Cosmos DB**
2. Choose **API**: Select **NoSQL** (Core SQL)
3. Configure settings:
   - **Resource Group**: Your resource group
   - **Account Name**: e.g., `petstore-cosmos-db`
   - **Location**: Same as your app services
   - **Capacity mode**: Provisioned throughput (or Serverless for dev/test)
   - **Apply Free Tier Discount**: Yes (if available)
4. Click **Review + Create** → **Create**
5. Wait for deployment to complete

#### Using Azure CLI

```bash
# Variables
RESOURCE_GROUP="your-resource-group"
COSMOS_ACCOUNT_NAME="petstore-cosmos-db"
LOCATION="eastus"

# Create Cosmos DB account
az cosmosdb create \
  --name $COSMOS_ACCOUNT_NAME \
  --resource-group $RESOURCE_GROUP \
  --locations regionName=$LOCATION failoverPriority=0 \
  --default-consistency-level Session \
  --enable-automatic-failover false
```

### Step 2: Create Database and Container

#### Using Azure Portal

1. Go to your Cosmos DB account
2. Click **Data Explorer** → **New Container**
3. Configure:
   - **Database id**: `petstore` (or create new)
   - **Container id**: `orders`
   - **Partition key**: `/id`
   - **Throughput**: 400 RU/s (minimum, can scale later)
4. Click **OK**

#### Using Azure CLI

```bash
# Create database
az cosmosdb sql database create \
  --account-name $COSMOS_ACCOUNT_NAME \
  --resource-group $RESOURCE_GROUP \
  --name petstore

# Create container with partition key /id
az cosmosdb sql container create \
  --account-name $COSMOS_ACCOUNT_NAME \
  --resource-group $RESOURCE_GROUP \
  --database-name petstore \
  --name orders \
  --partition-key-path "/id" \
  --throughput 400
```

### Step 3: Get Connection Information

#### Using Azure Portal

1. Go to your Cosmos DB account
2. Click **Keys** (under Settings)
3. Copy:
   - **URI**: The endpoint URL
   - **PRIMARY KEY**: The primary key

#### Using Azure CLI

```bash
# Get URI
az cosmosdb show \
  --name $COSMOS_ACCOUNT_NAME \
  --resource-group $RESOURCE_GROUP \
  --query "documentEndpoint" \
  --output tsv

# Get primary key
az cosmosdb keys list \
  --name $COSMOS_ACCOUNT_NAME \
  --resource-group $RESOURCE_GROUP \
  --query "primaryMasterKey" \
  --output tsv
```

## Configuration for Different Environments

### Local Development (Without Cosmos DB)

By default, the service runs with Cosmos DB disabled:

```yaml
# application.yml (default)
azure:
  cosmos:
    enabled: false
```

⚠️ **Note**: With Cosmos DB disabled, the service will fail to start. For local development, either:
1. Set up Azure Cosmos DB Emulator (see below)
2. Use a dev/test Cosmos DB account
3. Keep using the old cache-based version

### Local Development (With Cosmos DB Emulator)

**Install Cosmos DB Emulator:**
- Download from: https://aka.ms/cosmosdb-emulator
- Or use Docker:

```bash
docker run -d \
  --name cosmosdb-emulator \
  -p 8081:8081 \
  -p 10251:10251 \
  -p 10252:10252 \
  -p 10253:10253 \
  -p 10254:10254 \
  mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator
```

**Configure environment variables:**

```bash
# PowerShell
$env:AZURE_COSMOS_ENABLED="true"
$env:AZURE_COSMOS_URI="https://localhost:8081"
$env:AZURE_COSMOS_KEY="C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIw/Jw=="
$env:AZURE_COSMOS_DATABASE="petstore"
$env:AZURE_COSMOS_CONTAINER="orders"

# Bash/Linux
export AZURE_COSMOS_ENABLED=true
export AZURE_COSMOS_URI=https://localhost:8081
export AZURE_COSMOS_KEY=C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIw/Jw==
export AZURE_COSMOS_DATABASE=petstore
export AZURE_COSMOS_CONTAINER=orders
```

**Run the service:**

```bash
mvn spring-boot:run
```

### Azure App Service Configuration

#### Using Azure Portal

1. Go to your **Order Service** App Service
2. Click **Configuration** → **Application settings**
3. Add these settings:

```
AZURE_COSMOS_ENABLED=true
AZURE_COSMOS_URI=https://<your-cosmos-account>.documents.azure.com:443/
AZURE_COSMOS_KEY=<your-primary-key>
AZURE_COSMOS_DATABASE=petstore
AZURE_COSMOS_CONTAINER=orders
```

4. Click **Save** → **Continue**
5. **Restart** the app service

#### Using Azure CLI

```bash
RESOURCE_GROUP="your-resource-group"
APP_SERVICE_NAME="your-orderservice-name"
COSMOS_URI="https://petstore-cosmos-db.documents.azure.com:443/"
COSMOS_KEY="your-cosmos-key"

az webapp config appsettings set \
  --resource-group $RESOURCE_GROUP \
  --name $APP_SERVICE_NAME \
  --settings \
    AZURE_COSMOS_ENABLED=true \
    AZURE_COSMOS_URI="$COSMOS_URI" \
    AZURE_COSMOS_KEY="$COSMOS_KEY" \
    AZURE_COSMOS_DATABASE=petstore \
    AZURE_COSMOS_CONTAINER=orders

# Restart the app
az webapp restart \
  --resource-group $RESOURCE_GROUP \
  --name $APP_SERVICE_NAME
```

## Data Model

### Order Document Structure

```json
{
  "id": "68FAE9B1D86B794F0AE0ADD35A437428",
  "email": "customer@example.com",
  "status": "placed",
  "complete": false,
  "products": [
    {
      "id": 1,
      "name": "Dog Food",
      "quantity": 2,
      "photoURL": "https://example.com/photo.jpg"
    }
  ]
}
```

### Partition Key

- **Partition Key**: `/id` (order ID)
- Each order is its own partition (good for point reads/writes)
- Order IDs are typically session IDs (32-character hex strings)

## Performance Considerations

### Request Units (RUs)

Cosmos DB charges based on Request Units:
- **Read order**: ~1 RU
- **Write/Update order**: ~5-10 RUs
- **Query orders**: Varies based on complexity

### Throughput Settings

**Development/Test:**
- Start with: 400 RU/s (minimum)
- Cost: ~$24/month

**Production:**
- Monitor actual usage
- Consider autoscale: 400-4000 RU/s
- Or serverless for unpredictable workloads

### Optimization Tips

1. **Use Point Reads**: Reading by `id` and partition key is cheapest
2. **Batch Operations**: Group related writes together
3. **Index Only What You Need**: Default indexing is good for this use case
4. **Monitor RU Consumption**: Check logs for RU charges

## Monitoring and Troubleshooting

### Check Application Logs

```bash
# View real-time logs
az webapp log tail \
  --resource-group <your-resource-group> \
  --name <your-orderservice-name>
```

Look for:
- ✅ `Successfully connected to Cosmos DB container: orders`
- ✅ `Successfully saved order ... Request charge: X RUs`
- ✗ `Failed to connect to Cosmos DB`
- ✗ `Cosmos DB container is not available`

### Common Issues

#### Issue: "Cosmos DB is not configured"

**Symptoms:**
- Service fails to start
- Logs show: "Cosmos DB client is not available"

**Solution:**
1. Verify `AZURE_COSMOS_ENABLED=true`
2. Check URI and KEY are set correctly
3. Ensure database and container exist

#### Issue: Connection Timeout

**Symptoms:**
- `SocketTimeoutException` or connection errors

**Solution:**
1. Check firewall rules in Cosmos DB
2. Enable "Allow access from Azure Portal" and "Allow access from Azure datacenters"
3. For local dev: Add your IP to firewall rules

#### Issue: High RU Consumption

**Symptoms:**
- 429 (Too Many Requests) errors
- Slow performance

**Solution:**
1. Monitor RU/s usage in Azure Portal
2. Increase provisioned throughput
3. Enable autoscale
4. Optimize queries

#### Issue: Authorization Failed

**Symptoms:**
- `Unauthorized` or `Forbidden` errors

**Solution:**
1. Verify the Cosmos DB key is correct
2. Check if key has been regenerated
3. Ensure URI includes `:443/` port

### Cosmos DB Metrics

Monitor in Azure Portal → Your Cosmos DB → Metrics:

- **Total Requests**: Number of operations
- **Total Request Units**: RU consumption
- **Throttled Requests**: 429 errors
- **Storage**: Data size
- **Availability**: Uptime percentage

## Migration from Cache to Cosmos DB

### Data Loss Warning

⚠️ **Important**: Switching from in-memory cache to Cosmos DB means:
- Existing orders in cache will be lost on restart
- This is expected behavior for ephemeral cart data
- For production, plan migration carefully

### Migration Steps

1. **Deploy Cosmos DB** resources first
2. **Test** thoroughly in dev/staging environment
3. **Configure** App Service with Cosmos DB settings
4. **Deploy** updated application
5. **Monitor** logs and metrics after deployment

## Security Best Practices

### 1. Use Managed Identity (Recommended)

Instead of storing keys in configuration:

```bash
# Enable managed identity
az webapp identity assign \
  --resource-group $RESOURCE_GROUP \
  --name $APP_SERVICE_NAME

# Grant Cosmos DB access
IDENTITY_PRINCIPAL_ID=$(az webapp identity show \
  --resource-group $RESOURCE_GROUP \
  --name $APP_SERVICE_NAME \
  --query principalId \
  --output tsv)

# Assign role (requires Azure Cosmos DB account owner)
az cosmosdb sql role assignment create \
  --account-name $COSMOS_ACCOUNT_NAME \
  --resource-group $RESOURCE_GROUP \
  --role-definition-name "Cosmos DB Built-in Data Contributor" \
  --principal-id $IDENTITY_PRINCIPAL_ID \
  --scope "/dbs/petstore/colls/orders"
```

### 2. Use Azure Key Vault

Store Cosmos DB connection strings in Key Vault:

```bash
# Store in Key Vault
az keyvault secret set \
  --vault-name <your-keyvault> \
  --name CosmosDbKey \
  --value "<cosmos-key>"

# Reference in App Service
AZURE_COSMOS_KEY=@Microsoft.KeyVault(SecretUri=https://<vault>.vault.azure.net/secrets/CosmosDbKey/)
```

### 3. Network Security

- Enable **Private Endpoints** for Cosmos DB
- Use **Virtual Network** integration
- Configure **Firewall rules** to allow only App Service IPs

## Cost Optimization

### Serverless Option

For dev/test or unpredictable workloads:

```bash
az cosmosdb create \
  --name $COSMOS_ACCOUNT_NAME \
  --resource-group $RESOURCE_GROUP \
  --capabilities EnableServerless \
  --locations regionName=$LOCATION
```

- Pay only for RUs consumed
- No minimum throughput charge
- Good for < 1M operations/month

### Autoscale

For production with variable load:

```bash
az cosmosdb sql container throughput update \
  --account-name $COSMOS_ACCOUNT_NAME \
  --resource-group $RESOURCE_GROUP \
  --database-name petstore \
  --name orders \
  --max-throughput 4000
```

- Scales between 10% and 100% of max
- Billed per second
- Good for spiky workloads

## Additional Resources

- [Azure Cosmos DB Documentation](https://docs.microsoft.com/en-us/azure/cosmos-db/)
- [Cosmos DB Java SDK](https://docs.microsoft.com/en-us/azure/cosmos-db/sql/sql-api-sdk-java-v4)
- [Cosmos DB Emulator](https://docs.microsoft.com/en-us/azure/cosmos-db/local-emulator)
- [Best Practices](https://docs.microsoft.com/en-us/azure/cosmos-db/best-practice-dotnet)

## Quick Reference Commands

```bash
# View Cosmos DB details
az cosmosdb show \
  --name <cosmos-account> \
  --resource-group <resource-group>

# List containers
az cosmosdb sql container list \
  --account-name <cosmos-account> \
  --resource-group <resource-group> \
  --database-name petstore

# Query data (using portal or SDK)
SELECT * FROM c WHERE c.email = "customer@example.com"

# Monitor RU consumption
az monitor metrics list \
  --resource <cosmos-resource-id> \
  --metric TotalRequestUnits \
  --aggregation Total
```

