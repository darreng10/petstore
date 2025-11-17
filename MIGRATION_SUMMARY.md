# Migration Summary: In-Memory Storage to Azure PostgreSQL

This document summarizes the changes made to migrate both microservices from in-memory storage to Azure PostgreSQL database.

## Overview

Both `petstorepetservice` and `petstoreproductservice` have been updated to use PostgreSQL database instead of in-memory storage with YAML-based data loading.

## Changes Made

### 1. Dependencies (pom.xml)

**Added to both services:**
- `spring-boot-starter-data-jpa` - Spring Data JPA support
- `postgresql` - PostgreSQL JDBC driver

### 2. Entity Models

**Pet Service:**
- `Pet.java` - Converted to JPA entity with annotations
  - Added `@Entity`, `@Table`, `@Id`, `@GeneratedValue`
  - Configured relationships: `@ManyToOne` for Category, `@ManyToMany` for Tags
  - Added `@Enumerated` for Status enum
- `Category.java` - Converted to JPA entity
- `Tag.java` - Converted to JPA entity

**Product Service:**
- `Product.java` - Converted to JPA entity with annotations
  - Same structure as Pet entity
- `Category.java` - Converted to JPA entity
- `Tag.java` - Converted to JPA entity

### 3. Repository Layer

**Created new JPA repositories:**
- `petstorepetservice/repository/PetRepository.java`
  - Extends `JpaRepository<Pet, Long>`
  - Custom query method: `findByStatusIn(List<String> status)`
  
- `petstoreproductservice/repository/ProductRepository.java`
  - Extends `JpaRepository<Product, Long>`
  - Custom query method: `findByStatusIn(List<String> status)`

### 4. Service Layer Updates

**Pet Service (`PetService.java`):**
- Replaced `DataPreload` dependency with `PetRepository`
- Updated methods to use repository:
  - `findPetsByStatus()` - Uses `petRepository.findByStatusIn()`
  - `findPetById()` - Uses `petRepository.findById()`
  - `getAllPets()` - Uses `petRepository.findAll()`
  - `getPetCount()` - Uses `petRepository.count()`

**Product Service (`ProductService.java`):**
- Same changes as Pet Service
- Uses `ProductRepository` for all database operations

### 5. Configuration Files

**application.yml (both services):**

Added PostgreSQL configuration:
```yaml
spring:
  datasource:
    url: ${AZURE_POSTGRESQL_URL:jdbc:postgresql://localhost:5432/petstore}
    username: ${AZURE_POSTGRESQL_USER:petstore}
    password: ${AZURE_POSTGRESQL_PASSWORD:password}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
  sql:
    init:
      mode: always
      continue-on-error: false
```

Removed:
- All YAML-based data preload configuration

### 6. Data Initialization Scripts

**Created SQL files for initial data:**
- `petstorepetservice/src/main/resources/data.sql`
  - Initializes 31 pets with categories and tags
  - Uses `ON CONFLICT DO NOTHING` for idempotent inserts
  
- `petstoreproductservice/src/main/resources/data.sql`
  - Initializes 11 products with categories and tags
  - Uses `ON CONFLICT DO NOTHING` for idempotent inserts

### 7. Removed Files

**Deleted obsolete classes:**
- `petstorepetservice/model/DataPreload.java`
- `petstoreproductservice/model/DataPreload.java`

**Updated:**
- `PetServiceApplication.java` - Removed DataPreload bean

### 8. Documentation

**Created new documentation files:**
- `petstorepetservice/AZURE_POSTGRESQL_SETUP.md` - Detailed setup guide
- `petstoreproductservice/AZURE_POSTGRESQL_SETUP.md` - Detailed setup guide
- Updated `README.md` for both services with database information

**Created Docker Compose files:**
- `petstorepetservice/docker-compose.yml` - Local development setup
- `petstoreproductservice/docker-compose.yml` - Local development setup

## Database Schema

### Tables Created

**Pet Service:**
1. `categories` - Pet categories (Dog, Cat, Fish)
2. `tags` - Pet tags (doggie, kittie, fishy, small, large)
3. `pets` - Main pet data
4. `pet_tags` - Many-to-many relationship table

**Product Service:**
1. `categories` - Product categories (Dog Toy, Dog Food, Cat Toy, etc.)
2. `tags` - Product tags (small, large)
3. `products` - Main product data
4. `product_tags` - Many-to-many relationship table

## Environment Variables

### Required for Azure Deployment

Both services require these environment variables:
- `AZURE_POSTGRESQL_URL` - JDBC connection URL
- `AZURE_POSTGRESQL_USER` - Database username
- `AZURE_POSTGRESQL_PASSWORD` - Database password

### Optional
- `PETSTOREPETSERVICE_SERVER_PORT` - Pet service port (default: 8080)
- `PETSTOREPRODUCTSERVICE_SERVER_PORT` - Product service port (default: 8080)

## Migration Benefits

1. **Persistence** - Data survives application restarts
2. **Scalability** - Multiple instances can share the same database
3. **ACID Compliance** - Transactional integrity for data operations
4. **Query Performance** - Database indexing and optimization
5. **Data Management** - Standard SQL tools for backup, restore, and maintenance
6. **Azure Integration** - Seamless integration with Azure PostgreSQL managed service

## Local Development

### Using Docker Compose

```bash
# Pet Service
cd petstorepetservice
docker-compose up

# Product Service
cd petstoreproductservice
docker-compose up
```

### Using Local PostgreSQL

```bash
# Create database
createdb petstore

# Set environment variables
export AZURE_POSTGRESQL_URL=jdbc:postgresql://localhost:5432/petstore
export AZURE_POSTGRESQL_USER=petstore
export AZURE_POSTGRESQL_PASSWORD=password

# Run services
cd petstorepetservice && mvn spring-boot:run
cd petstoreproductservice && mvn spring-boot:run
```

## Testing

Both services maintain the same REST API endpoints. No API changes were made, ensuring backward compatibility.

### Endpoints remain unchanged:
- Pet Service: 
  - `GET /pets` - Get all pets
  - `GET /pets/findByStatus?status=available` - Find by status
  - `GET /pets/{petId}` - Find by ID
  
- Product Service:
  - `GET /products` - Get all products
  - `GET /products/findByStatus?status=available` - Find by status
  - `GET /products/{productId}` - Find by ID

## Rollback Plan

If needed, you can rollback by:
1. Reverting to the previous commit before this migration
2. The in-memory storage with YAML configuration will be restored

## Next Steps

1. Deploy services to Azure App Service
2. Configure Application Settings with database credentials
3. Verify firewall rules on Azure PostgreSQL server
4. Test endpoints to ensure data is correctly loaded
5. Monitor application logs for any database connection issues
6. Consider implementing database migrations with Flyway or Liquibase for future schema changes

