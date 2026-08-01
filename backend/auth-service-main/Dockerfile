# Use JDK 17 base image (Temurin - replacement for deprecated openjdk images)
FROM eclipse-temurin:17-jdk-jammy

# Set working directory
WORKDIR /app

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copy Maven wrapper and pom.xml first for better caching
COPY mvnw ./
COPY mvnw.cmd ./
COPY .mvn .mvn
COPY pom.xml ./

# Make Maven wrapper executable
RUN chmod +x mvnw

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Create logs directory
RUN mkdir -p logs

# Expose port 8081
EXPOSE 8081

# Run the application
CMD ["java", "-jar", "target/auth-service-0.0.1-SNAPSHOT.jar"]