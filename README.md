# ☕ Borgol — Coffee Enthusiast Platform

**Course:** ICSI486 Software Construction
**Student:** С.Тэмүүлэн — 22B1NUM6637
**Live:** https://borgol-production.up.railway.app

---

## Overview

Borgol is a **full-stack coffee social platform** where enthusiasts share recipes, explore cafes, log brew sessions, follow hashtags, save recipes, and connect with other coffee lovers — powered by a Service-Oriented Architecture and an AI coffee assistant.

The system is built on **SOA**:
- **JSON REST API** — Javalin (Java 21), handles all platform features
- **SOAP Auth Service** — Spring-WS (Spring Boot), issues and validates JWT tokens
- **Web Frontend** — Vanilla HTML/CSS/JS, served directly from the Java server
- **PostgreSQL** — hosted on Railway for cloud persistence
- **Bean AI** — Google Gemini 1.5 Flash chatbot, coffee-focused assistant

---

## 🗺️ Pages

| Page | URL | Purpose |
|---|---|---|
| `index.html` | `/` | Main app — feed, recipes, cafes, explore, journal, learn, map |
| `profile.html` | `/profile.html` | Profile, avatar upload, bio, saved recipes, hashtags |
| `brew-timer.html` | `/brew-timer.html` | ⏱️ Brew timer + ratio calculator |
| `admin.html` | `/admin.html` | 🔐 Admin panel — moderate reports (id=1 only) |
| `login.html` | `/login.html` | Sign in |
| `register.html` | `/register.html` | Create account |

---

## ✨ Features

### 🤖 Bean — AI Coffee Assistant _(new)_
- Floating **☕ chat widget** on every page (bottom-right)
- Powered by **Google Gemini 1.5 Flash** (free tier — 1,500 req/day)
- Streaming word-by-word replies via Server-Sent Events
- Knows Borgol's platform, Ulaanbaatar's coffee scene, and all brew methods
- Session history persists across navigations
- Responds in the user's language (Mongolian, English, etc.)
- Unread dot indicator when a reply arrives while panel is closed

### 🔐 Authentication (SOA/SOAP)
- SOAP `RegisterUser` — create new account
- SOAP `LoginUser` — sign in, receive JWT
- SOAP `ValidateToken` — middleware validates every protected request
- HMAC-SHA256 JWT (7-day expiry), SHA-256 + salt password hashing
- Graceful fallback to local JWT if SOAP service is unreachable

### 👤 Profile
- **Avatar upload** — client-side canvas compression (max 240px, JPEG), stored as base64 `TEXT`
- **Editable bio** — saved to PostgreSQL
- **Expertise level** — BEGINNER → ENTHUSIAST → BARISTA → EXPERT
- **Flavor preference tags** — add/remove personal flavor chips
- **Stats** — recipe count, follower/following counts

### 📖 Recipes
- Create, edit, delete with brew time, difficulty, ingredients, instructions
- **Photo upload** — client-side compressed, stored as base64 `TEXT` (up to 8 MB)
- Flavor tags, drink type filter
- ❤️ Like / unlike
- 💬 Comments
- 🔖 Save / unsave (accessible from profile)
- 🚩 Report (sends to admin moderation queue)

### 🏠 Feed
- Recipes from users you follow
- Explore tab — trending recipes when following nobody

### 🏷️ Hashtags
- Follow hashtags to surface matching recipes in feed
- Trending hashtags panel

### ☕ Cafes
- Browse and add cafes with star ratings and reviews
- 📍 Nearby filter — GPS + radius search
- Interactive Leaflet.js map (OpenStreetMap, no API key)

### 📓 Brew Journal
- Personal brew log with 6-axis **radar chart** (Aroma, Taste, Acidity, Body, Sweetness, Finish)
- CSV export

### ⏱️ Brew Timer
Step-by-step guided brewing for 6 methods:

| Method | Steps | Ratio Presets |
|---|---|---|
| V60 | 6 (bloom → drain) | 1:15, 1:16, 1:17 |
| Espresso | 4 | 1:2, 1:2.5, 1:3 |
| French Press | 6 | 1:10, 1:12, 1:15 |
| AeroPress | 7 | 1:10, 1:13, 1:16 |
| Moka Pot | 6 | 1:7, 1:8, 1:9 |
| Cold Brew | 6 (12–18 h) | 1:5, 1:6, 1:8 |

