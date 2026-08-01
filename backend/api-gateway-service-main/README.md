# MapMyTour API Gateway Service

Enterprise-grade API Gateway built with Spring Cloud Gateway for routing and managing microservices.

## Architecture

### Production / Staging Architecture
```
Client → Nginx → API Gateway (port 8080) → Microservices via internal Docker container names
```

The gateway routes requests to microservices using Docker container names:
- `http://new-auth-service:8081`
- `http://core-service:8083`
- `http://travel-service:8085`
- etc.

### Local Development Architecture
```
Developer Machine → API Gateway (localhost:8080) → Microservices (localhost:808X)
```

For local development, the gateway routes to `localhost` ports when environment variables are set.

---

## Local Development Instructions

### Prerequisites
- Java 17+
- Maven 3.6+
- All microservices running locally on their respective ports

### Running the Gateway Locally

1. **Start all microservices on localhost** (each in its own terminal or IDE):
   ```bash
   # Auth Service
   # Port: 8081
   
   # Utils Service
   # Port: 8082
   
   # Core Service
   # Port: 8083
   
   # AI Service
   # Port: 8084
   
   # Travel Service
   # Port: 8085
   
   # Customer Service
   # Port: 8086
   
   # Review Service
   # Port: 8087
   
   # Payment Service
   # Port: 8088
   
   # Booking Service
   # Port: 8089
   
   # Blog Service
   # Port: 8090
   
   # Chat Service
   # Port: 8091
   
   # Hotel Service
   # Port: 8092
   
   # Supplier Service
   # Port: 8093
   
   # Employee Service
   # Port: 8094
   
   # Document Service
   # Port: 8095
   
   # Notification Service
   # Port: 9090
   ```

2. **Create a `.env.local` file** (or set environment variables):
   ```bash
   # Local Development Service URLs
   AUTH_USER_SERVICE_URL=http://localhost:8081
   UTILS_SERVICE_URL=http://localhost:8082
   CORE_SERVICE_URL=http://localhost:8083
   CHAT_SERVICE_URL=http://localhost:8084
   TRAVEL_SERVICE_URL=http://localhost:8085
   CUSTOMER_SUPPORT_SERVICE_URL=http://localhost:8086
   REVIEWS_SERVICE_URL=http://localhost:8087
   PAYMENT_SERVICE_URL=http://localhost:8088
   BOOKING_SERVICE_URL=http://localhost:8089
   BLOG_SERVICE_URL=http://localhost:8090
   HOTEL_SERVICE_URL=http://localhost:8092
   SUPPLIER_SERVICE_URL=http://localhost:8093
   EMPLOYEE_SERVICE_URL=http://localhost:8094
   DOCUMENT_SERVICE_URL=http://localhost:8095
   NOTIFICATION_SERVICE_URL=http://localhost:9090
   AGENT_SERVICE_URL=http://localhost:8103
   LEAD_SERVICE_URL=http://localhost:8100
   GST_SERVICE_URL=http://localhost:8096
   FRAUD_SERVICE_URL=http://localhost:8097
   AUDIT_SERVICE_URL=http://localhost:8098
   REPORT_SERVICE_URL=http://localhost:8099
   GROUP_BOOKING_SERVICE_URL=http://localhost:8104
   CORPORATE_TRAVEL_SERVICE_URL=http://localhost:8105
   LOYALTY_SERVICE_URL=http://localhost:8101
   
   # Gateway Configuration
   SERVER_PORT=8080
   SPRING_PROFILES_ACTIVE=local
   ```

3. **Run the gateway**:
   ```bash
   # Option 1: Using Maven with environment variables
   export $(cat .env.local | xargs) && ./mvnw spring-boot:run
   
   # Option 2: Using IDE run configuration
   # Set environment variables in your IDE's run configuration
   
   # Option 3: Using Java directly
   java -jar target/api-gateway-service-*.jar
   ```

4. **Test the gateway**:
   ```bash
   # Health check
   curl http://localhost:8080/actuator/health
   
   # Test auth service routing
   curl http://localhost:8080/api/v1/auth/login
   
   # Test core service routing
   curl http://localhost:8080/api/v1/core/tours
   ```

### Environment Variable Configuration

**Local Development (Default):**
- No environment variables needed
- Gateway defaults to `localhost:port` for all services
- Just start services on their ports

**Docker/Production:**
- Set environment variables to use Docker service names:
  ```bash
  export AUTH_SERVICE_URL=http://auth-service:8081
  export BOOKING_SERVICE_URL=http://booking-service:8089
  export PAYMENT_SERVICE_URL=http://payment-service:8088
  export CORE_SERVICE_URL=http://core-service:8083
  export TRAVEL_SERVICE_URL=http://travel-service:8085
  export REVIEWS_SERVICE_URL=http://review-service:8087
  export BLOG_SERVICE_URL=http://blog-service:8090
  export CUSTOMER_SUPPORT_SERVICE_URL=http://customer-service:8086
  export UTILS_SERVICE_URL=http://utils-service:8082
  export CHAT_SERVICE_URL=http://chat-service:8084
  export HOTEL_SERVICE_URL=http://hotel-service:8092
  export SUPPLIER_SERVICE_URL=http://supplier-service:8093
  export EMPLOYEE_SERVICE_URL=http://employee-service:8094
  export AGENT_SERVICE_URL=http://agent-service:8103
  export LEAD_SERVICE_URL=http://lead-service:8097
  export GST_SERVICE_URL=http://accounting-gst-service:8098
  export FRAUD_SERVICE_URL=http://fraud-detection-service:8099
  export AUDIT_SERVICE_URL=http://audit-log-service:8100
  export REPORT_SERVICE_URL=http://report-analytics-service:8101
  export DOCUMENT_SERVICE_URL=http://document-service:8095
  export NOTIFICATION_SERVICE_URL=http://notification-service:9090
  export GROUP_BOOKING_SERVICE_URL=http://group-booking-service:8102
  export CORPORATE_TRAVEL_SERVICE_URL=http://corporate-travel-service:8103
  ```

