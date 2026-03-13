# ============================================================
# Stage 1: Build with Gradle
# ============================================================
FROM gradle:8.7-jdk17 AS builder

WORKDIR /app

# Copy build files first (dependency caching layer)
COPY build.gradle settings.gradle ./

# Download dependencies ahead of source copy
RUN gradle dependencies --no-daemon || true

# Copy full source
COPY src ./src

# Build fat JAR (Shadow plugin)
RUN gradle shadowJar --no-daemon -x test

# ============================================================
# Stage 2: Runtime — minimal JRE image
# ============================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Directory for H2 database files
RUN mkdir -p /app/data

# Copy the fat JAR produced by shadowJar
COPY --from=builder /app/build/libs/academic-events.jar app.jar

# App port and H2 TCP server port
EXPOSE 8080
EXPOSE 9092

# Defaults (overridable via docker-compose env)
ENV PORT=8080
ENV DB_URL=jdbc:h2:tcp://localhost:9092/./data/academic_events

ENTRYPOINT ["java", "-jar", "app.jar"]
