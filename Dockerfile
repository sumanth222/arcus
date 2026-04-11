# ─── Stage 1: Build ───────────────────────────────────────────────────────────
# Uses full JDK to compile and package the app into a fat JAR.
# This stage is DISCARDED after the build — none of the JDK, Gradle,
# or source code ends up in the final deployed image.
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Copy gradle wrapper and config first (layer-caching)
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle

# Download dependencies (cached unless build.gradle changes)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# Copy source and build the fat JAR
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
# Starts FRESH from a slim JRE-only image (~200 MB vs ~600 MB).
# Only the compiled JAR from Stage 1 is copied in — nothing else.
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Non-root user for security
RUN groupadd --system arcus && useradd --system --gid arcus arcus

# Copy the fat JAR from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

RUN chown arcus:arcus app.jar
USER arcus

# Expose the default Spring Boot port
EXPOSE 8080

# JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]


