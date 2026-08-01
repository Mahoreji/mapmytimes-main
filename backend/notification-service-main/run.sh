#!/bin/bash

echo "🚀 Starting Notification Service..."

# Check if go.mod exists
if [ ! -f "go.mod" ]; then
    echo "❌ go.mod not found. Please run setup.sh first."
    exit 1
fi

# Install dependencies
echo "📦 Installing dependencies..."
go mod tidy

# Create logs directory if it doesn't exist
mkdir -p logs

# Start the service
echo "🔄 Starting server..."
go run cmd/server/main.go