# ================================
# ChatNexus - Multi-Stage Dockerfile
# Optimized for Railway & Render Deployment
# ================================

# Stage 1: Build Stage
# Use Maven image to build the Spring Boot application
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .

# Download dependencies (this layer caches dependencies)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for faster build)
RUN mvn clean package -DskipTests -B

# ================================
# Stage 2: Runtime Stage
# Use lightweight JRE image for production
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install curl for health checks (optional but recommended)
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -g 1000 appuser && \
    adduser -D -u 1000 -G appuser appuser

# Copy JAR from builder stage
COPY --from=builder /app/target/ChatNexus-3.0.0.jar app.jar

# Change ownership to non-root user
RUN chown appuser:appuser /app/app.jar

# Switch to non-root user
USER appuser

# Expose port (Railway/Render will override with PORT env var)
EXPOSE 8080

# Health check for container orchestration
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application with environment variables support
# Railway & Render automatically inject PORT and other env vars
ENTRYPOINT ["java", "-jar", "app.jar"]

