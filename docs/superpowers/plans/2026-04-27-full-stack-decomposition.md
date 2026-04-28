# Full-Stack Decomposition Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Break three God Classes (BorgolRepository ~1900 lines, BorgolService ~1210 lines, BorgolApiServer ~1300 lines) and a monolithic frontend (index.html ~4566 lines) into domain-scoped units without changing any behavior.

**Architecture:** Hexagonal (ports-and-adapters). Interfaces live in `core/ports/`, implementations in `infrastructure/persistence/`. Domain services in `core/application/`, each receiving only its needed port interface. Domain routers in `ui/web/routers/`. `BorgolService` stays as a Facade — the desktop layer never changes. Frontend split into `<script src="">` JS files, no build step.

**Tech Stack:** Java 21, Javalin 6, H2 (dev) / PostgreSQL (prod), JUnit 5 (already in pom.xml), Maven wrapper (`./mvnw`), Vanilla JS

**Verification command throughout:** `./mvnw compile -q` — must pass after every task before committing.

---

## File Map

### New files (create)
```
src/main/java/borgol/
  core/ports/
    UserRepositoryPort.java
    RecipeRepositoryPort.java
    BrewGuideRepositoryPort.java
    JournalRepositoryPort.java
    CafeRepositoryPort.java
    AchievementRepositoryPort.java
  core/application/
    UserService.java
    RecipeService.java
    BrewGuideService.java
    JournalService.java
    CafeService.java
    AchievementService.java
  infrastructure/persistence/
    SchemaInitializer.java
    UserRepository.java
    RecipeRepository.java
    BrewGuideRepository.java
    JournalRepository.java
    CafeRepository.java
    AchievementRepository.java
  ui/web/
    routers/
      UserRouter.java
      RecipeRouter.java
      BrewGuideRouter.java
      JournalRouter.java
      CafeRouter.java
      AchievementRouter.java
    dto/
      JournalReq.java
      CheckinReq.java
      CollectionReq.java
      CollectionRecipeReq.java

src/main/resources/public/js/
  api.js
  auth.js
  brew-guides.js
  recipes.js
  journal.js
  cafes.js
  achievements.js
  app.js

src/test/java/borgol/
  UserServiceTest.java
  JournalServiceTest.java
  AchievementServiceTest.java
```

### Modified files
```
src/main/java/borgol/
  infrastructure/persistence/BorgolRepository.java  — becomes thin delegator
  core/application/BorgolService.java               — becomes Facade over domain services
  ui/web/BorgolApiServer.java                       — delegates to domain routers
  app/Main.java                                     — updated wiring
  resources/public/index.html                       — remove inline JS, add <script> tags
```

---

## Task 1: Core Port Interfaces

**Files:**
- Create: `src/main/java/borgol/core/ports/UserRepositoryPort.java`
- Create: `src/main/java/borgol/core/ports/RecipeRepositoryPort.java`
- Create: `src/main/java/borgol/core/ports/BrewGuideRepositoryPort.java`
- Create: `src/main/java/borgol/core/ports/JournalRepositoryPort.java`
- Create: `src/main/java/borgol/core/ports/CafeRepositoryPort.java`
- Create: `src/main/java/borgol/core/ports/AchievementRepositoryPort.java`

- [ ] **Step 1: Create UserRepositoryPort**

```java
// src/main/java/borgol/core/ports/UserRepositoryPort.java
package borgol.core.ports;

import borgol.core.domain.Recipe;
import borgol.core.domain.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserRepositoryPort {
    Optional<User> findUserById(int id);
    Optional<User> findUserByEmail(String email);
    Optional<User> findUserByUsername(String username);
    User createUser(String username, String email, String passwordHash);
    void deleteUser(int id);
    void updateUser(int id, String bio, String avatarUrl, String expertiseLevel);
    List<String> getUserFlavorPrefs(int userId);
    void setUserFlavorPrefs(int userId, List<String> flavors);
    int getFollowerCount(int userId);
    int getFollowingCount(int userId);
    int getUserRecipeCount(int userId);
    boolean isFollowing(int followerId, int followingId);
    void followUser(int followerId, int followingId);
    void unfollowUser(int followerId, int followingId);
    List<User> searchUsers(String query);
    List<User> findAllUsers(int limit);
    List<User> getFollowingUsers(int userId);
    List<User> getFollowerUsers(int userId);
    void blockUser(int blockerId, int blockedId);
    void unblockUser(int blockerId, int blockedId);
    boolean isBlocked(int blockerId, int blockedId);
    void followHashtag(int userId, String tag);
    void unfollowHashtag(int userId, String tag);
    List<String> getUserHashtags(int userId);
    void createNotification(int userId, String type, int fromUserId, int contentId, String message);
    List<Map<String, Object>> getNotifications(int userId, int limit);
    void markNotificationsRead(int userId);
    int getUnreadNotificationCount(int userId);
}
```

- [ ] **Step 2: Create RecipeRepositoryPort**

```java
// src/main/java/borgol/core/ports/RecipeRepositoryPort.java
package borgol.core.ports;

import borgol.core.domain.BeanBag;
import borgol.core.domain.Equipment;
import borgol.core.domain.Recipe;
import borgol.core.domain.RecipeComment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RecipeRepositoryPort {
    List<Recipe> findAllRecipes(int currentUserId, String search, String drinkType, String sort);
    List<Recipe> getFeedRecipes(int userId, int limit);
    List<Recipe> getUserRecipes(int authorId, int currentUserId);
    Optional<Recipe> findRecipeById(int id, int currentUserId);
    Recipe createRecipe(Recipe r);
    Recipe updateRecipe(Recipe r);
    boolean deleteRecipe(int id, int userId);
    boolean likeRecipe(int userId, int recipeId);
    boolean unlikeRecipe(int userId, int recipeId);
    List<RecipeComment> findCommentsByRecipeId(int recipeId);
    RecipeComment addComment(int recipeId, int authorId, String content);
    List<Recipe> getLikedRecipes(int userId, int currentUserId);
    void saveRecipe(int userId, int recipeId);
    void unsaveRecipe(int userId, int recipeId);
    boolean isRecipeSaved(int userId, int recipeId);
    List<Recipe> getSavedRecipes(int userId, int currentUserId);
    List<Recipe> getRecipesByHashtag(int currentUserId, String tag);
    List<Map<String, Object>> getTrendingHashtags(int limit);
    void createReport(int reporterId, String contentType, int contentId, String reason, String description);
    List<Map<String, Object>> getAllReports(String status);
    void resolveReport(int reportId, int resolvedBy, String status);
    int getPendingReportCount();
    List<Map<String, Object>> getCollections(int userId);
    Map<String, Object> createCollection(int userId, String name, String description, boolean isPublic);
    void deleteCollection(int id, int userId);
    void addRecipeToCollection(int collectionId, int recipeId, int userId);
    void removeRecipeFromCollection(int collectionId, int recipeId, int userId);
    List<Map<String, Object>> getCollectionRecipes(int collectionId);
    List<Equipment> getEquipmentByUser(int userId);
    Equipment addEquipment(int userId, String category, String name, String brand, String notes);
    Optional<Equipment> getEquipmentById(int id);
    void deleteEquipment(int id, int userId);
}
```

- [ ] **Step 3: Create BrewGuideRepositoryPort**

```java
// src/main/java/borgol/core/ports/BrewGuideRepositoryPort.java
package borgol.core.ports;

import borgol.core.domain.BrewGuide;
import borgol.core.domain.LearnArticle;

import java.util.List;
import java.util.Optional;

public interface BrewGuideRepositoryPort {
    List<BrewGuide> findAllBrewGuides();
    Optional<BrewGuide> findBrewGuideById(int id);
    List<LearnArticle> findAllLearnArticles();
    Optional<LearnArticle> findLearnArticleById(int id);
    boolean isStaticContentSeeded();
    void seedBrewGuide(BrewGuide g);
    void seedLearnArticle(LearnArticle a);
    boolean isBeanArticlesSeeded();
    boolean isDrinkArticlesSeeded();
}
```

- [ ] **Step 4: Create JournalRepositoryPort**

```java
// src/main/java/borgol/core/ports/JournalRepositoryPort.java
package borgol.core.ports;

import borgol.core.domain.BeanBag;
import borgol.core.domain.BrewJournalEntry;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface JournalRepositoryPort {
    List<BrewJournalEntry> getJournalEntries(int userId);
    Optional<BrewJournalEntry> findJournalEntry(int id, int userId);
    BrewJournalEntry createJournalEntry(BrewJournalEntry e);
    BrewJournalEntry updateJournalEntry(BrewJournalEntry e);
    boolean deleteJournalEntry(int id, int userId);
    List<BeanBag> getBeanBags(int userId);
    BeanBag createBeanBag(BeanBag b);
    BeanBag updateBeanBag(BeanBag b);
    void deleteBeanBag(int id, int userId);
    Map<String, Object> getJournalStats(int userId);
}
```

- [ ] **Step 5: Create CafeRepositoryPort**

```java
// src/main/java/borgol/core/ports/CafeRepositoryPort.java
package borgol.core.ports;

import borgol.core.domain.CafeListing;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CafeRepositoryPort {
    List<CafeListing> findAllCafes(int currentUserId, String search, String district);
    Optional<CafeListing> findCafeById(int id, int currentUserId);
    CafeListing createCafe(CafeListing c);
    void updateCafeCoordinates(int cafeId, double lat, double lng);
    List<CafeListing> findCafesNearby(int currentUserId, double lat, double lng, double radiusKm);
    boolean rateCafe(int userId, int cafeId, int rating, String review);
    boolean isCafesSeeded();
    Map<String, Object> checkIn(int cafeId, int userId, String note);
    List<Map<String, Object>> getCheckins(int cafeId);
}
```

- [ ] **Step 6: Create AchievementRepositoryPort**

```java
// src/main/java/borgol/core/ports/AchievementRepositoryPort.java
package borgol.core.ports;

import java.util.List;
import java.util.Map;

public interface AchievementRepositoryPort {
    List<Map<String, Object>> getAchievements(int userId, Map<String, String[]> meta);
    List<String> checkAndAwardAchievements(int userId, Map<String, String[]> meta);
}
```

- [ ] **Step 7: Compile**

```bash
./mvnw compile -q
```
Expected: no output (success).

- [ ] **Step 8: Commit**

