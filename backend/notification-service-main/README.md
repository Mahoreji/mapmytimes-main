# Notification Service

A robust notification service built with Go that supports email, SMS, and push notifications with async processing and scheduling capabilities.

## Features

- **Multiple Notification Types**: Email, SMS, Push notifications
- **Async Processing**: Non-blocking notification sending
- **Scheduling**: Schedule notifications for future delivery
- **Retry Logic**: Automatic retry for failed notifications
- **Logging**: Comprehensive logging with file rotation
- **Database Integration**: PostgreSQL with GORM
- **Caching**: Redis for queue management
- **REST API**: Clean REST API with standardized responses

## Quick Start

```bash
# Make setup script executable
chmod +x setup.sh

# Run setup
./setup.sh

# Install dependencies
go mod tidy

# Start the service
go run cmd/server/main.go
```

## API Endpoints

### Send Notification
```
POST /api/v1/notification/send
```

**Request Body:**
```json
{
    "type": "email",
    "recipient": "user@example.com",
    "subject": "Test Subject",
    "body": "Test message body",
    "scheduled_at": 1640995200000,
    "metadata": {
        "source": "user_registration"
    }
}
```

**Response:**
```json
{
    "success": true,
    "statusCode": 200,
    "message": "Notification sent successfully",
    "data": {
        "id": "123e4567-e89b-12d3-a456-426614174000",
        "status": "queued",
        "message": "Notification queued successfully"
    }
}
```

### Get Notification Status
```
GET /api/v1/notification/{id}
```

### Health Check
```
GET /api/v1/health
```

## Docker Deployment

### Production Deployment

The production configuration is defined in `docker-run.sh` and `docker-compose.yml`. 

#### Option 1: Using Docker Run Script (Recommended)

```bash
# Build the Docker image
make docker-build

# Run with production configuration
make docker-run-prod
# OR
./docker-run.sh
```

#### Option 2: Using Docker Compose

```bash
# Start services
make docker-compose-up
# OR
docker-compose up -d

# Stop services
make docker-compose-down
# OR
docker-compose down
```

#### Option 3: Manual Docker Run

```bash
docker stop notification-service 2>/dev/null
docker rm notification-service 2>/dev/null

docker run -d --name notification-service \
  -p 9090:9090 \
  -e PORT=9090 \
  -e GIN_MODE=release \
  -e DB_HOST=150.241.245.162 \
  -e DB_PORT=5432 \
  -e DB_NAME=mapmytour_prod \
  -e DB_USERNAME=admin_prod \
  -e DB_PASSWORD=MapMyTour@Prod885839!SuperSecure \
  -e DB_SSL_MODE=prefer \
  -e EMAIL_HOST=smtp.zoho.in \
  -e EMAIL_PORT=465 \
  -e EMAIL_USERNAME=hello@mapmytimes.com \
  -e EMAIL_PASSWORD=AsRrjFCenepY \
  -e CONTACT_FORM_RECIPIENT_EMAIL=support@mapmytimes.com \
  -e CONTACT_FORM_AUTO_REPLY=true \
  -e REDIS_HOST=150.241.245.162 \
  -e REDIS_PORT=6379 \
  -e REDIS_PASSWORD=MapMyTour@ProdRedis885839!SuperSecure \
  -e REDIS_DB=0 \
  -e ENABLE_AUTH=false \
  -e RATE_LIMIT_ENABLED=true \
  -e RATE_LIMIT_RPS=100 \
  -e RATE_LIMIT_BURST=200 \
  notification-service:latest
```

**View logs:**
```bash
docker logs -f notification-service
```

**Check health:**
```bash
curl http://localhost:9090/api/v1/health
```

## Configuration

The service uses environment variables for configuration. See `production.config.env` for the exact production configuration:

