# 🪴 Borgol — Кофе Сонирхогчдын Платформ

**Хичээл:** ICSI486 Программ хангамжийн бүтээлт
**Оюутан:** С.Тэмүүлэн — 22B1NUM6637
**Бие даасан төсөл:** JavaFX Desktop App — Borgol Coffee Enthusiast Platform

---

## Тойм

Borgol бол кофе сонирхогчдод зориулсан **JavaFX desktop нийгмийн платформ** юм. Хэрэглэгчид жор хуваалцах, кафе нээн судлах, хувийн дэвтэрт буцалтын тэмдэглэл хөтлөх, бусад кофе сонирхогчдыг дагах, гарын авлага болон нийтлэлээр суралцах боломжтой — бүгдийг нь нэг тохилог, хариу мэдрэмжтэй desktop програмаас.

---

## Дэлгэцийн зураг

| Жорын лент | Харанхуй горим | Дэвтрийн радар диаграм |
|---|---|---|
| Зурагтай карт, дуртай, сэтгэгдэл | Бүрэн харанхуй загвар | Буцалтын сессийн радар диаграм |

---

## Функцүүд

### 🔐 Нэвтрэлт
- JWT суурьтай бүртгэл / нэвтрэлт (HMAC-SHA256, 7 хоногийн хугацаа)
- Нууц үгийг SHA-256 + санамсаргүй давс (`saltHex:hashHex`) хэлбэрээр хадгалдаг
- Сесс `AppSession` singleton-д хадгалагдана

### 📖 Жорууд
- Зургийн URL баннертай кофений жор үүсгэх, засварлах, устгах
- Дуртай / дургүй, жор тус бүрд сэтгэгдэл
- Эрэмбэлэлт: **Шинэ** эсвэл **Алдартай**
- Ундааны төрлөөр шүүх: Espresso, Latte, Pour Over, Cold Brew гэх мэт
- Баруун самбар: Хамгийн их дуртай жорууд + Дагах санал болгох хэрэглэгчид

### 📰 Лент
- Дагадаг хэрэглэгчдийн жорууд
- **Нээн судлах хэсэг** — хэн ч дагаагүй үед алдартай жорууд харагдана

### ☕ Кафенүүд
- Кафений жагсаалт харах, нэмэх, үнэлгээ болон шүүмж бичих
- **📍 Ойролцоо** — GPS координат болон радиусаар шүүх (Улаанбаатар анхдагч)
- Одон үнэлгээ болон хувийн шүүмж

### 📓 Буцалтын дэвтэр
- Буурцаг, арга, шарсан байдал, тун, усны температур, буцалтын хугацаа, гарц бүхий хувийн тэмдэглэл
- JavaFX Canvas-ээр дүрсэлсэн 6 тэнхлэгтэй **радар диаграм** (Аромат, Амт, Хүчиллэг, Бие, Амтат, Төгсгөл)
- Тэмдэглэлийг **CSV** файл болгон экспортлох

### 📚 Сурах & Буцалтын гарын авлагууд
- 6 эмхэтгэсэн Буцалтын гарын авлага: Pour Over (V60), French Press, AeroPress, Espresso, Cold Brew, Moka Pot
- 6 Сурах нийтлэл: Шарсан түвшин, Гарцын шинжлэх ухаан, Усны чанар, Амт мэдрэх, Arabica vs Robusta, Нунтаглалтын хэмжээ

### 👥 Хүмүүс
- Хэрэглэгч дагах / дагахаа болих
- Жорын жагсаалт болон дагагчийн статистиктай нийтийн профайл харах
- Танд тохирох хүмүүсийн санал болгох

### 👤 Профайл
- Намтар, тоног төхөөрөмжийн жагсаалт, мэргэжлийн түвшин, амт уул сонголтыг засварлах
- Табууд: Миний жорууд, Дуртай жорууд, Тоног төхөөрөмж
- Callback-ээр sidebar статистик шинэчлэх (бүрэн дахин барихгүй)

### 🌙 Харанхуй горим
- Бүрэн харанхуй загвар (Facebook харанхуй өнгөний палитр: `#18191A` / `#242526` / `#3A3B3C`)
- Sidebar дахь товч — бүх хавтангийг тэр даруй дахин барина
- Бүх inline стиль `UiUtils` загварын туслах функцүүд (`bg()`, `card()`, `text()`, `sub()` гэх мэт) ашиглана

### 🔔 Toast мэдэгдэл
- Жор хадгалах, дэвтрийн тэмдэглэл, профайл шинэчлэх үед шар өнгийн fade-in/out overlay

---

## Технологийн стек

| Давхарга | Технологи |
|---|---|
| UI | JavaFX 21 (BorderPane, VBox, HBox, StackPane, Canvas, ScrollPane) |
| Backend | Javalin 6.3 (REST API, JSON) |
| Өгөгдлийн сан | H2 (файл дээр суурилсан, upsert-д MERGE INTO) |
| Нэвтрэлт | Гадаад сангүй өөрийн HMAC-SHA256 JWT |
| Бүтээх | Maven Wrapper (`mvnw.cmd`) |
| Хэл | Java 21 |

---

## Архитектур

