FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
# proto nằm trong services/payment-service/src/main/proto (đi theo src bên dưới).
COPY services/payment-service/pom.xml /workspace/services/payment-service/
WORKDIR /workspace/services/payment-service
RUN mvn -B -ntp dependency:go-offline
COPY services/payment-service/src ./src
RUN mvn -B -ntp clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app && mkdir -p /app && chown -R app:app /app
WORKDIR /app
COPY --from=build /workspace/services/payment-service/target/payment-service.jar app.jar
USER app
EXPOSE 8085 9085
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC"
HEALTHCHECK --interval=10s --timeout=5s --start-period=40s --retries=6 \
  CMD wget -q -O- http://localhost:8085/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
