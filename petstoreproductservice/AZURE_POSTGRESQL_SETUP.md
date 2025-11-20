# Azure PostgreSQL Configuration Guide

This guide explains how to configure the Product Service to connect to Azure PostgreSQL Database.

## Environment Variables

The application requires the following environment variables to connect to Azure PostgreSQL:

### Required Variables

- `AZURE_POSTGRESQL_URL` - JDBC connection URL for your Azure PostgreSQL database
  - Format: `jdbc:postgresql://<server-name>.postgres.database.azure.com:5432/<database-name>?sslmode=require`
  - Example: `jdbc:postgresql://mypetstore.postgres.database.azure.com:5432/petstore?sslmode=require`

- `AZURE_POSTGRESQL_USER` - Database username
  - Format: `<username>@<server-name>`
  - Example: `petstore_admin@mypetstore`

- `AZURE_POSTGRESQL_PASSWORD` - Database password
  - Example: `YourSecurePassword123!`

### Optional Variables

- `PETSTOREPRODUCTSERVICE_SERVER_PORT` - Server port (default: 8080)

## Local Development Setup

For local development, you can use default values:

```bash
export AZURE_POSTGRESQL_URL=jdbc:postgresql://localhost:5432/petstore
export AZURE_POSTGRESQL_USER=petstore
export AZURE_POSTGRESQL_PASSWORD=password
```

## Azure App Service Configuration

When deploying to Azure App Service, configure these as Application Settings:

1. Navigate to your App Service in Azure Portal
2. Go to Configuration → Application settings
3. Add the following settings:
   - `AZURE_POSTGRESQL_URL`
   - `AZURE_POSTGRESQL_USER`
   - `AZURE_POSTGRESQL_PASSWORD`

## Database Schema

The application uses Hibernate with `ddl-auto: update` mode, which will automatically:
- Create tables if they don't exist
- Update existing tables based on entity definitions

The database schema includes:
- `products` - Main product entity table
- `categories` - Product categories
- `tags` - Tags for products
- `product_tags` - Many-to-many relationship table

## Initial Data Loading

The application includes a `data.sql` file that will automatically populate the database with initial product data on startup. This runs with `spring.sql.init.mode: always`.

## Database Connection Pooling

Spring Boot automatically configures HikariCP connection pooling. You can customize pool settings by adding these properties to your environment:

```
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
```

## Troubleshooting

### Connection Issues

1. Ensure your Azure PostgreSQL server allows connections from your IP/Azure service
2. Verify SSL is enabled if required by your server
3. Check that the database exists before starting the application
4. Verify firewall rules in Azure PostgreSQL settings

### SSL/TLS Configuration

Azure PostgreSQL requires SSL by default. The connection URL should include `?sslmode=require`:

```
jdbc:postgresql://yourserver.postgres.database.azure.com:5432/petstore?sslmode=require
```

### Performance Tuning

For production environments, consider:
- Adjusting connection pool sizes based on load
- Enabling query logging during testing: `spring.jpa.show-sql=true`
- Monitoring slow queries and adding appropriate indexes