```bash
git add src/main/java/borgol/core/ports/UserRepositoryPort.java \
        src/main/java/borgol/core/ports/RecipeRepositoryPort.java \
        src/main/java/borgol/core/ports/BrewGuideRepositoryPort.java \
        src/main/java/borgol/core/ports/JournalRepositoryPort.java \
        src/main/java/borgol/core/ports/CafeRepositoryPort.java \
        src/main/java/borgol/core/ports/AchievementRepositoryPort.java
git commit -m "refactor: add domain repository port interfaces (hexagonal ports layer)"
```

---

## Task 2: SchemaInitializer

Extract the 350-line `initSchema()` method from `BorgolRepository` into its own class. `BorgolRepository` will call `SchemaInitializer` in its constructor until the full split is complete.

**Files:**
- Create: `src/main/java/borgol/infrastructure/persistence/SchemaInitializer.java`
- Modify: `src/main/java/borgol/infrastructure/persistence/BorgolRepository.java`

- [ ] **Step 1: Create SchemaInitializer**

```java
// src/main/java/borgol/infrastructure/persistence/SchemaInitializer.java
package borgol.infrastructure.persistence;

import borgol.infrastructure.config.DatabaseConnection;

import java.sql.Statement;

/**
 * Бүх хүснэгтийн бүтцийг нэг газарт үүсгэнэ.
 * Загвар: Single Responsibility — зөвхөн DDL үүрэгтэй.
 * Идемпотент: CREATE TABLE IF NOT EXISTS — хэдэн удаа дуудсан ч аюулгүй.
 */
public class SchemaInitializer {

    private final DatabaseConnection db;

    public SchemaInitializer(DatabaseConnection db) {
        this.db = db;
    }

    public void run() {
        try (Statement s = db.getConnection().createStatement()) {
            // ── paste the entire body of BorgolRepository.initSchema() here ──
            // Lines 41-358 of BorgolRepository.java (every s.execute(...) block)
            // Nothing changes except: remove the surrounding private void initSchema() {}
            // and replace conn() with db.getConnection()
        } catch (Exception e) {
            throw new RuntimeException("Schema init failed", e);
        }
    }
}
```

Copy the body of `initSchema()` from `BorgolRepository.java` lines 41–358 into the `run()` method above. Replace every `conn()` call with `db.getConnection()`.

- [ ] **Step 2: Update BorgolRepository constructor to use SchemaInitializer**

In `BorgolRepository.java`, replace the constructor body:
```java
// Before:
public BorgolRepository(DatabaseConnection db) {
    this.db = db;
    initSchema();
}

// After:
public BorgolRepository(DatabaseConnection db) {
    this.db = db;
    new SchemaInitializer(db).run();
}
```

Also delete the `private void initSchema()` method (lines 40–358) — it is now in `SchemaInitializer`.

- [ ] **Step 3: Compile**

```bash
./mvnw compile -q
```
Expected: no output.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/borgol/infrastructure/persistence/SchemaInitializer.java \
        src/main/java/borgol/infrastructure/persistence/BorgolRepository.java
git commit -m "refactor: extract SchemaInitializer from BorgolRepository (SRP)"
```

---

## Task 3: UserRepository

Extract all user/social/notification methods from `BorgolRepository` into `UserRepository` implementing `UserRepositoryPort`.

**Files:**
- Create: `src/main/java/borgol/infrastructure/persistence/UserRepository.java`

- [ ] **Step 1: Create UserRepository**

```java
// src/main/java/borgol/infrastructure/persistence/UserRepository.java
package borgol.infrastructure.persistence;

import borgol.core.domain.User;
import borgol.core.ports.UserRepositoryPort;
import borgol.infrastructure.config.DatabaseConnection;

import java.sql.*;
import java.util.*;

/**
 * Хэрэглэгч, нийгмийн граф, мэдэгдлийн SQL үйлдлүүд.
 * Загвар: Repository (GoF Data Access) — UserRepositoryPort-г хэрэгжүүлнэ.
 */
public class UserRepository implements UserRepositoryPort {

    private final DatabaseConnection db;

    public UserRepository(DatabaseConnection db) {
        this.db = db;
    }

    private Connection conn() { return db.getConnection(); }

    // ── paste methods from BorgolRepository.java ──────────────────────────────
    // findUserById          lines 380–391
    // findUserByEmail       lines 392–403
    // findUserByUsername    lines 404–414
    // createUser            lines 415–430
    // deleteUser            lines 431–438
    // updateUser            lines 439–460
    // getUserFlavorPrefs    lines 461–472
    // setUserFlavorPrefs    lines 473–493
    // getFollowerCount      lines 494–497
    // getFollowingCount     lines 498–501
    // getUserRecipeCount    lines 502–505
    // isFollowing           lines 506–516
    // followUser            lines 517–525
    // unfollowUser          lines 526–534
    // searchUsers           lines 535–550
    // findAllUsers          lines 1079–1095
    // getFollowingUsers     lines 1115–1131
    // getFollowerUsers      lines 1132–1150
    // blockUser             lines 1684–1699
    // unblockUser           lines 1700–1708
    // isBlocked             lines 1709–1715
    // followHashtag         lines 1716–1724
    // unfollowHashtag       lines 1725–1733
    // getUserHashtags       lines 1734–1745
    // createNotification    lines 1560–1575
    // getNotifications      lines 1576–1607
    // markNotificationsRead lines 1608–1615
    // getUnreadNotificationCount lines 1616–1621
    //
    // For each method: copy body verbatim. Replace conn() → conn() (same helper above).
    // No SQL changes needed.
}
```

- [ ] **Step 2: Compile**

```bash
./mvnw compile -q
```
Expected: no output.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/borgol/infrastructure/persistence/UserRepository.java
git commit -m "refactor: extract UserRepository from BorgolRepository (SRP)"
```

---

## Task 4: RecipeRepository

Extract all recipe/comment/like/save/collection/equipment/report methods.

**Files:**
- Create: `src/main/java/borgol/infrastructure/persistence/RecipeRepository.java`

- [ ] **Step 1: Create RecipeRepository**

```java
// src/main/java/borgol/infrastructure/persistence/RecipeRepository.java
package borgol.infrastructure.persistence;

import borgol.core.domain.*;
import borgol.core.ports.RecipeRepositoryPort;
import borgol.infrastructure.config.DatabaseConnection;

import java.sql.*;
import java.util.*;

/**
 * Жор, коммент, like, хадгалах, collections, тоног төхөөрөмж, мэдээлэл.
 * Загвар: Repository (GoF) — RecipeRepositoryPort хэрэгжүүлнэ.
 */
public class RecipeRepository implements RecipeRepositoryPort {

    private final DatabaseConnection db;

    public RecipeRepository(DatabaseConnection db) {
        this.db = db;
    }

    private Connection conn() { return db.getConnection(); }

    // ── paste methods from BorgolRepository.java ──────────────────────────────
    // findAllRecipes             lines 551–582
    // getFeedRecipes             lines 583–605
    // getUserRecipes             lines 606–623
    // findRecipeById             lines 624–639
    // createRecipe               lines 640–668
    // updateRecipe               lines 669–692
    // deleteRecipe               lines 693–701
    // likeRecipe                 lines 702–721
    // unlikeRecipe               lines 722–735
    // findCommentsByRecipeId     lines 736–753
    // addComment                 lines 754–791
    // getLikedRecipes            lines 1096–1114
    // saveRecipe                 lines 1512–1524
    // unsaveRecipe               lines 1525–1533
    // isRecipeSaved              lines 1534–1538
    // getSavedRecipes            lines 1539–1559
    // getRecipesByHashtag        lines 1746–1764
    // getTrendingHashtags        lines 1765–1789
    // createReport               lines 1622–1636
    // getAllReports              lines 1637–1664
    // resolveReport              lines 1665–1677
    // getPendingReportCount      lines 1678–1683
    // getCollections             lines 2030–2056
    // createCollection           lines 2057–2074
    // deleteCollection           lines 2075–2081
    // addRecipeToCollection      lines 2082–2095
    // removeRecipeFromCollection lines 2096–2109
    // getCollectionRecipes       lines 2110–2130
    // getEquipmentByUser         lines 1436–1447
    // addEquipment               lines 1448–1466
    // getEquipmentById           lines 1467–1477
    // deleteEquipment            lines 1478–1511
    //
    // Copy each method body verbatim. Replace conn() → conn() (helper above).
}
```

- [ ] **Step 2: Compile**

```bash
./mvnw compile -q
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/borgol/infrastructure/persistence/RecipeRepository.java
git commit -m "refactor: extract RecipeRepository from BorgolRepository (SRP)"
```

---

## Task 5: BrewGuideRepository

**Files:**
- Create: `src/main/java/borgol/infrastructure/persistence/BrewGuideRepository.java`

- [ ] **Step 1: Create BrewGuideRepository**

```java
// src/main/java/borgol/infrastructure/persistence/BrewGuideRepository.java
package borgol.infrastructure.persistence;

import borgol.core.domain.BrewGuide;
import borgol.core.domain.LearnArticle;
import borgol.core.ports.BrewGuideRepositoryPort;
import borgol.infrastructure.config.DatabaseConnection;

import java.sql.*;
import java.util.*;

/**
 * Хөрөнгийн удирдлага: дархалгааны заавар, суралцах нийтлэл, seed.
 * Загвар: Repository — BrewGuideRepositoryPort хэрэгжүүлнэ.
 */
public class BrewGuideRepository implements BrewGuideRepositoryPort {

    private final DatabaseConnection db;

    public BrewGuideRepository(DatabaseConnection db) {
        this.db = db;
    }

    private Connection conn() { return db.getConnection(); }

    // ── paste from BorgolRepository.java ─────────────────────────────────────
    // findAllBrewGuides      lines 1257–1267
    // findBrewGuideById      lines 1268–1280
    // findAllLearnArticles   lines 1281–1291
    // findLearnArticleById   lines 1292–1304
    // isStaticContentSeeded  lines 1305–1308
    // seedBrewGuide          lines 1309–1325
    // seedLearnArticle       lines 1326–1340
    // isBeanArticlesSeeded   lines 1341–1344
    // isCafesSeeded          lines 1345–1348  ← move here, CafeRepository will delegate
    // isDrinkArticlesSeeded  lines 1349–1352
    //
    // Note: isCafesSeeded is only used by BorgolService.seedEnrichedContent()
    // to guard cafe seeding — keep it here, CafeRepository does not need it.
}
```

