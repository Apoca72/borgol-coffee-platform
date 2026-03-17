# Borgol — Кофе Сонирхогчдын Платформ

ICSI486 Программ хангамжийн бүтээлт хичээлийн бие даасан төсөл. Web SPA болон JavaFX desktop клиент хоёуланг нэг системд нэгтгэсэн full-stack платформ.

**Стек:** Java 21 · JavaFX · Javalin 6.3 · H2 · Custom JWT · Maven

---

## Юу хийдэг вэ?

- **Нэвтрэх** — JWT токен, SHA-256 + salt нууц үг хаш, 7 хоногийн хүчинтэй хугацаа
- **Жор** — нийтлэх, засах, устгах, зураг, like, сэтгэгдэл
- **Feed** — дагадаг хүмүүсийн жорын цаг хугацааны дараалал; дагадаг хүн байхгүй үед Discover режимд орно
- **Кафе** — жагсаалт, 1–5 одоор үнэлгээ, GPS-д суурилсан ойр дотно эрэмбэ
- **Brew Journal** — хувийн тэмдэглэл, 6 тэнхлэгт радар график (Aroma, Flavor, Acidity, Body, Sweet, Finish), PDF экспорт
- **Brew Guides** — Pour Over, French Press, AeroPress, Espresso, Cold Brew, Moka Pot алхам алхмаар
- **Суралцах** — 6 нийтлэл: шарах түвшин, extraction, усны чанар, Arabica vs Robusta гэх мэт
- **Нийгэмлэг** — follow/unfollow, профайл, дагагч/дагаж буй хэрэглэгчдийн жагсаалт, хайлт
- **Desktop клиент** — native JavaFX апп, dark/light theme, ижил backend дээр ажилладаг

---

## Архитектур

Төсөл нь **Clean Architecture**-ийн 4 давхаргатай. Дотоод давхарга нь гадаадаас огт хамааралгүй:

```
UI (Javalin routes + JavaFX views)
    |
Application (BorgolService — бизнес логик)
    |
Domain (User, Recipe, CafeListing, BrewJournalEntry, BrewGuide, LearnArticle)
    |
Infrastructure (BorgolRepository, DatabaseConnection, JwtUtil, PasswordUtil)
```

Domain давхарга нь ямар ч framework-оос хамааралгүй цэвэр Java. Нэг `BorgolService` дээр Web API болон Desktop клиент хоёуланг ажиллуулсан.

---

## Design Pattern-ууд

### 1. Singleton
`DatabaseConnection.getInstance()` — DB connection апп-ын туршид нэг л удаа үүснэ.

```java
public static DatabaseConnection getInstance() {
    if (instance == null) instance = new DatabaseConnection();
    return instance;
}
```

### 2. Factory
`RepositoryFactory` — `database.properties`-ийн тохиргооноос хамаарч repository үүсгэнэ.

```java
public static IMenuRepository createMenuRepository() {
    String mode = db.getProperty("app.persistence.mode");
    return switch (mode) {
        case "DB"  -> new JdbcMenuRepository(db);
        case "MEM" -> new InMemoryMenuRepository();
    };
}
```

### 3. Repository / DAO
`BorgolRepository`, `JdbcMenuRepository` — бизнес логикийг SQL-аас тусгаарлана. Service давхарга JDBC-г шууд мэдэхгүй.

### 4. Observer
`MenuChangeObserver` interface + `ConsoleMenuObserver`. Цэс өөрчлөгдөхөд бүртгэгдсэн observer-уудад автоматаар мэдэгдэнэ.

```java
public interface MenuChangeObserver {
    void onItemAdded(MenuItem item);
    void onItemUpdated(MenuItem item);
    void onItemDeleted(int id);
}
```

### 5. Facade
`BorgolService` — олон дотоод дуудлагыг нэг энгийн API болгон нуудаг. Controller нь дотоод нарийн төвөгтэй байдлыг мэдэхгүй.

### 6. Port & Adapter (Hexagonal)
`IMenuRepository` (Port) — `JdbcMenuRepository` / `InMemoryMenuRepository` (Adapter). Domain нь конкрет технологи мэдэхгүй.

```
Core (Port)           Infrastructure (Adapter)
IMenuRepository  <--  JdbcMenuRepository
                 <--  InMemoryMenuRepository
```

---

## SOLID зарчмууд

### S — Single Responsibility
Нэг класс нэг л зүйлийн төлөө хариуцлагатай:
- `JwtUtil` — зөвхөн JWT үүсгэх, шалгах
- `PasswordUtil` — зөвхөн нууц үг хаш хийх
- `DatabaseConnection` — зөвхөн DB холболт
- `BorgolRepository` — зөвхөн өгөгдлийн сантай харилцах
- `BorgolService` — зөвхөн бизнес дүрэм
- `BorgolApiServer` — зөвхөн HTTP route тодорхойлох

### O — Open/Closed
Өргөтгөлд нээлттэй, өөрчлөлтөд хаалттай. Шинэ persistence горим нэмэхэд `IMenuRepository`, `MenuService`-г хөндөхгүй — `RepositoryFactory`-д нэг мөр л нэмнэ. Шинэ `EmailMenuObserver` нэмэхэд `MenuService`-г өөрчлөхгүй.

### L — Liskov Substitution
`IMenuRepository`-ийн аль ч хэрэгжүүлэлтийг солиход `MenuService` ямар ч ялгаагүйгээр ажилладаг.

