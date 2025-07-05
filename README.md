# OneBus Backend

A Spring Boot application for real-time bus tracking with HTTP data ingestion and WebSocket streaming.

## Architecture Overview

The application follows a clear separation of concerns:

1. **HTTP Endpoints**: Receive bus coordinate updates from tracking devices
2. **Redis Storage**: Store latest bus locations for fast access
3. **WebSocket Streaming**: Stream real-time bus data to subscribed clients
4. **PostgreSQL**: Persistent storage for historical bus location data

## Features

- **Real-time Bus Tracking**: Stream bus locations to clients via WebSocket
- **Bus Subscription**: Clients can subscribe to specific bus numbers and directions
- **Geographic Queries**: Find nearest buses based on location and direction
- **Data Persistence**: Store bus location history in PostgreSQL
- **Redis Caching**: Fast access to current bus locations
- **RESTful API**: HTTP endpoints for data ingestion and queries

## API Endpoints

### Data Ingestion
- `POST /api/tracker/payload` - Receive bus location updates
- `PUT /api/buses/{busId}` - Update bus details

### Data Retrieval
- `GET /api/buses/nearest?lat={lat}&lon={lon}&tripDirection={direction}` - Find nearest bus
- `GET /api/buses/{busNumber}/location?direction={direction}` - Get specific bus location
- `GET /api/buses/active` - Get all active bus numbers
- `POST /api/clear` - Clear tracking data

## WebSocket Streaming

### Connection
Connect to WebSocket endpoint: `/ws/bus-updates`

### Subscription
Send subscription request to `/app/subscribe`:
```json
{
  "busNumber": "101",
  "direction": "Northbound"
}
```

### Unsubscription
Send unsubscription request to `/app/unsubscribe`:
```json
{
  "busNumber": "101",
  "direction": "Northbound"
}
```

### Receiving Updates
Subscribe to topic: `/topic/bus/{busNumber}_{direction}`

Example: `/topic/bus/101_Northbound`

## Bus Location Data Format

### HTTP Payload (POST /api/tracker/payload)
```json
{
  "trackerImei": "359339072173798",
  "lat": -26.2091,
  "lon": 28.0583,
  "speedKmh": 45.0,
  "headingDegrees": 180.0,
  "tripDirection": "Northbound",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### WebSocket Update Format
```json
{
  "busId": "BUS001",
  "busNumber": "101",
  "trackerImei": "359339072173798",
  "lat": -26.2091,
  "lon": 28.0583,
  "speedKmh": 45.0,
  "headingDegrees": 180.0,
  "tripDirection": "Northbound",
  "busDriver": "John Doe",
  "busCompany": "City Transit",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## Configuration

### Application Properties
```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/onebus
spring.datasource.username=postgres
spring.datasource.password=password

# Redis
spring.redis.host=localhost
spring.redis.port=6379

# WebSocket
spring.websocket.max-text-message-size=8192
```

## Running the Application

1. **Start PostgreSQL and Redis**
2. **Run the application**:
   ```bash
   ./mvnw spring-boot:run
   ```

3. **Test with the provided client**:
   - Open `http://localhost:8080/test-client.html`
   - Enter a bus number and direction
   - Click Subscribe to start receiving real-time updates

## Testing

### Send Test Data
```bash
curl -X POST http://localhost:8080/api/tracker/payload \
  -H "Content-Type: application/json" \
  -d '{
    "trackerImei": "359339072173798",
    "lat": -26.2091,
    "lon": 28.0583,
    "speedKmh": 45.0,
    "headingDegrees": 180.0,
    "tripDirection": "Northbound",
    "timestamp": "2024-01-15T10:30:00Z"
  }'
```

### Get Bus Location
```bash
curl "http://localhost:8080/api/buses/101/location?direction=Northbound"
```

## Architecture Benefits

1. **Scalability**: HTTP endpoints can handle high-volume data ingestion
2. **Real-time**: WebSocket streaming provides immediate updates to clients
3. **Efficiency**: Redis caching ensures fast data access
4. **Reliability**: PostgreSQL provides data persistence
5. **Flexibility**: Clients can subscribe to specific buses and directions

## Dependencies

- Spring Boot 3.5.3
- Spring WebSocket
- Spring Data Redis
- Spring Data JPA
- PostgreSQL with PostGIS
- Redis
- JTS Geometry Library