```env
# Server
PORT=9090
GIN_MODE=release

# Database
DB_HOST=150.241.245.162
DB_PORT=5432
DB_NAME=mapmytour_prod
DB_USERNAME=admin_prod
DB_PASSWORD=MapMyTour@Prod885839!SuperSecure
DB_SSL_MODE=prefer

# Email (Gmail SMTP)
EMAIL_HOST=smtp.zoho.in
EMAIL_PORT=465
EMAIL_USERNAME=hello@mapmytimes.com
EMAIL_PASSWORD=AsRrjFCenepY

# Contact Form
CONTACT_FORM_RECIPIENT_EMAIL=support@mapmytimes.com
CONTACT_FORM_AUTO_REPLY=true

# Redis
REDIS_HOST=150.241.245.162
REDIS_PORT=6379
REDIS_PASSWORD=MapMyTour@ProdRedis885839!SuperSecure
REDIS_DB=0

# Security
ENABLE_AUTH=false
RATE_LIMIT_ENABLED=true
RATE_LIMIT_RPS=100
RATE_LIMIT_BURST=200
```

## Notification Types

1. **Email**: Uses SMTP (Gmail) for sending emails
2. **SMS**: Uses Twilio for SMS delivery
3. **Push**: Framework ready for FCM/APNs integration

## Architecture

```
notification-service/
├── cmd/server/          # Application entry point
├── config/              # Configuration management
├── internal/
│   ├── app/
│   │   ├── notification/  # Core notification logic
│   │   ├── email/        # Email service
│   │   └── push/         # Push notification service
│   ├── db/              # Database connections
│   ├── router/          # HTTP routing
│   └── utils/           # Utility functions
├── logs/                # Log files
└── docs/                # API documentation
```

## Logging

Logs are written to:
- `logs/notification-service-{date}.log` - General application logs
- `logs/error.log` - Error-specific logs

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make changes
4. Run tests
5. Submit a pull request

## License

MIT License

---

## 🔔 Push Notifications (FCM)

Push notifications are powered by **Firebase Cloud Messaging (FCM)**. To enable them:

### Step 1: Get Firebase Credentials
1. Go to [Firebase Console](https://console.firebase.google.com) → Your Project → Project Settings → Service Accounts
2. Click **Generate new private key** to download a JSON file

### Step 2: Configure the Service

**Option A — JSON String (best for Docker/K8s/Vercel):**
```bash
FCM_SERVICE_ACCOUNT_JSON={"type":"service_account","project_id":"mapmytour-prod",...}
```

**Option B — File path (best for VMs):**
```bash
FCM_SERVICE_ACCOUNT_FILE=/etc/secrets/firebase-service-account.json
```

### Push Notification API Endpoints

All endpoints require `Authorization: Bearer <JWT>` header.

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/notification/push` | Send to a single device token |
| `POST` | `/api/v1/notification/push/topic` | Broadcast to an FCM topic |
| `POST` | `/api/v1/notification/push/multicast` | Send to up to 500 tokens at once |
| `POST` | `/api/v1/notification/push/subscribe` | Subscribe tokens to a topic |
| `POST` | `/api/v1/notification/push/unsubscribe` | Unsubscribe tokens from a topic |
| `GET`  | `/api/v1/notification/push/status` | Check FCM service health |

### Example: Send to a single device
```json
POST /api/v1/notification/push
{
  "recipient": "FCM_DEVICE_TOKEN_HERE",
  "type": "push",
  "subject": "Booking Confirmed!",
  "body": "Your tour to Goa is confirmed for May 15."
}
```

### Example: Broadcast to a topic
```json
POST /api/v1/notification/push/topic
{
  "topic": "booking-updates",
  "title": "Flash Sale!",
  "body": "50% off all Rajasthan tours today only.",
  "data": { "type": "SALE", "action": "OPEN_TOURS" }
}
```

### Example: Multicast to multiple tokens
```json
POST /api/v1/notification/push/multicast
{
  "tokens": ["TOKEN_1", "TOKEN_2", "TOKEN_3"],
  "title": "Your trip is tomorrow!",
  "body": "Check your itinerary and pack your bags."
}
```

### Predefined Topics
| Topic | When to Use |
|-------|-------------|
| `booking-updates` | Booking status changes |
| `flash-sales` | Time-limited deals |
| `tour-reminders` | Day-before trip reminders |
| `news` | Platform announcements |
