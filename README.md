# ☕ Borgol — Кофе Сонирхогчдын Платформ

**Хичээл:** ICSI486 Программ хангамжийн бүтээлт
**Оюутан:** С.Тэмүүлэн — 22B1NUM6637
**Бие даасан төсөл:** JavaFX Desktop App + SOA Integration — Borgol Coffee Enthusiast Platform

---

## Тойм

Borgol бол кофе сонирхогчдод зориулсан **JavaFX desktop нийгмийн платформ** юм. Хэрэглэгчид жор хуваалцах, кафе нээн судлах, хувийн дэвтэрт буцалтын тэмдэглэл хөтлөх, бусад кофе сонирхогчдыг дагах, гарын авлага болон нийтлэлээр суралцах боломжтой.

**Lab 06-аас эхлэн** системд **Service-Oriented Architecture (SOA)** нэвтрүүлсэн:
- **User JSON Service** — REST API профайл удирдлага (Javalin, порт 7000)
- **User SOAP Service** — SOAP нэвтрэлтийн сервис (Spring-WS, порт 8081)
- **Frontend Application** — Нэвтрэх, бүртгэх, профайл хуудсууд

---

## 🆕 Lab 06 — JSON & SOAP Интеграц

### Системийн архитектур

```
┌─────────────────────────────────────────────────────────┐
│                    Frontend (HTML/CSS/JS)                │
│         login.html │ register.html │ profile.html        │
└───────────────┬─────────────────────────────────────────┘
                │ HTTP REST (JSON)
                ▼
┌───────────────────────────────────┐
│   User JSON Service  :7000        │
│   BorgolApiServer (Javalin)       │
│                                   │
│  POST /api/soap/register  ──────┐ │
│  POST /api/soap/login     ──────┤ │
│  authRequired() middleware ─────┤ │
└───────────────────────────────┬─┘ │
                SOAP XML over HTTP  │
                ▼                   │
┌───────────────────────────────┐   │
│   User SOAP Service  :8081    │   │
│   Spring-WS (AuthEndpoint)    │   │
│                               │   │
│  ▸ RegisterUser               │   │
│  ▸ LoginUser → JWT token      │   │
│  ▸ ValidateToken → true/false │   │
└───────────────────────────────┘
```

### Нэвтрэлтийн урсгал (Authentication Flow)

```
1. Бүртгэл:
   Frontend → POST /api/soap/register
           → JSON Service → SOAP RegisterUser
           → SOAP дансны мэдээлэл хадгалж, userId буцаана
           → JSON Service профайл үүсгэнэ
           → JWT token буцаана

2. Нэвтрэлт:
   Frontend → POST /api/soap/login
           → JSON Service → SOAP LoginUser
           → SOAP нууц үг шалгаж, JWT token үүсгэнэ
           → Token frontend-д буцаж, localStorage-д хадгалагдана

3. Хамгаалалттай хүсэлт:
   Frontend → GET /api/users/:id (Bearer token)
           → JSON Service middleware → SOAP ValidateToken
           → SOAP: valid=true + userId
           → JSON Service профайл буцаана
           (SOAP унтарсан тохиолдолд локал JWT руу буцна)
```

### Өгөгдлийн сангийн шийдвэр

**Option 2 — Тусдаа өгөгдлийн сан** сонгосон:

| Сервис | Өгөгдлийн сан | Хадгалдаг зүйл |
|---|---|---|
| SOAP Auth Service | In-memory (ConcurrentHashMap) | Нэвтрэлтийн данс, нууц үгийн хэш, JWT |
| JSON Profile Service | H2 (файл: `data/cafe_db`) | Профайл, жор, кафе, дэвтэр |

Давуу тал: Сервис бүр бие даасан, нэг нь унтарсан ч нөгөө нь ажилласаар байна.

---

### 🧼 SOAP Сервис (`soap-auth-service/`)

**Технологи:** Spring Boot 3.2.5, Spring-WS, JAXB, Jakarta EE
**Порт:** 8081
**WSDL:** `http://localhost:8081/ws/authService.wsdl`
**Endpoint:** `POST http://localhost:8081/ws`

