# Stage 1 - Build
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src/ src/
RUN mvn clean package -DskipTests -B

# Stage 2 - Runtime
FROM eclipse-temurin:17-jre-alpine
LABEL org.opencontainers.image.title="arremateai-gateway"
LABEL org.opencontainers.image.description="API Gateway do ArremateAI - roteamento, JWT e propagação de headers"
LABEL org.opencontainers.image.vendor="ArremateAI"
LABEL org.opencontainers.image.source="https://github.com/Quintanilha09/arremateai-gateway"
LABEL org.opencontainers.image.licenses="MIT"
RUN apk add --no-cache curl
RUN addgroup -S appgroup && adduser -S appuser -G appgroup -u 1001
WORKDIR /app
COPY --from=build /app/target/arremateai-gateway-0.0.1-SNAPSHOT.jar app.jar
RUN chown -R appuser:appgroup /app
USER appuser
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=40s \
    CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
