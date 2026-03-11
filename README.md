# MyCafé — Coffee Shop Menu Management System

**Хичээл:** ICSI486 Программ хангамжийн бүтээлт
**Оюутан:** С.Тэмүүлэн — 22B1NUM6637
**Бие даасан төсөл:** Milestone 1 — Архитектур ба Өгөгдөл хадгалалтын тохиргоо

---

## Тойм

Java console програм бөгөөд кофе шопын цэсийг (Menu) удирдана.
H2 файл өгөгдлийн санг JDBC-ээр ашиглана. Hexagonal Architecture (Ports & Adapters) загварыг дагана.
Дөрвөн загварыг (Singleton, Factory, DAO, Observer) хэрэгжүүлсэн.

---

## Архитектур

```
mn.edu.num.cafe
├── core/                              ← Цөм: DB, UI-аас тусгаарлагдсан
│   ├── domain/
│   │   ├── MenuItem.java              — Entity (id, name, category, price, available)
│   │   └── MenuCategory.java          — Enum (COFFEE, TEA, SMOOTHIE, FOOD, DESSERT)
│   ├── ports/
│   │   ├── IMenuRepository.java       — Secondary Port (репозиторын интерфейс)
│   │   └── MenuChangeObserver.java    — Observer Port (мэдэгдэлийн интерфейс)
│   └── application/
│       ├── MenuService.java           — Бизнес логик + Observer мэдэгдэл
│       └── MenuDto.java               — DTO (Java Record)
├── infrastructure/                    ← Техник хэрэгжүүлэлт
│   ├── config/
│   │   └── DatabaseConnection.java    — Singleton (DCL + volatile)
│   └── persistence/
│       ├── JdbcMenuRepository.java    — DAO + Outbound Adapter (H2 JDBC)
│       ├── InMemoryMenuRepository.java — In-memory (тест / demo)
│       └── RepositoryFactory.java     — Factory (config-driven)
└── app/
    ├── ConsoleMenuObserver.java       — Observer хэрэгжүүлэлт
    └── Main.java                      — Composition Root
```

**Давхаргын дүрмүүд:**
- `core/` давхаргад `java.sql.*`, `javax.swing.*` импортлохгүй
- `MenuService` нь зөвхөн `IMenuRepository` портоор ажиллана
- Бүх объект холболт `Main.java`-д хийгдэнэ (Composition Root)
- `database.properties` `.gitignore`-д бүртгэгдсэн (нууц үг git-д орохгүй)

---

## Хэрэгжүүлсэн загварууд

### 1. Singleton — DatabaseConnection
Double-Checked Locking ашиглан thread-safe нэг л холболтыг баталгаажуулна.

```java
private static volatile DatabaseConnection instance;

public static DatabaseConnection getInstance() {
    if (instance == null) {
        synchronized (DatabaseConnection.class) {
            if (instance == null) {
                instance = new DatabaseConnection();
            }
        }
    }
    return instance;
}
```

### 2. Factory — RepositoryFactory
`database.properties`-ийн `app.persistence.mode` утгаас хамааран тохирох репозиторыг буцаана:
- `DB`  → `JdbcMenuRepository`    (H2 файл өгөгдлийн сан)
- `MEM` → `InMemoryMenuRepository` (санах ой, тест орчинд)

### 3. DAO — JdbcMenuRepository
`IMenuRepository` интерфейсийг хэрэгжүүлсэн JDBC адаптер.
Бүх query-д `PreparedStatement` ашигладаг — SQL Injection хориглоно.

### 4. Observer — MenuChangeObserver
`MenuService` нь `MenuChangeObserver` портоор бүртгэгдсэн observer-уудад
`onItemAdded`, `onItemUpdated`, `onItemDeleted` мэдэгдэл илгээнэ.

```java
// MenuService-д бүртгэнэ
menuService.addObserver(new ConsoleMenuObserver());

// Ирээдүйд нэмж болох observer-ууд:
// menuService.addObserver(new EmailMenuObserver(...));
// menuService.addObserver(new LogFileMenuObserver(...));
```

---

## Тохиргоо

`src/main/resources/database.properties` файлыг үүсгэж дараах агуулгыг бичнэ үү (**git-д орохгүй**):

```properties
app.persistence.mode=DB
db.driver=org.h2.Driver
db.url=jdbc:h2:./data/cafe_db
db.user=sa
db.password=
```

Санах ойн горимд ажиллуулахын тулд: `app.persistence.mode=MEM`

---

## Ажиллуулах

**Eclipse:**
1. `File → Import → General → Existing Projects into Workspace`
2. `cafe-project` сонгоно
3. `src/main/resources/database.properties` файл үүсгэнэ
4. `Main.java → Run As → Java Application`

**Maven wrapper (терминал):**
```bash
./mvnw.cmd compile              # Эмпиль хийх
./mvnw.cmd test                 # Тест ажиллуулах
./mvnw.cmd exec:java            # Ажиллуулах
```

**Технологи:** Java 21, Maven, H2 (файл горим), JUnit 5

---

## Тест

`MenuServiceTest` — InMemoryMenuRepository ашиглан 6 тест:

| Тест | Шалгах зүйл |
|------|-------------|
| `addItem_savesAndReturnsDto` | Шинэ зүйл нэмэгдэх |
| `addItem_emptyName_throws` | Хоосон нэр → exception |
| `addItem_negativePrice_throws` | Сөрөг үнэ → exception |
| `updateItem_changesFieldsCorrectly` | Зүйл шинэчлэгдэх |
| `removeItem_deletesFromRepository` | Зүйл устгагдах |
| `observer_notifiedOnAddUpdateDelete` | Observer 3 мэдэгдэл авах |

---

## Жишээ гаралт

```
╔══════════════════════════════════════════╗
║   MyCafé — Coffee Shop Menu Management   ║
╚══════════════════════════════════════════╝

>>> Adding menu items...
  [OBSERVER] ✓ Added  : MenuItem{id=1, name='Espresso', ...}
  [OBSERVER] ✓ Added  : MenuItem{id=2, name='Caffe Latte', ...}
  ...

--- Final Menu ---
  ID   Name                   Category    Price  Available
  ──────────────────────────────────────────────────────────
  1    Espresso Shot          COFFEE      $ 3.75  ✓
  2    Caffe Latte            COFFEE      $ 4.50  ✓
  3    Matcha Latte           TEA         $ 4.00  ✓
  5    Tiramisu               DESSERT     $ 5.50  ✓
```
