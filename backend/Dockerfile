
# Stage 1: Build application with Maven
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build artifact
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Run as non-root user for security best practices
RUN addgroup -S appgroup && adduser -S appuser -G appgroup && \
    mkdir -p /app/data/photos /app/data/conventions /app/data/justificatifs && \
    chown -R appuser:appgroup /app && \
    chmod 700 /app/data/justificatifs

USER appuser

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-jar", "app.jar"]
