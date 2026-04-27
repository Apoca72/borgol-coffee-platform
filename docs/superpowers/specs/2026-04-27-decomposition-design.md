# Full-Stack Decomposition Design
**Date:** 2026-04-27  
**Approach:** Full Layer Decomposition (Approach 2)  
**Context:** Class project graded on design patterns / OOP / SOLID

---

## Problem

Three God Classes span all three application layers:

| File | Lines | Responsibility |
|---|---|---|
| `BorgolRepository.java` | ~1,900 | All SQL for all domains |
| `BorgolService.java` | ~1,210 | All business logic |
| `BorgolApiServer.java` | ~1,300 | All 81 HTTP endpoints |
| `index.html` | ~4,566 | All frontend (161 functions) |

Each class violates SRP. Adding any feature touches all four files simultaneously.

---

## Section 1: Package Architecture

Hexagonal / ports-and-adapters. Domain interfaces live in `core/ports/`, implementations in `infrastructure/persistence/`.

```
borgol/
  core/
    domain/          # entities (unchanged)
    ports/           # repository interfaces (one per domain)
    application/     # one service per domain
  infrastructure/
    persistence/     # one repository impl per domain + SchemaInitializer
    security/        # JwtUtil, PasswordUtil (unchanged)
  ui/
    web/
      routers/       # one router per domain
      dto/           # request/response POJOs
    desktop/         # unchanged (JournalPane, etc.)
```

**Patterns:** Hexagonal Architecture, Repository Pattern, ISP (services depend only on the interface they need), DIP (services depend on interfaces, not implementations).

---

## Section 2: Repository Layer

Split `BorgolRepository.java` by domain. Each class implements its `core/ports/` interface.

```
infrastructure/persistence/
  UserRepository.java           # users, auth tokens
  BrewGuideRepository.java      # brew guides + learn articles
  RecipeRepository.java         # recipes + collections
  JournalRepository.java        # journal entries + weather
  CafeRepository.java           # cafes + check-ins
  AchievementRepository.java    # badges + awards
  SchemaInitializer.java        # all CREATE TABLE IF NOT EXISTS on startup
```

`ConnectionPool` is injected into each repository via constructor — shared, not duplicated.

`core/ports/` interfaces:
```
UserRepositoryPort.java
BrewGuideRepositoryPort.java
RecipeRepositoryPort.java
JournalRepositoryPort.java
CafeRepositoryPort.java
AchievementRepositoryPort.java
```

---

## Section 3: Service Layer

Split `BorgolService.java` by domain. Each service receives only the repository interfaces it needs.

```
core/application/
  UserService.java              # register, login, profile
  BrewGuideService.java         # guides, learn articles
  RecipeService.java            # recipes + collections
  JournalService.java           # journal entries, weather sanitization
  CafeService.java              # cafes + check-ins
  AchievementService.java       # BADGE_META map, checkAndAwardAchievements()
```

**Dependency rule:** `AchievementService` is injected into `JournalService` and `CafeService` — the two entry points that trigger badge checks.

**Facade retained:** `BorgolService.java` becomes a thin delegation wrapper. All existing desktop calls (`JournalPane`, etc.) continue to work with zero changes. This is an explicit Facade pattern — callable out in the writeup.

```java
public class BorgolService {
    // constructor injects all six services
    public List<BrewJournalEntry> getJournalEntries(int userId) {
        return journalService.getJournalEntries(userId); // pure delegation
    }
}
```

---

## Section 4: Router Layer

Split `BorgolApiServer.java` into domain routers. Each router registers its own routes via `register(Javalin app)`.

```
ui/web/routers/
  UserRouter.java               # /api/auth/*, /api/profile
  BrewGuideRouter.java          # /api/brew-guides, /api/learn
  RecipeRouter.java             # /api/recipes, /api/collections
  JournalRouter.java            # /api/journal
  CafeRouter.java               # /api/cafes, /api/checkins
  AchievementRouter.java        # /api/achievements

ui/web/dto/
  JournalReq.java
  CheckinReq.java
  CollectionReq.java
  CollectionRecipeReq.java
  # ... one DTO per request body
```

`BorgolApiServer` becomes a coordinator:
```java
public class BorgolApiServer {
    public void start() {
        Javalin app = Javalin.create();
        new UserRouter(userService).register(app);
        new RecipeRouter(recipeService).register(app);
        // ...
        app.start(7000);
    }
}
```

`ApiGateway.java` is untouched — it wraps the `Javalin` instance at the outermost layer (Chain of Responsibility pattern, cross-cutting concerns: CORS, rate limiting, auth).

---

## Section 5: Frontend Split

`index.html` stays plain files — no build step, no bundler, no modules. Split via `<script src="">` tags.

```
resources/public/
  index.html                    # shell: nav, page containers, modal skeletons, CSS (~300 lines)
  js/
    api.js                      # fetch wrappers, auth headers, BASE_URL constant
    auth.js                     # login, register, logout, token storage (window.authToken)
    brew-guides.js              # brew guides + learn page
    recipes.js                  # recipes + collections
    journal.js                  # journal entries, weather fetch
    cafes.js                    # cafe map/list + check-ins
    achievements.js             # badge display, checkAchievements()
    app.js                      # showPage(), nav wiring, page init — loaded last
```

Load order (bottom of `<body>` in `index.html`):
```html
<script src="js/api.js"></script>
<script src="js/auth.js"></script>
<script src="js/brew-guides.js"></script>
<script src="js/recipes.js"></script>
<script src="js/journal.js"></script>
<script src="js/cafes.js"></script>
<script src="js/achievements.js"></script>
<script src="js/app.js"></script>
```

Global state (`window.authToken`, `window.currentUser`) set by `auth.js`, read by all domain files. No import/export — same runtime behavior as the monolith, just split across files the browser caches individually.

---

## Design Patterns Summary (for writeup)

| Pattern | Where |
|---|---|
| Hexagonal Architecture | `core/ports/` interfaces, infra implements them |
| Repository Pattern | `*Repository` classes behind `*RepositoryPort` interfaces |
| Facade | `BorgolService` delegates to domain services, desktop compat |
| Chain of Responsibility | `ApiGateway` middleware chain |
| SRP | One class per domain per layer |
| ISP | Each service only receives the interface it needs |
| DIP | Services depend on `Port` interfaces, not `Repository` impls |

---

## What Does NOT Change

- `ApiGateway.java` — untouched
- `JwtUtil.java`, `PasswordUtil.java` — untouched  
- `core/domain/` entities — untouched
- Desktop UI (`JournalPane.java`, etc.) — untouched (Facade shields it)
- Database schema — untouched
- No build tools added, no new dependencies
