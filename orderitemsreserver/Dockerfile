# Use the official Azure Functions Java base image
FROM mcr.microsoft.com/azure-functions/java:4-java21-build AS build

# Set working directory
WORKDIR /workspace/app

# Copy Maven files
COPY pom.xml .
COPY host.json .

# Copy source code
COPY src src

# Build the application
RUN mvn clean package -DskipTests

# Use the Azure Functions Java runtime image for production
FROM mcr.microsoft.com/azure-functions/java:4-java21

# Set environment variables
ENV AzureWebJobsScriptRoot=/home/site/wwwroot \
    AzureFunctionsJobHost__Logging__Console__IsEnabled=true

# Copy built artifacts from build stage
COPY --from=build /workspace/app/target/azure-functions/orderitemsreserver/ /home/site/wwwroot

# Expose the default Azure Functions port
EXPOSE 80

# The base image already has the correct entrypoint configured

