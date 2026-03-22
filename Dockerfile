# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src src
RUN ./mvnw package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S prepcreatine && adduser -S prepcreatine -G prepcreatine

# Copy layered JAR from build stage
COPY --from=build /app/target/prepcreatine-backend-*.jar app.jar

# Switch to non-root user
USER prepcreatine

EXPOSE 8080

# Health check for Railway/Render
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-Dspring.profiles.active=prod", \
            "--enable-preview", \
            "-jar", "app.jar"]
