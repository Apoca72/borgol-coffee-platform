#!/bin/bash
# ────────────────────────────────────────────────────────────────────────────
# deploy.sh — Borgol Coffee Platform: Render deployment helper
#
# Render deploys automatically on every push to the connected branch.
# This script is a local pre-push sanity check — it builds the fat JAR
# and verifies there are no compile errors before you push.
#
# Usage:  bash deploy.sh
# ────────────────────────────────────────────────────────────────────────────

set -e

echo "=== Borgol: pre-deploy build check ==="

# ── 1. Compile and package (skip tests for speed) ────────────────────────────
echo "[1/2] Building fat JAR..."
mvn package -B -q -DskipTests -Dfile.encoding=UTF-8
echo "      ✓ Build succeeded: target/cafe-project-1.0-SNAPSHOT.jar"

# ── 2. Quick smoke-test in web mode (exits after 3s) ─────────────────────────
echo "[2/2] Smoke-testing web mode startup..."
timeout 8s java -Dfile.encoding=UTF-8 \
    -DMODE=web \
    -jar target/cafe-project-1.0-SNAPSHOT.jar &>/tmp/borgol-smoke.log &
PID=$!
sleep 5
if kill -0 "$PID" 2>/dev/null; then
    kill "$PID" 2>/dev/null || true
    echo "      ✓ Server started successfully (smoke test passed)"
else
    echo "      ✗ Server failed to start — check /tmp/borgol-smoke.log"
    cat /tmp/borgol-smoke.log
    exit 1
fi

echo ""
echo "=== All checks passed. Push to trigger Render deploy: ==="
echo "    git push origin main"
