# ═══════════════════════════════════════════════════════════════
#  Borgol Coffee Platform — JSON Service (Web Mode)
#  Runs the Javalin REST API on Render (no JavaFX / no display)
# ═══════════════════════════════════════════════════════════════

FROM maven:3.9.6-eclipse-temurin-21-alpine

WORKDIR /app

# ── 1. Cache Maven dependencies (only re-runs when pom.xml changes) ──
COPY pom.xml ./
RUN mvn dependency:resolve dependency:resolve-plugins \
        -B -q 2>/dev/null || true

# ── 2. Copy source and build fat JAR ───────────────────────────
# -Dfile.encoding=UTF-8 → Монгол/Unicode тайлбар бүхий Java файлуудыг
# Alpine Linux-д зөв compile хийнэ (Alpine default charset нь UTF-8 биш)
COPY src ./src
RUN mvn package -B -q -DskipTests -Dfile.encoding=UTF-8

# ── 3. Runtime configuration ───────────────────────────────────
# MODE=web  → starts Javalin REST server, skips JavaFX
# PORT      → overridden at runtime by Render (default: 7000)
# All other env vars (DATABASE_URL, REDIS_*, JWT_SECRET, etc.)
# are injected by Render via render.yaml — no hardcoding needed
ENV MODE=web
ENV PORT=7000

EXPOSE ${PORT}

# ── 4. Run the executable fat JAR ──────────────────────────────
CMD ["java", "-Dfile.encoding=UTF-8", "-jar", "target/cafe-project-1.0-SNAPSHOT.jar"]