- [ ] **Step 2: Compile**

```bash
./mvnw compile -q
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/borgol/infrastructure/persistence/BrewGuideRepository.java
git commit -m "refactor: extract BrewGuideRepository from BorgolRepository (SRP)"
```

---

## Task 6: JournalRepository

**Files:**
- Create: `src/main/java/borgol/infrastructure/persistence/JournalRepository.java`

- [ ] **Step 1: Create JournalRepository**

```java
// src/main/java/borgol/infrastructure/persistence/JournalRepository.java
package borgol.infrastructure.persistence;

import borgol.core.domain.BeanBag;
import borgol.core.domain.BrewJournalEntry;
import borgol.core.ports.JournalRepositoryPort;
import borgol.infrastructure.config.DatabaseConnection;

import java.sql.*;
import java.util.*;

/**
 * Дарлагын тэмдэглэл, шош уут, статистик.
 * Загвар: Repository — JournalRepositoryPort хэрэгжүүлнэ.
 */
public class JournalRepository implements JournalRepositoryPort {

    private final DatabaseConnection db;

    public JournalRepository(DatabaseConnection db) {
        this.db = db;
    }

    private Connection conn() { return db.getConnection(); }

    // ── paste from BorgolRepository.java ─────────────────────────────────────
    // getJournalEntries     lines 1151–1162
    // findJournalEntry      lines 1163–1173
    // createJournalEntry    lines 1174–1211
    // updateJournalEntry    lines 1212–1246
    // deleteJournalEntry    lines 1247–1256
    // getBeanBags           lines 1790–1801
    // createBeanBag         lines 1802–1825
    // updateBeanBag         lines 1826–1847
    // deleteBeanBag         lines 1848–1875
    // getJournalStats       lines 1876–1927
    //
    // Also copy the private helper: mapJournal() — used by getJournalEntries and findJournalEntry.
    // BorgolRepository still has its own copy of mapJournal() for the delegator period.
}
```

- [ ] **Step 2: Compile**

```bash
./mvnw compile -q
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/borgol/infrastructure/persistence/JournalRepository.java
git commit -m "refactor: extract JournalRepository from BorgolRepository (SRP)"
```

---

## Task 7: CafeRepository

**Files:**
- Create: `src/main/java/borgol/infrastructure/persistence/CafeRepository.java`

- [ ] **Step 1: Create CafeRepository**

```java
// src/main/java/borgol/infrastructure/persistence/CafeRepository.java
package borgol.infrastructure.persistence;

import borgol.core.domain.CafeListing;
import borgol.core.ports.CafeRepositoryPort;
import borgol.infrastructure.config.DatabaseConnection;

import java.sql.*;
import java.util.*;

/**
 * Кафе жагсаалт, үнэлгээ, check-in.
 * Загвар: Repository — CafeRepositoryPort хэрэгжүүлнэ.
 */
public class CafeRepository implements CafeRepositoryPort {

    private final DatabaseConnection db;

    public CafeRepository(DatabaseConnection db) {
        this.db = db;
    }

    private Connection conn() { return db.getConnection(); }

    // ── paste from BorgolRepository.java ─────────────────────────────────────
    // findAllCafes           lines 792–826
    // findCafeById           lines 827–847
    // createCafe             lines 848–875
    // updateCafeCoordinates  lines 876–886
    // findCafesNearby        lines 887–916
    // rateCafe               lines 917–1078  (includes mapCafe helper — copy it too)
    // isCafesSeeded          lines 1345–1348  ← duplicate from BrewGuideRepository
    //                        (keep both; BrewGuideRepository.isCafesSeeded guards seeding
    //                         in BorgolService; CafeRepository.isCafesSeeded implements port)
    // checkIn                lines 1928–1940
    // getCheckins            lines 1941–1964
    //
    // Also copy private helpers used by these methods (mapCafe, etc.).
}
```

- [ ] **Step 2: Compile**

```bash
./mvnw compile -q
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/borgol/infrastructure/persistence/CafeRepository.java
git commit -m "refactor: extract CafeRepository from BorgolRepository (SRP)"
```

---

## Task 8: AchievementRepository

**Files:**
- Create: `src/main/java/borgol/infrastructure/persistence/AchievementRepository.java`

- [ ] **Step 1: Create AchievementRepository**

```java
// src/main/java/borgol/infrastructure/persistence/AchievementRepository.java
package borgol.infrastructure.persistence;

import borgol.core.ports.AchievementRepositoryPort;
import borgol.infrastructure.config.DatabaseConnection;

import java.sql.*;
import java.util.*;

/**
 * Амжилтын badge-уудын SQL үйлдлүүд.
 * Загвар: Repository — AchievementRepositoryPort хэрэгжүүлнэ.
 */
public class AchievementRepository implements AchievementRepositoryPort {

    private final DatabaseConnection db;

    public AchievementRepository(DatabaseConnection db) {
        this.db = db;
    }

    private Connection conn() { return db.getConnection(); }

    // ── paste from BorgolRepository.java ─────────────────────────────────────
    // getAchievements            lines 1965–1982
    // checkAndAwardAchievements  lines 1983–2029
}
```

- [ ] **Step 2: Compile**

```bash
./mvnw compile -q
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/borgol/infrastructure/persistence/AchievementRepository.java
git commit -m "refactor: extract AchievementRepository from BorgolRepository (SRP)"
```

---

## Task 9: BorgolRepository Becomes a Delegator

`BorgolRepository` is now a thin wrapper that delegates every call to the appropriate domain repository. All existing callers (`BorgolService`) continue to work without modification. This is a pure mechanical replacement — no logic changes.

**Files:**
- Modify: `src/main/java/borgol/infrastructure/persistence/BorgolRepository.java`

- [ ] **Step 1: Replace BorgolRepository body**

Delete all method bodies from `BorgolRepository.java`. Replace the entire class with the delegator below. The constructor wires all six domain repositories.

