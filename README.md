# ☕ Borgol — Coffee Enthusiast Platform

**Course:** ICSI486 Software Construction
**Student:** С.Тэмүүлэн — 22B1NUM6637
**Project:** JavaFX Desktop App + SOA Integration + Full-Stack Web Platform

---

## Overview

Borgol is a **full-stack coffee social platform** where enthusiasts share recipes, explore cafes, log brew sessions, follow hashtags, save recipes, and connect with other coffee lovers.

The system is built on **Service-Oriented Architecture (SOA)**:
- **JSON REST API** — Javalin (port 7000), handles all platform features
- **SOAP Auth Service** — Spring-WS (port 8081), issues and validates JWT tokens
- **Web Frontend** — Vanilla HTML/CSS/JS, served directly from the Java server
- **PostgreSQL** — hosted on Railway for cloud persistence

---

## 🗺️ Frontend Pages

| Page | URL | Purpose |
|---|---|---|
| `index.html` | `/` | Main app — feed, recipes, cafes, explore, journal, learn, map |
| `profile.html` | `/profile.html` | Profile edit, avatar upload, saved recipes, followed hashtags |
| `brew-timer.html` | `/brew-timer.html` | ⏱️ Brew timer + ratio calculator |
| `admin.html` | `/admin.html` | 🔐 Admin panel — moderate reports (id=1 only) |
| `login.html` | `/login.html` | Sign in |
| `register.html` | `/register.html` | Create account |

---

## ✨ Features

### 🔐 Authentication (SOA/SOAP)
- SOAP `RegisterUser` — create new account
- SOAP `LoginUser` — sign in, receive JWT
- SOAP `ValidateToken` — middleware validates every protected request
- HMAC-SHA256 JWT (7-day expiry), SHA-256 + salt password hashing
- Graceful fallback to local JWT if SOAP service is unreachable

### 👤 Profile
- **Avatar upload** — client-side canvas compression (max 240px, 75% JPEG), stored as base64 in `TEXT` DB column
- **Editable bio** — rich text bio field, saved to PostgreSQL
- **Expertise level** — BEGINNER → ENTHUSIAST → BARISTA → EXPERT
- **Flavor preference tags** — add/remove personal flavor chips
- **Stats** — recipe count, follower/following counts

### 📖 Recipes
- Create, edit, delete recipes with brew time, difficulty, ingredients, instructions
- Flavor tags, drink type filter (Espresso, Latte, Pour Over, Cold Brew, etc.)
- ❤️ Like / unlike
- 💬 Comments
- 🔖 Save / unsave recipes (accessible from profile)
- 🚩 Report recipes (sends to admin moderation queue)

### 🏠 Feed
- Recipes from users you follow
- Explore tab — trending recipes when following nobody

### 🏷️ Hashtags
- Follow hashtags to surface matching recipes in feed
- Trending hashtags panel
- Unfollow with one click from profile

### ☕ Cafes
- Browse and add cafes
- Star ratings and text reviews
- 📍 Nearby filter — GPS + radius search
- Interactive Leaflet.js map (OpenStreetMap, no API key)

### 📓 Brew Journal
- Personal brew log entries with 6-axis **radar chart** (Aroma, Taste, Acidity, Body, Sweetness, Finish)
- CSV export

### ⏱️ Brew Timer _(new)_
Select a brew method and get guided, step-by-step assistance:

| Method | Steps | Ratio Presets |
|---|---|---|
| V60 | 6 steps (bloom → drain) | 1:15, 1:16, 1:17 |
| Espresso | 4 steps (dose → extract) | 1:2, 1:2.5, 1:3 |
| French Press | 6 steps | 1:10, 1:12, 1:15 |
| AeroPress | 7 steps | 1:10, 1:13, 1:16 |
| Moka Pot | 6 steps | 1:7, 1:8, 1:9 |
| Cold Brew | 6 steps (12-18 h) | 1:5, 1:6, 1:8 |

