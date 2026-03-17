# Borgol — Кофе Сонирхогчдын Платформ

ICSI486 Программ хангамжийн бүтээлт хичээлийн бие даасан төсөл. Web SPA болон JavaFX desktop клиент хоёуланг нэг системд нэгтгэсэн full-stack платформ.

**Стек:** Java 21 · JavaFX · Javalin 6.3 · H2 · Custom JWT · Maven

---

## Юу хийдэг вэ?

Кофе сонирхогчид жор хуваалцах, кафе үнэлэх, brew журнал хөтлөх, мэдлэг олж авах боломжтой нийгэмлэгийн платформ.

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

Domain давхарга нь ямар ч framework-оос хамааралгүй цэвэр Java. Үүнийг ашиглан нэг `BorgolService` дээр Web API болон Desktop клиент хоёуланг ажиллуулсан — бизнес логик хоёр удаа бичигдээгүй.

---

## Design Pattern-ууд

| Pattern | Хаана | Зачим |
|---|---|---|
| Repository | `BorgolRepository` | Бүх SQL нэг дор; service давхарга JDBC-г шууд мэдэхгүй |
| Service Layer | `BorgolService` | Бизнес дүрмийн цорын ганц удирдлагын цэг |
| Singleton | `DatabaseConnection.getInstance()` | DB connection апп-ын туршид нэг л удаа үүснэ |
| Facade | `BorgolService` | Олон repository дуудлагыг нэг энгийн API болгон нуудаг |
| DTO / Value Object | `User`, `Recipe` гэх мэт | Давхаргуудын хооронд өгөгдөл дамжуулах объект |

---

## Аюулгүй байдал

**JWT** — гадны сангүй, `javax.crypto.Mac` ашиглан HMAC-SHA256-аар өөрөө хэрэгжүүлсэн. Токен нь хэрэглэгчийн ID болон хугацааны мэдээлэл агуулна; Javalin-ий `before()` handler-аар хамгаалагдсан route бүрт шалгагдана.

**Нууц үг** — SHA-256 + тухайн хэрэглэгчид зориулсан random salt. `saltHex:hashHex` форматаар хадгалагдана. Salt нь rainbow table болон precomputation халдлагаас хамгаална.

---

## Технологи сонгосон шалтгаан

**Яагаад Spring Boot биш Javalin?**
Javalin нь Jetty HTTP сервер дээр хамгийн бага хийсвэрлэлт нэмдэг. Route, middleware бүгд тодорхой харагдана — annotation дотор нуугдахгүй. Clean Architecture-д тохиромжтой, хамааралтай сан цөөн.

**Яагаад PostgreSQL биш H2?**
File-based тул тохиргоо, суулгалт шаардахгүй. Repository давхарга л JDBC-г мэдэхийн учир PostgreSQL руу шилжихэд зөвхөн connection string өөрчлөхөд хангалттай.

**Яагаад ORM ашиглаагүй?**
SQL шууд бичих нь query-г бүрэн контрольдох боломж өгнө. Repository pattern нь ORM-тэй ижил тусгаарлалтыг хангадаг — хийсвэрлэл дутахгүй, нэмэлт complexity байхгүй.

**Яагаад JWT-г өөрөө хэрэгжүүлсэн?**
Гадны сан ашиглахаас илүүтэй токены бүтэц (header.payload.signature), гарын үсгийн алгоритм, баталгаажуулалтын процессыг гүнзгий ойлгох зорилготой байсан.

---

## Техникийн онцлог шийдлүүд

**Radar Chart** — library ашиглаагүй. Trigonometry (sin/cos)-оор 6 тэнхлэгийн координат тооцоолж Canvas/SVG дээр шууд зурсан.

**PDF export** — SVG radar chart-ийг агуулсан print-ready HTML үүсгэж, шинэ цонхонд нээж browser-ийн print-to-PDF ашигладаг.

**Discover Feed** — дагадаг хүн байхгүй үед feed хоосон харагдахаас сэргийлж санамсаргүй жорын fallback нэмсэн.

**JavaFX зургийн харьцаа** — `setFitWidth()` + `setFitHeight()` хоёуланг тавихад харьцаа гажуудна. `StackPane` + `Rectangle` clip + `fitWidthProperty().bind()` хослолоор шийдсэн.

**Idempotent seeding** — demo өгөгдлийн seed нь auto-increment ID биш email-аар шалгадаг тул апп дахин ажиллуулахад давхардал гардаггүй.

---

## Ажиллуулах

**Web сервер (хурдан эхлэл):**
```
mvnw.cmd exec:java -Dexec.mainClass=mn.edu.num.cafe.app.MainWeb
```
`http://localhost:7000` дээр нээгдэнэ

**Desktop клиент:**
```
mvnw.cmd exec:java -Dexec.mainClass=mn.edu.num.cafe.app.Main
```

**Demo хэрэглэгчид:**
```
coffee@borgol.mn / password123
sara@borgol.mn   / password123
tea@borgol.mn    / password123
```

---

## Төслийн бүтэц

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
    web/                BorgolApiServer  (30+ REST endpoint)
    desktop/            BorgolApp        (JavaFX)
src/main/resources/public/
  index.html            Vanilla JS SPA (~2400 мөр)
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