- **Bidirectional ratio calculator** — change coffee → water auto-updates, and vice versa
- Animated SVG ring countdown, pause/resume/reset
- System notification on completion
- `env(safe-area-inset-bottom)` for iOS Safari notch

### 🚩 Reports & Admin Panel
- Any user can report a recipe or user with reason + description
- **Admin panel** (`/admin.html`) — first registered account only:
  - Live pending/resolved/dismissed counts
  - Filter tabs, resolve/dismiss buttons
  - Skeleton loading, toast notifications

### 🔔 Notifications
- Bell icon with unread count in navbar
- Notified on likes, comments, follows

### 👥 Social Graph
- Follow / unfollow users
- Block / unblock users

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│             Web Frontend (HTML / CSS / Vanilla JS)              │
│  index.html │ profile.html │ brew-timer.html │ admin.html       │
│                                                                 │
│                     ☕ Bean chat widget                         │
└──────────────────────────┬──────────────────────────────────────┘
                           │ HTTP REST (JSON) + SSE
                           ▼
┌──────────────────────────────────────┐
│   JSON REST API  (Javalin)           │
│   BorgolApiServer                    │
│                                      │
│  POST /api/bean/chat ─────────────►  Google Gemini 1.5 Flash
│                                      │
│  authRequired() ──────────────────┐  │
│  POST /api/soap/register ─────────┤  │
│  POST /api/soap/login ────────────┤  │ SOAP XML
│                                   ▼  │
│  ┌─────────────────────────────┐     │
│  │  SOAP Auth Service  :8081   │     │
│  │  Spring-WS (Spring Boot)    │     │
│  │  ▸ RegisterUser             │     │
│  │  ▸ LoginUser → JWT          │     │
│  │  ▸ ValidateToken            │     │
│  └─────────────────────────────┘     │
│                                      │
│  BorgolRepository ────────────────►  PostgreSQL (Railway)
└──────────────────────────────────────┘
```

### Code Structure

```
cafe-project/
├── soap-auth-service/              SOAP auth microservice (Spring Boot)
│   └── src/main/java/com/example/soapauth/
│       ├── endpoint/AuthEndpoint   @PayloadRoot SOAP dispatcher
│       ├── service/AuthService     HMAC-SHA256 JWT + SHA-256 passwords
│       ├── dto/                    JAXB request/response DTOs
│       └── config/                 Spring-WS + CORS config
│
└── src/main/java/mn/edu/num/cafe/
    ├── app/Main.java               Composition root
    ├── core/
    │   ├── domain/                 Entity classes (User, Recipe, Cafe…)
    │   └── application/BorgolService  Business logic + admin checks
    ├── infrastructure/
    │   ├── persistence/BorgolRepository  All SQL + schema migrations
    │   └── security/
    │       ├── JwtUtil             Local JWT fallback
    │       ├── PasswordUtil        SHA-256 + salt
    │       └── SoapAuthClient      SOAP HTTP client
    └── ui/
        ├── web/BorgolApiServer     REST endpoints + Bean AI + admin
        └── desktop/                JavaFX panes (legacy)
