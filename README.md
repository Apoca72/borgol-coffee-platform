# 🪴 Borgol — Coffee Enthusiast Platform

**Хичээл:** ICSI486 Программ хангамжийн бүтээлт
**Оюутан:** С.Тэмүүлэн — 22B1NUM6637
**Бие даасан төсөл:** JavaFX Desktop App — Borgol Coffee Enthusiast Platform

---

## Overview

Borgol is a full-featured **JavaFX desktop social platform** for coffee enthusiasts. Users can share recipes, discover cafés, track brew sessions in a personal journal, follow other coffee lovers, and learn through guides and articles — all within a polished, responsive desktop app.

---

## Screenshots

| Recipes Feed | Dark Mode | Journal Radar |
|---|---|---|
| Cards with image banners, likes, comments | Full dark theme toggle | Brew session radar chart |

---

## Features

### 🔐 Auth
- Register / Login with JWT-based session (HMAC-SHA256, 7-day expiry)
- Passwords stored as SHA-256 + random salt (`saltHex:hashHex`)
- Session persisted in `AppSession` singleton

### 📖 Recipes
- Create, edit, delete coffee recipes with image URL banners
- Like / unlike, comment threads per recipe
- Sort: **Recent** or **Popular**
- Filter by drink type: Espresso, Latte, Pour Over, Cold Brew, etc.
- Right panel: Top Liked recipes + People to Follow suggestions

### 📰 Feed
- Recipes from users you follow
- **Discover section** — shows popular recipes when not following anyone yet

### ☕ Cafés
- Browse and add café listings with ratings and reviews
- **📍 Near Me** — filter by GPS coordinates and radius (defaults to Ulaanbaatar)
- Star ratings with personal review text

### 📓 Brew Journal
- Personal brew log entries with bean, method, roast, dose, water temp, brew time, yield
- 6-axis **radar chart** (Aroma, Flavor, Acidity, Body, Sweet, Finish) rendered via JavaFX Canvas
- Export entries as **CSV** file

### 📚 Learn & Brew Guides
- 6 curated Brew Guides: Pour Over (V60), French Press, AeroPress, Espresso, Cold Brew, Moka Pot
- 6 Learn Articles: Roast Levels, Extraction Science, Water Quality, Flavor Tasting, Arabica vs Robusta, Grind Size

### 👥 People
- Follow / unfollow users
- View public profiles with recipe lists and follower stats
- People You May Know suggestions

### 👤 Profile
- Edit bio, equipment list, expertise level, flavor preferences
- Tabs: My Recipes, Liked Recipes, Equipment
- Sidebar stats refresh via callback (no full rebuild)

### 🌙 Dark Mode
- Full dark theme (Facebook dark palette: `#18191A` / `#242526` / `#3A3B3C`)
- Toggle button in sidebar — rebuilds all panes instantly
- All inline styles use `UiUtils` theme helpers (`bg()`, `card()`, `text()`, `sub()`, etc.)

### 🔔 Toast Notifications
- Amber fade-in/out overlay on recipe save, journal entry, profile update, etc.

---

## Tech Stack

| Layer | Technology |
|---|---|
| UI | JavaFX 21 (BorderPane, VBox, HBox, StackPane, Canvas, ScrollPane) |
| Backend | Javalin 6.3 (REST API, JSON) |
| Database | H2 (file-based, MERGE INTO for upserts) |
| Auth | Custom HMAC-SHA256 JWT (no external library) |
| Build | Maven Wrapper (`mvnw.cmd`) |
| Language | Java 21 |

---

## Architecture

