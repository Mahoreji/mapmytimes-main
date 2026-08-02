#!/bin/bash

# Production Docker Run Script for Notification Service
# This script matches the exact production configuration

echo "🐳 Stopping and removing existing container (if any)..."
docker stop notification-service 2>/dev/null
docker rm notification-service 2>/dev/null

echo "🚀 Starting notification-service container..."
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

if [ $? -eq 0 ]; then
    echo "✅ Container started successfully!"
    echo "📋 Container name: notification-service"
    echo "🌐 Service available at: http://localhost:9090"
    echo "📊 Health check: http://localhost:9090/api/v1/health"
    echo ""
    echo "View logs with: docker logs -f notification-service"
else
    echo "❌ Failed to start container"
    exit 1
fi

