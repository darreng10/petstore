# How Azure Portal Configuration Works - Detailed Explanation

## The Configuration Flow

When you set configuration in Azure Portal, here's exactly what happens:

```
┌─────────────────────────────────────────────────────────────────┐
│  1. Azure Portal - Application Settings                         │
│     You set: AZURE_COSMOS_URI = https://mydb.documents.azure... │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│  2. Azure App Service - Environment Variables                   │
│     App Service converts these to environment variables         │
│     Available to your Java application as: $AZURE_COSMOS_URI   │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│  3. Spring Boot - application.yml                               │
│     Reads environment variables:                                │
│     azure:                                                      │
│       cosmos:                                                   │
│         uri: ${AZURE_COSMOS_URI:}                              │
│         key: ${AZURE_COSMOS_KEY:}                              │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│  4. Java Code - CosmosDbConfig.java                            │
│     @Value("${azure.cosmos.uri:}") injects the value          │
│     private String cosmosUri; // Now contains your URI         │
└─────────────────────────────────────────────────────────────────┘
```

## Step-by-Step: Setting Up in Azure Portal

### Step 1: Get Your Cosmos DB Connection Details

First, you need to get the URI and Key from your Cosmos DB account:

#### **Option A: Using Azure Portal**

1. Go to **Azure Portal** (portal.azure.com)
2. Navigate to your **Cosmos DB account** (e.g., "petstore-cosmos-db")
3. Click **"Keys"** in the left menu (under Settings)
4. You'll see:
   - **URI**: `https://petstore-cosmos-db.documents.azure.com:443/`
   - **PRIMARY KEY**: A long string like `abc123...xyz==`
