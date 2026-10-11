# --- Build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml first and pre-fetch dependencies as a separate layer -
# this layer only gets invalidated when pom.xml changes, not on every
# code change, so most rebuilds skip re-downloading the whole internet.
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# --- Runtime stage ---
# A JRE (not a full JDK) image, and alpine (not the full Debian base) -
# both meaningfully shrink the final image, which matters for deploy
# speed and Render's free-tier resource limits.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Render (and most platforms) inject a PORT env var and route traffic to
# it - the app must listen on whatever PORT says, not a hardcoded value.
# See application.yml: server.port: ${PORT:8080}
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
