# Borgol Coffee Enthusiast Platform

A full-stack coffee enthusiast platform with a web SPA and native desktop client, developed as an individual project for ICSI486 Software Construction.

**Stack:** Java 21 · JavaFX · Javalin 6.3 · H2 · Custom JWT · Maven

---

## What it does

Borgol lets coffee enthusiasts share recipes, discover cafes, keep a brew journal, and learn about coffee in one platform.

- **Authentication** — JWT-based login, SHA-256 + salt password hashing, 7-day token expiry
- **Recipes** — create, edit, delete with images, likes, and comments
- **Feed** — personalized social timeline; falls back to Discover when following list is empty
- **Cafes** — listings with 1–5 star ratings and GPS proximity sorting
- **Brew Journal** — personal entries with a 6-axis radar chart (Aroma, Flavor, Acidity, Body, Sweet, Finish) and PDF export
- **Brew Guides** — step-by-step guides for Pour Over, French Press, AeroPress, Espresso, Cold Brew, Moka Pot
- **Learn** — 6 educational articles covering roast levels, extraction science, water quality, and more
- **Community** — follow/unfollow users, profile pages, followers/following lists, search
- **Desktop client** — native JavaFX app with dark/light theme toggle, sharing the same backend service layer

---

## Architecture

The project follows Clean Architecture with four explicit layers, each depending only inward:

```
UI (Javalin routes + JavaFX views)
    |
Application (BorgolService — business logic)
    |
Domain (User, Recipe, CafeListing, BrewJournalEntry, BrewGuide, LearnArticle)
    |
Infrastructure (BorgolRepository, DatabaseConnection, JwtUtil, PasswordUtil)
```

The Domain layer has zero framework dependencies — it is plain Java. This makes core logic independently testable and allows the same service to power both the web API and the desktop client without duplicating business rules.

---

## Design Patterns

| Pattern | Where | Why |
|---|---|---|
| Repository | `BorgolRepository` | Isolates all SQL behind a single interface; the service layer never touches JDBC directly |
| Service Layer | `BorgolService` | Single orchestration point for business rules, sitting between controllers and repositories |
| Singleton | `DatabaseConnection.getInstance()` | Ensures one shared H2 connection across the application lifetime |
| Facade | `BorgolService` | Exposes simple methods that internally coordinate multiple repository calls |
| DTO / Value Object | `User`, `Recipe`, etc. | Immutable data carriers that cross layer boundaries |

---

## Security

**JWT** — implemented from scratch using HMAC-SHA256 without any third-party library. The token encodes user ID and expiry; the signature is verified on every protected request via a Javalin `before()` handler.

**Passwords** — SHA-256 hashed with a per-user random salt, stored as `saltHex:hashHex`. The salt prevents rainbow table and precomputation attacks.

---

## Technology choices

**Why Javalin over Spring Boot?**
Javalin adds minimal abstraction over the Jetty HTTP server. For a project where understanding each layer matters, it keeps routing and middleware explicit without hiding behavior behind annotations and auto-configuration.

**Why H2 over PostgreSQL?**
File-based embedded database removes infrastructure setup from the project. Because the Repository layer is the only place that touches JDBC, switching to PostgreSQL requires changing only the connection string and driver dependency — nothing else.

**Why no ORM?**
Writing SQL directly gives full control over queries and reinforces understanding of relational mapping. The Repository pattern provides the same separation an ORM would, without the abstraction overhead.

**Why custom JWT?**
Using `javax.crypto.Mac` directly rather than a library forces a concrete understanding of the token structure (header.payload.signature), the signing algorithm, and the verification process.

---

## Technical highlights

- **Radar Chart** — drawn with pure Canvas math using trigonometry to compute polygon vertices across 6 axes; no charting library used
- **PDF export** — generates a print-ready HTML document with embedded SVG radar chart, opened in a new window for browser print-to-PDF
- **Discover Feed** — when a user follows nobody, the feed falls back to a randomized recipe set so the experience is never empty on first run
- **Image aspect ratio (JavaFX)** — `StackPane` + `Rectangle` clip + `fitWidthProperty().bind()` preserves the original ratio without distortion regardless of container size
- **Idempotent seeding** — demo data seed checks by email rather than by auto-increment ID, so re-running the application never duplicates records

---

## Running the application

**Web server only (fast start):**
```
mvnw.cmd exec:java -Dexec.mainClass=mn.edu.num.cafe.app.MainWeb
```
Opens on `http://localhost:7000`

**Desktop client:**
```
mvnw.cmd exec:java -Dexec.mainClass=mn.edu.num.cafe.app.Main
```

**Demo accounts:**
```
coffee@borgol.mn / password123
sara@borgol.mn   / password123
tea@borgol.mn    / password123
```

---

## Project structure

```
src/main/java/mn/edu/num/cafe/
  app/                  Main.java, MainWeb.java
  core/
    domain/             User, Recipe, CafeListing, BrewJournalEntry, BrewGuide, LearnArticle
    application/        BorgolService
  infrastructure/
    persistence/        BorgolRepository
    config/             DatabaseConnection
    security/           JwtUtil, PasswordUtil
  ui/
    web/                BorgolApiServer  (30+ REST endpoints)
    desktop/            BorgolApp        (JavaFX)
src/main/resources/public/
  index.html            Vanilla JS SPA (~2400 lines)
```

---

## API reference

| Method | Path | Description |
|---|---|---|
| POST | /api/auth/register | Register |
| POST | /api/auth/login | Login, returns JWT |
| GET | /api/auth/me | Current user |
| GET/POST | /api/recipes | List / create recipes |
| GET/PUT/DELETE | /api/recipes/{id} | Single recipe |
| POST | /api/recipes/{id}/like | Toggle like |
| GET/POST | /api/recipes/{id}/comments | Comments |
| GET | /api/feed | Personalized feed |
| GET/POST | /api/cafes | List / create cafes |
| POST | /api/cafes/{id}/rate | Rate a cafe |
| GET/POST | /api/journal | Journal entries |
| PUT/DELETE | /api/journal/{id} | Edit / delete entry |
| GET | /api/brew-guides | All brew guides |
| GET | /api/learn | All learn articles |
| POST/DELETE | /api/users/{id}/follow | Follow / unfollow |
| GET | /api/users/search | Search users |