5. **Copy both values** (you'll need them in Step 2)

![Cosmos DB Keys](https://docs.microsoft.com/azure/cosmos-db/media/secure-access-to-data/nosql-database-security-master-key-portal.png)

#### **Option B: Using Azure CLI**

```bash
# Get the Cosmos DB account name
COSMOS_ACCOUNT="petstore-cosmos-db"
RESOURCE_GROUP="your-resource-group"

# Get URI (endpoint)
az cosmosdb show \
  --name $COSMOS_ACCOUNT \
  --resource-group $RESOURCE_GROUP \
  --query "documentEndpoint" \
  --output tsv

# Output: https://petstore-cosmos-db.documents.azure.com:443/

# Get Primary Key
az cosmosdb keys list \
  --name $COSMOS_ACCOUNT \
  --resource-group $RESOURCE_GROUP \
  --query "primaryMasterKey" \
  --output tsv

# Output: abc123def456...xyz==
```

### Step 2: Configure Your Order Service in Azure Portal

Now, set these values in your Order Service App Service:

#### **Using Azure Portal (Recommended for beginners)**

1. Go to **Azure Portal** (portal.azure.com)

2. Navigate to your **Order Service** App Service
   - Example name: "petstore-orderservice"

3. Click **"Configuration"** in the left menu (under Settings)

4. Click **"+ New application setting"**

5. **Add these 5 settings ONE BY ONE:**

   **Setting 1:**
   ```
   Name:  AZURE_COSMOS_ENABLED
   Value: true
   ```
   Click "OK"

   **Setting 2:**
   ```
   Name:  AZURE_COSMOS_URI
   Value: https://petstore-cosmos-db.documents.azure.com:443/
   ```
   ⚠️ **IMPORTANT**: Use YOUR actual URI from Step 1!
   Click "OK"

   **Setting 3:**
   ```
   Name:  AZURE_COSMOS_KEY
   Value: <paste-your-primary-key-here>
   ```
   ⚠️ **IMPORTANT**: Paste YOUR actual key from Step 1!
   Click "OK"

   **Setting 4:**
   ```
   Name:  AZURE_COSMOS_DATABASE
   Value: petstore
   ```
   Click "OK"

   **Setting 5:**
   ```
   Name:  AZURE_COSMOS_CONTAINER
   Value: orders
   ```
   Click "OK"

6. Click **"Save"** at the top of the Configuration page

7. Click **"Continue"** when prompted (this will restart your app)

8. Wait for the app to restart (30-60 seconds)

#### **Using Azure CLI (For automation)**

```bash
# Set your variables
RESOURCE_GROUP="your-resource-group"
ORDER_SERVICE="petstore-orderservice"
COSMOS_ACCOUNT="petstore-cosmos-db"

# Get Cosmos DB credentials
COSMOS_URI=$(az cosmosdb show \
  --name $COSMOS_ACCOUNT \
  --resource-group $RESOURCE_GROUP \
  --query "documentEndpoint" \
  --output tsv)

COSMOS_KEY=$(az cosmosdb keys list \
  --name $COSMOS_ACCOUNT \
  --resource-group $RESOURCE_GROUP \
  --query "primaryMasterKey" \
  --output tsv)

# Configure App Service with all settings at once
az webapp config appsettings set \
  --resource-group $RESOURCE_GROUP \
  --name $ORDER_SERVICE \
  --settings \
    AZURE_COSMOS_ENABLED=true \
    AZURE_COSMOS_URI="$COSMOS_URI" \
    AZURE_COSMOS_KEY="$COSMOS_KEY" \
    AZURE_COSMOS_DATABASE=petstore \
    AZURE_COSMOS_CONTAINER=orders

# Restart the app
az webapp restart \
  --resource-group $RESOURCE_GROUP \
  --name $ORDER_SERVICE

echo "Configuration complete!"
```

## How Spring Boot Reads These Settings

Let's look at what happens in your code:

### 1. application.yml Configuration

Your `application.yml` file looks like this:

```yaml
azure:
  cosmos:
    enabled: ${AZURE_COSMOS_ENABLED:false}
    uri: ${AZURE_COSMOS_URI:}
    key: ${AZURE_COSMOS_KEY:}
    database: ${AZURE_COSMOS_DATABASE:petstore}
    container: ${AZURE_COSMOS_CONTAINER:orders}
```

**Explanation:**
- `${AZURE_COSMOS_URI:}` means:
  - Read the environment variable `AZURE_COSMOS_URI`
  - If not found, use empty string (`:` means "default to")
- The environment variables come from Azure Portal Application Settings

### 2. Java Code Injection

In `CosmosDbConfig.java`:

```java
@Value("${azure.cosmos.uri:}")
private String cosmosUri;

@Value("${azure.cosmos.key:}")
private String cosmosKey;
```

**What happens:**
1. Spring Boot loads `application.yml`
2. It sees `${AZURE_COSMOS_URI:}`
3. It looks for environment variable `AZURE_COSMOS_URI`
4. Azure App Service provides this from Application Settings
5. Spring injects the value into `cosmosUri` field

## Complete Example Walkthrough

Let's say you have:
- **Cosmos DB Account**: `my-petstore-db`
- **Resource Group**: `petstore-rg`
- **Order Service**: `petstore-order-api`

### Step-by-Step Commands

```bash
# 1. Get your Cosmos DB details
az cosmosdb show \
  --name my-petstore-db \
  --resource-group petstore-rg \
  --query "documentEndpoint" \
  --output tsv

# Output: https://my-petstore-db.documents.azure.com:443/
# ⬆️ This is your AZURE_COSMOS_URI

az cosmosdb keys list \
  --name my-petstore-db \
  --resource-group petstore-rg \
  --query "primaryMasterKey" \
  --output tsv

# Output: abc123def456...xyz789==
# ⬆️ This is your AZURE_COSMOS_KEY

# 2. Set these in your Order Service
az webapp config appsettings set \
  --resource-group petstore-rg \
  --name petstore-order-api \
  --settings \
    AZURE_COSMOS_ENABLED=true \
    AZURE_COSMOS_URI="https://my-petstore-db.documents.azure.com:443/" \
    AZURE_COSMOS_KEY="abc123def456...xyz789==" \
    AZURE_COSMOS_DATABASE=petstore \
    AZURE_COSMOS_CONTAINER=orders

# 3. Restart the app
az webapp restart \
  --resource-group petstore-rg \
  --name petstore-order-api
```

## Verification

### 1. Check Configuration in Azure Portal

1. Go to your Order Service
2. Click **Configuration**
3. You should see all 5 settings:
   - AZURE_COSMOS_ENABLED
   - AZURE_COSMOS_URI
   - AZURE_COSMOS_KEY
   - AZURE_COSMOS_DATABASE
   - AZURE_COSMOS_CONTAINER

### 2. Check Application Logs

```bash
# View logs in real-time
az webapp log tail \
  --resource-group petstore-rg \
  --name petstore-order-api
```

**Look for these SUCCESS messages:**
```
✅ Initializing Cosmos DB client for URI: https://my-petstore-db.documents.azure.com:443/, Database: petstore, Container: orders
✅ Successfully connected to Cosmos DB container: orders
✅ OrderRepository initialized with Cosmos DB support
```

**If you see these ERROR messages:**
```
❌ Cosmos DB is enabled but URI is not configured!
❌ CRITICAL: OrderRepository is not available!
```
→ Go back to Step 2 and ensure all settings are added correctly.

### 3. Test the API

```bash
# Get service URL
SERVICE_URL=$(az webapp show \
  --resource-group petstore-rg \
  --name petstore-order-api \
  --query "defaultHostName" \
  --output tsv)

# Test health endpoint
curl https://$SERVICE_URL/petstoreorderservice/v2/health

# Should return:
# {
#   "status": "UP",
#   "service": "order-service",
#   ...
# }
```

## Common Mistakes to Avoid

### ❌ Mistake 1: Missing Port Number in URI

**Wrong:**
```
AZURE_COSMOS_URI=https://petstore-cosmos-db.documents.azure.com/
```

**Correct:**
```
AZURE_COSMOS_URI=https://petstore-cosmos-db.documents.azure.com:443/
```
⚠️ Must include `:443/` at the end!

### ❌ Mistake 2: Forgetting to Save and Restart

After adding settings in Azure Portal:
1. Click **"Save"** ✅
2. Click **"Restart"** or wait for auto-restart ✅

Settings don't take effect until restart!

### ❌ Mistake 3: Using Wrong Key Type

Use the **Primary Key** or **Secondary Key**, NOT:
- ❌ Read-only keys
- ❌ Connection strings
- ❌ Resource tokens

### ❌ Mistake 4: Typos in Setting Names

Must be EXACT:
- ✅ `AZURE_COSMOS_URI` (correct)
- ❌ `AZURE_COSMOS_URL` (wrong)
- ❌ `COSMOSDB_URI` (wrong)

## Security Best Practices

### Option 1: Use Azure Key Vault (Recommended for Production)

Instead of storing the key directly in Application Settings:

1. **Store key in Key Vault:**
   ```bash
   az keyvault secret set \
     --vault-name my-keyvault \
     --name CosmosDbKey \
     --value "abc123...xyz=="
   ```

2. **Reference from App Service:**
   ```
   Name:  AZURE_COSMOS_KEY
   Value: @Microsoft.KeyVault(SecretUri=https://my-keyvault.vault.azure.net/secrets/CosmosDbKey/)
   ```

### Option 2: Use Managed Identity (Most Secure)

Configure Order Service to use Managed Identity for Cosmos DB access:

```bash
# Enable Managed Identity on Order Service
az webapp identity assign \
  --resource-group petstore-rg \
  --name petstore-order-api

# Grant access to Cosmos DB
# (Requires Cosmos DB RBAC configuration)
```

## Summary

### The Simple Answer:

1. **Get URI and Key** from Cosmos DB → Keys section
2. **Add to Order Service** → Configuration → Application settings
3. **Save and Restart** the app
4. **Spring Boot reads** environment variables automatically
5. **Java code uses** the injected values to connect

### The Configuration Path:

```
Azure Portal Settings 
    ↓
Environment Variables 
    ↓
application.yml (reads ${AZURE_COSMOS_URI:}) 
    ↓
Java Code (@Value annotation) 
    ↓
Cosmos DB Connection
```

## Need Help?

- **Can't find Cosmos DB Keys?** → Go to Cosmos DB → Keys (left menu)
- **Settings not taking effect?** → Make sure you clicked Save and Restart
- **Connection errors?** → Check TROUBLESHOOTING.md
- **Want to test locally?** → See COSMOS_DB_CONFIGURATION.md

---

**Key Takeaway**: When you set Application Settings in Azure Portal, they become environment variables that Spring Boot automatically reads through the `${VARIABLE_NAME}` syntax in `application.yml`. Your Java code then uses `@Value` annotations to inject these values.


