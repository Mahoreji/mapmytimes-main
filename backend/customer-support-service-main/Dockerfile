# Stage 1: Build
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
COPY .env .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create logs directory and set permissions
RUN mkdir -p logs && groupadd -r spring && useradd -r -g spring spring && chown spring:spring logs

USER spring:spring

# Copy the built artifact from the build stage
COPY --from=build /app/target/customer-support-service-0.0.1-SNAPSHOT.jar app.jar
COPY --from=build /app/.env .

# Configuration for production
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# Expose the service port
EXPOSE 8086

# Health check (optional but recommended)
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -f http://localhost:8086/actuator/health || exit 1

# Entry point to run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]