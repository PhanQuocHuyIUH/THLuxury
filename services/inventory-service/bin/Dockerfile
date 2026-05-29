FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
# Copy proto files first
COPY proto /workspace/proto
# Copy inventory-service sources
COPY services/inventory-service/pom.xml /workspace/services/inventory-service/
WORKDIR /workspace/services/inventory-service
RUN mvn -B -ntp dependency:go-offline
COPY services/inventory-service/src ./src
RUN mvn -B -ntp clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app && mkdir -p /app && chown -R app:app /app
WORKDIR /app
COPY --from=build /workspace/services/inventory-service/target/inventory-service-0.1.0.jar app.jar
USER app
EXPOSE 8083 9083
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC"
HEALTHCHECK --interval=10s --timeout=5s --start-period=40s --retries=6 \
  CMD wget -q -O- http://localhost:8083/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
