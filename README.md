# Borgol — Кофе Сонирхогчдын Платформ

**Хичээл:** ICSI486 Программ хангамжийн бүтээлт
**Оюутан:** С.Тэмүүлэн — 22B1NUM6637
**Цахим хаяг:** https://borgol-production.up.railway.app

---

## Танилцуулга

Borgol нь кофе сонирхогчдод зориулсан нийгмийн платформ бөгөөд хэрэглэгчид жор хуваалцах, кафе судлах, дарлалтын тэмдэглэл хөтлөх, хэштэг дагах, жор хадгалах болон бусад хэрэглэгчтэй холбогдох боломж олгодог.

Систем нь үйлчилгээнд суурилсан архитектур (SOA) дээр бүтээгдсэн:
- JSON REST API — Javalin (Java 21), бүх платформын функцийг хариуцна
- SOAP Auth Service — Spring-WS (Spring Boot), JWT токен олгох, баталгаажуулах
- Веб интерфэйс — Vanilla HTML/CSS/JS, Java сервер дээрээс шууд хүргэгдэнэ
- PostgreSQL — Railway дээр байршуулсан өгөгдлийн сан
- Bean AI — Google Gemini 1.5 Flash дээр суурилсан кофены туслагч чат бот

---

## Хуудсууд

| Хуудас | Зам | Зорилго |
|---|---|---|
| index.html | / | Үндсэн апп — feed, жор, кафе, судлах, тэмдэглэл, суралцах, газрын зураг |
| profile.html | /profile.html | Профайл засах, зураг оруулах, биографи, хадгалсан жор, хэштэг |
| brew-timer.html | /brew-timer.html | Дарлалтын таймер ба харьцааны тооцоолуур |
| admin.html | /admin.html | Админ самбар — гомдол шийдвэрлэх (зөвхөн id=1 хэрэглэгч) |
| login.html | /login.html | Нэвтрэх |
| register.html | /register.html | Бүртгүүлэх |

---

## Функцүүд

### Bean — AI кофены туслагч

- Хуудас бүрт байрших чат цонх (баруун доод буланд)
- Google Gemini 1.5 Flash дээр суурилсан (өдөрт 1500 хүсэлт үнэгүй)
- Server-Sent Events ашиглан хариуг бодит цагт үг бүрээр дамжуулна
- Платформын мэдлэг, Улаанбаатарын кофены газрууд, бүх дарлалтын аргыг мэддэг
- Хэрэглэгчийн хэл дээр хариулна (монгол, англи гэх мэт)

### Нэвтрэх ба бүртгэх (SOA/SOAP)

- SOAP RegisterUser — шинэ бүртгэл үүсгэх
- SOAP LoginUser — нэвтрэх, JWT токен олгох
- SOAP ValidateToken — хамгаалагдсан хүсэлт бүрт шалгах
- HMAC-SHA256 JWT (7 хоногийн хүчинтэй хугацаа), SHA-256 + давс нууц үг хаш
- SOAP унасан тохиолдолд local JWT-р fallback хийнэ

### Профайл

- Зураг оруулах — client-side canvas-аар compress хийх (max 240px, JPEG), base64 TEXT болгон хадгалах
- Bio засах — PostgreSQL-д хадгалагдана
- Мэргэшлийн түвшин — BEGINNER, ENTHUSIAST, BARISTA, EXPERT
- Амтын сонголтын шошго — нэмэх, хасах
- Статистик — жорын тоо, дагагч, дагаж буй тоо

### Жор

- Жор үүсгэх, засах, устгах — дарлалтын хугацаа, хүндрэл, найрлага, заавар
- Зураг оруулах — client-side compress хийгдсэн, base64 TEXT болгон хадгалах (8 MB хүртэл)
- Амтын шошго, ундааны төрлөөр шүүх
- Like дарах, сэтгэгдэл бичих
- Жор хадгалах, профайлаас үзэх
- Жор мэдээлэх (гомдол шийдвэрлэх дараалалд орно)

### Feed

- Дагаж буй хэрэглэгчдийн жорууд
- Хэн ч дагаагүй бол trending жорыг харуулна

### Хэштэг

- Хэштэг дагаж feed-д холбогдох жорыг гарган ирэх
- Тренд хэштэгийн самбар

### Кафе

- Кафе үзэх, нэмэх, одоор үнэлэх, сэтгэгдэл бичих
- GPS-д суурилсан ойролцоо хайлт
- Leaflet.js газрын зураг (OpenStreetMap, API түлхүүр шаардахгүй)