```
mn.edu.num.cafe
├── app/
│   └── Main.java                    — Composition root, seeds demo data, launches JavaFX
├── core/
│   ├── domain/                      — Entity classes
│   │   ├── User, Recipe, CafeListing
│   │   ├── RecipeComment, BrewJournalEntry
│   │   ├── BrewGuide, LearnArticle
│   │   └── MenuItem (legacy)
│   └── application/
│       ├── BorgolService.java       — All business logic
│       └── MenuService.java         — Legacy menu service
├── infrastructure/
│   ├── persistence/
│   │   └── BorgolRepository.java   — All SQL / H2 queries
│   └── security/
│       ├── JwtUtil.java             — HMAC-SHA256 JWT
│       └── PasswordUtil.java        — SHA-256 + salt hashing
└── ui/
    ├── web/
    │   └── BorgolApiServer.java     — 30+ REST endpoints (Javalin)
    └── desktop/
        ├── BorgolApp.java           — JavaFX Application entry
        ├── MainWindow.java          — BorderPane root, sidebar, dark mode
        ├── UiUtils.java             — Shared helpers: avatars, toasts, dialogs, theme colors
        ├── AppSession.java          — Logged-in user state
        ├── RecipesPane.java         — Recipe feed with CRUD
        ├── FeedPane.java            — Social feed + Discover fallback
        ├── CafesPane.java           — Café cards + Near Me
        ├── JournalPane.java         — Brew journal + radar chart
        ├── LearnPane.java           — Guides & articles
        ├── PeoplePane.java          — User discovery
        └── ProfilePane.java         — Profile editor + tabs
```

---

## Running the App

**Prerequisites:** Java 21, Maven (wrapper included)

```cmd
cd "C:\Users\thatu\OneDrive\Desktop\cafe-project"
mvnw.cmd javafx:run
```

The app seeds the H2 database on first launch and opens a desktop window.

> ⚠️ On **Windows Command Prompt** use `mvnw.cmd javafx:run` (no `./` prefix)
> On **PowerShell / Git Bash** use `./mvnw.cmd javafx:run`

---

## Demo Accounts

| Email | Password | Username |
|---|---|---|
| `coffee@borgol.mn` | `password123` | coffee_master |
| `sara@borgol.mn` | `password123` | barista_sara |
| `tea@borgol.mn` | `password123` | tea_lover |

Plus 5 extra seeded users: `latte_king`, `espresso_pro`, `cold_brew_queen`, `matcha_monk`, `roast_geek`

---

## Design Patterns Used

| Pattern | Where |
|---|---|
| **Singleton** | `DatabaseConnection` (DCL + volatile), `AppSession` |
| **Factory** | `RepositoryFactory` (DB vs MEM mode) |
| **DAO / Repository** | `BorgolRepository` — all SQL isolated behind service layer |
| **Observer** | `MenuChangeObserver` (legacy menu service) |
| **Hexagonal Architecture** | Core domain isolated from UI and DB layers |
| **Callback / Runnable** | `ProfilePane(onProfileUpdated)` — sidebar refresh without full rebuild |

---

## API Endpoints (Web Layer)

| Group | Endpoints |
|---|---|
| Auth | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` |
| Users | `GET /api/users`, `PUT /api/users/me`, `POST/DELETE /api/users/{id}/follow`, `GET /api/users/search` |
| Recipes | `GET/POST /api/recipes`, `GET/PUT/DELETE /api/recipes/{id}`, `POST /api/recipes/{id}/like`, `GET/POST /api/recipes/{id}/comments` |
| Feed | `GET /api/feed` |
| Cafés | `GET/POST /api/cafes`, `GET /api/cafes/{id}`, `POST /api/cafes/{id}/rate` |
| Journal | `GET/POST /api/journal`, `PUT/DELETE /api/journal/{id}` |
| Learn | `GET /api/brew-guides`, `GET /api/brew-guides/{id}`, `GET /api/learn`, `GET /api/learn/{id}` |

---

## Seeded Static Content

**Brew Guides:** Pour Over (V60), French Press, AeroPress, Espresso, Cold Brew, Moka Pot
**Learn Articles:** Roast Levels, Extraction Science, Water Quality, Flavor Tasting, Arabica vs Robusta, Grind Size Guide

---

## Branch

`feature/borgol-platform` → [GitHub](https://github.com/Apoca72/borgol-coffee-platform)
