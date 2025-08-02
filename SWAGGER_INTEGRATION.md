# OneBus Backend API - Swagger Integration

## Overview
This document describes the Swagger/OpenAPI integration for the OneBus Backend API. Swagger provides interactive API documentation and testing capabilities.

## Dependencies Added
- **SpringDoc OpenAPI Starter Web MVC UI** (version 2.6.0)
  - Provides OpenAPI 3 support for Spring Boot applications
  - Includes Swagger UI for interactive API documentation

## Configuration

### Maven Dependency
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

### Application Properties
```properties
# Swagger/OpenAPI Configuration
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.operationsSorter=method
springdoc.packages-to-scan=com.backend.onebus.controller
```

### OpenAPI Configuration Class
Location: `src/main/java/com/backend/onebus/config/OpenApiConfig.java`

This class configures the OpenAPI specification with:
- API information (title, description, version)
- Contact information
- License details
- Server configurations (development and production)

## API Documentation Features

### Documented Endpoints

#### Bus Tracking Controller (`/api`)
- **POST** `/tracker/payload` - Receive GPS tracking data from bus devices
- **GET** `/buses/nearest` - Find nearest bus to a location
- **GET** `/buses/active` - Get list of active buses
- **POST** `/buses` - Create a new bus record
- **GET** `/buses/{busNumber}/location` - Get specific bus location
- **PUT** `/buses/{busId}` - Update bus details
- **POST** `/clear` - Clear tracking data

#### Route Management
- **GET** `/routes` - Get all active routes
- **GET** `/routes/{routeId}` - Get specific route by ID
- **POST** `/routes` - Create a new route
- **POST** `/routes/{routeId}/stops` - Add stops to a route
- **POST** `/routes/import-json` - Import routes from JSON data
- **GET** `/routes/{busNumber}/{direction}/buses` - Get available buses for route

#### Debug Endpoints
- **GET** `/debug/subscriptions` - Debug subscription information
- **GET** `/debug/websocket-test` - Test WebSocket connection

### WebSocket Controller
The `BusStreamingController` handles WebSocket connections for real-time bus updates:
- Message mapping: `/subscribe`
- Subscription status: `/topic/subscription/status`

## Accessing Swagger UI

### Local Development
1. Start the application: `./mvnw spring-boot:run`
2. Open browser to: `http://localhost:8080/swagger-ui.html`

### API Documentation JSON
- OpenAPI JSON specification: `http://localhost:8080/api-docs`
- OpenAPI YAML specification: `http://localhost:8080/api-docs.yaml`

## Swagger Annotations Used

### Controller Level
- `@Tag` - Groups related endpoints
- `@RestController` - Marks as REST controller

### Method Level
- `@Operation` - Describes the operation
- `@ApiResponses` - Defines possible responses
- `@ApiResponse` - Individual response specification

### Parameter Level
- `@Parameter` - Documents request parameters
- `@RequestBody` - Documents request body
- `@PathVariable` - Documents path variables

### Schema Level
- `@Schema` - Documents model schemas
- `@Content` - Defines response content type

## Model Documentation

The following models are documented in Swagger:
- `Bus` - Bus entity with registration details
- `BusLocation` - GPS location data
- `Route` - Bus route information
- `RouteStop` - Individual stops on routes

## Testing with Swagger UI

1. Navigate to the Swagger UI interface
2. Select an endpoint to test
3. Fill in required parameters
4. Click "Try it out" to execute the request
5. View the response with status code and data

## Security Considerations

- Currently, the API is open without authentication
- In production, consider adding security annotations:
  - `@SecurityRequirement`
  - `@SecurityScheme`

## Production Deployment

For production deployment:
1. Update server URLs in `OpenApiConfig.java`
2. Consider disabling Swagger UI in production:
   ```properties
   springdoc.swagger-ui.enabled=false
   ```
3. Keep API docs available for internal use:
   ```properties
   springdoc.api-docs.enabled=true
   ```

## Additional Resources

- [SpringDoc OpenAPI Documentation](https://springdoc.org/)
- [OpenAPI Specification](https://swagger.io/specification/)
- [Swagger UI Documentation](https://swagger.io/tools/swagger-ui/)

## Maintenance

- Keep SpringDoc version updated
- Review and update API documentation regularly
- Ensure all new endpoints are properly documented
- Test API documentation with each release