```

---

## 🗄️ Database

PostgreSQL hosted on Railway. Schema auto-created and migrated on startup.

| Table | Purpose |
|---|---|
| `borgol_users` | Accounts — username, email, bio, avatar (`TEXT`), expertise |
| `user_flavor_prefs` | Per-user flavor tags |
| `user_follows` | Follow graph |
| `recipes` | Coffee recipes (image_url: `TEXT`) |
| `recipe_flavor_tags` | Recipe tags |
| `recipe_likes` | Like records |
| `recipe_comments` | Comments |
| `saved_recipes` | Bookmarked recipes |
| `cafes` | Cafe listings |
| `cafe_ratings` | Ratings + reviews |
| `brew_guides` | Brew method guides |
| `brew_journal` | Personal brew log |
| `user_equipment` | User gear |
| `learn_articles` | Educational articles |
| `user_hashtags` | Followed hashtags |
| `reports` | Reports (pending / resolved / dismissed) |
| `notifications` | User notifications |
| `blocked_users` | Block list |

---

## 🔌 API Endpoints

| Group | Endpoints |
|---|---|
| **Auth (SOAP)** | `POST /api/soap/register`, `POST /api/soap/login` |
| **Auth (local)** | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` |
| **Users** | `GET /api/users/{id}`, `PUT /api/users/me`, `DELETE /api/users/{id}` |
| **Social** | `POST/DELETE /api/users/{id}/follow`, `POST/DELETE /api/users/{id}/block` |
| **Hashtags** | `POST/DELETE /api/hashtags/{tag}/follow`, `GET /api/hashtags/trending` |
| **Recipes** | `GET/POST /api/recipes`, `GET/PUT/DELETE /api/recipes/{id}` |
| **Recipe actions** | `POST /api/recipes/{id}/like`, `GET/POST /api/recipes/{id}/comments`, `POST /api/recipes/{id}/save` |
| **Feed** | `GET /api/feed` |
| **Cafes** | `GET/POST /api/cafes`, `POST /api/cafes/{id}/rate`, `GET /api/cafes/nearby` |
| **Journal** | `GET/POST /api/journal`, `PUT/DELETE /api/journal/{id}` |
| **Guides** | `GET /api/brew-guides`, `GET /api/learn` |
| **Notifications** | `GET /api/notifications`, `POST /api/notifications/read` |
| **Reports** | `POST /api/report` |
| **Admin** | `GET /api/admin/reports?status=`, `POST /api/admin/reports/{id}/resolve`, `GET /api/admin/stats` |
| **Bean AI** | `POST /api/bean/chat` — streaming SSE (Google Gemini 1.5 Flash) |

---

## 🚀 Running Locally

### 1. SOAP Auth Service (start first)

```bash
cd soap-auth-service
mvn spring-boot:run
```

> WSDL at `http://localhost:8081/ws/authService.wsdl`

### 2. JSON API + Frontend

```bash
cd cafe-project
mvnw.cmd javafx:run
```

> App at `http://localhost:7000`

### 3. Environment Variables (for Bean AI)

```bash
GEMINI_API_KEY=your-google-ai-studio-key   # https://aistudio.google.com/apikey
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| REST API | Javalin 6.3 |
| SOAP Auth | Spring Boot 3.2.5 + Spring-WS |
| AI Chatbot | Google Gemini 1.5 Flash (free tier) |
| Frontend | HTML5 / CSS3 / Vanilla JS |
| Database | PostgreSQL on Railway |
| Desktop UI | JavaFX 21 (legacy) |
| JWT | HMAC-SHA256 |
| Passwords | SHA-256 + random salt |
| Maps | Leaflet.js + OpenStreetMap |
| Fonts | Playfair Display + DM Sans |
| Build | Maven Wrapper |
| Language | Java 21 |
| Hosting | Railway |

---

## 🎨 Design System

**Warm Light palette** — inspired by morning espresso

| Token | Value | Usage |
|---|---|---|
| `--espresso` | `#0C0400` | Navbar |
| `--caramel` | `#A8621E` | Primary actions |
| `--amber` | `#F5C060` | Active states |
| `--cream` | `#F6E8CC` | Subtle backgrounds |
| `--milk` | `#FDFAF3` | Page background |

Typography: **Playfair Display** (headings) + **DM Sans** (body)

---

## 🧩 Design Patterns

| Pattern | Where |
|---|---|
| **Singleton** | `DatabaseConnection` (DCL + volatile), `AppSession` |
| **Repository / DAO** | `BorgolRepository` — all SQL |
| **Service Layer** | `BorgolService` — business logic |
| **Proxy / Delegation** | `BorgolApiServer` → `SoapAuthClient` → SOAP |
| **Observer** | `MenuChangeObserver` (legacy desktop) |
| **Hexagonal Architecture** | Core domain isolated from UI and DB |
| **Strategy** | SOAP auth with local JWT fallback |

---

## 🔐 Admin System

The first registered account (`id = 1`) is automatically admin.

- Admin-only endpoints return `403 Forbidden` for all others
- Reports lifecycle: `pending` → `resolved` / `dismissed`
- `resolved_by` and `resolved_at` tracked per report

---

`feature/borgol-platform` → [github.com/Apoca72/borgol-coffee-platform](https://github.com/Apoca72/borgol-coffee-platform)

> Also mirrored to GitLab for academic submission.