#### SOAP операциуд

```xml
<!-- 1. RegisterUser — шинэ хэрэглэгч бүртгэх -->
<RegisterUserRequest>
  <username>coffee_master</username>
  <email>coffee@borgol.mn</email>
  <password>secret123</password>
</RegisterUserRequest>
→ <RegisterUserResponse>
    <success>true</success>
    <message>Registration successful</message>
    <userId>1</userId>
  </RegisterUserResponse>

<!-- 2. LoginUser — нэвтрэх, JWT token авах -->
<LoginUserRequest>
  <email>coffee@borgol.mn</email>
  <password>secret123</password>
</LoginUserRequest>
→ <LoginUserResponse>
    <success>true</success>
    <token>eyJhbGc...</token>
    <userId>1</userId>
    <username>coffee_master</username>
    <message>Login successful</message>
  </LoginUserResponse>

<!-- 3. ValidateToken — JSON сервисийн middleware дуудна -->
<ValidateTokenRequest>
  <token>eyJhbGc...</token>
</ValidateTokenRequest>
→ <ValidateTokenResponse>
    <valid>true</valid>
    <userId>1</userId>
    <username>coffee_master</username>
  </ValidateTokenResponse>
```

#### Файлын бүтэц

```
soap-auth-service/
├── pom.xml                                    Spring Boot 3.2.5 + Spring-WS
└── src/main/
    ├── java/com/example/soapauth/
    │   ├── SoapAuthApplication.java           Программ эхлүүлэх цэг
    │   ├── config/
    │   │   ├── WebServiceConfig.java          WSDL тохиргоо, servlet бүртгэл
    │   │   └── CorsFilter.java                CORS — frontend-аас дуудахыг зөвшөөрнө
    │   ├── dto/
    │   │   ├── package-info.java              JAXB namespace тодорхойлолт
    │   │   ├── RegisterUserRequest/Response   Бүртгэлийн DTO
    │   │   ├── LoginUserRequest/Response      Нэвтрэлтийн DTO
    │   │   └── ValidateTokenRequest/Response  Token шалгалтын DTO
    │   ├── model/
    │   │   └── AuthUser.java                  Нэвтрэлтийн хэрэглэгч (in-memory)
    │   ├── service/
    │   │   └── AuthService.java               HMAC-SHA256 JWT + SHA-256 нууц үг
    │   └── endpoint/
    │       └── AuthEndpoint.java              @PayloadRoot SOAP dispatcher
    └── resources/
        ├── application.properties             Порт: 8081
        └── xsd/auth-service.xsd               Contract-first WSDL схем
```

---

### 🔗 JSON Сервисийн өөрчлөлтүүд (`cafe-project/`)

#### Шинэ: `SoapAuthClient.java`

JSON сервисийг SOAP сервистэй холбох HTTP клиент. Гадаад сан ашиглалгүй Java-ийн стандарт `HttpClient`-ийг ашиглан SOAP XML илгээж, хариу задлана.

```
mn.edu.num.cafe.infrastructure.security.SoapAuthClient
  ├── register(username, email, password) → AuthResult
  ├── login(email, password)              → AuthResult
  └── validateToken(token)               → ValidationResult
```

SOAP сервис унтарсан тохиолдолд автоматаар локал JWT руу буцна — тасралтгүй ажиллагаа хангагдана.

#### Шинэ endpoint-үүд

| Endpoint | Тайлбар |
|---|---|
| `POST /api/soap/register` | SOAP RegisterUser руу дамжуулна → профайл үүсгэнэ |
| `POST /api/soap/login` | SOAP LoginUser руу дамжуулна → SOAP JWT буцаана |
| `DELETE /api/users/:id` | [auth] Профайл устгах (Lab 06 шаардлага) |

#### Шинэчлэгдсэн: `authRequired()` middleware

```
Өмнө:  Bearer token → локал JWT шалгалт → userId
Одоо:  Bearer token → SOAP ValidateToken → userId
                           ↓ SOAP унтарсан бол
                       локал JWT шалгалт → userId
```

