# ═══════════════════════════════════════════════════════════════
#  Borgol SOAP Auth Service
#  Spring Boot — RegisterUser / LoginUser / ValidateToken
# ═══════════════════════════════════════════════════════════════

# ── Stage 1: Build fat JAR ─────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Cache dependencies separately from source
COPY pom.xml ./
RUN mvn dependency:go-offline -B -q

# Build
COPY src ./src
RUN mvn clean package -B -q -DskipTests

# ── Stage 2: Minimal runtime image ────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar soap-auth.jar

# PORT is set automatically by Railway
EXPOSE 8081

CMD ["java", "-jar", "soap-auth.jar"]
