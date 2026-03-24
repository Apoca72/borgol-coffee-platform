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

### Мэдэгдэл

- Навигацийн хонхны дүрс, уншаагүй тоолуур
- Like, сэтгэгдэл, дагасны мэдэгдэл

---

## Архитектур

```
Веб интерфэйс (HTML / CSS / Vanilla JS)
  index.html | profile.html | brew-timer.html | admin.html

         HTTP REST (JSON) + SSE
                  |
         JSON REST API (Javalin)
         BorgolApiServer

  POST /api/bean/chat ─────► Google Gemini 1.5 Flash

  POST /api/soap/register ──────────┐
  POST /api/soap/login ─────────────┤  SOAP XML
                                    ▼
                     SOAP Auth Service (Spring-WS)
                       ├── RegisterUser
                       ├── LoginUser → JWT
                       └── ValidateToken

  BorgolRepository ──────────────────► PostgreSQL (Railway)
```

### Кодын бүтэц

```
cafe-project/
├── soap-auth-service/              SOAP auth микросервис (Spring Boot)
│   └── src/main/java/com/example/soapauth/
│       ├── endpoint/AuthEndpoint   SOAP dispatcher
│       ├── service/AuthService     JWT + нууц үг
│       ├── dto/                    JAXB DTO
│       └── config/                 Spring-WS тохиргоо
│
└── src/main/java/mn/edu/num/cafe/
    ├── app/Main.java               Composition Root
    ├── core/
    │   ├── domain/                 Entity классууд
    │   └── application/BorgolService   Бизнесийн логик
    ├── infrastructure/
    │   ├── persistence/BorgolRepository  SQL, схемийн миграц
    │   └── security/
    │       ├── JwtUtil             Дотоод JWT
    │       ├── PasswordUtil        SHA-256 + давс
    │       └── SoapAuthClient      SOAP HTTP клиент
    └── ui/
        ├── web/BorgolApiServer     REST endpoint, Bean AI, админ
        └── desktop/                JavaFX (legacy)
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
| Мэдэгдэл | GET /api/notifications, POST /api/notifications/read |
| Гомдол | POST /api/report |
| Админ | GET /api/admin/reports, POST /api/admin/reports/{id}/resolve, GET /api/admin/stats |
| Bean AI | POST /api/bean/chat — SSE дамжуулалт (Google Gemini 1.5 Flash) |

---

## Local-д ажиллуулах

### 1. SOAP Auth Service (эхлэж эхлүүлнэ)

```bash
cd soap-auth-service
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

---

## Загвар хэв маягууд

| Загвар | Байршил | Тайлбар |
|---|---|---|
| Singleton | DatabaseConnection (DCL + volatile) | Програмын туршид нэг DB холболтын объект |
| Repository / DAO | BorgolRepository | SQL-г сервисээс тусгаарлана |
| Service Layer | BorgolService | Бизнесийн дүрэм нэг газарт |
| Front Controller | BorgolApiServer | Бүх HTTP хүсэлтийг нэг цэгт хүлээнэ |
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

`feature/borgol-platform` — [github.com/Apoca72/borgol-coffee-platform](https://github.com/Apoca72/borgol-coffee-platform)

GitLab-д академик илгээлтийн зорилгоор тусдаа хуулбарлагдана.