### Environment Variable Override Pattern (Legacy)

The gateway uses environment variables with Docker container names as defaults:

**Production (Docker):**
- Default: `http://new-auth-service:8081`
- No environment variable needed

**Local Development:**
- Override: `AUTH_USER_SERVICE_URL=http://localhost:8081`
- Set in `.env.local` or IDE run configuration

### Service URL Environment Variables

| Service | Environment Variable | Production Default | Local Dev Override |
|---------|---------------------|-------------------|-------------------|
| Auth | `AUTH_USER_SERVICE_URL` | `http://new-auth-service:8081` | `http://localhost:8081` |
| Utils | `UTILS_SERVICE_URL` | `http://utils-service:8082` | `http://localhost:8082` |
| Core | `CORE_SERVICE_URL` | `http://core-service:8083` | `http://localhost:8083` |
| AI/Chat | `CHAT_SERVICE_URL` | `http://ai-service:8084` | `http://localhost:8084` |
| Travel | `TRAVEL_SERVICE_URL` | `http://travel-service:8085` | `http://localhost:8085` |
| Customer | `CUSTOMER_SUPPORT_SERVICE_URL` | `http://customer-service:8086` | `http://localhost:8086` |
| Review | `REVIEWS_SERVICE_URL` | `http://review-service:8087` | `http://localhost:8087` |
| Payment | `PAYMENT_SERVICE_URL` | `http://payment-service:8088` | `http://localhost:8088` |
| Booking | `BOOKING_SERVICE_URL` | `http://booking-service:8089` | `http://localhost:8089` |
| Blog | `BLOG_SERVICE_URL` | `http://blog-service:8090` | `http://localhost:8090` |
| Hotel | `HOTEL_SERVICE_URL` | `http://hotel-service:8092` | `http://localhost:8092` |
| Supplier | `SUPPLIER_SERVICE_URL` | `http://supplier-service:8093` | `http://localhost:8093` |
| Employee | `EMPLOYEE_SERVICE_URL` | `http://employee-service:8094` | `http://localhost:8094` |
| Document | `DOCUMENT_SERVICE_URL` | `http://document-service:8095` | `http://localhost:8095` |
| Notification | `NOTIFICATION_SERVICE_URL` | `http://notification-service:9090` | `http://localhost:9090` |

---

## Docker Deployment

### Production Configuration

In production, the gateway runs in Docker and communicates with microservices via Docker container names. All services must be on the same Docker network.

**Example docker-compose.yml snippet:**
```yaml
services:
  api-gateway:
    container_name: api-gateway-service
    image: mapmytour/api-gateway:latest
    ports:
      - "8080:8080"
    networks:
      - mapmytour-network
    environment:
      # Production uses Docker container names (defaults in application.yml)
      # No need to override unless using different container names
      SERVER_PORT: 8080
      SPRING_PROFILES_ACTIVE: prod

  new-auth-service:
    container_name: new-auth-service
    image: mapmytour/auth-service:latest
    ports:
      - "8081:8081"
    networks:
      - mapmytour-network

  core-service:
    container_name: core-service
    image: mapmytour/core-service:latest
    ports:
      - "8083:8083"
    networks:
      - mapmytour-network

networks:
  mapmytour-network:
    driver: bridge
```

**Important:** Container names in docker-compose.yml must match the service names used in `application.yml`:
- `new-auth-service` (not `auth-service`)
- `core-service`
- `travel-service`
- etc.

---

## Configuration

### Key Configuration Files
- `src/main/resources/application.yml` - Main configuration
- Environment variables override defaults

### Discovery Locator
- **Disabled** by default (`spring.cloud.gateway.discovery.locator.enabled=false`)
- Services are routed directly via container names or localhost URLs

### Port Configuration
- Default: `8080`
- Override: `SERVER_PORT=8080`

---

## Features

- ✅ JWT Authentication & Authorization
- ✅ Rate Limiting (Redis-based)
- ✅ Circuit Breaker (Resilience4j)
- ✅ CORS Support
- ✅ Request/Response Logging
- ✅ Health Checks & Metrics
- ✅ Service Discovery (via direct container names)

---

## Troubleshooting

### Gateway can't reach microservices locally
- Ensure all microservices are running on their ports
- Check environment variables are set correctly
- Verify service URLs in logs on startup

### Gateway can't reach microservices in Docker
- Verify all services are on the same Docker network
- Check container names match exactly (case-sensitive)
- Ensure services are healthy: `docker ps`

### Port conflicts
- Change `SERVER_PORT` environment variable
- Ensure no other service is using port 8080

## Integration Verification

### Verify Core Service Connectivity (from within api-gateway container)

```bash
# Exec into api-gateway container
docker compose exec api-gateway sh
# OR
docker-compose exec api-gateway sh

# Test core-service health endpoint
wget -O- http://core-service:8083/actuator/health

# Test destinations endpoint via gateway
curl -X POST http://localhost:8080/api/v1/destinations/search \
  -H "Content-Type: application/json" \
  -d '{"query": "test"}'
```

### Verify Route Configuration

The `core-service-destinations` route uses `${CORE_SERVICE_URL:http://localhost:8083}`:
- **Local development**: Defaults to `http://localhost:8083`
- **Docker/production**: Uses `CORE_SERVICE_URL=http://core-service:8083` from environment

Check route configuration:
```bash
curl http://localhost:8080/actuator/gateway/routes | jq '.[] | select(.id=="core-service-destinations")'
```

---

## License

Proprietary - MapMyTour