- **Bidirectional ratio calculator** — change coffee weight → water auto-updates, and vice versa
- Animated SVG ring countdown with phase name
- Pause / Resume / Reset controls
- Active step highlights and auto-scrolls
- System notification when brew is complete
- `env(safe-area-inset-bottom)` for iOS Safari notch support

### 🚩 Reports & Admin Panel _(new)_
Users can report any recipe or user with a reason + optional description.

**Admin panel** (`/admin.html`) — accessible only to user `id=1`:
- Live pending/resolved/dismissed counts
- Filter tabs: Pending / Resolved / Dismissed
- Shows reporter username, content type + ID, reason, description, timestamp
- **Resolve** (marks content as actioned) or **Dismiss** (clears without action)
- Skeleton loading screens, toast notifications

### 🔔 Notifications
- Real-time bell icon in navbar
- Notified on new likes, comments, follows
- Mark all as read

### 👥 Social Graph
- Follow / unfollow users
- Follower / following counts on profile

### 🌙 Settings
- Language switcher (English / Монгол)
- Dark mode toggle

---

## 🏗️ Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│              Web Frontend (HTML / CSS / Vanilla JS)              │
│  index.html │ profile.html │ brew-timer.html │ admin.html        │
└───────────────────────────┬──────────────────────────────────────┘
                            │ HTTP REST (JSON)
                            ▼
┌──────────────────────────────────────┐
│   JSON REST API  :7000               │
│   BorgolApiServer (Javalin)          │
│                                      │
│  authRequired() ──────────────────┐  │
│  POST /api/soap/register ─────────┤  │
│  POST /api/soap/login ────────────┤  │
│  GET  /api/auth/me ───────────────┘  │
│                      SOAP XML        │
│                          ▼           │
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
├── soap-auth-service/                  SOAP auth microservice (Spring Boot)
│   └── src/main/java/com/example/soapauth/
│       ├── endpoint/AuthEndpoint        @PayloadRoot SOAP dispatcher
│       ├── service/AuthService          HMAC-SHA256 JWT + SHA-256 passwords
│       ├── dto/                         JAXB request/response DTOs
│       └── config/                      Spring-WS + CORS config
│
└── src/main/java/mn/edu/num/cafe/
    ├── app/Main.java                    Composition root
    ├── core/
    │   ├── domain/                      Entity classes (User, Recipe, Cafe…)
    │   └── application/BorgolService    Business logic + admin checks
    ├── infrastructure/
    │   ├── persistence/BorgolRepository All SQL queries, schema init + migrations
    │   └── security/
    │       ├── JwtUtil                  Local JWT fallback
    │       ├── PasswordUtil             SHA-256 + salt
    │       └── SoapAuthClient           SOAP HTTP client
    └── ui/
        ├── web/BorgolApiServer          REST endpoints + admin routes
        └── desktop/                     JavaFX panes (legacy desktop)
```

---

## 🗄️ Database

**PostgreSQL** hosted on Railway. Schema is auto-created and migrated on startup.

| Table | Purpose |
|---|---|
| `borgol_users` | Accounts — username, email, bio, avatar (`TEXT`), expertise level |
| `user_flavor_prefs` | Per-user flavor tags |
| `user_follows` | Follow graph |
| `recipes` | Coffee recipes |
| `recipe_flavor_tags` | Recipe tags |
| `recipe_likes` | Like records |
| `recipe_comments` | Comments |
| `saved_recipes` | Bookmarked recipes |
| `cafe_listings` | Cafe records |
| `cafe_ratings` | Ratings + reviews |
| `brew_guides` | Brew method guides |
| `brew_journal` | Personal brew log |
| `user_equipment` | User gear |
| `learn_articles` | Educational articles |
| `user_hashtags` | Followed hashtags |
| `reports` | Content reports (status: pending/resolved/dismissed) |
| `notifications` | User notifications |
| `blocked_users` | Block list |

---

## 🔌 API Endpoints

| Group | Endpoints |
|---|---|
| **Auth (SOAP)** | `POST /api/soap/register`, `POST /api/soap/login` |
| **Auth (local)** | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` |
| **Users** | `GET /api/users/{id}`, `PUT /api/users/me`, `DELETE /api/users/{id}` |
| **Hashtags** | `GET /api/users/me/hashtags`, `POST/DELETE /api/hashtags/{tag}/follow`, `GET /api/hashtags/trending` |
| **Recipes** | `GET/POST /api/recipes`, `GET/PUT/DELETE /api/recipes/{id}` |
| **Social** | `POST/DELETE /api/recipes/{id}/like`, `GET/POST /api/recipes/{id}/comments` |
| **Saved** | `GET /api/saved`, `POST/DELETE /api/saved/{recipeId}` |
| **Feed** | `GET /api/feed` |
| **Cafes** | `GET/POST /api/cafes`, `POST /api/cafes/{id}/rate` |
| **Journal** | `GET/POST /api/journal`, `PUT/DELETE /api/journal/{id}` |
| **Guides** | `GET /api/brew-guides`, `GET /api/learn` |
| **People** | `GET /api/users/search`, `POST/DELETE /api/users/{id}/follow`, `GET /api/users/{id}/followers` |
| **Notifications** | `GET /api/notifications`, `POST /api/notifications/read` |
| **Reports** | `POST /api/reports` |
| **Admin** | `GET /api/admin/reports?status=`, `POST /api/admin/reports/{id}/resolve`, `GET /api/admin/stats` |