### Дарлалтын тэмдэглэл

- Хувийн дарлалтын бичлэг, 6 тэнхлэгт radar chart (Aroma, Taste, Acidity, Body, Sweetness, Finish)
- CSV экспорт

### Дарлалтын таймер

6 аргад зориулсан алхам алхмаар удирдамж:

| Арга | Алхам | Харьцааны тохиргоо |
|---|---|---|
| V60 | 6 (цэцэглэх — урсах) | 1:15, 1:16, 1:17 |
| Espresso | 4 | 1:2, 1:2.5, 1:3 |
| French Press | 6 | 1:10, 1:12, 1:15 |
| AeroPress | 7 | 1:10, 1:13, 1:16 |
| Moka Pot | 6 | 1:7, 1:8, 1:9 |
| Cold Brew | 6 (12-18 цаг) | 1:5, 1:6, 1:8 |

- Хоёр талт харьцааны тооцоолуур — кофены жин өөрчлөхөд ус автоматаар шинэчлэгдэнэ
- SVG цагираг тоолуур, түр зогсоох, үргэлжлүүлэх, дахин эхлүүлэх
- Дарлалт дууссанд системийн мэдэгдэл

### Гомдол ба админ самбар

- Хэрэглэгч аливаа жор эсвэл хэрэглэгчийг шалтгаан, тайлбартайгаар мэдээлж болно
- Админ самбар (/admin.html) — зөвхөн анх бүртгүүлсэн хэрэглэгч (id=1) хандах боломжтой
- Хүлээгдэж буй, шийдвэрлэсэн, татгалзсан гомдлын тоо, шүүлтийн таб
- Шийдвэрлэх, татгалзах товч, skeleton loading, toast мэдэгдэл

### Мэдэгдэл (бодит цаг — Redis Pub/Sub + SSE)

- Навигацийн хонхны дүрс, уншаагүй тоолуур
- Like, сэтгэгдэл, дагасны мэдэгдэл
- Redis Pub/Sub `borgol:notify:{userId}` сувгаар нэвтрэн Server-Sent Events (SSE) хүлээн авна
- Polling-г бүрэн орлосон: `GET /api/notifications/stream` — нээлттэй холболтоор 25 секунд тутамд heartbeat явуулна
- EventSource нь `?token=` query параметраар JWT дамжуулна (browser-н хязгаарлалтаас болж)

### Харанхуй горим (Dark Mode)

- Навбарт 🌙 / ☀️ товч — нэг дарагдалаар тохиргоо хадгалагдана
- `localStorage` дотор `borgol_dark` гэсэн утгаар хадгалагдана
- Шинэ хэрэглэгч системийн `prefers-color-scheme` тохиргоог автоматаар уншина
- Дулаан хүрэн/кофе өнгөний палитр — брэндийн өнгийг хадгалсан харанхуй горим

---

## Архитектур

### Системийн бүрэлдэхүүн (хоёр тусдаа серверийн үйл ажиллагаа)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                        FRONTEND  (browser-д ажиллана)                        │
│                                                                              │
│  index.html      — Үндсэн апп (Feed, Recipes, Cafes, Journal, Timer, Learn)  │
│  profile.html    — Профайл засах, зураг оруулах                              │
│  brew-timer.html — Дарлалтын таймер, харьцааны тооцоолуур                    │
│  admin.html      — Тайлан самбар (id=1 хэрэглэгчид)                         │
│  login.html      — Нэвтрэх хуудас                                            │
│  register.html   — Бүртгэл хуудас                                            │
│                                                                              │
│  Технологи: Vanilla HTML / CSS custom properties / ES2022 JS                 │
│  Хүргэлт: Javalin static file serving (port 7000)                            │
└────────────────────────┬─────────────────────────────────────────────────────┘
                         │  HTTP REST (JSON) + Server-Sent Events (SSE)
                         ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│               SERVER 1 — Javalin REST API  (port 7000)                       │
