FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN addgroup --system tripforge && adduser --system --ingroup tripforge tripforge
USER tripforge
COPY --from=builder /app/target/ai-orchestrator-service-1.0.0.jar app.jar
EXPOSE 8089
HEALTHCHECK --interval=15s --timeout=10s --start-period=90s --retries=12 \
  CMD curl -sf http://localhost:8089/actuator/health | grep -q '"status":"UP"\|"status":"DEGRADED"' || exit 1
ENTRYPOINT ["java", "-jar", "-Djava.security.egd=file:/dev/./urandom", "app.jar"]
