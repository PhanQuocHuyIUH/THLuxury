# ============================================================
# Stage 1 — Build với Maven + JDK 21
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

# ============================================================
# Stage 2 — Runtime JRE 21 slim
# ============================================================
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app && \
    mkdir -p /app /secrets && chown -R app:app /app /secrets
WORKDIR /app
COPY --from=build /workspace/target/identity-service.jar app.jar

USER app
EXPOSE 8081
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC"
HEALTHCHECK --interval=10s --timeout=5s --start-period=40s --retries=6 \
  CMD wget -q -O- http://localhost:8081/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