```java
package borgol.infrastructure.persistence;

import borgol.core.domain.*;
import borgol.infrastructure.config.DatabaseConnection;

import java.sql.*;
import java.util.*;

/**
 * Fagade delegator — domain repository-уудыг нэгтгэнэ.
 * BorgolService (Desktop Facade) хуучин API-г дуудсаар байна.
 *
 * Загвар: Facade (GoF) — дотоод хуваагдлыг нуун, нэг интерфейс гаргана.
 */
public class BorgolRepository {

    // Domain repositories — injected via constructor
    final UserRepository      userRepo;
    final RecipeRepository    recipeRepo;
    final BrewGuideRepository brewGuideRepo;
    final JournalRepository   journalRepo;
    final CafeRepository      cafeRepo;
    final AchievementRepository achievementRepo;

    public BorgolRepository(DatabaseConnection db) {
        new SchemaInitializer(db).run();
        this.userRepo        = new UserRepository(db);
        this.recipeRepo      = new RecipeRepository(db);
        this.brewGuideRepo   = new BrewGuideRepository(db);
        this.journalRepo     = new JournalRepository(db);
        this.cafeRepo        = new CafeRepository(db);
        this.achievementRepo = new AchievementRepository(db);
    }

    // ── User ─────────────────────────────────────────────────────────────────
    public Optional<User> findUserById(int id)                          { return userRepo.findUserById(id); }
    public Optional<User> findUserByEmail(String email)                 { return userRepo.findUserByEmail(email); }
    public Optional<User> findUserByUsername(String username)           { return userRepo.findUserByUsername(username); }
    public User createUser(String u, String e, String p)                { return userRepo.createUser(u, e, p); }
    public void deleteUser(int id)                                      { userRepo.deleteUser(id); }
    public void updateUser(int id, String bio, String av, String exp)   { userRepo.updateUser(id, bio, av, exp); }
    public List<String> getUserFlavorPrefs(int uid)                     { return userRepo.getUserFlavorPrefs(uid); }
    public void setUserFlavorPrefs(int uid, List<String> f)             { userRepo.setUserFlavorPrefs(uid, f); }
    public int getFollowerCount(int uid)                                 { return userRepo.getFollowerCount(uid); }
    public int getFollowingCount(int uid)                                { return userRepo.getFollowingCount(uid); }
    public int getUserRecipeCount(int uid)                               { return userRepo.getUserRecipeCount(uid); }
    public boolean isFollowing(int a, int b)                            { return userRepo.isFollowing(a, b); }
    public void followUser(int a, int b)                                { userRepo.followUser(a, b); }
    public void unfollowUser(int a, int b)                              { userRepo.unfollowUser(a, b); }
    public List<User> searchUsers(String q)                             { return userRepo.searchUsers(q); }
    public List<User> findAllUsers(int limit)                           { return userRepo.findAllUsers(limit); }
    public List<User> getFollowingUsers(int uid)                        { return userRepo.getFollowingUsers(uid); }
    public List<User> getFollowerUsers(int uid)                         { return userRepo.getFollowerUsers(uid); }
    public void blockUser(int a, int b)                                 { userRepo.blockUser(a, b); }
    public void unblockUser(int a, int b)                               { userRepo.unblockUser(a, b); }
    public boolean isBlocked(int a, int b)                              { return userRepo.isBlocked(a, b); }
    public void followHashtag(int uid, String tag)                      { userRepo.followHashtag(uid, tag); }
    public void unfollowHashtag(int uid, String tag)                    { userRepo.unfollowHashtag(uid, tag); }
    public List<String> getUserHashtags(int uid)                        { return userRepo.getUserHashtags(uid); }
    public void createNotification(int u, String t, int f, int c, String m) { userRepo.createNotification(u, t, f, c, m); }
    public List<Map<String,Object>> getNotifications(int uid, int lim)  { return userRepo.getNotifications(uid, lim); }
    public void markNotificationsRead(int uid)                          { userRepo.markNotificationsRead(uid); }
    public int getUnreadNotificationCount(int uid)                      { return userRepo.getUnreadNotificationCount(uid); }

    // ── Recipe ────────────────────────────────────────────────────────────────
    public List<Recipe> findAllRecipes(int uid, String s, String d, String so) { return recipeRepo.findAllRecipes(uid, s, d, so); }
    public List<Recipe> getFeedRecipes(int uid, int lim)                { return recipeRepo.getFeedRecipes(uid, lim); }
    public List<Recipe> getUserRecipes(int aid, int uid)                { return recipeRepo.getUserRecipes(aid, uid); }
    public Optional<Recipe> findRecipeById(int id, int uid)             { return recipeRepo.findRecipeById(id, uid); }
    public Recipe createRecipe(Recipe r)                                { return recipeRepo.createRecipe(r); }
    public Recipe updateRecipe(Recipe r)                                { return recipeRepo.updateRecipe(r); }
    public boolean deleteRecipe(int id, int uid)                        { return recipeRepo.deleteRecipe(id, uid); }
    public boolean likeRecipe(int uid, int rid)                         { return recipeRepo.likeRecipe(uid, rid); }
    public boolean unlikeRecipe(int uid, int rid)                       { return recipeRepo.unlikeRecipe(uid, rid); }
    public List<RecipeComment> findCommentsByRecipeId(int rid)          { return recipeRepo.findCommentsByRecipeId(rid); }
    public RecipeComment addComment(int rid, int aid, String c)         { return recipeRepo.addComment(rid, aid, c); }
    public List<Recipe> getLikedRecipes(int uid, int cuid)              { return recipeRepo.getLikedRecipes(uid, cuid); }
    public void saveRecipe(int uid, int rid)                            { recipeRepo.saveRecipe(uid, rid); }
    public void unsaveRecipe(int uid, int rid)                          { recipeRepo.unsaveRecipe(uid, rid); }
    public boolean isRecipeSaved(int uid, int rid)                      { return recipeRepo.isRecipeSaved(uid, rid); }
    public List<Recipe> getSavedRecipes(int uid, int cuid)              { return recipeRepo.getSavedRecipes(uid, cuid); }
    public List<Recipe> getRecipesByHashtag(int cuid, String tag)       { return recipeRepo.getRecipesByHashtag(cuid, tag); }
    public List<Map<String,Object>> getTrendingHashtags(int lim)        { return recipeRepo.getTrendingHashtags(lim); }
    public void createReport(int r, String ct, int ci, String re, String d) { recipeRepo.createReport(r, ct, ci, re, d); }
    public List<Map<String,Object>> getAllReports(String status)         { return recipeRepo.getAllReports(status); }
    public void resolveReport(int rid, int by, String s)                { recipeRepo.resolveReport(rid, by, s); }
    public int getPendingReportCount()                                   { return recipeRepo.getPendingReportCount(); }
    public List<Map<String,Object>> getCollections(int uid)             { return recipeRepo.getCollections(uid); }
    public Map<String,Object> createCollection(int uid, String n, String d, boolean p) { return recipeRepo.createCollection(uid, n, d, p); }
    public void deleteCollection(int id, int uid)                       { recipeRepo.deleteCollection(id, uid); }
    public void addRecipeToCollection(int cid, int rid, int uid)        { recipeRepo.addRecipeToCollection(cid, rid, uid); }
    public void removeRecipeFromCollection(int cid, int rid, int uid)   { recipeRepo.removeRecipeFromCollection(cid, rid, uid); }
    public List<Map<String,Object>> getCollectionRecipes(int cid)       { return recipeRepo.getCollectionRecipes(cid); }
    public List<Equipment> getEquipmentByUser(int uid)                  { return recipeRepo.getEquipmentByUser(uid); }
    public Equipment addEquipment(int uid, String cat, String n, String b, String no) { return recipeRepo.addEquipment(uid, cat, n, b, no); }
    public Optional<Equipment> getEquipmentById(int id)                 { return recipeRepo.getEquipmentById(id); }
    public void deleteEquipment(int id, int uid)                        { recipeRepo.deleteEquipment(id, uid); }

    // ── BrewGuide ─────────────────────────────────────────────────────────────
    public List<BrewGuide> findAllBrewGuides()                          { return brewGuideRepo.findAllBrewGuides(); }
    public Optional<BrewGuide> findBrewGuideById(int id)                { return brewGuideRepo.findBrewGuideById(id); }
    public List<LearnArticle> findAllLearnArticles()                    { return brewGuideRepo.findAllLearnArticles(); }
    public Optional<LearnArticle> findLearnArticleById(int id)          { return brewGuideRepo.findLearnArticleById(id); }
    public boolean isStaticContentSeeded()                              { return brewGuideRepo.isStaticContentSeeded(); }
    public void seedBrewGuide(BrewGuide g)                              { brewGuideRepo.seedBrewGuide(g); }
    public void seedLearnArticle(LearnArticle a)                        { brewGuideRepo.seedLearnArticle(a); }
    public boolean isBeanArticlesSeeded()                               { return brewGuideRepo.isBeanArticlesSeeded(); }
    public boolean isCafesSeeded()                                      { return brewGuideRepo.isCafesSeeded(); }
    public boolean isDrinkArticlesSeeded()                              { return brewGuideRepo.isDrinkArticlesSeeded(); }

    // ── Journal ───────────────────────────────────────────────────────────────
    public List<BrewJournalEntry> getJournalEntries(int uid)            { return journalRepo.getJournalEntries(uid); }
    public Optional<BrewJournalEntry> findJournalEntry(int id, int uid) { return journalRepo.findJournalEntry(id, uid); }
    public BrewJournalEntry createJournalEntry(BrewJournalEntry e)      { return journalRepo.createJournalEntry(e); }
    public BrewJournalEntry updateJournalEntry(BrewJournalEntry e)      { return journalRepo.updateJournalEntry(e); }
    public boolean deleteJournalEntry(int id, int uid)                  { return journalRepo.deleteJournalEntry(id, uid); }
    public List<BeanBag> getBeanBags(int uid)                           { return journalRepo.getBeanBags(uid); }
    public BeanBag createBeanBag(BeanBag b)                             { return journalRepo.createBeanBag(b); }
    public BeanBag updateBeanBag(BeanBag b)                             { return journalRepo.updateBeanBag(b); }
    public void deleteBeanBag(int id, int uid)                          { journalRepo.deleteBeanBag(id, uid); }
    public Map<String,Object> getJournalStats(int uid)                  { return journalRepo.getJournalStats(uid); }

    // ── Cafe ──────────────────────────────────────────────────────────────────
    public List<CafeListing> findAllCafes(int uid, String s, String d)  { return cafeRepo.findAllCafes(uid, s, d); }
    public Optional<CafeListing> findCafeById(int id, int uid)          { return cafeRepo.findCafeById(id, uid); }
    public CafeListing createCafe(CafeListing c)                        { return cafeRepo.createCafe(c); }
    public void updateCafeCoordinates(int id, double lat, double lng)   { cafeRepo.updateCafeCoordinates(id, lat, lng); }
    public List<CafeListing> findCafesNearby(int uid, double la, double lo, double r) { return cafeRepo.findCafesNearby(uid, la, lo, r); }
    public boolean rateCafe(int uid, int cid, int r, String rev)        { return cafeRepo.rateCafe(uid, cid, r, rev); }
    public Map<String,Object> checkIn(int cid, int uid, String note)    { return cafeRepo.checkIn(cid, uid, note); }
    public List<Map<String,Object>> getCheckins(int cid)                { return cafeRepo.getCheckins(cid); }

    // ── Achievement ───────────────────────────────────────────────────────────
    public List<Map<String,Object>> getAchievements(int uid, Map<String,String[]> meta) { return achievementRepo.getAchievements(uid, meta); }
    public List<String> checkAndAwardAchievements(int uid, Map<String,String[]> meta)   { return achievementRepo.checkAndAwardAchievements(uid, meta); }
}
```

- [ ] **Step 2: Compile**

```bash
./mvnw compile -q
```
Expected: no output. If missing methods appear, add the delegation stubs.

- [ ] **Step 3: Run existing tests**

```bash
./mvnw test -q
```
Expected: `MenuServiceTest` passes (5/5). BorgolRepository is not tested directly — compile success is sufficient.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/borgol/infrastructure/persistence/BorgolRepository.java
git commit -m "refactor: BorgolRepository becomes thin delegator (Facade pattern)"
```

---

## Task 10: Domain Services

Create all six domain service classes. Each receives only the port interface(s) it needs (ISP). Add unit tests for the two services with non-trivial logic: `JournalService` and `AchievementService`.

**Files:**
- Create: `src/main/java/borgol/core/application/UserService.java`
- Create: `src/main/java/borgol/core/application/RecipeService.java`
- Create: `src/main/java/borgol/core/application/BrewGuideService.java`
- Create: `src/main/java/borgol/core/application/JournalService.java`
- Create: `src/main/java/borgol/core/application/CafeService.java`
- Create: `src/main/java/borgol/core/application/AchievementService.java`
- Create: `src/test/java/borgol/JournalServiceTest.java`
- Create: `src/test/java/borgol/AchievementServiceTest.java`

- [ ] **Step 1: Write JournalServiceTest (failing)**

```java
// src/test/java/borgol/JournalServiceTest.java
package borgol;

