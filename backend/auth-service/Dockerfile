# SmartFleet Auth Service
# Multi-stage Dockerfile for optimized production builds

# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper and pom files
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies
RUN ./mvnw dependency:go-offline -B || true

# Copy source code
COPY src/ src/

# Build the application
RUN ./mvnw package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Install wget for healthcheck
RUN apk add --no-cache wget

# Create non-root user for security
RUN addgroup -S smartfleet && adduser -S smartfleet -G smartfleet

# Copy JAR from builder
COPY --from=builder /app/target/*.jar app.jar

# Create directory for OTel agent (mounted from volume)
RUN mkdir -p /otel && chown smartfleet:smartfleet /otel

USER smartfleet

# Environment variables for OpenTelemetry
ENV OTEL_SERVICE_NAME=auth-service
ENV OTEL_EXPORTER_OTLP_ENDPOINT=http://sf-tempo:4317
ENV OTEL_METRICS_EXPORTER=none

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# Run application with virtual threads and OTel agent
ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:+ZGenerational", "-javaagent:/otel/opentelemetry-javaagent.jar", "-jar", "app.jar"]
