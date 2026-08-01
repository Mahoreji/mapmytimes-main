# =============================================================================
# Multi-Stage Dockerfile for Blog Service (MapMyTour)
# Optimized for: Clean build, Health Monitoring, Auto-Restart, Durability
# =============================================================================

# ---------------------------
# STAGE 1: Build Application
# ---------------------------
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Install build dependencies with retry
RUN apk update && \
    apk add --no-cache --update maven curl bash || \
    (sleep 5 && apk update && apk add --no-cache --update maven curl bash)

# Copy project files
COPY pom.xml ./
COPY mvnw ./
COPY .mvn .mvn

# Pre-download dependencies (cached layer)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy project source
COPY src ./src

# Clean build the application
RUN ./mvnw clean package -DskipTests -Dmaven.test.skip=true

# ----------------------------
# STAGE 2: Runtime Environment
# ----------------------------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install runtime dependencies (with retry)
RUN apk update && \
    apk add --no-cache --update curl procps bash || \
    (sleep 5 && apk update && apk add --no-cache --update curl procps bash)

# Add secure non-root user
RUN addgroup -g 1001 appuser && adduser -D -u 1001 -G appuser appuser

# Create essential directories
RUN mkdir -p logs /app/temp && chown -R appuser:appuser /app

# Copy built JAR from builder
COPY --from=builder /app/target/blog-service-0.0.1-SNAPSHOT.jar app.jar

# Copy configuration
COPY .env .env

# Ensure ownership and security
RUN chown -R appuser:appuser /app
USER appuser

# Expose port
EXPOSE 8090

# ---------------------------------------
# HEALTH CHECK (uses Spring Boot actuator)
# ---------------------------------------
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=5 \
  CMD curl -f http://localhost:8090/actuator/health || exit 1

# ---------------------------------------
# JVM & SPRING BOOT OPTIMIZATION
# ---------------------------------------
ENV JAVA_OPTS="\
  -Xms512m -Xmx1024m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UseContainerSupport \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/app/logs/ \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.profiles.active=prod"

ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8090
ENV LOGGING_LEVEL_ROOT=INFO
ENV LOGGING_LEVEL_IN_MAPMYTOUR_BLOG=INFO

# Durability and recovery flags
ENV APP_AUTO_REPAIR_ENABLED=true
ENV APP_DURABILITY_ENABLED=true
ENV APP_MONITORING_ENABLED=true
ENV APP_ERROR_RECOVERY_ENABLED=true
ENV APP_AUTO_HEALING_ENABLED=true

# ---------------------------------------
# ENTRY POINT
# ---------------------------------------
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