```java
IMenuRepository repo = RepositoryFactory.createMenuRepository();
menuService = new MenuService(repo, observer); // repo яг юу байхаас үл хамаарна
```

### I — Interface Segregation
`MenuChangeObserver` интерфейс нарийн тодорхой — цэс өөрчлөлтөнд хамааралтай 3 метод л байна. Ашиглахгүй методыг implement хийхийг шаардахгүй.

### D — Dependency Inversion
Дээд давхарга доод давхаргаас хамаарахгүй — хоёулаа абстракцаас хамаарна:

```
MenuService (дээд)  -->  IMenuRepository (абстракци)
                                ^
                    JdbcMenuRepository (доод)
```

`MenuService` нь `JdbcMenuRepository`-г шууд мэдэхгүй. Dependency чиглэл үргэлж дотогш.

---

## Аюулгүй байдал

**JWT** — гадны сангүй, `javax.crypto.Mac` ашиглан HMAC-SHA256-аар өөрөө хэрэгжүүлсэн. Javalin-ий `before()` handler-аар хамгаалагдсан route бүрт шалгагдана.

**Нууц үг** — SHA-256 + random salt. `saltHex:hashHex` форматаар хадгалагдана. Salt нь rainbow table болон precomputation халдлагаас хамгаална.

---

## Технологи сонгосон шалтгаан

**Яагаад Spring Boot биш Javalin?** Javalin нь Jetty дээр хамгийн бага хийсвэрлэлт нэмдэг. Route, middleware бүгд тодорхой харагдана. Clean Architecture-д тохиромжтой.

**Яагаад PostgreSQL биш H2?** File-based тул тохиргоо шаардахгүй. Repository давхарга л JDBC-г мэдэх учир PostgreSQL руу шилжихэд зөвхөн connection string өөрчилнө.

**Яагаад ORM ашиглаагүй?** SQL шууд бичих нь query-г бүрэн контрольдох боломж өгнө. Repository pattern нь ORM-тэй ижил тусгаарлалтыг хангана.

**Яагаад JWT-г өөрөө хэрэгжүүлсэн?** Токены бүтэц, гарын үсгийн алгоритм, баталгаажуулалтын процессыг гүнзгий ойлгох зорилготой байсан.

---

## Техникийн онцлог шийдлүүд

- **Radar Chart** — library ашиглаагүй, trigonometry (sin/cos)-оор 6 тэнхлэгийн координат тооцоолж Canvas/SVG дээр зурсан
- **PDF export** — SVG chart агуулсан print-ready HTML үүсгэж browser print-to-PDF ашигладаг
- **Discover Feed** — дагадаг хүн байхгүй үед санамсаргүй жорын fallback
- **JavaFX зургийн харьцаа** — `StackPane` + `Rectangle` clip + `fitWidthProperty().bind()` хослол
- **Idempotent seeding** — email-аар шалгадаг тул дахин ажиллуулахад давхардал гардаггүй

---

## Ажиллуулах

**Web сервер:**
```
mvnw.cmd exec:java -Dexec.mainClass=mn.edu.num.cafe.app.MainWeb
```
`http://localhost:7000`

**Desktop клиент:**
```
mvnw.cmd exec:java -Dexec.mainClass=mn.edu.num.cafe.app.Main
```

**Demo хэрэглэгчид:** `coffee@borgol.mn` / `sara@borgol.mn` / `tea@borgol.mn` — нууц үг: `password123`

---

## Төслийн бүтэц

```
src/main/java/mn/edu/num/cafe/
  app/                  Main.java, MainWeb.java
  core/
    domain/             User, Recipe, CafeListing, BrewJournalEntry, BrewGuide, LearnArticle
    application/        BorgolService
    ports/              IMenuRepository, MenuChangeObserver
  infrastructure/
    persistence/        BorgolRepository, JdbcMenuRepository, InMemoryMenuRepository, RepositoryFactory
    config/             DatabaseConnection
    security/           JwtUtil, PasswordUtil
  ui/
    web/                BorgolApiServer  (30+ REST endpoint)
    desktop/            BorgolApp        (JavaFX)
src/main/resources/
  database.properties
  public/index.html     Vanilla JS SPA (~2400 мөр)
```

---

## API

| Метод | Зам | Тайлбар |
|---|---|---|
| POST | /api/auth/register | Бүртгүүлэх |
| POST | /api/auth/login | Нэвтрэх, JWT буцаана |
| GET | /api/auth/me | Одоогийн хэрэглэгч |
| GET/POST | /api/recipes | Жорын жагсаалт / үүсгэх |
| GET/PUT/DELETE | /api/recipes/{id} | Нэг жор |
| POST | /api/recipes/{id}/like | Like toggle |
| GET/POST | /api/recipes/{id}/comments | Сэтгэгдэл |
| GET | /api/feed | Personalized feed |
| GET/POST | /api/cafes | Кафены жагсаалт / үүсгэх |
| POST | /api/cafes/{id}/rate | Кафе үнэлэх |
| GET/POST | /api/journal | Журналын бичлэг |
| PUT/DELETE | /api/journal/{id} | Засах / устгах |
| GET | /api/brew-guides | Бүх brew guide |
| GET | /api/learn | Бүх нийтлэл |
| POST/DELETE | /api/users/{id}/follow | Follow / unfollow |
| GET | /api/users/search | Хэрэглэгч хайх |
