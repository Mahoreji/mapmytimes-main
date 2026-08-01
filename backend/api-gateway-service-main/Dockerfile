# syntax=docker/dockerfile:1.6

# ---------- build stage ----------
    FROM maven:3.9-eclipse-temurin-17 AS build
    WORKDIR /src
    
    # Copy wrapper + pom first to leverage layer caching
    COPY pom.xml ./
    COPY .mvn .mvn
    COPY mvnw mvnw
    RUN chmod +x mvnw || true
    
    # Warm Maven cache (fallback to mvn if wrapper not present)
    RUN --mount=type=cache,target=/root/.m2 \
        ./mvnw -q -DskipTests dependency:go-offline || mvn -q -DskipTests dependency:go-offline
    
    # Copy sources and build
    COPY src ./src
    RUN --mount=type=cache,target=/root/.m2 \
        ./mvnw -q -DskipTests clean package || mvn -q -DskipTests clean package
    
    # Pick the built jar and standardize its name
    RUN JAR="$(ls -1 target/*.jar | head -n 1)" && cp "$JAR" /app.jar
    
    # ---------- run stage ----------
    FROM eclipse-temurin:17-jre
    
    # Sensible JVM defaults for containers; set prod profile by default
    ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=25 -Dfile.encoding=UTF-8" \
        SPRING_PROFILES_ACTIVE=prod
    
    WORKDIR /app
    
    # Create non-root user, minimal runtime tools for healthcheck, then clean
    RUN useradd -r -u 10001 app \
     && mkdir -p /app/logs \
     && chown -R app:app /app \
     && apt-get update \
     && apt-get install -y --no-install-recommends ca-certificates curl tzdata \
     && rm -rf /var/lib/apt/lists/*
    
    # Copy artifact and environment files
    COPY --from=build /app.jar ./app.jar
    COPY .env ./.env
    
    USER app
    EXPOSE 8080
    
    # Healthcheck hits Spring Boot Actuator if enabled
    HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
      CMD curl -fsS http://localhost:8080/actuator/health || exit 1
    
    ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
    