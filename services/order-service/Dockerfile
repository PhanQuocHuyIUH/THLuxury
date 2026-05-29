FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
# proto nằm trong services/order-service/src/main/proto (đi theo src bên dưới).
COPY services/order-service/pom.xml /workspace/services/order-service/
WORKDIR /workspace/services/order-service
RUN mvn -B -ntp dependency:go-offline
COPY services/order-service/src ./src
RUN mvn -B -ntp clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app && mkdir -p /app && chown -R app:app /app
WORKDIR /app
COPY --from=build /workspace/services/order-service/target/order-service.jar app.jar
USER app
EXPOSE 8084
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC"
HEALTHCHECK --interval=10s --timeout=5s --start-period=40s --retries=6 \
  CMD wget -q -O- http://localhost:8084/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