│               JVM процесс 1  |  Main.java → Composition Root                 │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐     │
│  │  GATEWAY LAYER  —  ApiGateway.java                                  │     │
│  │                                                                     │     │
│  │  • CORS headers (app.before)                                        │     │
│  │  • Rate limiting: Redis INCR+EXPIRE → 5 req / 60s per IP           │     │
│  │  • Request logging (audit trail)                                    │     │
│  │  • Auth resolution: JWT → SOAP ValidateToken fallback               │     │
│  │    (BorgolApiServer calls gateway.authenticate() — never JwtUtil    │     │
│  │     directly — architectural boundary / private subnet pattern)     │     │
│  └────────────────────────────┬────────────────────────────────────────┘     │
│                               │                                              │
│  ┌────────────────────────────▼────────────────────────────────────────┐     │
│  │  HTTP ADAPTER  —  BorgolApiServer.java  (Front Controller)          │     │
│  │                                                                     │     │
│  │  30+ REST endpoints:                                                │     │
│  │  /api/auth/*          /api/users/*        /api/recipes/*            │     │
│  │  /api/cafes/*         /api/feed           /api/journal/*            │     │
│  │  /api/hashtags/*      /api/notifications/stream  (SSE)              │     │
│  │  /api/bean/chat       /api/report         /api/admin/*              │     │
│  │  /api/soap/register   /api/soap/login     /api/menu/*               │     │
│  └────────────────────────────┬────────────────────────────────────────┘     │
│                               │                                              │
│  ┌────────────────────────────▼────────────────────────────────────────┐     │
│  │  APPLICATION SERVICE  —  BorgolService.java  (Business Logic)       │     │
│  │                                                                     │     │
│  │  • User CRUD, follow/unfollow, block                                │     │
│  │  • Recipe CRUD, like, comment, save                                 │     │
│  │  • Café listing, rating, nearby search                              │     │
│  │  • Feed generation (followed users + trending fallback)             │     │
│  │  • Brew journal + guide management                                  │     │
│  │  • Notification creation → eventBus.publish()                      │     │
│  │  • Trending hashtags via Redis Sorted Set                          │     │
│  │  • User profile caching via Redis Hash                             │     │
│  │  • Bean AI chat (Google Gemini 1.5 Flash via Anthropic SDK)        │     │
│  │  • Admin: report resolution, stats                                  │     │
│  └────────────────────────────┬────────────────────────────────────────┘     │
│                               │                                              │
│  ┌────────────────────────────▼────────────────────────────────────────┐     │
│  │  DATA ACCESS  —  BorgolRepository.java  (Repository/DAO)            │     │
│  │                                                                     │     │
│  │  All SQL: PreparedStatement, idempotent schema migration on startup │     │
│  │  H2 (local ./data/cafe_db.mv.db) or PostgreSQL (DATABASE_URL env)  │     │
│  └────────────────────────────┬────────────────────────────────────────┘     │
│                               │                                              │
│  ┌────────────────────────────▼────────────────────────────────────────┐     │
│  │  DATABASE  —  H2 (local) / PostgreSQL Railway (production)          │     │
│  └─────────────────────────────────────────────────────────────────────┘     │
│                                                                              │
│  CROSS-CUTTING INFRASTRUCTURE (wired in Main.java):                          │
│  • DatabaseConnection.java   — DCL Singleton, JdbcConnection                │
│  • RedisClient.java          — DCL Singleton, JedisPool                     │
│  • RedisEventBus.java        — Pub/Sub virtual thread fan-out               │
│  • EmailService.java         — Jakarta Mail SMTP (registration/reset)       │
│  • JwtUtil.java              — HMAC-SHA256 JWT (no external lib)            │
│  • PasswordUtil.java         — SHA-256 + SecureRandom salt                  │
│  • SoapAuthClient.java       — HTTP Proxy → Server 2                        │
└──────────────────────────────────────────────────────────────────────────────┘
         │ Redis Pub/Sub (borgol:notify:{userId})          │ SMTP
         ▼                                                  ▼
┌─────────────────────┐                          ┌──────────────────────┐
│  Redis (Railway)    │                          │  Email (SMTP relay)  │
│  redis-cache-a      │                          │  Jakarta Mail 2.0    │
│  private subnet     │                          └──────────────────────┘
│                     │
│  String: recipes,   │
│    feed, cafes      │
│  Hash: users        │
│  Sorted Set:        │
│    trending tags    │
│  INCR: rate limit   │
│  Pub/Sub: notifs    │
└─────────────────────┘

         │ SOAP/XML (RegisterUser, LoginUser, ValidateToken)
         ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│               SERVER 2 — SOAP Auth Service  (port 8081)                      │
│               JVM процесс 2  |  Spring Boot 3.2.5 + Spring-WS               │
│                                                                              │
│  AuthEndpoint.java    — SOAP dispatcher (routes XML to service methods)      │
│  AuthService.java     — JWT generation (HMAC-SHA256), password hashing       │
│  WebServiceConfig.java — Spring-WS config, WSDL exposure                    │
│  CorsFilter.java      — CORS for SOAP service                                │
│                                                                              │
│  Storage: ConcurrentHashMap (in-memory — stateless, no DB)                   │
│  WSDL:    http://localhost:8081/ws/authService.wsdl                          │
│  Schema:  xsd/auth-service.xsd  (JAXB namespace: http://num.edu.mn/soapauth) │
│                                                                              │
│  DTO classes (JAXB):                                                         │
│  LoginUserRequest/Response | RegisterUserRequest/Response                    │
│  ValidateTokenRequest/Response                                               │
└──────────────────────────────────────────────────────────────────────────────┘

Fallback chain (Server 1): if SOAP service is unreachable →
  SoapAuthClient catches exception → falls back to local JwtUtil
```

---

### Файлын бүтэц (бүрэн, архитектурын үүргээр)

```
borgol-coffee-platform/
│
│  ── DEPLOYMENT & BUILD ─────────────────────────────────────────────────
├── pom.xml                     Maven build (Java 21, Javalin 6.3, JavaFX 21,
│                               Jedis 5.1, Jakarta Mail, Anthropic SDK 2.16.1,
│                               H2, PostgreSQL, Gson, Jackson, JUnit 5)
├── mvnw / mvnw.cmd             Maven wrapper (no local Maven install needed)
├── Dockerfile                  Container image — SERVER 1 (Javalin REST API)
├── railway.toml                Railway deploy config — SERVER 1
├── deploy.sh                   Deployment helper script
├── .github/workflows/
│   └── deploy.yml              CI/CD pipeline (GitHub Actions)
│
│  ── FRONTEND ───────────────────────────────────────────────────────────
│  Served statically by Javalin at port 7000 from target/classes/public/
│
├── src/main/resources/public/
│   ├── index.html              ★ MAIN APP — 6-tab SPA
│   │                           Tabs: Feed | Recipes | Cafes | Journal | Learn | Timer
│   │                           Features: dark mode toggle, Bean AI chat (FAB),
│   │                             SSE notification bell, GPS map, radar chart,
│   │                             PDF/CSV export, QuickBrew overlay
│   ├── profile.html            User profile — bio edit, avatar upload (canvas
│   │                             compress → 240px JPEG → base64), equipment,
│   │                             flavor prefs, saved recipes
│   ├── brew-timer.html         Standalone brew timer — 6 methods, SVG ring
│   │                             timer, ratio calculator, step guide
│   ├── admin.html              Admin dashboard (id=1 only) — report queue,
│   │                             resolve/reject, stats
│   ├── login.html              Login form → POST /api/soap/login
│   └── register.html           Register form → POST /api/soap/register
│
├── src/main/resources/
│   ├── database.properties     Persistence config (mode=DB, H2 JDBC URL, sa)
│   ├── style.css               JavaFX desktop UI stylesheet (warm espresso theme)
│   └── style-dark.css          JavaFX desktop dark mode variant
│
│  ── SERVER 1: JAVALIN REST API (port 7000) ─────────────────────────────
│
├── src/main/java/borgol/
│   │
│   ├── app/
│   │   ├── Main.java           ★ COMPOSITION ROOT
│   │   │                       Reads MODE env var (web | desktop | javafx)
│   │   │                       Wires: DB → Repo → Service → Gateway → Server
│   │   │                       Starts: RedisEventBus Pub/Sub virtual thread
│   │   │                       Registers: ConsoleMenuObserver
│   │   └── ConsoleMenuObserver.java
│   │                           Observer concrete impl — prints menu changes
│   │
│   ├── core/                   ── DOMAIN (no framework dependencies) ──────
│   │   ├── domain/
│   │   │   ├── User.java       Entity: id, username, email, passwordHash,
│   │   │   │                     bio, avatarUrl, expertiseLevel, flavorPrefs
│   │   │   ├── Recipe.java     Entity: authorId, title, drinkType, ingredients,
│   │   │   │                     instructions, brewTime, difficulty, imageUrl,
│   │   │   │                     flavorTags, likeCount
│   │   │   ├── RecipeComment.java  Entity: recipeId, authorId, content
│   │   │   ├── CafeListing.java    Entity: name, address, city, district,
│   │   │   │                         phone, hours, avgRating
│   │   │   ├── BrewJournalEntry.java  Entity: userId, coffeeBean, origin,
│   │   │   │                           roastLevel, brewMethod, waterTempC,
│   │   │   │                           doseG, brewTimeSec, aroma/flavor/
│   │   │   │                           acidity/body/sweetness/finish (1-5)
│   │   │   ├── BrewGuide.java  Entity: methodName, brewTimeMin, steps[], params
│   │   │   ├── LearnArticle.java  Entity: title, category, content, readTimeMin
│   │   │   ├── Equipment.java  Entity: userId, category (GRINDER|BREWER|
│   │   │   │                     KETTLE|SCALE|OTHER), brand, notes
│   │   │   ├── MenuItem.java   Entity: id, name, MenuCategory, price, available
│   │   │   └── MenuCategory.java  Enum: COFFEE|TEA|SMOOTHIE|FOOD|DESSERT
│   │   │
│   │   ├── ports/              ── HEXAGONAL OUTBOUND PORTS (interfaces) ──
│   │   │   ├── IMenuRepository.java    Port: save/findById/getAll/delete
│   │   │   └── MenuChangeObserver.java Port: onItemAdded/Removed/Updated
│   │   │
│   │   └── application/        ── APPLICATION SERVICES (business logic) ──
│   │       ├── BorgolService.java  ★ CORE SERVICE
│   │       │                       User/Recipe/Café/Journal/Feed/Notif/AI
│   │       │                       Redis: ZINCRBY (trending), HSET (user hash)
│   │       │                       Publishes to RedisEventBus after notif create
│   │       ├── MenuService.java    Menu CRUD + Observer notification dispatch
│   │       └── MenuDto.java        Java Record DTO: MenuItem projection
│   │
│   ├── infrastructure/         ── INFRASTRUCTURE (framework adapters) ─────
│   │   │
│   │   ├── config/
│   │   │   └── DatabaseConnection.java  Singleton (DCL + volatile)
│   │   │                                H2 if no DATABASE_URL, else PostgreSQL
│   │   │
│   │   ├── persistence/        ── DATA ACCESS LAYER ──────────────────────
│   │   │   ├── BorgolRepository.java    ★ MASTER DAO
│   │   │   │                            All SQL for 18+ tables, PreparedStatement,
│   │   │   │                            MERGE INTO (upserts), idempotent schema
│   │   │   ├── JdbcMenuRepository.java  IMenuRepository impl (JDBC)
│   │   │   ├── InMemoryMenuRepository.java  IMenuRepository impl (HashMap, tests)
│   │   │   └── RepositoryFactory.java   Creates correct impl from database.properties
│   │   │
│   │   ├── security/           ── AUTH & SECURITY ─────────────────────────
│   │   │   ├── JwtUtil.java     Custom HMAC-SHA256 JWT (javax.crypto.Mac)
│   │   │   │                    No external JWT lib — 7-day expiry, constant-time compare
│   │   │   ├── PasswordUtil.java  SHA-256 + SecureRandom salt → "saltHex:hashHex"
│   │   │   └── SoapAuthClient.java  HTTP Proxy → SERVER 2
│   │   │                            Calls RegisterUser / LoginUser / ValidateToken
│   │   │                            Graceful degradation: falls back to JwtUtil on failure
│   │   │
│   │   ├── cache/              ── REDIS LAYER ─────────────────────────────
│   │   │   ├── RedisClient.java     Singleton (DCL + volatile): JedisPool
│   │   │   │                        REDIS_HOST/PORT/PASSWORD from env
│   │   │   │                        pool() exposes JedisPool for Pub/Sub ops
│   │   │   └── CacheKeyBuilder.java Utility: builds Redis keys
│   │   │                            borgol:recipe:{id} | borgol:user:{id}
│   │   │                            borgol:feed:userId:{id} | borgol:trending
│   │   │                            borgol:cafes:nearby:{lat}:{lng}
│   │   │                            borgol:ratelimit:{ip} | borgol:notify:{userId}
│   │   │
│   │   ├── messaging/          ── EVENT BUS ───────────────────────────────
│   │   │   └── RedisEventBus.java  Redis Pub/Sub wrapper
│   │   │                           One virtual thread: PSUBSCRIBE borgol:notify:*
│   │   │                           Fan-out: notif event → Consumer<String> SSE handlers
│   │   │                           subscribe(userId, handler) / unsubscribe(userId, handler)
│   │   │
│   │   └── email/              ── EMAIL ────────────────────────────────────
│   │       └── EmailService.java   Jakarta Mail 2.0 SMTP
│   │                               Sends: registration confirmation, password reset
│   │                               Config: SMTP_HOST/PORT/USER/PASSWORD/EMAIL_FROM env vars
│   │
│   └── ui/                     ── ADAPTERS (UI layer) ─────────────────────
│       │
│       ├── web/                ── HTTP ADAPTERS (production) ───────────────
│       │   ├── ApiGateway.java     ★ GATEWAY
│       │   │                       registerFilters(app): CORS, rate-limit before-filters
│       │   │                       authenticate(ctx, required): SOAP→JWT fallback
│       │   │                       rateLimitAuth(ctx): Redis INCR+EXPIRE, fail-open
│       │   ├── BorgolApiServer.java  ★ FRONT CONTROLLER (30+ routes)
│       │   │                         Delegates auth to ApiGateway (never calls JwtUtil)
│       │   │                         SSE: /api/notifications/stream — heartbeat loop
│       │   │                         SSE: /api/bean/chat — Gemini token streaming
│       │   └── CafeApiServer.java    Legacy REST adapter (menu endpoints)
│       │
│       └── desktop/            ── JAVAFX UI ADAPTERS (local MODE=desktop) ──
│           ├── BorgolApp.java       JavaFX Application entry, window setup
│           ├── MainWindow.java      BorderPane: navbar + swappable center pane
│           ├── AppSession.java      Static: current userId, username
│           ├── FeedPane.java        Social feed (3-column)
│           ├── RecipesPane.java     Recipe browser, create/edit/like/comment
│           ├── CafesPane.java       Café list, rate, nearby
│           ├── JournalPane.java     Brew log + radar chart SVG + CSV export
│           ├── BrewTimerPane.java   Timer (V60/Espresso/FP/AeroPress/Moka/CB)
│           ├── LearnPane.java       Master-detail: guides + articles
│           ├── ProfilePane.java     Bio, avatar upload (canvas), equipment
│           ├── AdminPane.java       Report queue TableView
│           ├── PeoplePane.java      User discovery cards
│           ├── MapPane.java         Leaflet.js WebView (café map)
│           ├── QuickBrewOverlay.java  Modal brew method step guide
│           └── UiUtils.java         Shared styling helpers
│
│  ── TEST ────────────────────────────────────────────────────────────────
└── src/test/java/borgol/
    └── MenuServiceTest.java    JUnit 5: CRUD + Observer with InMemoryMenuRepository
│
│  ── SERVER 2: SOAP AUTH SERVICE (port 8081) ────────────────────────────
│  Separate JVM process, separate Docker container, separate Railway service
│
└── soap-auth-service/
    ├── pom.xml                 Spring Boot 3.2.5, Spring-WS, JAXB, Wsdl4j
    ├── Dockerfile              Container image — SERVER 2
    ├── railway.toml            Railway deploy config — SERVER 2
    │
    └── src/main/java/borgol/auth/
        ├── SoapAuthApplication.java  Spring Boot entry (port 8081)
        │
        ├── config/
        │   ├── WebServiceConfig.java  Spring-WS WSDL exposure config
        │   └── CorsFilter.java        CORS for SOAP requests
        │
        ├── endpoint/
        │   └── AuthEndpoint.java     ★ SOAP DISPATCHER
        │                             @PayloadRoot routes XML payload:
        │                             RegisterUserRequest → register()
        │                             LoginUserRequest    → login()
        │                             ValidateTokenRequest → validateToken()
        │
        ├── service/
        │   └── AuthService.java      JWT generation (HMAC-SHA256)
        │                             Password hashing (SHA-256 + salt)
        │                             In-memory user store (ConcurrentHashMap)
        │
        ├── model/
        │   └── AuthUser.java         username, passwordHash (in-memory entity)
        │
        ├── dto/                      JAXB-annotated request/response classes
        │   ├── LoginUserRequest/Response.java
        │   ├── RegisterUserRequest/Response.java
        │   └── ValidateTokenRequest/Response.java
        │
        └── src/main/resources/
            ├── application.properties  port=8081, JWT secret, log levels
            └── xsd/auth-service.xsd    WSDL schema (namespace: num.edu.mn/soapauth)
```

---

## Өгөгдлийн сан

PostgreSQL, Railway дээр deploy хийгдсэн. Schema нь startup бүрт автоматаар үүсч, migrate хийгдэнэ.

| Хүснэгт | Зориулалт |
|---|---|
| borgol_users | Бүртгэл — нэр, и-мэйл, биографи, зураг (TEXT), мэргэшил |
| user_flavor_prefs | Хэрэглэгч бүрийн амтын сонголт |
| user_follows | Дагах граф |
| recipes | Кофе жор (image_url: TEXT) |
| recipe_flavor_tags | Жорын шошго |
| recipe_likes | Like бүртгэл |
| recipe_comments | Сэтгэгдэл |
| saved_recipes | Хадгалсан жор |
| cafes | Кафены жагсаалт |
| cafe_ratings | Үнэлгээ ба сэтгэгдэл |
| brew_guides | Дарлалтын арга зааварчилгаа |
| brew_journal | Хувийн дарлалтын тэмдэглэл |
| user_equipment | Хэрэглэгчийн тоног төхөөрөмж |
| learn_articles | Боловсролын нийтлэл |
| user_hashtags | Дагасан хэштэг |
| reports | Гомдол (хүлээгдэж буй / шийдвэрлэсэн / татгалзсан) |
| notifications | Мэдэгдэл |
| blocked_users | Хаасан хэрэглэгч |

---

## API

| Бүлэг | Endpoint |
|---|---|
| Auth (SOAP) | POST /api/soap/register, POST /api/soap/login |
| Auth (дотоод) | POST /api/auth/register, POST /api/auth/login, GET /api/auth/me |
| Хэрэглэгч | GET /api/users/{id}, PUT /api/users/me, DELETE /api/users/{id} |
| Нийгмийн | POST/DELETE /api/users/{id}/follow, POST/DELETE /api/users/{id}/block |
| Хэштэг | POST/DELETE /api/hashtags/{tag}/follow, GET /api/hashtags/trending |
| Жор | GET/POST /api/recipes, GET/PUT/DELETE /api/recipes/{id} |
| Жорын үйлдэл | POST /api/recipes/{id}/like, GET/POST /api/recipes/{id}/comments, POST /api/recipes/{id}/save |
| Feed | GET /api/feed |
| Кафе | GET/POST /api/cafes, POST /api/cafes/{id}/rate, GET /api/cafes/nearby |
| Тэмдэглэл | GET/POST /api/journal, PUT/DELETE /api/journal/{id} |
| Зааварчилгаа | GET /api/brew-guides, GET /api/learn |
| Мэдэгдэл | GET /api/notifications, POST /api/notifications/read, GET /api/notifications/stream (SSE) |
| Гомдол | POST /api/report |
| Админ | GET /api/admin/reports, POST /api/admin/reports/{id}/resolve, GET /api/admin/stats |
| Bean AI | POST /api/bean/chat — SSE дамжуулалт (Google Gemini 1.5 Flash) |

---

## Local-д ажиллуулах

### 1. SOAP Auth Service (эхлэж эхлүүлнэ)

```bash
cd auth
mvn spring-boot:run
```

WSDL: http://localhost:8081/ws/authService.wsdl

### 2. JSON API ба интерфэйс

```bash
cd cafe-project
mvnw.cmd javafx:run
```

Апп: http://localhost:7000

### 3. Bean AI-д зориулсан environment variable

```
GEMINI_API_KEY=google-ai-studio-таас-авсан-түлхүүр
```

---

## Технологийн стек

| Давхарга | Технологи |
|---|---|
| REST API | Javalin 6.3 |
| SOAP Auth | Spring Boot 3.2.5 + Spring-WS |
| AI чат бот | Google Gemini 1.5 Flash |
| Интерфэйс | HTML5 / CSS3 / Vanilla JS |
| Өгөгдлийн сан | PostgreSQL (Railway) |
| Desktop | JavaFX 21 (legacy) |
| JWT | HMAC-SHA256 |
| Нууц үг | SHA-256 + санамсаргүй давс |
| Газрын зураг | Leaflet.js + OpenStreetMap |
| Фонт | Playfair Display + DM Sans |
| Build tool | Maven |
| Програмчлалын хэл | Java 21 |
| Deployment | Railway |
| Кэш | Redis (Jedis 5.1 — railway.internal) |
| Имэйл | Jakarta Mail 2.0 (SMTP) |

---

## Кэш давхарга (Redis)

**String cache (JSON):**

| Нөөц | Кэш түлхүүр | TTL |
|---|---|---|
| Жор | `borgol:recipe:{id}` | 300с |
| Feed | `borgol:feed:userId:{id}` | 60с |
| Ойролцоо кафе | `borgol:cafes:nearby:{lat}:{lng}` | 120с |

**Өгөгдлийн бүтцийн Redis операцууд:**

| Бүтэц | Түлхүүр | Зориулалт |
|---|---|---|
| Hash | `borgol:user:{id}` | Хэрэглэгчийн профайлын 8 талбарыг тус тусад шинэчлэх — 600с TTL |
| Sorted Set | `borgol:trending` | Хэштэгийн оноо — жор үүсгэх/устгахад `ZINCRBY`, дээд 20-ийг `ZREVRANGE` |
| INCR+EXPIRE | `borgol:ratelimit:{ip}` | Rate limit — 5 оролдлого / 60с тутамд IP тус бүрт |
| Pub/Sub | `borgol:notify:{userId}` | Бодит цагийн мэдэгдэл — `PUBLISH` → SSE fan-out → browser |

Redis `redis-cache-a` нь Railway-н хувийн дотоод сүлжээнд ажиллана — гадна сүлжээнд нээлттэй биш.

## Имэйл үйлчилгээ

Бүртгэл болон нууц үг сэргээх явцад имэйл автоматаар илгээгдэнэ.
SMTP тохиргоо: `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `EMAIL_FROM` орчны хувьсагчаар тохируулна.

---

## Загвар хэв маягууд

| Загвар | Байршил | Тайлбар |
|---|---|---|
| Singleton | DatabaseConnection (DCL + volatile) | Програмын туршид нэг DB холболтын объект |
| Repository / DAO | BorgolRepository | SQL-г сервисээс тусгаарлана |
| Service Layer | BorgolService | Бизнесийн дүрэм нэг газарт |
| Front Controller | BorgolApiServer | Бүх HTTP хүсэлтийг нэг цэгт хүлээнэ |
| API Gateway | ApiGateway | CORS, rate limiting, auth шийдвэрлэлт нэг давхаргад төвлөрнэ |
| Publisher–Subscriber | RedisEventBus | Redis Pub/Sub-ээр мэдэгдэл нийтлэн, SSE-ээр хэрэглэгч тус бүрт хүргэнэ |
| Proxy / Adapter | SoapAuthClient | JSON сервис SOAP-г шууд мэдэхгүйгээр ашиглана |
| Observer | MenuChangeObserver | Цэсийн өөрчлөлтийг сонсогч |
| Strategy | Main.java (MODE=web/desktop) | Нэг codebase, хоёр горимд ажиллана |
| Hexagonal | Core domain | UI болон DB-ийн технологиос тусгаарлагдана |
| Composition Root | Main.java | Бүх dependency нэг газарт угсарна |
| Fallback Chain | soapLogin / soapRegister | SOAP унасан үед local JWT-р fallback хийнэ |

---

## Код дахь тайлбар

Бүх Java эх кодонд монгол хэл дээрх дэлгэрэнгүй тайлбар нэмэгдсэн:

| Файл | Тайлбарласан зүйлс |
|---|---|
| Main.java | Composition Root загвар, Strategy горим, Observer угсралт |
| DatabaseConnection.java | DCL Singleton, volatile, connection fallback дараалал |
| BorgolRepository.java | Repository/DAO загвар, PreparedStatement, идемпотент миграц, CASCADE |
| BorgolService.java | Service Layer, Fail Fast баталгаажуулалт, Toggle логик, Hexagonal архитектур |
| BorgolApiServer.java | Front Controller, CORS, SOA урсгал, Proxy/Delegate, Least Privilege |
| JwtUtil.java | JWT RFC 7519 бүтэц, HMAC-SHA256, constant-time comparison |
| PasswordUtil.java | Salted Hash загвар, SecureRandom, SHA-256, Defense in Depth |
| SoapAuthClient.java | Proxy/Adapter загвар, Graceful Degradation, SOAP envelope бүтэц |

---

## Админ систем

Анх бүртгүүлсэн хэрэглэгч (id=1) автоматаар админ болно.

- Админ зориулалтын endpoint-уудад бусад хэрэглэгч хандахад 403 Forbidden буцаана
- Гомдлын амьдралын мөчлөг: хүлээгдэж буй → шийдвэрлэсэн / татгалзсан
- resolved_by, resolved_at баганаар хэн, хэзээ шийдвэрлэсэн бүртгэгдэнэ

---

`feat/redis-cache-email-service` — [github.com/Apoca72/borgol-coffee-platform](https://github.com/Apoca72/borgol-coffee-platform)

GitLab-д академик илгээлтийн зорилгоор тусдаа хуулбарлагдана.
