# Notification Service Makefile

# Variables
APP_NAME := notification-service
MAIN_PATH := cmd/server/main.go
BUILD_DIR := build
BINARY_NAME := $(APP_NAME)
VERSION := 1.0.0
COMMIT := $(shell git rev-parse --short HEAD 2>/dev/null || echo "unknown")
BUILD_TIME := $(shell date -u +"%Y-%m-%dT%H:%M:%SZ")

# Go parameters
GOCMD := go
GOBUILD := $(GOCMD) build
GOCLEAN := $(GOCMD) clean
GOTEST := $(GOCMD) test
GOGET := $(GOCMD) get
GOMOD := $(GOCMD) mod
GOFMT := $(GOCMD) fmt

# Build flags
LDFLAGS := -ldflags "-X main.Version=$(VERSION) -X main.Commit=$(COMMIT) -X main.BuildTime=$(BUILD_TIME)"

# Colors for output
RED := \033[0;31m
GREEN := \033[0;32m
YELLOW := \033[0;33m
BLUE := \033[0;34m
PURPLE := \033[0;35m
CYAN := \033[0;36m
WHITE := \033[0;37m
NC := \033[0m # No Color

.PHONY: all setup build run test clean help install deps tidy format lint docker-build docker-run logs

# Default target
all: clean deps build

## Install: Install dependencies
install: deps

## Deps: Download and install dependencies
deps:
	@echo "$(BLUE)📦 Installing dependencies...$(NC)"
	@$(GOMOD) download
	@$(GOMOD) tidy
	@echo "$(GREEN)✅ Dependencies installed!$(NC)"

## Tidy: Clean up dependencies
tidy:
	@echo "$(BLUE)🧹 Tidying dependencies...$(NC)"
	@$(GOMOD) tidy
	@$(GOMOD) verify

## Format: Format Go code
format:
	@echo "$(YELLOW)🎨 Formatting code...$(NC)"
	@$(GOFMT) ./...
	@echo "$(GREEN)✅ Code formatted!$(NC)"

## Lint: Run linter (requires golangci-lint)
lint:
	@echo "$(PURPLE)🔍 Running linter...$(NC)"
	@if command -v golangci-lint >/dev/null 2>&1; then \
		golangci-lint run; \
	else \
		echo "$(YELLOW)⚠️ golangci-lint not installed. Install with: go install github.com/golangci/golangci-lint/cmd/golangci-lint@latest$(NC)"; \
	fi

## Build: Build the application
build: deps
	@echo "$(BLUE)🔨 Building $(APP_NAME)...$(NC)"
	@mkdir -p $(BUILD_DIR)
	@$(GOBUILD) $(LDFLAGS) -o $(BUILD_DIR)/$(BINARY_NAME) $(MAIN_PATH)
	@echo "$(GREEN)✅ Build completed! Binary: $(BUILD_DIR)/$(BINARY_NAME)$(NC)"

## Build-linux: Build for Linux
build-linux: deps
	@echo "$(BLUE)🔨 Building $(APP_NAME) for Linux...$(NC)"
	@mkdir -p $(BUILD_DIR)
	@GOOS=linux GOARCH=amd64 $(GOBUILD) $(LDFLAGS) -o $(BUILD_DIR)/$(BINARY_NAME)-linux-amd64 $(MAIN_PATH)
	@echo "$(GREEN)✅ Linux build completed!$(NC)"

## Build-windows: Build for Windows
build-windows: deps
	@echo "$(BLUE)🔨 Building $(APP_NAME) for Windows...$(NC)"
	@mkdir -p $(BUILD_DIR)
	@GOOS=windows GOARCH=amd64 $(GOBUILD) $(LDFLAGS) -o $(BUILD_DIR)/$(BINARY_NAME)-windows-amd64.exe $(MAIN_PATH)
	@echo "$(GREEN)✅ Windows build completed!$(NC)"

## Build-mac: Build for macOS
build-mac: deps
	@echo "$(BLUE)🔨 Building $(APP_NAME) for macOS...$(NC)"
	@mkdir -p $(BUILD_DIR)
	@GOOS=darwin GOARCH=amd64 $(GOBUILD) $(LDFLAGS) -o $(BUILD_DIR)/$(BINARY_NAME)-darwin-amd64 $(MAIN_PATH)
	@echo "$(GREEN)✅ macOS build completed!$(NC)"

## Build-all: Build for all platforms
build-all: build-linux build-windows build-mac
	@echo "$(GREEN)✅ All platform builds completed!$(NC)"

## Run: Run the application in development mode
run: deps
	@echo "$(CYAN)🚀 Starting $(APP_NAME)...$(NC)"
	@mkdir -p logs
	@$(GOCMD) run $(MAIN_PATH)

## Run-build: Run the built binary
run-build: build
	@echo "$(CYAN)🚀 Starting built $(APP_NAME)...$(NC)"
	@./$(BUILD_DIR)/$(BINARY_NAME)

## Test: Run tests
test:
	@echo "$(YELLOW)🧪 Running tests...$(NC)"
	@$(GOTEST) -v ./...
	@echo "$(GREEN)✅ Tests completed!$(NC)"

## Test-coverage: Run tests with coverage
test-coverage:
	@echo "$(YELLOW)🧪 Running tests with coverage...$(NC)"
	@$(GOTEST) -coverprofile=coverage.out ./...
	@$(GOCMD) tool cover -html=coverage.out -o coverage.html
	@echo "$(GREEN)✅ Coverage report generated: coverage.html$(NC)"

## Test-apis: Test all API endpoints
test-apis:
	@echo "$(YELLOW)🧪 Testing API endpoints...$(NC)"
	@chmod +x test_apis.sh
	@./test_apis.sh

## Clean: Clean build artifacts
clean:
	@echo "$(RED)🧹 Cleaning...$(NC)"
	@$(GOCLEAN)
	@rm -rf $(BUILD_DIR)
	@rm -f coverage.out coverage.html
	@rm -rf logs/*.log
	@echo "$(GREEN)✅ Cleaned!$(NC)"

## Clean-logs: Clean log files only
clean-logs:
	@echo "$(RED)🧹 Cleaning logs...$(NC)"
	@rm -rf logs/*.log
	@echo "$(GREEN)✅ Logs cleaned!$(NC)"

## Logs: Show recent logs
logs:
	@echo "$(CYAN)📋 Recent logs:$(NC)"
	@if [ -f "logs/notification-service-$(shell date +%Y-%m-%d).log" ]; then \
		tail -f logs/notification-service-$(shell date +%Y-%m-%d).log; \
	else \
		echo "$(YELLOW)No logs found for today$(NC)"; \
	fi

## Logs-error: Show error logs
logs-error:
	@echo "$(RED)🚨 Error logs:$(NC)"
	@if [ -f "logs/error.log" ]; then \
		tail -f logs/error.log; \
	else \
		echo "$(YELLOW)No error logs found$(NC)"; \
	fi

## Docker-build: Build Docker image
docker-build:
	@echo "$(BLUE)🐳 Building Docker image...$(NC)"
	@docker build -t $(APP_NAME):$(VERSION) .
	@docker tag $(APP_NAME):$(VERSION) $(APP_NAME):latest
	@echo "$(GREEN)✅ Docker image built!$(NC)"

## Docker-run: Run Docker container
docker-run:
	@echo "$(CYAN)🐳 Running Docker container...$(NC)"
	@docker run -d --name $(APP_NAME) -p 9090:9090 --env-file .env $(APP_NAME):latest

## Docker-run-prod: Run Docker container with production configuration
docker-run-prod:
	@echo "$(CYAN)🐳 Running Docker container with production configuration...$(NC)"
	@chmod +x docker-run.sh
	@./docker-run.sh

## Docker-compose-up: Start using docker-compose
docker-compose-up:
	@echo "$(CYAN)🐳 Starting services with docker-compose...$(NC)"
	@docker-compose up -d

## Docker-compose-down: Stop services using docker-compose
docker-compose-down:
	@echo "$(RED)🐳 Stopping services with docker-compose...$(NC)"
	@docker-compose down

## Docker-stop: Stop Docker container
docker-stop:
	@echo "$(RED)🐳 Stopping Docker container...$(NC)"
	@docker stop $(APP_NAME) || true
	@docker rm $(APP_NAME) || true

## Docker-logs: Show Docker logs
docker-logs:
	@docker logs -f $(APP_NAME)

## Health: Check service health
health:
	@echo "$(CYAN)🏥 Checking service health...$(NC)"
	@curl -s http://localhost:9090/api/v1/health | jq '.' || echo "$(RED)Service not responding$(NC)"

## Status: Show service status
status:
	@echo "$(CYAN)📊 Service status:$(NC)"
	@curl -s http://localhost:9090/api/v1/notification/stats | jq '.' || echo "$(RED)Service not responding$(NC)"

## Dev: Run in development mode with auto-reload (requires air)
dev:
	@echo "$(CYAN)🔄 Starting development mode...$(NC)"
	@if command -v air >/dev/null 2>&1; then \
		air; \
	else \
		echo "$(YELLOW)⚠️ Air not installed. Install with: go install github.com/cosmtrek/air@latest$(NC)"; \
		echo "$(CYAN)Falling back to regular run...$(NC)"; \
		$(MAKE) run; \
	fi

## Install-tools: Install development tools
install-tools:
	@echo "$(BLUE)🛠️ Installing development tools...$(NC)"
	@go install github.com/cosmtrek/air@latest
	@go install github.com/golangci/golangci-lint/cmd/golangci-lint@latest
	@echo "$(GREEN)✅ Development tools installed!$(NC)"

## Benchmark: Run benchmarks
benchmark:
	@echo "$(YELLOW)⚡ Running benchmarks...$(NC)"
	@$(GOTEST) -bench=. -benchmem ./...

## Update: Update dependencies
update:
	@echo "$(BLUE)⬆️ Updating dependencies...$(NC)"
	@$(GOGET) -u ./...
	@$(GOMOD) tidy

## Version: Show version information
version:
	@echo "$(CYAN)📋 Version Information:$(NC)"
	@echo "  App Name: $(APP_NAME)"
	@echo "  Version:  $(VERSION)"
	@echo "  Commit:   $(COMMIT)"
	@echo "  Build:    $(BUILD_TIME)"

## Help: Show this help message
help:
	@echo "$(CYAN)🚀 $(APP_NAME) Makefile Commands:$(NC)"
	@echo ""
	@grep -E '^## [a-zA-Z_-]+:.*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = "## "}; {split($$2, a, ":"); printf "  $(YELLOW)%-15s$(NC) %s\n", a[1], a[2]}'
	@echo ""
	@echo "$(CYAN)📖 Usage Examples:$(NC)"
	@echo "  $(GREEN)make run$(NC)            # Start development server"
	@echo "  $(GREEN)make build$(NC)          # Build production binary"
	@echo "  $(GREEN)make test$(NC)           # Run all tests"
	@echo "  $(GREEN)make test-apis$(NC)      # Test API endpoints"
	@echo "  $(GREEN)make docker-build$(NC)   # Build Docker image"
	@echo "  $(GREEN)make logs$(NC)           # View application logs"
	@echo "  $(GREEN)make health$(NC)         # Check service health"
	@echo ""