import borgol.core.application.JournalService;
import borgol.core.domain.BeanBag;
import borgol.core.domain.BrewJournalEntry;
import borgol.core.ports.AchievementRepositoryPort;
import borgol.core.ports.JournalRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class JournalServiceTest {

    // Minimal stub — returns empty lists, records calls
    static class StubJournalRepo implements JournalRepositoryPort {
        BrewJournalEntry lastCreated;
        @Override public List<BrewJournalEntry> getJournalEntries(int uid) { return List.of(); }
        @Override public Optional<BrewJournalEntry> findJournalEntry(int id, int uid) { return Optional.empty(); }
        @Override public BrewJournalEntry createJournalEntry(BrewJournalEntry e) { lastCreated = e; return e; }
        @Override public BrewJournalEntry updateJournalEntry(BrewJournalEntry e) { return e; }
        @Override public boolean deleteJournalEntry(int id, int uid) { return true; }
        @Override public List<BeanBag> getBeanBags(int uid) { return List.of(); }
        @Override public BeanBag createBeanBag(BeanBag b) { return b; }
        @Override public BeanBag updateBeanBag(BeanBag b) { return b; }
        @Override public void deleteBeanBag(int id, int uid) {}
        @Override public Map<String, Object> getJournalStats(int uid) { return Map.of(); }
    }

    static class StubAchievementRepo implements AchievementRepositoryPort {
        @Override public List<Map<String,Object>> getAchievements(int uid, Map<String,String[]> meta) { return List.of(); }
        @Override public List<String> checkAndAwardAchievements(int uid, Map<String,String[]> meta) { return List.of(); }
    }

    private JournalService svc;
    private StubJournalRepo repo;

    @BeforeEach void setUp() {
        repo = new StubJournalRepo();
        svc  = new JournalService(repo, new AchievementService(new StubAchievementRepo()));
    }

    @Test void weatherData_truncatedAt300Chars() {
        String longWeather = "x".repeat(500);
        svc.createJournalEntry(1, "Ethiopia Yirgacheffe", "pour_over",
                "18g", "300ml", 93, 4, "notes", longWeather);
        assertNotNull(repo.lastCreated);
        assertTrue(repo.lastCreated.getWeatherData().length() <= 300,
                "weatherData must be capped at 300 chars");
    }

    @Test void weatherData_null_storedAsEmpty() {
        svc.createJournalEntry(1, "Sumatra", "espresso",
                "18g", "40ml", 92, 5, "bold", null);
        assertEquals("", repo.lastCreated.getWeatherData(),
                "null weatherData should be stored as empty string");
    }
}
```

- [ ] **Step 2: Run test — expect compile failure (JournalService not yet created)**

```bash
./mvnw test -pl . -Dtest=JournalServiceTest -q 2>&1 | head -5
```
Expected: compilation error — `cannot find symbol: class JournalService`.

- [ ] **Step 3: Create AchievementService**

```java
// src/main/java/borgol/core/application/AchievementService.java
package borgol.core.application;

import borgol.core.ports.AchievementRepositoryPort;

import java.util.List;
import java.util.Map;

/**
 * Badge logic. BADGE_META нь энд хадгалагдана — DB schema биш.
 * Загвар: SRP — зөвхөн achievement бизнес дүрэм.
 */
public class AchievementService {

    // Badge metadata: id → [label, description, icon]
    // Mirrors BorgolService.BADGE_META — copied exactly
    public static final Map<String, String[]> BADGE_META = Map.of(
        "first_brew",      new String[]{"First Brew",      "Logged your first brew",           "☕"},
        "ten_brews",       new String[]{"Dedicated Brewer","Logged 10 brews",                  "🏅"},
        "first_cafe",      new String[]{"Explorer",        "Checked in to your first cafe",    "📍"},
        "five_cafes",      new String[]{"Cafe Hopper",     "Checked in to 5 different cafes",  "🗺️"},
        "first_recipe",    new String[]{"Recipe Creator",  "Created your first recipe",        "📝"},
        "ten_likes",       new String[]{"Popular",         "Received 10 likes on a recipe",    "❤️"},
        "first_collection",new String[]{"Collector",       "Created your first collection",    "📚"},
        "bean_tracker",    new String[]{"Bean Tracker",    "Added your first bean bag",        "🫘"}
    );

    private final AchievementRepositoryPort repo;

    public AchievementService(AchievementRepositoryPort repo) {
        this.repo = repo;
    }

    public List<Map<String, Object>> getAchievements(int userId) {
        return repo.getAchievements(userId, BADGE_META);
    }

    public List<String> checkAndAwardAchievements(int userId) {
        return repo.checkAndAwardAchievements(userId, BADGE_META);
    }
}
```

- [ ] **Step 4: Create JournalService**

```java
// src/main/java/borgol/core/application/JournalService.java
package borgol.core.application;

import borgol.core.domain.BeanBag;
import borgol.core.domain.BrewJournalEntry;
import borgol.core.ports.JournalRepositoryPort;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Дарлагын тэмдэглэл, шош уут, статистикийн бизнес логик.
 * Загвар: SRP — зөвхөн journal domain.
 */
public class JournalService {

    private final JournalRepositoryPort repo;
    private final AchievementService    achievements;

    public JournalService(JournalRepositoryPort repo, AchievementService achievements) {
        this.repo         = repo;
        this.achievements = achievements;
    }

    public List<BrewJournalEntry> getJournalEntries(int userId) {
        return repo.getJournalEntries(userId);
    }

    public Optional<BrewJournalEntry> findJournalEntry(int id, int userId) {
        return repo.findJournalEntry(id, userId);
    }

    public BrewJournalEntry createJournalEntry(
            int userId, String beanOrigin, String brewMethod,
            String doseGrams, String yieldMl, int tempC,
            int rating, String notes, String weatherData) {

        BrewJournalEntry e = new BrewJournalEntry();
        e.setUserId(userId);
        e.setBeanOrigin(beanOrigin);
        e.setBrewMethod(brewMethod);
        e.setDoseGrams(doseGrams);
        e.setYieldMl(yieldMl);
        e.setTempC(tempC);
        e.setRating(rating);
        e.setNotes(notes != null ? notes : "");
        // Guard: VARCHAR(300) column — truncate before insert
        e.setWeatherData(weatherData != null
                ? weatherData.substring(0, Math.min(weatherData.length(), 300))
                : "");

        BrewJournalEntry saved = repo.createJournalEntry(e);
        achievements.checkAndAwardAchievements(userId);
        return saved;
    }

    public BrewJournalEntry updateJournalEntry(
            int id, int userId, String beanOrigin, String brewMethod,
            String doseGrams, String yieldMl, int tempC,
            int rating, String notes, String weatherData) {

        BrewJournalEntry e = new BrewJournalEntry();
        e.setId(id);
        e.setUserId(userId);
        e.setBeanOrigin(beanOrigin);
        e.setBrewMethod(brewMethod);
        e.setDoseGrams(doseGrams);
        e.setYieldMl(yieldMl);
        e.setTempC(tempC);
        e.setRating(rating);
        e.setNotes(notes != null ? notes : "");
        e.setWeatherData(weatherData != null
                ? weatherData.substring(0, Math.min(weatherData.length(), 300))
                : "");
        return repo.updateJournalEntry(e);
    }

    public boolean deleteJournalEntry(int id, int userId) {
        return repo.deleteJournalEntry(id, userId);
    }

    public List<BeanBag> getBeanBags(int userId)      { return repo.getBeanBags(userId); }
    public BeanBag createBeanBag(BeanBag b)           { achievements.checkAndAwardAchievements(b.getUserId()); return repo.createBeanBag(b); }
    public BeanBag updateBeanBag(BeanBag b)           { return repo.updateBeanBag(b); }
    public void deleteBeanBag(int id, int userId)     { repo.deleteBeanBag(id, userId); }
    public Map<String, Object> getJournalStats(int u) { return repo.getJournalStats(u); }
}
```

- [ ] **Step 5: Run JournalServiceTest — expect pass**

```bash
./mvnw test -Dtest=JournalServiceTest -q
```
Expected: `Tests run: 2, Failures: 0, Errors: 0`.

- [ ] **Step 6: Create remaining domain services**

Create `UserService.java`:
```java
// src/main/java/borgol/core/application/UserService.java
package borgol.core.application;

import borgol.core.domain.User;
import borgol.core.ports.UserRepositoryPort;
import borgol.infrastructure.security.JwtUtil;
import borgol.infrastructure.security.PasswordUtil;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Хэрэглэгч бүртгэл, нэвтрэх, профайл, нийгмийн граф.
 * JwtUtil + PasswordUtil-г directly дуудна (security layer).
 */
public class UserService {

    private final UserRepositoryPort repo;

    public UserService(UserRepositoryPort repo) {
        this.repo = repo;
    }

    // Delegate every method from BorgolService that touches only UserRepositoryPort.
    // Methods to extract from BorgolService.java (copy body, change repo.xxx → repo.xxx):
    //   register(username, email, password)     — hash password, createUser, return AuthResult
    //   login(email, password)                  — findByEmail, verify hash, issue JWT
    //   getMe(userId)                           — findUserById, build profile map
    //   updateProfile(...)                      — updateUser + setUserFlavorPrefs
    //   followUser(followerId, followingId)     — followUser
    //   unfollowUser(followerId, followingId)   — unfollowUser
    //   searchUsers(query)                      — searchUsers
    //   getNotifications(userId, limit)         — getNotifications
    //   markNotificationsRead(userId)           — markNotificationsRead
    //   getUnreadNotificationCount(userId)      — getUnreadNotificationCount
    //   blockUser / unblockUser / isBlocked
    //   followHashtag / unfollowHashtag / getUserHashtags
    //
    // Copy each method body from BorgolService verbatim.
    // Change: this.repo.xxx → repo.xxx
}
```

Create `RecipeService.java`:
```java
// src/main/java/borgol/core/application/RecipeService.java
package borgol.core.application;

import borgol.core.domain.*;
import borgol.core.ports.RecipeRepositoryPort;
import borgol.core.ports.UserRepositoryPort;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Жор, коммент, like, хадгалах, коллекц, тоног төхөөрөмж, report.
 * UserRepositoryPort-г хүлээн авна: createNotification дуудахад хэрэгтэй.
 */
public class RecipeService {

    private final RecipeRepositoryPort recipeRepo;
    private final UserRepositoryPort   userRepo;    // notification side-effect
    private final AchievementService   achievements;

    public RecipeService(RecipeRepositoryPort recipeRepo,
                         UserRepositoryPort userRepo,
                         AchievementService achievements) {
        this.recipeRepo   = recipeRepo;
        this.userRepo     = userRepo;
        this.achievements = achievements;
    }

    // Extract from BorgolService.java all methods touching recipeRepo:
    //   findAllRecipes, getFeedRecipes, getUserRecipes, findRecipeById
    //   createRecipe, updateRecipe, deleteRecipe
    //   toggleLike (calls likeRecipe/unlikeRecipe + createNotification)
    //   addComment  (calls addComment + createNotification)
    //   getLikedRecipes, saveRecipe, unsaveRecipe, isRecipeSaved, getSavedRecipes
    //   getRecipesByHashtag, getTrendingHashtags
    //   createReport, getAllReports, resolveReport, getPendingReportCount
    //   getCollections, createCollection, deleteCollection
    //   addRecipeToCollection, removeRecipeFromCollection, getCollectionRecipes
    //   getEquipmentByUser, addEquipment, getEquipmentById, deleteEquipment
    //
    // Copy method bodies verbatim. Replace:
    //   this.repo.findAllRecipes  → recipeRepo.findAllRecipes
    //   this.repo.createNotification → userRepo.createNotification
    //   this.repo.checkAndAwardAchievements → achievements.checkAndAwardAchievements
}
```

Create `BrewGuideService.java`:
```java
// src/main/java/borgol/core/application/BrewGuideService.java
package borgol.core.application;

