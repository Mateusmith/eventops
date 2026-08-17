FROM maven:3.9.11-eclipse-temurin-21-alpine AS construcao
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S eventops && adduser -S eventops -G eventops
WORKDIR /app
COPY --from=construcao /workspace/target/gestao-eventos-*.jar app.jar
USER eventops
EXPOSE 8080 9090
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=5 \
  CMD wget -q -O /dev/null http://localhost:${EVENTOPS_PORTA_HEALTHCHECK:-8080}/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
