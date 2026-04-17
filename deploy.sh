#!/bin/bash
# ────────────────────────────────────────────────────────────────────────────
# deploy.sh — Borgol Coffee Platform: Redis кэш + Email үйлчилгээ байршуулалт
#
# Ашиглах: bash deploy.sh
# Шаардлага: git remote origin тохируулагдсан байх ёстой
# ────────────────────────────────────────────────────────────────────────────

set -e  # Алдаа гарвал шууд зогсоно

echo "=== Borgol: Redis + Email байршуулалт эхлэв ==="

# ── Кэш давхаргын файлууд ─────────────────────────────────────────────────
git add src/main/java/mn/edu/num/cafe/infrastructure/cache/
# ── Имэйл үйлчилгээний файлууд ────────────────────────────────────────────
git add src/main/java/mn/edu/num/cafe/infrastructure/email/
# ── Бизнесийн логик (кэш + имэйл холболт) ────────────────────────────────
git add src/main/java/mn/edu/num/cafe/core/application/BorgolService.java
# ── Maven хамаарлууд (Jedis, Jakarta Mail, Gson) ──────────────────────────
git add pom.xml
# ── Railway байршуулалтын тохиргоо (Redis + SMTP орчны хувьсагчид) ────────
git add railway.toml
# ── README баримт бичиг шинэчлэл ─────────────────────────────────────────
git add README.md

git commit -m "feat: Redis cache layer (redis-cache-a) + Email service

- infrastructure/cache/RedisClient.java — DCL Singleton, Jedis pool
- infrastructure/cache/CacheKeyBuilder.java — borgol:{resource}:{id} key scheme
- BorgolService: cache-aside on getRecipeById, getUserById, getFeed, getCafesNearby
- infrastructure/email/EmailService.java — Jakarta Mail SMTP singleton
- Welcome + password reset + notification email templates
- railway.toml: REDIS_* and SMTP_* env var blocks added
"

git push origin main

echo "=== Байршуулалт амжилттай дууссан ==="