---

## 🚀 Running Locally

### 1. SOAP Auth Service (start first)

```cmd
cd soap-auth-service
mvn spring-boot:run
```

> ✅ WSDL available at `http://localhost:8081/ws/authService.wsdl`

### 2. JSON API + Frontend

```cmd
cd cafe-project
mvnw.cmd javafx:run
```

> ✅ App at `http://localhost:7000`

### 3. Pages

```
http://localhost:7000/               ← Main app
http://localhost:7000/brew-timer.html ← Brew Timer
http://localhost:7000/profile.html   ← Profile
http://localhost:7000/admin.html     ← Admin (first user only)
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| REST API | Javalin 6.3 (port 7000) |
| SOAP Auth | Spring Boot 3.2.5 + Spring-WS (port 8081) |
| Frontend | HTML5 / CSS3 / Vanilla JS |
| Database | PostgreSQL on Railway |
| Desktop UI | JavaFX 21 (legacy) |
| JWT | HMAC-SHA256 (no external library) |
| Passwords | SHA-256 + random salt |
| Maps | Leaflet.js + OpenStreetMap |
| Fonts | Playfair Display + DM Sans |
| Build | Maven Wrapper (`mvnw.cmd`) |
| Language | Java 21 |

---

## 🎨 Design System

**Warm Light palette** — inspired by morning espresso

| Token | Value | Usage |
|---|---|---|
| `--espresso` | `#0C0400` | Navbar background |
| `--caramel` | `#A8621E` | Primary actions, links |
| `--amber` | `#F5C060` | Active nav, highlights |
| `--cream` | `#F6E8CC` | Subtle backgrounds |
| `--milk` | `#FDFAF3` | Page background |

Typography: **Playfair Display** (headings) + **DM Sans** (body)

---

## 🧩 Design Patterns

| Pattern | Where |
|---|---|
| **Singleton** | `DatabaseConnection` (DCL + volatile), `AppSession` |
| **Repository / DAO** | `BorgolRepository` |
| **Service Layer** | `BorgolService` |
| **Proxy / Delegation** | `BorgolApiServer` → `SoapAuthClient` → SOAP service |
| **Observer** | `MenuChangeObserver` (legacy desktop) |
| **Hexagonal Architecture** | Core domain isolated from UI and DB |
| **Strategy** | SOAP auth with local JWT fallback |

---

## 🔐 Admin System

The first registered account (`id = 1`) is automatically the admin.

- Admin-only endpoints return `403 Forbidden` for all other users
- Reports lifecycle: `pending` → `resolved` or `dismissed`
- `resolved_by` and `resolved_at` tracked per report

---

## Branch

`feature/borgol-platform` → [GitHub](https://github.com/Apoca72/borgol-coffee-platform)

> Also mirrored to GitLab for academic submission.