#### CORS тохиргоо нэмэгдсэн

Frontend өөр порттоос (5500, 3000 г.м) дуудахад алдаа гарахгүй болно.

---

### 🎨 Frontend Хуудсууд

**Загвар:** Dark Espresso Luxury — харанхуй кофений өнгөний палитр
**Фонт:** Cormorant Garamond (гарчиг) + DM Sans (биеийн текст)
**Байршил:** `src/main/resources/public/`

| Хуудас | URL | Үүрэг |
|---|---|---|
| `register.html` | `/register.html` | Бүртгэл → SOAP RegisterUser |
| `login.html` | `/login.html` | Нэвтрэлт → SOAP LoginUser → JWT хадгалах |
| `profile.html` | `/profile.html` | Профайл унших/засах → SOAP ValidateToken |

Хуудас бүрд **SOA архитектурын баннер** харагдана — `Frontend → JSON Service → SOAP Service` урсгалыг бодит цагт харуулна.

---

## Програмыг ажиллуулах

### 1. SOAP Auth Сервис (эхлээд ажиллуулна)

```cmd
cd "C:\Users\thatu\OneDrive\Desktop\cafe-project\soap-auth-service"
mvn spring-boot:run
```

> ✅ `http://localhost:8081/ws/authService.wsdl` — WSDL нээгдсэн

### 2. JSON Сервис + Frontend

```cmd
cd "C:\Users\thatu\OneDrive\Desktop\cafe-project"
mvnw.cmd javafx:run
```

> ✅ `http://localhost:7000/register.html` — Frontend нээгдсэн

### 3. Frontend хаяг

```
http://localhost:7000/register.html   ← Бүртгэл
http://localhost:7000/login.html      ← Нэвтрэлт
http://localhost:7000/profile.html    ← Профайл
```

---

## Функцүүд

### 🔐 Нэвтрэлт (SOAP)
- SOAP `RegisterUser` — шинэ данс үүсгэх
- SOAP `LoginUser` — нэвтэрч JWT token авах
- SOAP `ValidateToken` — JSON сервисийн middleware дуудна
- HMAC-SHA256 JWT (7 хоногийн хугацаа), SHA-256 + давс нууц үг хэш
- JSON сервис SOAP-д нэвтрэлтийг бүрэн шилжүүлнэ

### 👤 Профайл CRUD (JSON REST)
- `POST /api/soap/register` — профайл үүсгэх
- `GET /api/users/:id` — профайл унших [auth]
- `PUT /api/users/me` — профайл засах [auth]
- `DELETE /api/users/:id` — профайл устгах [auth]

### 📖 Жорууд
- Зургийн URL баннертай кофений жор үүсгэх, засварлах, устгах
- Дуртай / дургүй, жор тус бүрд сэтгэгдэл
- Эрэмбэлэлт: **Шинэ** эсвэл **Алдартай**
- Ундааны төрлөөр шүүх: Espresso, Latte, Pour Over, Cold Brew гэх мэт

### 📰 Лент
- Дагадаг хэрэглэгчдийн жорууд
- **Нээн судлах хэсэг** — хэн ч дагаагүй үед алдартай жорууд

### ☕ Кафенүүд
- Кафений жагсаалт харах, нэмэх, үнэлгээ болон шүүмж бичих
- **📍 Ойролцоо** — GPS координат болон радиусаар шүүх

### 📓 Буцалтын дэвтэр
- 6 тэнхлэгтэй **радар диаграм** (Аромат, Амт, Хүчиллэг, Бие, Амтат, Төгсгөл)
- CSV экспорт

### 🌙 Харанхуй горим
- Бүрэн харанхуй загвар (Facebook харанхуй палитр)

---

## Технологийн стек

