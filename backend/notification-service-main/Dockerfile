# Build stage
FROM golang:1.24.0-alpine AS builder

# Set working directory
WORKDIR /app

# Install build dependencies
RUN apk add --no-cache git ca-certificates tzdata

# Copy go mod files
COPY go.mod go.sum ./

# Download dependencies
RUN go mod download

# Copy source code
COPY . .

# Build the application
RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o notification-service cmd/server/main.go

# Final stage
FROM alpine:3.19

# Install runtime dependencies
RUN apk --no-cache add ca-certificates tzdata curl

# Create app directory
WORKDIR /app

# Add secure non-root user (consistent with UID 1001 pattern)
RUN addgroup -g 1001 appgroup && \
    adduser -S -u 1001 -G appgroup appuser

# Create logs directory with proper ownership
RUN mkdir -p logs && \
    chown -R appuser:appgroup /app

# Copy the binary from builder stage
COPY --from=builder /app/notification-service .

# Copy environment file (as requested by user)
COPY --from=builder /app/.env .env

# Make the binary executable and ensure ownership
RUN chmod +x ./notification-service && \
    chown appuser:appgroup ./notification-service

# Use non-root user
USER appuser

# Expose port
EXPOSE 9090

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:9090/api/v1/health || exit 1

# Run the application
CMD ["./notification-service"]
