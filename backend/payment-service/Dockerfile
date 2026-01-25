# SmartFleet Payment Service
# Multi-stage Dockerfile for optimized production builds

# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B || true
COPY src/ src/
RUN ./mvnw package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN apk add --no-cache wget
RUN addgroup -S smartfleet && adduser -S smartfleet -G smartfleet
COPY --from=builder /app/target/*.jar app.jar
RUN mkdir -p /otel && chown smartfleet:smartfleet /otel

USER smartfleet

ENV OTEL_SERVICE_NAME=payment-service
ENV OTEL_EXPORTER_OTLP_ENDPOINT=http://sf-tempo:4317
ENV OTEL_METRICS_EXPORTER=none

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8084/actuator/health || exit 1

EXPOSE 8084
ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:+ZGenerational", "-javaagent:/otel/opentelemetry-javaagent.jar", "-jar", "app.jar"]