import borgol.core.domain.BrewGuide;
import borgol.core.domain.LearnArticle;
import borgol.core.ports.BrewGuideRepositoryPort;

import java.util.List;
import java.util.Optional;

/**
 * Дарлагын заавар, суралцах нийтлэл, seed үйлдлүүд.
 */
public class BrewGuideService {

    private final BrewGuideRepositoryPort repo;

    public BrewGuideService(BrewGuideRepositoryPort repo) {
        this.repo = repo;
    }

    // Extract from BorgolService.java:
    //   getBrewGuides()             → repo.findAllBrewGuides()
    //   getBrewGuide(id)            → repo.findBrewGuideById(id)
    //   getLearnArticles()          → repo.findAllLearnArticles()
    //   getLearnArticle(id)         → repo.findLearnArticleById(id)
    //   seedStaticContent()         → guards + seedBrewGuide/seedLearnArticle
    //   seedEnrichedContent()       → isBeanArticlesSeeded / isDrinkArticlesSeeded guards
    //
    // Copy method bodies verbatim. Replace this.repo → repo.
}
```

Create `CafeService.java`:
```java
// src/main/java/borgol/core/application/CafeService.java
package borgol.core.application;

import borgol.core.domain.CafeListing;
import borgol.core.ports.CafeRepositoryPort;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Кафе жагсаалт, үнэлгээ, check-in.
 */
public class CafeService {

    private final CafeRepositoryPort   repo;
    private final AchievementService   achievements;

    public CafeService(CafeRepositoryPort repo, AchievementService achievements) {
        this.repo         = repo;
        this.achievements = achievements;
    }

    // Extract from BorgolService.java:
    //   getCafes(userId, search, district)        → repo.findAllCafes
    //   getCafe(id, userId)                       → repo.findCafeById
    //   createCafe(...)                           → repo.createCafe
    //   updateCafeCoordinates(id, lat, lng)       → repo.updateCafeCoordinates
    //   findCafesNearby(userId, lat, lng, km)     → repo.findCafesNearby
    //   rateCafe(userId, cafeId, rating, review)  → repo.rateCafe
    //   checkIn(cafeId, userId, note)             → repo.checkIn + achievements.check
    //   getCheckins(cafeId)                       → repo.getCheckins
    //
    // Copy method bodies verbatim. Replace this.repo → repo.
    // checkIn must call achievements.checkAndAwardAchievements(userId) after repo.checkIn.
}
```

- [ ] **Step 7: Compile**

```bash
./mvnw compile -q
```
Expected: no output.

- [ ] **Step 8: Run all tests**

```bash
./mvnw test -q
```
Expected: `MenuServiceTest` (5 tests) + `JournalServiceTest` (2 tests) = 7 passed.

- [ ] **Step 9: Commit**

```bash
git add src/main/java/borgol/core/application/ src/test/java/borgol/JournalServiceTest.java
git commit -m "refactor: extract domain services (SRP, ISP) — JournalService, AchievementService, UserService, RecipeService, BrewGuideService, CafeService"
```

---

## Task 11: Update BorgolService as Facade

`BorgolService` now delegates to the six domain services. No logic remains in `BorgolService` itself — pure delegation. The desktop layer (`BorgolApp`, `JournalPane`) and `Main.java` call `BorgolService` unchanged.

**Files:**
- Modify: `src/main/java/borgol/core/application/BorgolService.java`

- [ ] **Step 1: Replace BorgolService body with delegator**

Keep the existing constructor signature (`BorgolRepository repo, RedisEventBus eventBus`) so `Main.java` requires no change yet. Wire domain services internally.

```java
package borgol.core.application;

import borgol.core.domain.*;
import borgol.infrastructure.messaging.RedisEventBus;
import borgol.infrastructure.persistence.BorgolRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Facade — desktop (JavaFX) болон seed code-д зориулсан нэгдсэн API.
 *
 * Загвар: Facade (GoF) — зургаан domain service-г нэг интерфейсийн цаана нуун,
 * хуучин дуудлагуудыг өөрчлөлтгүй ажиллуулна.
 *
 * SOLID — OCP: шинэ функционал нэмэхдээ зөвхөн холбогдох domain service-г
 * өөрчилнө; энэ Facade нь хаалттай.
 */
public class BorgolService {

    private final UserService      userService;
    private final RecipeService    recipeService;
    private final BrewGuideService brewGuideService;
    private final JournalService   journalService;
    private final CafeService      cafeService;
    private final AchievementService achievementService;

    // Keep original constructor signature — Main.java unchanged
    public BorgolService(BorgolRepository repo, RedisEventBus eventBus) {
        AchievementService ach = new AchievementService(repo.achievementRepo);
        this.achievementService = ach;
        this.userService        = new UserService(repo.userRepo);
        this.recipeService      = new RecipeService(repo.recipeRepo, repo.userRepo, ach);
        this.brewGuideService   = new BrewGuideService(repo.brewGuideRepo);
        this.journalService     = new JournalService(repo.journalRepo, ach);
        this.cafeService        = new CafeService(repo.cafeRepo, ach);
    }

    // ── Delegate every public method to the appropriate domain service ─────────
    // Copy the method signature from the old BorgolService,
    // replace the body with a single-line delegation.
    //
    // Examples:
    public AuthResult register(String username, String email, String password) {
        return userService.register(username, email, password);
    }
    public AuthResult login(String email, String password) {
        return userService.login(email, password);
    }
    public Map<String, Object> getMe(int userId) {
        return userService.getMe(userId);
    }
    // ... repeat for every method in the original BorgolService
    // The pattern is always: return <domainService>.<methodName>(<args>);
    // or for void: <domainService>.<methodName>(<args>);
}
```

- [ ] **Step 2: Compile**

```bash
./mvnw compile -q
```
Expected: no output. Fix any missing delegation stubs until clean.

- [ ] **Step 3: Run tests**

```bash
./mvnw test -q
```
Expected: 7 tests pass.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/borgol/core/application/BorgolService.java
git commit -m "refactor: BorgolService becomes Facade over domain services (Facade pattern)"
```

---

## Task 12: DTO Package

Extract request POJOs from `BorgolApiServer.java` into `ui/web/dto/`.

**Files:**
- Create: `src/main/java/borgol/ui/web/dto/JournalReq.java`
- Create: `src/main/java/borgol/ui/web/dto/CheckinReq.java`
- Create: `src/main/java/borgol/ui/web/dto/CollectionReq.java`
- Create: `src/main/java/borgol/ui/web/dto/CollectionRecipeReq.java`

- [ ] **Step 1: Create DTO classes**

```java
// src/main/java/borgol/ui/web/dto/JournalReq.java
package borgol.ui.web.dto;

public class JournalReq {
    public String beanOrigin = "";
    public String brewMethod = "";
    public String doseGrams  = "";
    public String yieldMl    = "";
    public int    tempC      = 93;
    public int    rating     = 3;
    public String notes      = "";
    public String weatherData = "";
}
```

```java
// src/main/java/borgol/ui/web/dto/CheckinReq.java
package borgol.ui.web.dto;

public class CheckinReq {
    public int    cafeId = 0;
    public String note   = "";
}
```

```java
// src/main/java/borgol/ui/web/dto/CollectionReq.java
package borgol.ui.web.dto;

public class CollectionReq {
    public String  name        = "";
    public String  description = "";
    public boolean isPublic    = true;
}
```

```java
// src/main/java/borgol/ui/web/dto/CollectionRecipeReq.java
package borgol.ui.web.dto;

public class CollectionRecipeReq {
    public int recipeId = 0;
}
```

- [ ] **Step 2: Update BorgolApiServer imports**

In `BorgolApiServer.java`, find the inner static classes `JournalReq`, `CheckinReq`, `CollectionReq`, `CollectionRecipeReq` and delete them. Add imports:

```java
import borgol.ui.web.dto.JournalReq;
import borgol.ui.web.dto.CheckinReq;
import borgol.ui.web.dto.CollectionReq;
import borgol.ui.web.dto.CollectionRecipeReq;
```

- [ ] **Step 3: Compile**

```bash
./mvnw compile -q
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/borgol/ui/web/dto/ \
        src/main/java/borgol/ui/web/BorgolApiServer.java
git commit -m "refactor: extract DTO classes to ui/web/dto package"
```

---

## Task 13: Domain Routers

Create one router per domain. Each router receives its domain service and registers its own routes on `Javalin`.

**Files:**
- Create: `src/main/java/borgol/ui/web/routers/UserRouter.java`
- Create: `src/main/java/borgol/ui/web/routers/RecipeRouter.java`
- Create: `src/main/java/borgol/ui/web/routers/BrewGuideRouter.java`
- Create: `src/main/java/borgol/ui/web/routers/JournalRouter.java`
- Create: `src/main/java/borgol/ui/web/routers/CafeRouter.java`
- Create: `src/main/java/borgol/ui/web/routers/AchievementRouter.java`

- [ ] **Step 1: Create UserRouter**

```java
// src/main/java/borgol/ui/web/routers/UserRouter.java
package borgol.ui.web.routers;

import borgol.core.application.UserService;
import io.javalin.Javalin;
import io.javalin.http.Context;

/**
 * /api/auth/*, /api/users/* — Хэрэглэгчтэй холбоотой бүх route.
 * Загвар: SRP — зөвхөн user HTTP mapping.
 */
public class UserRouter {

    private final UserService svc;

    public UserRouter(UserService svc) {
        this.svc = svc;
    }

    public void register(Javalin app) {
        app.post("/api/auth/register", this::register);
        app.post("/api/auth/login",    this::login);
        app.get ("/api/auth/me",       this::me);
        app.get ("/api/users/{id}",    this::getUser);
        app.put ("/api/users/me",      this::updateProfile);
        app.post("/api/users/{id}/follow",    this::follow);
        app.delete("/api/users/{id}/follow",  this::unfollow);
        app.get ("/api/users/search",         this::search);
        app.get ("/api/notifications",        this::getNotifications);
        app.post("/api/notifications/read",   this::markRead);
        app.get ("/api/notifications/unread-count", this::unreadCount);
        app.post("/api/users/{id}/block",     this::block);
        app.delete("/api/users/{id}/block",   this::unblock);
        app.post("/api/hashtags/follow",      this::followHashtag);
        app.delete("/api/hashtags/{tag}/follow", this::unfollowHashtag);
        app.get ("/api/hashtags/mine",        this::myHashtags);
    }

    // ── Handler methods ───────────────────────────────────────────────────────
    // Move handler bodies from BorgolApiServer.java to here.
    // Each handler: private void handlerName(Context ctx) { ... }
    //
    // Find in BorgolApiServer.java: the lambda bodies for each route above.
    // Cut them out, paste as named methods here.
    // Replace borgol.xxx → svc.xxx
    //
    // Example (register handler from BorgolApiServer):
    private void register(Context ctx) {
        // paste body of the register lambda from BorgolApiServer.java
    }
    private void login(Context ctx) {
        // paste body of the login lambda
    }
    private void me(Context ctx) {
        // paste body of the /api/auth/me GET lambda
    }
    // ... and so on for every route registered above
}
```

