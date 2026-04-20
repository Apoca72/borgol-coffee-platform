# Borgol — Complete On-Disk Project Structure

> Last updated: 2026-04-20
> Machine: Windows 11 — `C:\Users\thatu\`

---

## Overview — Two Copies on Disk

There are **two separate git repositories** for this project on this machine.
They share the same GitLab origin remote but diverge significantly in content and purpose.

```
C:\Users\thatu\
├── OneDrive\Desktop\cafe-project\     ← ACTIVE DEVELOPMENT COPY
│                                         88 commits, fully compiled, GitHub + GitLab remotes
│                                         Branch: feat/redis-cache-email-service
│                                         This is the production-ready version.
│
└── eclipse-workspace\cafe-project\   ← ECLIPSE IDE COPY
                                          10 commits, no compiled artifacts, GitLab only
                                          Branch: feature/borgol-platform
                                          Used for Eclipse IDE development (older branch).
```

---

## Active Copy — `C:\Users\thatu\OneDrive\Desktop\cafe-project\`

**Git remotes:**
- `github` → https://github.com/Apoca72/borgol-coffee-platform  (primary push target)
- `origin` → https://gitlab.com/that.u.sir.name/NUM_SoftwareConstruction_IndividualProject  (academic submission)

**Branch:** `feat/redis-cache-email-service` (88 commits, up to date with GitHub)

```
cafe-project/
│
│  ── ROOT CONFIG ───────────────────────────────────────────────────────────
├── pom.xml                      Maven build descriptor
│                                  Java 21 · Javalin 6.3 · JavaFX 21.0.2
│                                  Jedis 5.1.0 · Jakarta Mail 2.0
│                                  Anthropic SDK 2.16.1 · H2 · PostgreSQL
│                                  Gson · Jackson · JUnit 5 · PDFBox 3.0.1
├── mvnw / mvnw.cmd              Maven wrapper (no local Maven required)
├── Dockerfile                   Container image for SERVER 1 (Javalin :7000)
├── railway.toml                 Railway.app deploy config for SERVER 1
├── deploy.sh                    Deployment helper script
├── README.md                    Full project documentation
├── STRUCTURE.md                 ← this file
├── .gitignore
├── .dockerignore
│
│  ── CI/CD ──────────────────────────────────────────────────────────────────
├── .github/
│   └── workflows/
│       └── deploy.yml           GitHub Actions pipeline
│
│  ── LOCAL DATABASE (H2 embedded, dev/offline only) ─────────────────────────
├── data/
│   ├── cafe_db.mv.db            H2 database file (229 KB, active)
│   ├── cafe_db.lock.db          Lock file (cleared when server stops)
│   ├── cafe_db.trace.db         SQL trace log
│   └── welcomed.flag            Seed-run marker (prevents duplicate seeding)
│
│  ── DOCUMENTATION ───────────────────────────────────────────────────────────
├── docs/
│   └── superpowers/
│       ├── plans/
│       │   ├── 2026-04-06-desktop-web-parity.md
│       │   └── 2026-04-14-parity-sprint.md
│       └── specs/
│           ├── 2026-04-06-desktop-web-parity-design.md
│           └── 2026-04-14-parity-sprint-design.md
│
│  ── COMPILED OUTPUT (target/ — DO NOT EDIT) ─────────────────────────────────
├── target/                                         66 MB total
│   ├── cafe-project-1.0-SNAPSHOT-shaded.jar        32 MB  ← deployable uber-jar
│   ├── cafe-project-1.0-SNAPSHOT.jar               32 MB
│   ├── original-cafe-project-1.0-SNAPSHOT.jar     283 KB
│   ├── classes/                                    compiled .class files
│   │   └── public/                                 352 KB  ← SERVED BY JAVALIN
│   │       ├── index.html         216 KB  (copy of src/main/resources/public/index.html)
│   │       ├── profile.html        45 KB
│   │       ├── brew-timer.html     27 KB
│   │       ├── admin.html          20 KB
│   │       ├── register.html       14 KB
│   │       └── login.html          13 KB
│   ├── test-classes/
│   └── surefire-reports/          JUnit test results
│
│  ── TOOLING ────────────────────────────────────────────────────────────────
├── .claude/
│   ├── launch.json              Dev server: `cmd /c mvnw.cmd exec:java` → port 7000
│   └── settings.local.json      Claude Code local config
├── .mvn/wrapper/
│   └── maven-wrapper.properties Maven wrapper config
├── .remember/                   Session notes (Claude Code internal)
├── .settings/                   Eclipse IDE project settings
├── .classpath                   Eclipse classpath descriptor
├── .git/                        Git history (88 commits)
│   └── gk/                     GitKraken metadata
│
│  ════════════════════════════════════════════════════════════════════════════
│  SERVER 1 — Javalin REST API + JavaFX Desktop  (port 7000)
│  src/main/java/borgol/
│  ════════════════════════════════════════════════════════════════════════════
│
├── src/
│   ├── main/
│   │   ├── java/borgol/
│   │   │   │
│   │   │   ├── app/                          ── ENTRY POINT ──────────────────
│   │   │   │   ├── Main.java                 ★ Composition Root
│   │   │   │   │                               Reads MODE env var: web | desktop
│   │   │   │   │                               Wires all dependencies bottom-up:
│   │   │   │   │                                 DB → Repo → Service
│   │   │   │   │                                 → EventBus → Gateway → Server
│   │   │   │   │                               Starts Redis Pub/Sub virtual thread
│   │   │   │   │                               Registers ConsoleMenuObserver
│   │   │   │   └── ConsoleMenuObserver.java  Observer concrete impl
│   │   │   │                                   Prints menu changes to stdout
│   │   │   │
│   │   │   ├── core/                         ── DOMAIN (zero framework deps) ──
│   │   │   │   │
│   │   │   │   ├── domain/                   Pure Java POJOs — no annotations
│   │   │   │   │   ├── User.java             id · username · email · passwordHash
│   │   │   │   │   │                           bio · avatarUrl · expertiseLevel
│   │   │   │   │   │                           followerCount · followingCount
│   │   │   │   │   ├── Recipe.java           id · authorId · title · drinkType
│   │   │   │   │   │                           ingredients · instructions
│   │   │   │   │   │                           brewTimeMins · difficulty · imageUrl
│   │   │   │   │   │                           flavorTags · likeCount · savedCount
│   │   │   │   │   ├── RecipeComment.java    id · recipeId · authorId · content
│   │   │   │   │   ├── CafeListing.java      id · name · address · city · district
│   │   │   │   │   │                           phone · hours · avgRating
│   │   │   │   │   ├── BrewJournalEntry.java id · userId · coffeeBean · origin
│   │   │   │   │   │                           roastLevel · brewMethod · waterTempC
│   │   │   │   │   │                           doseGrams · brewTimeSec
│   │   │   │   │   │                           aroma/flavor/acidity/body/
│   │   │   │   │   │                           sweetness/finish (1–5 stars)
│   │   │   │   │   ├── BrewGuide.java        id · methodName · brewTimeMin
│   │   │   │   │   │                           parameters · steps[]
│   │   │   │   │   ├── LearnArticle.java     id · title · category · content
│   │   │   │   │   │                           readTimeMin
│   │   │   │   │   ├── Equipment.java        id · userId · category
│   │   │   │   │   │                           (GRINDER|BREWER|KETTLE|SCALE|OTHER)
│   │   │   │   │   │                           brand · notes
│   │   │   │   │   ├── MenuItem.java         id · name · MenuCategory · price
│   │   │   │   │   │                           available
│   │   │   │   │   └── MenuCategory.java     Enum: COFFEE|TEA|SMOOTHIE|FOOD|DESSERT
│   │   │   │   │
│   │   │   │   ├── ports/                    Hexagonal outbound port interfaces
│   │   │   │   │   ├── IMenuRepository.java  save() · findById() · getAll() · delete()
│   │   │   │   │   └── MenuChangeObserver.java  onItemAdded/Removed/Updated()
│   │   │   │   │
│   │   │   │   └── application/              ── BUSINESS LOGIC ───────────────
│   │   │   │       ├── BorgolService.java    ★ Core application service
│   │   │   │       │                           User CRUD + follow/block
│   │   │   │       │                           Recipe CRUD + like/comment/save
│   │   │   │       │                           Café listing + rating + nearby
│   │   │   │       │                           Social feed generation
│   │   │   │       │                           Brew journal management
│   │   │   │       │                           Notification create → eventBus.publish()
│   │   │   │       │                           Trending: Redis ZINCRBY per flavor tag
│   │   │   │       │                           User cache: Redis HSET (8 fields, 600s)
│   │   │   │       │                           Bean AI: Gemini 1.5 Flash via SDK
│   │   │   │       │                           Admin: reports + stats
│   │   │   │       ├── MenuService.java      Menu CRUD + Observer dispatch
│   │   │   │       └── MenuDto.java          Java Record: MenuItem projection
│   │   │   │
│   │   │   ├── infrastructure/               ── FRAMEWORK ADAPTERS ───────────
│   │   │   │   │
│   │   │   │   ├── config/
│   │   │   │   │   └── DatabaseConnection.java  Singleton (DCL + volatile)
│   │   │   │   │                                  Reads database.properties
│   │   │   │   │                                  H2 if no DATABASE_URL env var
│   │   │   │   │                                  PostgreSQL if DATABASE_URL present
│   │   │   │   │
│   │   │   │   ├── persistence/              ── DATA ACCESS ──────────────────
│   │   │   │   │   ├── BorgolRepository.java ★ Master DAO (all 18+ tables)
│   │   │   │   │   │                           PreparedStatement · MERGE INTO
│   │   │   │   │   │                           Idempotent schema migration on start
│   │   │   │   │   │                           Seeds: brew guides, learn articles,
│   │   │   │   │   │                             demo users, cafes (if empty)
│   │   │   │   │   ├── JdbcMenuRepository.java    IMenuRepository → JDBC impl
│   │   │   │   │   ├── InMemoryMenuRepository.java IMenuRepository → HashMap impl
│   │   │   │   │   └── RepositoryFactory.java     Creates correct impl from properties
│   │   │   │   │
│   │   │   │   ├── security/                 ── AUTH ─────────────────────────
│   │   │   │   │   ├── JwtUtil.java          Custom HMAC-SHA256 JWT
│   │   │   │   │   │                           No external JWT lib
│   │   │   │   │   │                           javax.crypto.Mac + Base64
│   │   │   │   │   │                           7-day expiry · constant-time compare
│   │   │   │   │   ├── PasswordUtil.java     SHA-256 + SecureRandom salt
│   │   │   │   │   │                           Format: "saltHex:hashHex"
│   │   │   │   │   └── SoapAuthClient.java   HTTP Proxy → SERVER 2 (:8081)
│   │   │   │   │                               RegisterUser / LoginUser / ValidateToken
│   │   │   │   │                               Falls back to local JwtUtil on failure
│   │   │   │   │
│   │   │   │   ├── cache/                    ── REDIS LAYER ──────────────────
│   │   │   │   │   ├── RedisClient.java      Singleton (DCL + volatile)
│   │   │   │   │   │                           JedisPool · REDIS_HOST/PORT/PASSWORD
│   │   │   │   │   │                           pool() for Pub/Sub + data structure ops
│   │   │   │   │   └── CacheKeyBuilder.java  Redis key constants:
│   │   │   │   │                               borgol:recipe:{id}      300s TTL
│   │   │   │   │                               borgol:user:{id}        600s TTL (Hash)
│   │   │   │   │                               borgol:feed:userId:{id}  60s TTL
│   │   │   │   │                               borgol:cafes:nearby:{lat}:{lng} 120s
│   │   │   │   │                               borgol:trending          Sorted Set
│   │   │   │   │                               borgol:ratelimit:{ip}    INCR+EXPIRE
│   │   │   │   │                               borgol:notify:{userId}   Pub/Sub channel
│   │   │   │   │
│   │   │   │   ├── messaging/                ── EVENT BUS ────────────────────
│   │   │   │   │   └── RedisEventBus.java    Redis Pub/Sub wrapper
│   │   │   │   │                               1 virtual thread: PSUBSCRIBE *
│   │   │   │   │                               Fan-out → Consumer<String> SSE handlers
│   │   │   │   │                               subscribe(userId, handler)
│   │   │   │   │                               unsubscribe(userId, handler)
│   │   │   │   │
│   │   │   │   └── email/                    ── SMTP ─────────────────────────
│   │   │   │       └── EmailService.java     Jakarta Mail 2.0
│   │   │   │                                   Registration confirmation
│   │   │   │                                   Password reset
│   │   │   │                                   Env: SMTP_HOST · SMTP_PORT
│   │   │   │                                        SMTP_USER · SMTP_PASSWORD
│   │   │   │                                        EMAIL_FROM
│   │   │   │
│   │   │   └── ui/                           ── UI ADAPTERS ──────────────────
│   │   │       │
│   │   │       ├── web/                      ── HTTP (production) ────────────
│   │   │       │   ├── ApiGateway.java       ★ GATEWAY LAYER
│   │   │       │   │                           registerFilters(Javalin):
│   │   │       │   │                             · CORS headers (app.before)
│   │   │       │   │                             · Rate limit: Redis INCR+EXPIRE
│   │   │       │   │                               5 req / 60s per IP · fail-open
│   │   │       │   │                             · Request audit log (DEBUG)
│   │   │       │   │                           authenticate(ctx, required):
│   │   │       │   │                             · SOAP ValidateToken → JWT fallback
│   │   │       │   │                             · Token from header OR ?token= param
│   │   │       │   │                               (EventSource browser compatibility)
│   │   │       │   │
│   │   │       │   ├── BorgolApiServer.java  ★ FRONT CONTROLLER (30+ routes)
│   │   │       │   │                           All auth via gateway.authenticate()
│   │   │       │   │                           Never imports JwtUtil directly
│   │   │       │   │                           SSE endpoints:
│   │   │       │   │                             GET /api/notifications/stream
│   │   │       │   │                               25s heartbeat loop
│   │   │       │   │                             GET /api/bean/chat
│   │   │       │   │                               Gemini token streaming
│   │   │       │   │
│   │   │       │   └── CafeApiServer.java    Legacy menu REST adapter
│   │   │       │                               GET/POST/PUT/DELETE /api/menu/*
│   │   │       │
│   │   │       └── desktop/                  ── JAVAFX (MODE=desktop) ────────
│   │   │           ├── BorgolApp.java        JavaFX Application entry
│   │   │           ├── MainWindow.java       BorderPane: navbar + center pane
│   │   │           ├── AppSession.java       Static: userId + username
│   │   │           ├── FeedPane.java         Social feed (3-column layout)
│   │   │           ├── RecipesPane.java      Recipe browser + CRUD
│   │   │           ├── CafesPane.java        Café list + rating + GPS nearby
│   │   │           ├── JournalPane.java      Brew log + SVG radar chart + CSV
│   │   │           ├── BrewTimerPane.java    Timer: V60/Espresso/FP/AP/Moka/CB
│   │   │           ├── LearnPane.java        Master-detail: guides + articles
│   │   │           ├── ProfilePane.java      Bio + avatar upload + equipment
│   │   │           ├── AdminPane.java        Report queue TableView
│   │   │           ├── PeoplePane.java       User discovery cards
│   │   │           ├── MapPane.java          Leaflet.js WebView (café map)
│   │   │           ├── QuickBrewOverlay.java Modal brew method step guide
│   │   │           └── UiUtils.java          Shared styling helpers
│   │   │
│   │   └── resources/
│   │       ├── database.properties           app.persistence.mode=DB
│   │       │                                   H2 JDBC URL: ./data/cafe_db
│   │       │                                   credentials: sa / (empty)
│   │       ├── style.css                     JavaFX desktop stylesheet (warm theme)
│   │       ├── style-dark.css                JavaFX desktop dark variant
│   │       │
│   │       └── public/                       ── FRONTEND ─────────────────────
│   │           │   Served by Javalin at http://localhost:7000/
│   │           │   After editing: cp src/.../public/index.html target/classes/public/
│   │           │
│   │           ├── index.html   216 KB  ★ MAIN SPA
│   │           │                          6 tabs: Feed · Recipes · Cafes
│   │           │                                  Journal · Learn · Timer
│   │           │                          Dark mode: 🌙/☀️ toggle, localStorage
│   │           │                          Bean AI: FAB chat (Gemini 1.5 Flash SSE)
│   │           │                          Notifications: SSE bell (Redis Pub/Sub)
│   │           │                          Map: Leaflet.js + OpenStreetMap (no key)
│   │           │                          Radar chart: SVG, 6 axes
│   │           │                          Timer: QuickBrew overlay → full timer
│   │           │                          Export: PDF (window.print) + CSV (Blob)
│   │           │
│   │           ├── profile.html  45 KB  Profile edit · avatar (canvas→JPEG→base64)
│   │           │                          Equipment list · flavor prefs · saved recipes
│   │           ├── brew-timer.html 27 KB Standalone timer page (same as timer tab)
│   │           ├── admin.html    20 KB  Report queue · resolve/reject · stats
│   │           │                          Guard: fetches /api/admin/stats on load
│   │           │                          Redirects if 401/403
│   │           ├── register.html 14 KB  → POST /api/soap/register (SOAP fallback local)
│   │           └── login.html    13 KB  → POST /api/soap/login (SOAP fallback local)
│   │
│   └── test/
│       └── java/borgol/
│           └── MenuServiceTest.java   JUnit 5: CRUD + Observer pattern
│                                         uses InMemoryMenuRepository (no DB)
│
│  ════════════════════════════════════════════════════════════════════════════
│  SERVER 2 — SOAP Auth Service  (port 8081)
│  Separate JVM · Separate Docker container · Separate Railway service
│  ════════════════════════════════════════════════════════════════════════════
│
└── soap-auth-service/
    ├── pom.xml                  Spring Boot 3.2.5 parent
    │                              Spring-WS · JAXB · Wsdl4j · Lombok
    ├── Dockerfile               Container image for SERVER 2
    ├── railway.toml             Railway deploy config for SERVER 2
    ├── .dockerignore
    ├── .classpath / .project    Eclipse descriptors
    ├── .factorypath             Annotation processor config
    ├── .settings/               Eclipse IDE settings
    │
    ├── target/                  Compiled SOAP service artifacts
    │
    └── src/main/
        ├── java/borgol/auth/
        │   │
        │   ├── SoapAuthApplication.java   Spring Boot entry · port 8081
        │   │
        │   ├── config/
        │   │   ├── WebServiceConfig.java  Spring-WS config
        │   │   │                            Exposes WSDL at /ws/*.wsdl
        │   │   │                            Message dispatcher bean
        │   │   └── CorsFilter.java        CORS filter for SOAP responses
        │   │
        │   ├── endpoint/
        │   │   └── AuthEndpoint.java      ★ SOAP DISPATCHER
        │   │                                @PayloadRoot routing:
        │   │                                RegisterUserRequest → register()
        │   │                                LoginUserRequest    → login()
        │   │                                ValidateTokenRequest → validate()
        │   │
        │   ├── service/
        │   │   └── AuthService.java       JWT: HMAC-SHA256 (matches JwtUtil logic)
        │   │                                Password: SHA-256 + salt
        │   │                                Storage: ConcurrentHashMap (in-memory)
        │   │                                No database — stateless service
        │   │
        │   ├── model/
        │   │   └── AuthUser.java          In-memory entity: username · passwordHash
        │   │
        │   └── dto/                       JAXB-annotated (namespace: num.edu.mn/soapauth)
        │       ├── LoginUserRequest.java        username · password
        │       ├── LoginUserResponse.java       userId · jwtToken · message
        │       ├── RegisterUserRequest.java     username · password · email
        │       ├── RegisterUserResponse.java    userId · message
        │       ├── ValidateTokenRequest.java    token
        │       ├── ValidateTokenResponse.java   isValid · userId · username · expiresAt
        │       └── package-info.java            JAXB namespace declaration
        │
        └── resources/
            ├── application.properties     server.port=8081
            │                                jwt.secret=...
            │                                logging.level.org.springframework.ws=DEBUG
            └── xsd/
                └── auth-service.xsd       WSDL schema definition
                                             Types: all Request/Response shapes
```

---

## Eclipse Workspace Copy — `C:\Users\thatu\eclipse-workspace\cafe-project\`

**Git remotes:** `origin` → GitLab only (no GitHub remote)
**Branch:** `feature/borgol-platform` (10 commits — older, simpler codebase)
**Build:** Not compiled — `target/classes/` is empty, no JAR files

```
eclipse-workspace/cafe-project/
│
├── pom.xml                      Same Maven config (Java 21, Javalin, JavaFX)
├── mvnw / mvnw.cmd
├── README.md                    164 lines (basic version)
├── .gitignore · .classpath · .project
├── .claude/launch.json          Dev server config
├── .mvn/ · .settings/ · .remember/
│
├── data/                        H2 database (116 KB — older/smaller dataset)
│
├── src/main/java/borgol/
│   ├── app/
│   │   ├── Main.java            Composition Root (simpler wiring — no Redis/Gateway)
│   │   ├── MainWeb.java         Javalin-only entry (no JavaFX dependency)
│   │   └── ConsoleMenuObserver.java
│   │
│   ├── core/                    Same domain objects as OneDrive version
│   │   ├── domain/              (User, Recipe, CafeListing, etc.)
│   │   ├── ports/               (IMenuRepository, MenuChangeObserver)
│   │   └── application/
│   │       ├── BorgolService.java   Simpler — no Redis data structures,
│   │       │                          no event bus integration
│   │       ├── MenuService.java
│   │       └── MenuDto.java
│   │
│   └── infrastructure/
│       ├── config/DatabaseConnection.java
│       └── persistence/
│           ├── BorgolRepository.java
│           ├── JdbcMenuRepository.java
│           ├── InMemoryMenuRepository.java
│           └── RepositoryFactory.java
│           NOTE: No cache/, messaging/, email/, security/ packages
│                 No ApiGateway, no RedisEventBus, no EmailService
│
├── src/main/resources/
│   ├── database.properties
│   ├── style.css · style-dark.css
│   └── public/
│       └── index.html           Dark mode added (54 lines) — uncommitted
│                                Missing: SSE notification stream, many features
│
└── target/                      EMPTY — not compiled
    ├── classes/                 (no .class files, no public/ directory)
    └── (no JAR files)
```

**What this copy is missing vs OneDrive:**
- No `soap-auth-service/` directory (SOAP microservice not present)
- No `Dockerfile`, `railway.toml`, `deploy.sh` (not deployable)
- No `docs/` directory
- No `src/main/java/.../infrastructure/cache/` (no Redis)
- No `src/main/java/.../infrastructure/messaging/` (no Pub/Sub)
- No `src/main/java/.../infrastructure/email/` (no SMTP)
- No `src/main/java/.../infrastructure/security/` (no JwtUtil, SoapAuthClient)
- No `src/main/java/.../ui/web/ApiGateway.java`
- No `src/main/java/.../app/MainWeb.java` (actually present, but not in OneDrive)
- Not compiled (cannot run without `mvnw compile` first)

---

## Other Workspace Projects — `C:\Users\thatu\eclipse-workspace\`

```
eclipse-workspace/
├── .metadata/                   Eclipse workspace metadata
├── cafe-project/                (see above)
├── NUM_SoftwareConstruction_Lab3/   Lab assignment
├── soa-week01-intro/            SOA week 1 exercises
├── tms-project/                 Separate project (Time Management System?)
└── workspace_sc_lab1/           Software Construction lab 1
```

---

## Project Documentation — `C:\Users\thatu\Downloads\`

These are standalone documents (not in any git repo):

| File | Size | Date | Contents |
|------|------|------|----------|
| `Borgol_MEGA_Complete.docx` | 13.5 KB | Mar 23 | Comprehensive spec document |
| `Borgol_PM_Plan_v3.docx` | 85 KB | Mar 13 | Project management plan v3 |
| `Borgol_PM_v5_FINAL_CS2.docx` | 67.5 KB | Apr 1 | Final PM plan v5 |
| `Borgol_Notion_Database.csv` | 2.7 KB | Apr 1 | Database schema reference |
| `borgol-update.zip` | 48.7 KB | Mar 21 | Archived update package |

---

## Claude Code Config — `C:\Users\thatu\.claude\`

```
.claude/
├── settings.json                Global Claude Code config
├── .credentials.json            OAuth tokens + MCP auth (do not commit)
├── plans/
│   └── c-users-thatu-onedrive-...-buzzing-moonbeam.md   Active plan file
├── plugins/                     Installed Claude Code plugins
├── projects/                    Per-workspace memory + session history
│   ├── C--Users-thatu-eclipse-workspace-cafe-project/   Eclipse workspace sessions
│   └── C--Users-thatu-OneDrive-Desktop-cafe-project/    OneDrive sessions (active)
│       └── memory/
│           └── MEMORY.md        Persistent project memory (stack, keys, patterns)
├── skills/                      Custom skills
├── history.jsonl                Session command history (21 KB)
└── cache/                       Response cache
```

---

## Which Copy to Use

| Task | Use |
|------|-----|
| Running the server | `OneDrive\Desktop\cafe-project\` — has compiled target + `data/` |
| Editing Java backend | `OneDrive\Desktop\cafe-project\` — has all infrastructure packages |
| Editing frontend HTML | `OneDrive\Desktop\cafe-project\src\main\resources\public\` |
| Pushing to GitHub | `OneDrive\Desktop\cafe-project\` — has `github` remote |
| Academic submission | Either — both have `origin` GitLab remote |
| Eclipse IDE editing | `eclipse-workspace\cafe-project\` — configured for Eclipse |

**After editing any HTML file:**
```bash
cp src/main/resources/public/index.html target/classes/public/index.html
# then hard-reload browser: Ctrl+Shift+R or add ?cb=<timestamp>
```

**To run the server:**
```bash
cd "C:\Users\thatu\OneDrive\Desktop\cafe-project"
./mvnw exec:java           # web mode (default)
# OR
./mvnw javafx:run          # desktop + web mode
```

**Startup order (if using SOAP auth):**
```bash
# Terminal 1 — SOAP Auth Service (port 8081)
cd soap-auth-service && mvn spring-boot:run

# Terminal 2 — Javalin REST API (port 7000)
cd .. && ./mvnw exec:java
```