```
mn.edu.num.cafe
├── app/
│   └── Main.java                    — Composition root, туршилтын өгөгдөл тарих, JavaFX эхлүүлэх
├── core/
│   ├── domain/                      — Entity ангиуд
│   │   ├── User, Recipe, CafeListing
│   │   ├── RecipeComment, BrewJournalEntry
│   │   ├── BrewGuide, LearnArticle
│   │   └── MenuItem (legacy)
│   └── application/
│       ├── BorgolService.java       — Бүх бизнесийн логик
│       └── MenuService.java         — Legacy цэсний сервис
├── infrastructure/
│   ├── persistence/
│   │   └── BorgolRepository.java   — Бүх SQL / H2 хүсэлт
│   └── security/
│       ├── JwtUtil.java             — HMAC-SHA256 JWT
│       └── PasswordUtil.java        — SHA-256 + давс хэшлэлт
└── ui/
    ├── web/
    │   └── BorgolApiServer.java     — 30+ REST endpoint (Javalin)
    └── desktop/
        ├── BorgolApp.java           — JavaFX Application оруулах цэг
        ├── MainWindow.java          — BorderPane үндэс, sidebar, харанхуй горим
        ├── UiUtils.java             — Хуваалцсан туслах: avatar, toast, dialog, загварын өнгө
        ├── AppSession.java          — Нэвтэрсэн хэрэглэгчийн төлөв
        ├── RecipesPane.java         — CRUD бүхий жорын лент
        ├── FeedPane.java            — Нийгмийн лент + Нээн судлах
        ├── CafesPane.java           — Кафений карт + Ойролцоо
        ├── JournalPane.java         — Буцалтын дэвтэр + радар диаграм
        ├── LearnPane.java           — Гарын авлага & нийтлэлүүд
        ├── PeoplePane.java          — Хэрэглэгч нээн судлах
        └── ProfilePane.java         — Профайл засварлах + табууд
```

---

## Програмыг ажиллуулах

**Урьдчилсан нөхцөл:** Java 21, Maven (wrapper багтсан)

```cmd
cd "C:\Users\thatu\OneDrive\Desktop\cafe-project"
mvnw.cmd javafx:run
```

Програм анх ажиллахдаа H2 өгөгдлийн санг тарьж, desktop цонх нээнэ.

> ⚠️ **Windows Command Prompt** дээр `mvnw.cmd javafx:run` (`./` угтваргүй) ашиглана
> **PowerShell / Git Bash** дээр `./mvnw.cmd javafx:run` ашиглана

---

## Туршилтын бүртгэлүүд

| Имэйл | Нууц үг | Хэрэглэгчийн нэр |
|---|---|---|
| `coffee@borgol.mn` | `password123` | coffee_master |
| `sara@borgol.mn` | `password123` | barista_sara |
| `tea@borgol.mn` | `password123` | tea_lover |

Нэмэлт 5 тарьсан хэрэглэгч: `latte_king`, `espresso_pro`, `cold_brew_queen`, `matcha_monk`, `roast_geek`

---

## Ашигласан дизайны хэв маягууд

| Хэв маяг | Хаана |
|---|---|
| **Singleton** | `DatabaseConnection` (DCL + volatile), `AppSession` |
| **Factory** | `RepositoryFactory` (DB болон MEM горим) |
| **DAO / Repository** | `BorgolRepository` — бүх SQL сервисийн давхаргаас тусгаарлагдсан |
| **Observer** | `MenuChangeObserver` (legacy цэсний сервис) |
| **Hexagonal Architecture** | Core домэйн UI болон DB давхаргаас тусгаарлагдсан |
| **Callback / Runnable** | `ProfilePane(onProfileUpdated)` — бүрэн дахин барихгүйгээр sidebar шинэчлэх |

---

## API Endpoint-үүд (Вэб давхарга)

| Бүлэг | Endpoint-үүд |
|---|---|
| Нэвтрэлт | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` |
| Хэрэглэгчид | `GET /api/users`, `PUT /api/users/me`, `POST/DELETE /api/users/{id}/follow`, `GET /api/users/search` |
| Жорууд | `GET/POST /api/recipes`, `GET/PUT/DELETE /api/recipes/{id}`, `POST /api/recipes/{id}/like`, `GET/POST /api/recipes/{id}/comments` |
| Лент | `GET /api/feed` |
| Кафенүүд | `GET/POST /api/cafes`, `GET /api/cafes/{id}`, `POST /api/cafes/{id}/rate` |
| Дэвтэр | `GET/POST /api/journal`, `PUT/DELETE /api/journal/{id}` |
| Суралцах | `GET /api/brew-guides`, `GET /api/brew-guides/{id}`, `GET /api/learn`, `GET /api/learn/{id}` |

---

## Тарьсан статик агуулга

**Буцалтын гарын авлага:** Pour Over (V60), French Press, AeroPress, Espresso, Cold Brew, Moka Pot
**Сурах нийтлэлүүд:** Шарсан түвшин, Гарцын шинжлэх ухаан, Усны чанар, Амт мэдрэх, Arabica vs Robusta, Нунтаглалтын хэмжээ

---

## Салбар

`feature/borgol-platform` → [GitHub](https://github.com/Apoca72/borgol-coffee-platform)