| Давхарга | Технологи |
|---|---|
| Desktop UI | JavaFX 21 |
| JSON REST API | Javalin 6.3 (порт 7000) |
| SOAP Auth Service | Spring Boot 3.2.5 + Spring-WS (порт 8081) |
| Frontend | HTML5 / CSS3 / Vanilla JS |
| Профайлын DB | H2 (файл: `data/cafe_db`) |
| Auth DB | In-memory ConcurrentHashMap |
| SOAP схем | JAXB + XSD (contract-first) |
| JWT | HMAC-SHA256 (гадаад сангүй) |
| Нууц үг | SHA-256 + санамсаргүй давс |
| Бүтээх | Maven Wrapper (`mvnw.cmd`) |
| Хэл | Java 21 |

---

## Архитектур

```
cafe-project/
├── soap-auth-service/               ← 🆕 Lab 06: SOAP нэвтрэлтийн сервис
│   └── src/main/java/com/example/soapauth/
│       ├── endpoint/AuthEndpoint    SOAP dispatcher (@PayloadRoot)
│       ├── service/AuthService      JWT + нууц үг логик
│       ├── dto/                     JAXB request/response DTO-ууд
│       └── config/                  Spring-WS + CORS тохиргоо
│
└── src/main/java/mn/edu/num/cafe/
    ├── app/Main.java                Composition root
    ├── core/
    │   ├── domain/                  Entity ангиуд
    │   └── application/BorgolService  Бизнесийн логик
    ├── infrastructure/
    │   ├── persistence/BorgolRepository  SQL / H2
    │   └── security/
    │       ├── JwtUtil              Локал JWT (desktop-д)
    │       ├── PasswordUtil         SHA-256 + давс
    │       └── SoapAuthClient       🆕 SOAP HTTP клиент
    └── ui/
        ├── web/BorgolApiServer      🔄 SOAP proxy + CORS + шинэ endpoint
        └── desktop/                 JavaFX хавтангууд
```

---

## Ашигласан дизайны хэв маягууд

| Хэв маяг | Хаана |
|---|---|
| **Singleton** | `DatabaseConnection` (DCL + volatile), `AppSession` |
| **Factory** | `RepositoryFactory` |
| **DAO / Repository** | `BorgolRepository` |
| **Observer** | `MenuChangeObserver` (legacy) |
| **Hexagonal Architecture** | Core домэйн UI болон DB-аас тусгаарлагдсан |
| **Proxy / Delegation** | `BorgolApiServer` → `SoapAuthClient` → SOAP сервис |

---

## API Endpoint-үүд

| Бүлэг | Endpoint |
|---|---|
| Нэвтрэлт (локал) | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` |
| **Нэвтрэлт (SOAP)** | `POST /api/soap/register`, `POST /api/soap/login` |
| Хэрэглэгчид | `GET /api/users/{id}`, `PUT /api/users/me`, `DELETE /api/users/{id}` |
| Жорууд | `GET/POST /api/recipes`, `GET/PUT/DELETE /api/recipes/{id}` |
| Лент | `GET /api/feed` |
| Кафенүүд | `GET/POST /api/cafes`, `POST /api/cafes/{id}/rate` |
| Дэвтэр | `GET/POST /api/journal`, `PUT/DELETE /api/journal/{id}` |
| Суралцах | `GET /api/brew-guides`, `GET /api/learn` |

---

## Туршилтын бүртгэлүүд

| Имэйл | Нууц үг | Хэрэглэгчийн нэр |
|---|---|---|
| `coffee@borgol.mn` | `password123` | coffee_master |
| `sara@borgol.mn` | `password123` | barista_sara |
| `tea@borgol.mn` | `password123` | tea_lover |

> ⚠️ SOAP сервис **in-memory** хадгалалт ашигладаг тул дахин эхлүүлэхэд бүртгэлүүд устна. Frontend-ээс дахин бүртгэнэ.

---

## Бонус даалгавар (хэрэгжүүлсэн)

- ✅ **Bonus 3: JWT Token** — SOAP сервис HMAC-SHA256 JWT үүсгэж, JSON сервис SOAP-аар баталгаажуулна
- ✅ **Graceful Fallback** — SOAP унтарсан тохиолдолд JSON сервис локал JWT руу автоматаар буцна

---

## Салбар

`feature/borgol-platform` → [GitHub](https://github.com/Apoca72/borgol-coffee-platform)
