# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# DJL caches the downloaded all-MiniLM-L6-v2 weights here on first startup.
# Mount this as a volume in production so the model isn't re-downloaded on
# every container restart/redeploy.
ENV DJL_CACHE_DIR=/app/.djl.ai
RUN mkdir -p ${DJL_CACHE_DIR}

COPY --from=build /build/target/ai-customer-support.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
