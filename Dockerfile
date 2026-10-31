# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy Maven files
COPY pom.xml .
COPY mvnw* .
COPY .mvn .mvn

# Copy source code
COPY src src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /build/target/rifline-api-0.0.1-SNAPSHOT.jar app.jar

# Create a non-root user for security
RUN useradd -m -u 1000 appuser && chown -R appuser:appuser /app
USER appuser

# Expose the application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]