- [ ] **Step 2: Create RecipeRouter**

```java
// src/main/java/borgol/ui/web/routers/RecipeRouter.java
package borgol.ui.web.routers;

import borgol.core.application.RecipeService;
import borgol.ui.web.dto.CollectionReq;
import borgol.ui.web.dto.CollectionRecipeReq;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class RecipeRouter {

    private final RecipeService svc;

    public RecipeRouter(RecipeService svc) {
        this.svc = svc;
    }

    public void register(Javalin app) {
        app.get   ("/api/recipes",                          this::listRecipes);
        app.post  ("/api/recipes",                          this::createRecipe);
        app.get   ("/api/recipes/{id}",                     this::getRecipe);
        app.put   ("/api/recipes/{id}",                     this::updateRecipe);
        app.delete("/api/recipes/{id}",                     this::deleteRecipe);
        app.post  ("/api/recipes/{id}/like",                this::toggleLike);
        app.post  ("/api/recipes/{id}/comments",            this::addComment);
        app.get   ("/api/recipes/{id}/comments",            this::getComments);
        app.get   ("/api/feed",                             this::feed);
        app.post  ("/api/recipes/{id}/save",                this::saveRecipe);
        app.delete("/api/recipes/{id}/save",                this::unsaveRecipe);
        app.get   ("/api/saved-recipes",                    this::savedRecipes);
        app.get   ("/api/hashtags/{tag}/recipes",           this::byHashtag);
        app.get   ("/api/trending",                         this::trending);
        app.post  ("/api/reports",                          this::createReport);
        app.get   ("/api/admin/reports",                    this::getReports);
        app.put   ("/api/admin/reports/{id}",               this::resolveReport);
        app.get   ("/api/collections",                      this::getCollections);
        app.post  ("/api/collections",                      this::createCollection);
        app.delete("/api/collections/{id}",                 this::deleteCollection);
        app.get   ("/api/collections/{id}/recipes",         this::collectionRecipes);
        app.post  ("/api/collections/{id}/recipes",         this::addToCollection);
        app.delete("/api/collections/{id}/recipes/{rid}",   this::removeFromCollection);
        app.get   ("/api/equipment",                        this::getEquipment);
        app.post  ("/api/equipment",                        this::addEquipment);
        app.delete("/api/equipment/{id}",                   this::deleteEquipment);
    }

    // Move handler bodies from BorgolApiServer.java for each route above.
    // Replace borgol.xxx → svc.xxx
    private void listRecipes(Context ctx)         { /* paste from BorgolApiServer */ }
    private void createRecipe(Context ctx)        { /* paste */ }
    private void getRecipe(Context ctx)           { /* paste */ }
    private void updateRecipe(Context ctx)        { /* paste */ }
    private void deleteRecipe(Context ctx)        { /* paste */ }
    private void toggleLike(Context ctx)          { /* paste */ }
    private void addComment(Context ctx)          { /* paste */ }
    private void getComments(Context ctx)         { /* paste */ }
    private void feed(Context ctx)                { /* paste */ }
    private void saveRecipe(Context ctx)          { /* paste */ }
    private void unsaveRecipe(Context ctx)        { /* paste */ }
    private void savedRecipes(Context ctx)        { /* paste */ }
    private void byHashtag(Context ctx)           { /* paste */ }
    private void trending(Context ctx)            { /* paste */ }
    private void createReport(Context ctx)        { /* paste */ }
    private void getReports(Context ctx)          { /* paste */ }
    private void resolveReport(Context ctx)       { /* paste */ }
    private void getCollections(Context ctx)      { /* paste */ }
    private void createCollection(Context ctx)    { /* paste */ }
    private void deleteCollection(Context ctx)    { /* paste */ }
    private void collectionRecipes(Context ctx)   { /* paste */ }
    private void addToCollection(Context ctx)     { /* paste */ }
    private void removeFromCollection(Context ctx){ /* paste */ }
    private void getEquipment(Context ctx)        { /* paste */ }
    private void addEquipment(Context ctx)        { /* paste */ }
    private void deleteEquipment(Context ctx)     { /* paste */ }
}
```

- [ ] **Step 3: Create BrewGuideRouter**

```java
// src/main/java/borgol/ui/web/routers/BrewGuideRouter.java
package borgol.ui.web.routers;

import borgol.core.application.BrewGuideService;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class BrewGuideRouter {

    private final BrewGuideService svc;

    public BrewGuideRouter(BrewGuideService svc) {
        this.svc = svc;
    }

    public void register(Javalin app) {
        app.get("/api/brew-guides",       this::list);
        app.get("/api/brew-guides/{id}",  this::get);
        app.get("/api/learn",             this::listArticles);
        app.get("/api/learn/{id}",        this::getArticle);
    }

    private void list(Context ctx)         { /* paste from BorgolApiServer */ }
    private void get(Context ctx)          { /* paste */ }
    private void listArticles(Context ctx) { /* paste */ }
    private void getArticle(Context ctx)   { /* paste */ }
}
```

- [ ] **Step 4: Create JournalRouter**

```java
// src/main/java/borgol/ui/web/routers/JournalRouter.java
package borgol.ui.web.routers;

import borgol.core.application.JournalService;
import borgol.ui.web.dto.JournalReq;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class JournalRouter {

    private final JournalService svc;

    public JournalRouter(JournalService svc) {
        this.svc = svc;
    }

    public void register(Javalin app) {
        app.get   ("/api/journal",          this::list);
        app.post  ("/api/journal",          this::create);
        app.get   ("/api/journal/{id}",     this::get);
        app.put   ("/api/journal/{id}",     this::update);
        app.delete("/api/journal/{id}",     this::delete);
        app.get   ("/api/journal/stats",    this::stats);
        app.get   ("/api/bean-bags",        this::listBeans);
        app.post  ("/api/bean-bags",        this::createBean);
        app.put   ("/api/bean-bags/{id}",   this::updateBean);
        app.delete("/api/bean-bags/{id}",   this::deleteBean);
    }

    private void list(Context ctx)        { /* paste from BorgolApiServer */ }
    private void create(Context ctx)      { /* paste */ }
    private void get(Context ctx)         { /* paste */ }
    private void update(Context ctx)      { /* paste */ }
    private void delete(Context ctx)      { /* paste */ }
    private void stats(Context ctx)       { /* paste */ }
    private void listBeans(Context ctx)   { /* paste */ }
    private void createBean(Context ctx)  { /* paste */ }
    private void updateBean(Context ctx)  { /* paste */ }
    private void deleteBean(Context ctx)  { /* paste */ }
}
```

- [ ] **Step 5: Create CafeRouter**

```java
// src/main/java/borgol/ui/web/routers/CafeRouter.java
package borgol.ui.web.routers;

import borgol.core.application.CafeService;
import borgol.ui.web.dto.CheckinReq;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class CafeRouter {

    private final CafeService svc;

    public CafeRouter(CafeService svc) {
        this.svc = svc;
    }

    public void register(Javalin app) {
        app.get ("/api/cafes",                    this::list);
        app.post("/api/cafes",                    this::create);
        app.get ("/api/cafes/{id}",               this::get);
        app.post("/api/cafes/{id}/rate",          this::rate);
        app.get ("/api/cafes/nearby",             this::nearby);
        app.post("/api/cafes/{id}/checkin",       this::checkIn);
        app.get ("/api/cafes/{id}/checkins",      this::getCheckins);
    }

    private void list(Context ctx)       { /* paste from BorgolApiServer */ }
    private void create(Context ctx)     { /* paste */ }
    private void get(Context ctx)        { /* paste */ }
    private void rate(Context ctx)       { /* paste */ }
    private void nearby(Context ctx)     { /* paste */ }
    private void checkIn(Context ctx)    { /* paste */ }
    private void getCheckins(Context ctx){ /* paste */ }
}
```

- [ ] **Step 6: Create AchievementRouter**

```java
// src/main/java/borgol/ui/web/routers/AchievementRouter.java
package borgol.ui.web.routers;

import borgol.core.application.AchievementService;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class AchievementRouter {

    private final AchievementService svc;

    public AchievementRouter(AchievementService svc) {
        this.svc = svc;
    }

    public void register(Javalin app) {
        app.get ("/api/achievements",         this::list);
        app.post("/api/achievements/check",   this::check);
    }

    private void list(Context ctx)  { /* paste from BorgolApiServer */ }
    private void check(Context ctx) { /* paste */ }
}
```

- [ ] **Step 7: Compile**

```bash
./mvnw compile -q
```

- [ ] **Step 8: Commit**

```bash
git add src/main/java/borgol/ui/web/routers/
git commit -m "refactor: extract domain routers from BorgolApiServer (SRP, 6 routers)"
```

---

## Task 14: Update BorgolApiServer

`BorgolApiServer` becomes a coordinator. Its `setupRoutes()` method instantiates and registers all six routers. All route handler code is removed from this file.

**Files:**
- Modify: `src/main/java/borgol/ui/web/BorgolApiServer.java`

- [ ] **Step 1: Replace setupRoutes() with router delegation**

Find the `setupRoutes()` method in `BorgolApiServer.java` (or wherever routes are registered). Replace its entire body:

```java
private void setupRoutes() {
    // Загвар: Coordinator — бүх router-г нэг газарт бүртгэнэ.
    // ApiGateway (Chain of Responsibility) нь энэ app instance-г wraps хийнэ.
    new UserRouter(borgol.userService).register(app);
    new RecipeRouter(borgol.recipeService).register(app);
    new BrewGuideRouter(borgol.brewGuideService).register(app);
    new JournalRouter(borgol.journalService).register(app);
    new CafeRouter(borgol.cafeService).register(app);
    new AchievementRouter(borgol.achievementService).register(app);
    // Legacy menu routes (MenuService, SSE) stay here — not domain-split
}
```

Note: `borgol.userService` etc. are the package-visible fields on the new `BorgolService` Facade. Add package-visible getters to `BorgolService` if the fields are private:

```java
// In BorgolService.java — add package-visible accessors for BorgolApiServer
UserService      getUserService()      { return userService; }
RecipeService    getRecipeService()    { return recipeService; }
BrewGuideService getBrewGuideService() { return brewGuideService; }
JournalService   getJournalService()   { return journalService; }
CafeService      getCafeService()      { return cafeService; }
AchievementService getAchievementService() { return achievementService; }
```

Then in `setupRoutes()`:
```java
new UserRouter(borgol.getUserService()).register(app);
new RecipeRouter(borgol.getRecipeService()).register(app);
new BrewGuideRouter(borgol.getBrewGuideService()).register(app);
new JournalRouter(borgol.getJournalService()).register(app);
new CafeRouter(borgol.getCafeService()).register(app);
new AchievementRouter(borgol.getAchievementService()).register(app);
```

Delete all handler methods from `BorgolApiServer.java` that have been moved to routers. Keep only: SSE handler, `MenuService` routes, and any shared auth helpers.

- [ ] **Step 2: Compile**

```bash
./mvnw compile -q
```

- [ ] **Step 3: Run tests**

```bash
./mvnw test -q
```
Expected: 7 tests pass.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/borgol/ui/web/BorgolApiServer.java \
        src/main/java/borgol/core/application/BorgolService.java
git commit -m "refactor: BorgolApiServer becomes coordinator, delegates to domain routers"
```

---

## Task 15: Update Main.java Wiring

`Main.java` is the Composition Root. The only change is adding `SchemaInitializer` is already called inside `BorgolRepository` — no wiring change needed there. Verify the full startup works.

**Files:**
- No changes required to `Main.java` — `BorgolRepository` and `BorgolService` constructors are unchanged.

- [ ] **Step 1: Full build**

```bash
./mvnw compile -q
```
Expected: no output.

- [ ] **Step 2: Run full test suite**

```bash
./mvnw test -q
```
Expected: 7 tests pass (MenuServiceTest 5 + JournalServiceTest 2).

- [ ] **Step 3: Smoke test (web mode)**

Start the server in web mode and hit one endpoint:
```bash
# In one terminal:
MODE=web ./mvnw exec:java -Dexec.mainClass=borgol.app.Main -q &
sleep 5
curl -s http://localhost:7000/api/brew-guides | python3 -c "import sys,json; d=json.load(sys.stdin); print(f'{len(d)} brew guides OK')"
kill %1
```
Expected output: `N brew guides OK` (N > 0).

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "refactor: full-stack decomposition complete — all layers split, tests pass"
```

---

## Task 16: Frontend Split

Split `index.html` (4,566 lines) into 8 focused JS files. No build step — plain `<script src="">` tags.

**Files:**
- Create: `src/main/resources/public/js/api.js`
- Create: `src/main/resources/public/js/auth.js`
- Create: `src/main/resources/public/js/brew-guides.js`
- Create: `src/main/resources/public/js/recipes.js`
- Create: `src/main/resources/public/js/journal.js`
- Create: `src/main/resources/public/js/cafes.js`
- Create: `src/main/resources/public/js/achievements.js`
- Create: `src/main/resources/public/js/app.js`
- Modify: `src/main/resources/public/index.html`

- [ ] **Step 1: Create js/ directory**

```bash
mkdir -p src/main/resources/public/js
```

- [ ] **Step 2: Create api.js**

Cut from `index.html` all fetch/API utility functions. The key functions to move are any `apiFetch`, `BASE_URL`, auth-header helpers, and error-handling wrappers.

```js
// src/main/resources/public/js/api.js
// Global API utilities — loaded first, used by all other JS files.
// window.authToken is set by auth.js after login.

const BASE_URL = '';  // same origin

async function apiFetch(path, options = {}) {
    const headers = { 'Content-Type': 'application/json', ...options.headers };
    if (window.authToken) headers['Authorization'] = 'Bearer ' + window.authToken;
    const res = await fetch(BASE_URL + path, { ...options, headers });
    if (!res.ok) {
        const err = await res.json().catch(() => ({ error: res.statusText }));
        throw new Error(err.error || res.statusText);
    }
    return res.json();
}
```

Move any additional API helpers from `index.html` into this file.

- [ ] **Step 3: Create auth.js**

Cut login, register, logout functions and token storage from `index.html`:

```js
// src/main/resources/public/js/auth.js
// Authentication: login, register, logout, token persistence.
// Sets window.authToken and window.currentUser after successful auth.

window.authToken   = localStorage.getItem('borgol_token') || null;
window.currentUser = null;

async function login(email, password) {
    const data = await apiFetch('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password })
    });
    window.authToken = data.token;
    window.currentUser = data.user;
    localStorage.setItem('borgol_token', data.token);
    return data;
}

async function register(username, email, password) {
    const data = await apiFetch('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify({ username, email, password })
    });
    window.authToken = data.token;
    window.currentUser = data.user;
    localStorage.setItem('borgol_token', data.token);
    return data;
}

function logout() {
    window.authToken   = null;
    window.currentUser = null;
    localStorage.removeItem('borgol_token');
    showPage('login');
}
```

Move any remaining auth functions from `index.html` into this file.

- [ ] **Step 4: Create domain JS files**

For each domain file, cut the relevant functions from `index.html` into the new file. Use this mapping:

| File | Functions to move from index.html |
|---|---|
| `brew-guides.js` | `loadBrewGuides`, `renderBrewGuide`, `openBrewGuide`, `loadLearnArticles`, `renderLearnCard` |
| `recipes.js` | `loadRecipes`, `renderRecipeCard`, `openRecipe`, `openNewRecipe`, `saveRecipe`, `deleteRecipe`, `toggleLike`, `addComment`, `loadCollections`, `renderCollectionsGrid`, `openNewCollectionModal`, `saveCollection`, `deleteCollectionConfirm`, `openCollectionDetail`, `removeFromCollection`, `openAddToCollection`, `addToCollection` |
| `journal.js` | `loadJournal`, `renderJournalEntry`, `openJournalDetail`, `openNewJournalModal`, `openEditJournal`, `saveJournalEntry`, `deleteJournal`, `fetchJournalWeather`, `loadBeanBags`, `saveBeanBag`, `deleteBeanBag`, `loadJournalStats` |
| `cafes.js` | `loadCafes`, `renderCafeCard`, `openCafeDetail`, `openNewCafe`, `saveCafe`, `rateCafe`, `loadCafeCheckins`, `submitCheckin`, `findNearby` |
| `achievements.js` | `loadAchievements`, `renderBadge`, `checkAchievements` |
| `app.js` | `showPage`, `initApp`, `loadProfile`, nav click handlers, page init — loaded last |

Each file starts with a comment block:
```js
// src/main/resources/public/js/recipes.js
// Recipes page: list, detail, create, edit, delete, like, comment, collections.
// Reads: window.authToken, window.currentUser (set by auth.js)
```

- [ ] **Step 5: Update index.html — add script tags, remove inline JS**

Find the single `<script>` block in `index.html` (starts after the last `</div>` and before `</body>`). Replace it entirely:

```html
<!-- JS — loaded in dependency order, no bundler needed -->
<script src="js/api.js"></script>
<script src="js/auth.js"></script>
<script src="js/brew-guides.js"></script>
<script src="js/recipes.js"></script>
<script src="js/journal.js"></script>
<script src="js/cafes.js"></script>
<script src="js/achievements.js"></script>
<script src="js/app.js"></script>  <!-- wires everything, runs initApp() -->
```

`index.html` should now contain only: `<head>`, CSS styles, HTML structure (nav, page containers, modals), and these 8 `<script>` tags. No inline JS.

- [ ] **Step 6: Smoke test in browser**

Start the server:
```bash
MODE=web ./mvnw exec:java -Dexec.mainClass=borgol.app.Main -q &
```
Open `http://localhost:7000` in a browser. Verify:
- [ ] Login page loads
- [ ] Can login with `coffee@borgol.mn` / `password123`
- [ ] Recipes page loads and shows recipe cards
- [ ] Journal page loads
- [ ] No console errors

```bash
kill %1
```

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/public/js/ \
        src/main/resources/public/index.html
git commit -m "refactor: split index.html into 8 domain JS files (SRP, no build step)"
```

---

## Completion Checklist

After all tasks complete, verify against the design spec:

- [ ] `core/ports/` has 6 interfaces (UserRepositoryPort, RecipeRepositoryPort, BrewGuideRepositoryPort, JournalRepositoryPort, CafeRepositoryPort, AchievementRepositoryPort)
- [ ] `infrastructure/persistence/` has 6 domain repos + SchemaInitializer + BorgolRepository (delegator)
- [ ] `core/application/` has 6 domain services + BorgolService (Facade)
- [ ] `ui/web/routers/` has 6 routers
- [ ] `ui/web/dto/` has 4 DTO classes
- [ ] `resources/public/js/` has 8 JS files
- [ ] `index.html` has no inline `<script>` logic
- [ ] `Main.java` unchanged
- [ ] Desktop (`JournalPane.java`) unchanged
- [ ] `ApiGateway.java` unchanged
- [ ] `./mvnw test -q` passes (7 tests)
- [ ] App starts in web mode and serves the UI

## Pattern Reference (for writeup)

| Pattern | Location |
|---|---|
| Hexagonal Architecture | `core/ports/` → `infrastructure/persistence/` |
| Repository Pattern | `*Repository implements *RepositoryPort` |
| Facade | `BorgolService` delegates to domain services |
| Facade | `BorgolRepository` delegates to domain repositories |
| Chain of Responsibility | `ApiGateway` middleware chain (unchanged) |
| SRP | One class per domain per layer |
| ISP | Each service depends only on its needed port |
| DIP | Services depend on interfaces, not implementations |
| Composition Root | `Main.java` — one place wires all objects |
