# ProductService

A Java Spring Boot microservice on the back end, responsible for delivering Pet Product data to consumers.

## Database

This service uses **Azure PostgreSQL** for data persistence. Previously used in-memory storage has been replaced with a PostgreSQL database.

### Features
- JPA/Hibernate integration for database operations
- Automatic schema generation and updates
- Initial data loading via SQL scripts
- Connection pooling with HikariCP

## Configuration

See [AZURE_POSTGRESQL_SETUP.md](AZURE_POSTGRESQL_SETUP.md) for detailed configuration instructions.

### Quick Start with Docker

Run the service locally with PostgreSQL using Docker Compose:

```bash
docker-compose up
```

This will start:
- PostgreSQL database on port 5432
- Product Service on port 8081

### Environment Variables

Required environment variables:
- `AZURE_POSTGRESQL_URL` - Database connection URL
- `AZURE_POSTGRESQL_USER` - Database username
- `AZURE_POSTGRESQL_PASSWORD` - Database password

Optional:
- `PETSTOREPRODUCTSERVICE_SERVER_PORT` - Server port (default: 8080)

## Building

```bash
mvn clean package
```

## Running Locally

```bash
# Set environment variables
export AZURE_POSTGRESQL_URL=jdbc:postgresql://localhost:5432/petstore
export AZURE_POSTGRESQL_USER=petstore
export AZURE_POSTGRESQL_PASSWORD=password

# Run the application
mvn spring-boot:run
```