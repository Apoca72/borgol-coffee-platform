# ═══════════════════════════════════════════════════════════════
#  Borgol Coffee Platform — JSON Service (Web Mode)
#  Runs the Javalin REST API on Railway (no JavaFX / no display)
# ═══════════════════════════════════════════════════════════════

FROM maven:3.9.6-eclipse-temurin-21-alpine

WORKDIR /app

# ── 1. Copy pom.xml and pre-download all dependencies ──────────
# (This layer is cached as long as pom.xml doesn't change)
COPY pom.xml ./
RUN mvn dependency:resolve \
        dependency:resolve-plugins \
        -B -q 2>/dev/null || true

# ── 2. Copy source and compile ─────────────────────────────────
COPY src ./src
RUN mvn compile -B -q

# ── 3. Runtime configuration ───────────────────────────────────
# MODE=web  → starts Javalin REST server, skips JavaFX
# PORT      → set automatically by Railway
# SOAP_SERVICE_URL → set manually in Railway env vars after
#             deploying soap-auth-service
ENV MODE=web
ENV PORT=7000

EXPOSE ${PORT}

# ── 4. Start the application ───────────────────────────────────
CMD ["mvn", "exec:java", "-B", "-q"]
