# Parity Sprint Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the web/desktop feature parity gap by embedding Brew Timer as an SPA tab, adding QuickBrewOverlay to the web, and enriching seed data with specialty beans, UB cafes, and a drink guide.

**Architecture:** Three independent workstreams — (A) Java seed data, (B) web timer embedding, (C) web QuickBrewOverlay. All changes are additive. The existing `brew-timer.html` is kept untouched as a standalone page; `index.html` gains a `page-timer` section using the same JS logic, scoped with `_tmr_` prefixed state variables to avoid collisions.

**Tech Stack:** Java 17, JavaFX 21 (desktop, no changes), vanilla JS / HTML / CSS (web), PostgreSQL via JDBC (seed data)

---

## File Map

| File | What changes |
|------|-------------|
| `src/main/java/mn/edu/num/cafe/infrastructure/persistence/BorgolRepository.java` | Add `isBeanArticlesSeeded()`, `isCafesSeeded()` guard methods |
| `src/main/java/mn/edu/num/cafe/core/application/BorgolService.java` | Add `seedEnrichedContent()` with bean articles, UB cafes, drink guide |
| `src/main/java/mn/edu/num/cafe/app/Main.java` | Call `borgolService.seedEnrichedContent()` after existing `seedStaticContent()` |
| `src/main/resources/public/index.html` | Add timer CSS, `page-timer` HTML, timer JS (scoped), `loadRecipeIntoTimer()`, QuickBrewOverlay modal + JS, update nav links |

---

## Task 1: Seed guard methods in BorgolRepository

**Files:**
- Modify: `src/main/java/mn/edu/num/cafe/infrastructure/persistence/BorgolRepository.java` (after line 1280, before the mapping helpers section)

- [ ] **Step 1: Add two guard methods after `seedLearnArticle()`**

  Open `BorgolRepository.java`. After the closing `}` of `seedLearnArticle()` at line ~1280, insert:

  ```java
  public boolean isBeanArticlesSeeded() {
      return countNoParam("SELECT COUNT(*) FROM learn_articles WHERE category = 'Beans'") > 0;
  }

  public boolean isCafesSeeded() {
      return countNoParam("SELECT COUNT(*) FROM cafes") > 0;
  }
  ```

- [ ] **Step 2: Build to verify no compile errors**

  ```bash
  cd /c/Users/thatu/OneDrive/Desktop/cafe-project
  ./mvnw compile -q
  ```
  Expected: `BUILD SUCCESS` with no output.

- [ ] **Step 3: Commit**

  ```bash
  git add src/main/java/mn/edu/num/cafe/infrastructure/persistence/BorgolRepository.java
  git commit -m "feat: add isBeanArticlesSeeded and isCafesSeeded guards to BorgolRepository"
  ```

---

## Task 2: `seedEnrichedContent()` — specialty bean articles

**Files:**
- Modify: `src/main/java/mn/edu/num/cafe/core/application/BorgolService.java` (add method after `seedStaticContent()`)

- [ ] **Step 1: Add `seedEnrichedContent()` with bean articles**

  In `BorgolService.java`, after the closing `}` of `seedStaticContent()` (around line 620), insert the following new method. It uses the existing private `seedArticle()` helper and the new `isBeanArticlesSeeded()` guard:

  ```java
  public void seedEnrichedContent() {
      // ── Specialty Bean Articles ───────────────────────────────────────────
      if (!repo.isBeanArticlesSeeded()) {
          seedArticle("Ethiopian Yirgacheffe", "🌸", "Beans",
              "## Origin\nYirgacheffe is a town in the Gedeo Zone of southern Ethiopia, widely " +
              "regarded as the birthplace of coffee. Grown at 1,700–2,200 m altitude.\n\n" +
              "## Flavor Profile\nLight roast reveals **jasmine, blueberry, lemon zest, and bergamot**. " +
              "Bright, tea-like acidity with a delicate, clean finish.\n\n" +
              "## Roast\nBest enjoyed as a **light roast** (180–195°C) to preserve floral aromatics. " +
              "Medium roast brings out more caramel sweetness. Avoid dark — it destroys the character.\n\n" +
              "## Brew Pairings\nPour Over (V60), AeroPress, or filter drip. " +
              "High-temperature water (93–96°C) works well with light roasts.", 4);

          seedArticle("Colombian Huila", "🏔️", "Beans",
              "## Origin\nHuila is a mountainous department in southwest Colombia at 1,500–2,000 m. " +
              "Known for its volcanic soil, consistent rainfall, and family-run farms.\n\n" +
              "## Flavor Profile\n**Caramel sweetness, red apple, citrus, and milk chocolate** " +
              "with a smooth medium body and balanced acidity.\n\n" +
              "## Roast\nExcels as a **medium roast** (210–220°C). Retains sweetness without " +
              "becoming bitter. One of the most versatile origins for any brew method.\n\n" +
              "## Brew Pairings\nWorks beautifully for espresso, pour over, and French press. " +
              "An excellent daily driver for home baristas.", 4);

          seedArticle("Kenyan AA", "🦁", "Beans",
              "## Origin\nKenya's central highlands around Mt. Kenya and the Aberdare Range " +
              "(1,400–2,000 m). 'AA' is the largest screen size grade, indicating large, dense beans.\n\n" +
              "## Flavor Profile\n**Blackcurrant, tomato, grapefruit, and wine-like brightness**. " +
              "Complex, juicy acidity with a full body and lingering finish.\n\n" +
              "## Roast\n**Light to medium** (190–215°C). The SL28 and SL34 varietals express " +
              "maximum complexity when not over-roasted. A challenging but rewarding origin.\n\n" +
              "## Brew Pairings\nPour over showcases the brightness. French press enhances body. " +
              "Not ideal for espresso — acidity becomes sharp under pressure.", 4);

          seedArticle("Guatemalan Antigua", "🌋", "Beans",
              "## Origin\nGrown in the Antigua valley surrounded by three volcanoes — Agua, Fuego, " +
              "and Acatenango — at 1,500–1,700 m. Volcanic ash provides exceptional mineral richness.\n\n" +
              "## Flavor Profile\n**Dark chocolate, brown sugar, almond, and mild spice** with a " +
              "medium-heavy body and low, soft acidity.\n\n" +
              "## Roast\nShines at **medium-dark roast** (220–230°C). The chocolate notes deepen " +
              "without crossing into bitterness. One of the most consistent Central American origins.\n\n" +
              "## Brew Pairings\nExcellent for espresso and Moka Pot. Also exceptional as a " +
              "French press — the body holds up well to the metal filter.", 4);

          seedArticle("Indonesian Mandheling", "🌿", "Beans",
              "## Origin\nFrom the Batak highlands of North Sumatra, Indonesia, around Lake Toba " +
              "at 1,100–1,600 m. Processed using the unique 'wet-hulling' (Giling Basah) method.\n\n" +
              "## Flavor Profile\n**Earthy, cedar, dark chocolate, tobacco, and mushroom** with a " +
              "very full body and low acidity. Syrupy mouthfeel.\n\n" +
              "## Roast\nTraditionally roasted **dark** (230–245°C). The wet-hulling process " +
              "creates the characteristic earthy funk and heavy body. Not for the light-roast enthusiast.\n\n" +
              "## Brew Pairings\nFrench press and Moka Pot maximize the body. Also well-suited " +
              "to cold brew — the earthiness mellows beautifully over 18 hours.", 4);

          seedArticle("Brazilian Cerrado Mineiro", "🌾", "Beans",
              "## Origin\nThe Cerrado Mineiro region in Minas Gerais state, Brazil, at 800–1,300 m. " +
              "A designated Geographic Indication zone with dry climate and flat terrain — " +
              "suited to mechanized harvesting and consistent naturals.\n\n" +
              "## Flavor Profile\n**Milk chocolate, hazelnut, caramel, and soft sweetness** " +
              "with very low acidity and a smooth, round body.\n\n" +
              "## Roast\n**Medium roast** (210–220°C) highlights sweetness. The low acidity makes " +
              "it forgiving at medium-dark too. Brazil is the world's largest coffee producer " +
              "and Cerrado is its flagship specialty region.\n\n" +
              "## Brew Pairings\nThe go-to espresso base for blends. Excellent for cold brew " +
              "due to natural sweetness. Great entry-point origin for new specialty drinkers.", 4);
      }
  }
  ```

- [ ] **Step 2: Build to verify**

  ```bash
  ./mvnw compile -q
  ```
  Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

  ```bash
  git add src/main/java/mn/edu/num/cafe/core/application/BorgolService.java
  git commit -m "feat: seed 6 specialty coffee bean learn articles (Beans category)"
  ```

---

## Task 3: `seedEnrichedContent()` — UB cafe listings

**Files:**
- Modify: `src/main/java/mn/edu/num/cafe/core/application/BorgolService.java` (extend `seedEnrichedContent()`)

- [ ] **Step 1: Add a private `seedCafe()` helper at the bottom of the helpers section**

  After the existing `seedArticle()` private method (around line 643), add:

  ```java
  private void seedCafe(String name, String address, String district,
                        String description, String hours, double lat, double lng) {
      CafeListing c = new CafeListing();
      c.setName(name);
      c.setAddress(address);
      c.setDistrict(district);
      c.setCity("Ulaanbaatar");
      c.setDescription(description);
      c.setHours(hours);
      c.setLat(lat);
      c.setLng(lng);
      c.setSubmittedBy(0); // system — repo sets NULL for 0
      repo.createCafe(c);
  }
  ```

  Also add the required import at the top of the file (if not already present):
  ```java
  import mn.edu.num.cafe.core.domain.CafeListing;
  ```

- [ ] **Step 2: Add the UB cafe block inside `seedEnrichedContent()`**

  After the closing `}` of the bean articles block (still inside `seedEnrichedContent()`), add:

  ```java
      // ── Ulaanbaatar Cafe Listings ─────────────────────────────────────────
      if (!repo.isCafesSeeded()) {
          seedCafe(
              "Luna Blanca",
              "Peace Ave 15, Sukhbaatar District",
              "Sukhbaatar",
              "A beloved specialty coffee and brunch spot near the State Department Store. Known for single-origin pour overs and all-day breakfast.",
              "08:00–22:00",
              47.9184, 106.9177
          );
          seedCafe(
              "Nomads Coffee",
              "Olympic Street 12, Sukhbaatar District",
              "Sukhbaatar",
              "Local specialty roastery and café. Roasts their own beans in-house, with a rotating menu of Mongolian and imported single-origins.",
              "09:00–21:00",
              47.9161, 106.9203
          );
          seedCafe(
              "Café Amsterdam",
              "Baga Toiruu 6, Sukhbaatar District",
              "Sukhbaatar",
              "European-style café with fresh pastries, strong espresso, and a relaxed atmosphere. Popular with expats and students.",
              "08:30–21:30",
              47.9175, 106.9155
          );
          seedCafe(
              "Coffee Lab UB",
              "Seoul Street 34, Sukhbaatar District",
              "Sukhbaatar",
              "Specialty coffee training lab and tasting bar. Offers cupping sessions, barista courses, and a retail bean selection.",
              "10:00–20:00",
              47.9143, 106.9220
          );
          seedCafe(
              "Rocky Mountain Coffee",
              "Ulaanbaatar Hotel, Sukhbaatar District",
              "Sukhbaatar",
              "International specialty chain with consistent quality. Full espresso bar, seasonal drinks, and free Wi-Fi.",
              "07:00–22:00",
              47.9203, 106.9244
          );
          seedCafe(
              "Merkuri Coffee",
              "Tsagdaagiin Gudamj 8, Bayangol District",
              "Bayangol",
              "Minimalist pour-over focused café. Clean design, focused menu, and some of the best manual brew coffee in the city.",
              "09:00–20:00",
              47.9098, 106.8901
          );
          seedCafe(
              "Grand Coffee",
              "Narnii Road 22, Bayanzurkh District",
              "Bayanzurkh",
              "Large, comfortable café ideal for work and meetings. Extensive food menu alongside a full espresso bar.",
              "08:00–23:00",
              47.9226, 106.9498
          );
          seedCafe(
              "Espresso Yourself",
              "Zaisan Area, Khan-Uul District",
              "Khan-Uul",
              "Cozy neighborhood café in the Zaisan hills. Specialty roasts, homemade cakes, and a stunning view of the city.",
              "09:00–21:00",
              47.8912, 106.9063
          );
      }
  ```

- [ ] **Step 3: Build to verify**

  ```bash
  ./mvnw compile -q
  ```
  Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

  ```bash
  git add src/main/java/mn/edu/num/cafe/core/application/BorgolService.java
  git commit -m "feat: seed 8 Ulaanbaatar cafe listings with coordinates"
  ```

---

## Task 4: `seedEnrichedContent()` — drink guide article + wire up Main.java

**Files:**
- Modify: `src/main/java/mn/edu/num/cafe/core/application/BorgolService.java` (extend `seedEnrichedContent()`)
- Modify: `src/main/java/mn/edu/num/cafe/app/Main.java`

- [ ] **Step 1: Add the drink guide block inside `seedEnrichedContent()`**

  After the cafe listings block (still inside `seedEnrichedContent()`), add:

  ```java
      // ── Coffee Drink Guide Article ────────────────────────────────────────
      if (!repo.isDrinkArticleSeeded()) {
          seedArticle("Coffee Drink Guide", "☕", "Drinks",
              "## Espresso\n" +
              "A 25–30 ml concentrated shot extracted at 9 bar in 25–30 seconds. The foundation of most " +
              "café drinks. Intense, syrupy, with a golden-red crema. **Best bean:** Medium-dark or dark " +
              "roast (Brazilian, Guatemalan, or Italian blend).\n\n" +
              "## Americano\n" +
              "Espresso diluted with hot water (1:2–1:4). Maintains espresso flavor without the " +
              "concentration. **Milk ratio:** None. Often ordered black. Larger serving for those who " +
              "want espresso flavor in a longer drink.\n\n" +
              "## Latte\n" +
              "Espresso + steamed milk (150–200 ml) + thin microfoam layer. Ratio ~1:4 coffee to milk. " +
              "Creamy, mild, crowd-pleasing. **Best bean:** Medium roast (Colombian, Brazilian). " +
              "Canvas for latte art.\n\n" +
              "## Cappuccino\n" +
              "Equal thirds: espresso + steamed milk + thick milk foam. 150–180 ml total. " +
              "More intense than a latte, less sweet. Traditionally dry (more foam) or wet (more milk). " +
              "**Best bean:** Dark or medium-dark.\n\n" +
              "## Flat White\n" +
              "Double ristretto (short espresso) + 120 ml velvety microfoam — smaller and stronger than a latte. " +
              "Australian origin. **Best bean:** A blend or single origin with chocolate/nut notes.\n\n" +
              "## Cold Brew\n" +
              "Coffee steeped in cold water for 12–24 hours, then filtered. Smooth, naturally sweet, " +
              "low acidity. Serve over ice diluted 1:1. **Best bean:** Medium or dark roast with " +
              "chocolate and nut character (Brazilian Cerrado, Colombian).\n\n" +
              "## Pour Over\n" +
              "Manual filter brew using gravity. Produces a clean, bright, tea-like cup that highlights " +
              "delicate origin flavors. Methods: V60, Chemex, Kalita Wave. " +
              "**Best bean:** Light roast with floral/fruity notes (Ethiopian, Kenyan).\n\n" +
              "## French Press\n" +
              "Full-immersion brew with a metal mesh filter. Rich body, heavy mouthfeel, " +
              "more oils and texture than filter. 4-minute steep. **Best bean:** Medium-dark to dark " +
              "roast (Guatemalan, Indonesian Mandheling).", 6);
      }
  ```

  Also add `isDrinkArticleSeeded()` to `BorgolRepository.java` (after `isCafesSeeded()`):

  ```java
  public boolean isDrinkArticleSeeded() {
      return countNoParam("SELECT COUNT(*) FROM learn_articles WHERE category = 'Drinks'") > 0;
  }
  ```

- [ ] **Step 2: Wire `seedEnrichedContent()` in Main.java**

  In `Main.java`, find the line:
  ```java
  borgolService.seedStaticContent();
  ```
  Add one line immediately after it:
  ```java
  borgolService.seedEnrichedContent();
  ```

- [ ] **Step 3: Build to verify**

  ```bash
  ./mvnw compile -q
  ```
  Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

  ```bash
  git add src/main/java/mn/edu/num/cafe/core/application/BorgolService.java \
          src/main/java/mn/edu/num/cafe/infrastructure/persistence/BorgolRepository.java \
          src/main/java/mn/edu/num/cafe/app/Main.java
  git commit -m "feat: seed drink guide article + wire seedEnrichedContent in Main"
  ```

---

## Task 5: Add timer CSS to index.html

**Files:**
- Modify: `src/main/resources/public/index.html` (inside the `<style>` block)

- [ ] **Step 1: Find the end of the `<style>` block**

  Open `index.html`. The `<style>` block ends with `</style>` before `</head>`. Find the line with `</style>` (around line 476).

- [ ] **Step 2: Insert timer CSS before `</style>`**

  Add the following CSS block just before `</style>`. These classes are unique to the timer page — `.method-btn`, `.timer-card`, `.step-item`, etc. don't exist in the current index.html CSS. The only renamed classes are `.page-title` → `.timer-page-title` and `.page-sub` → `.timer-page-sub` to avoid potential future conflicts:

  ```css
  /* ── Brew Timer (page-timer) ─────────────────────────────────────────────── */
  .timer-page-title{font-family:var(--font-serif,serif);font-size:1.9rem;color:#2B1005;margin-bottom:4px}
  .timer-page-sub{color:var(--muted,#8A7054);font-size:.875rem;margin-bottom:28px}
  .method-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(110px,1fr));gap:10px;margin-bottom:28px}
  .method-btn{display:flex;flex-direction:column;align-items:center;gap:6px;padding:14px 10px;
    border-radius:14px;background:var(--surface,#FFFEF8);border:2px solid var(--border,#EDDBBA);
    cursor:pointer;transition:all .2s;font-family:inherit}
  .method-btn:hover{border-color:#CB8840;transform:translateY(-2px);box-shadow:0 2px 16px rgba(12,4,0,.08)}
  .method-btn.selected{border-color:#A8621E;background:rgba(168,98,30,.06);box-shadow:0 0 0 3px rgba(168,98,30,.12)}
  .method-icon{font-size:1.8rem}.method-name{font-size:.75rem;font-weight:700;color:#2B1005;text-align:center}
  .method-time{font-size:.65rem;color:var(--muted,#8A7054)}
  .ratio-card{background:var(--surface,#FFFEF8);border:1px solid var(--border,#EDDBBA);
    border-radius:20px;padding:24px 28px;margin-bottom:24px;box-shadow:0 2px 16px rgba(12,4,0,.08)}
  .ratio-card h3{font-family:var(--font-serif,serif);font-size:1.1rem;color:#2B1005;
    margin-bottom:18px;display:flex;align-items:center;gap:8px}
  .ratio-inputs{display:grid;grid-template-columns:1fr auto 1fr;gap:0;align-items:center}
  .ratio-field{display:flex;flex-direction:column;gap:4px}
  .ratio-label{font-size:.68rem;font-weight:700;text-transform:uppercase;letter-spacing:.07em;color:var(--muted,#8A7054)}
  .ratio-input{width:100%;padding:14px 16px;font-size:1.5rem;font-weight:700;color:#A8621E;
    background:var(--milk,#FDFAF3);border:2px solid var(--border,#EDDBBA);border-radius:14px;
    font-family:inherit;outline:none;transition:all .2s;text-align:center;-moz-appearance:textfield}
  .ratio-input::-webkit-inner-spin-button,.ratio-input::-webkit-outer-spin-button{-webkit-appearance:none}
  .ratio-input:focus{border-color:#A8621E;background:#fff;box-shadow:0 0 0 3px rgba(168,98,30,.1)}
  .ratio-input.linked{background:rgba(168,98,30,.04);border-color:rgba(168,98,30,.3)}
  .ratio-unit{font-size:.7rem;color:var(--muted,#8A7054);margin-top:2px;text-align:center}
  .ratio-sep{display:flex;flex-direction:column;align-items:center;gap:4px;padding:0 16px}
  .ratio-sep-line{color:var(--muted,#8A7054);font-size:1.2rem;font-weight:700}
  .ratio-sep-label{font-size:.65rem;color:var(--muted,#8A7054);white-space:nowrap}
  .ratio-presets{display:flex;gap:6px;flex-wrap:wrap;margin-top:14px}
  .ratio-preset{padding:5px 12px;border-radius:20px;font-size:.75rem;font-weight:600;
    background:var(--cream,#F6E8CC);border:1px solid var(--border,#EDDBBA);color:#A8621E;cursor:pointer;transition:.15s}
  .ratio-preset:hover,.ratio-preset.active{background:#A8621E;color:#fff;border-color:#A8621E}
  .timer-card{background:var(--surface,#FFFEF8);border:1px solid var(--border,#EDDBBA);
    border-radius:20px;padding:32px 28px;margin-bottom:24px;
    box-shadow:0 6px 30px rgba(12,4,0,.13);text-align:center}
  .timer-ring-wrap{position:relative;display:inline-flex;margin-bottom:24px}
  .timer-svg{transform:rotate(-90deg)}
  .timer-track{fill:none;stroke:var(--cream,#F6E8CC);stroke-width:8}
  .timer-arc{fill:none;stroke:#A8621E;stroke-width:8;stroke-linecap:round;transition:stroke-dashoffset .95s linear}
  .timer-arc.done{stroke:#2D6A4F}
  .timer-display{position:absolute;inset:0;display:flex;flex-direction:column;align-items:center;justify-content:center}
  .timer-time{font-family:var(--font-serif,serif);font-size:2.6rem;font-weight:700;color:#2B1005;line-height:1;letter-spacing:-.02em}
  .timer-phase{font-size:.75rem;color:var(--muted,#8A7054);margin-top:4px;font-weight:600}
  .timer-total-disp-txt{font-size:.68rem;color:var(--border,#EDDBBA);margin-top:2px}
  .timer-controls{display:flex;gap:12px;justify-content:center;margin-bottom:20px}
  .timer-btn{display:inline-flex;align-items:center;gap:8px;padding:12px 28px;
    border-radius:14px;border:none;font-family:inherit;font-size:.9rem;font-weight:700;cursor:pointer;transition:all .18s}
  .timer-btn.primary{background:linear-gradient(135deg,#A8621E,#CB8840);color:#fff;box-shadow:0 4px 16px rgba(168,98,30,.28)}
  .timer-btn.primary:hover{transform:translateY(-1px);box-shadow:0 6px 20px rgba(168,98,30,.38)}
  .timer-btn.secondary{background:var(--cream,#F6E8CC);color:#A8621E;border:1.5px solid var(--border,#EDDBBA)}
  .timer-btn.secondary:hover{background:var(--border,#EDDBBA)}
  .timer-btn:disabled{opacity:.45;cursor:not-allowed;transform:none !important}
  .steps-list{list-style:none;display:flex;flex-direction:column;gap:8px;text-align:left}
  .step-item{display:flex;align-items:center;gap:14px;padding:12px 16px;
    border-radius:14px;border:1.5px solid transparent;transition:all .3s;background:var(--milk,#FDFAF3)}
  .step-item.active{border-color:#A8621E;background:rgba(168,98,30,.05);animation:stepSlide .3s ease}
  .step-item.done{opacity:.55}
  .step-bullet{width:28px;height:28px;flex-shrink:0;border-radius:50%;display:flex;
    align-items:center;justify-content:center;font-size:.75rem;font-weight:700;
    border:2px solid var(--border,#EDDBBA);color:var(--muted,#8A7054);background:var(--surface,#FFFEF8);transition:.2s}
  .step-item.active .step-bullet{background:#A8621E;color:#fff;border-color:#A8621E}
  .step-item.done  .step-bullet{background:#2D6A4F;color:#fff;border-color:#2D6A4F}
  .step-time-lbl{font-size:.7rem;color:var(--muted,#8A7054);flex-shrink:0;margin-left:auto}
  .step-text{font-size:.875rem;color:var(--text,#180F08);line-height:1.4}
  .step-item.active .step-text{font-weight:600;color:#2B1005}
  .done-banner{display:none;text-align:center;padding:28px;
    background:linear-gradient(135deg,rgba(45,106,79,.08),rgba(45,106,79,.04));
    border:1.5px solid rgba(45,106,79,.25);border-radius:20px;margin-bottom:24px}
  .done-banner.show{display:block}
  .done-banner .icon{font-size:2.8rem;margin-bottom:10px}
  .done-banner h2{font-family:var(--font-serif,serif);font-size:1.6rem;color:#2D6A4F;margin-bottom:6px}
  .done-banner p{color:var(--muted,#8A7054);font-size:.875rem}
  /* QuickBrew overlay */
  .qb-progress{width:100%;height:4px;background:var(--border,#EDDBBA);border-radius:4px;margin-bottom:20px;overflow:hidden}
  .qb-progress-bar{height:100%;background:#A8621E;border-radius:4px;transition:width .3s}
  .qb-step-num{font-size:.75rem;font-weight:700;color:var(--muted,#8A7054);text-transform:uppercase;letter-spacing:.06em;margin-bottom:6px}
  .qb-step-text{font-size:1.05rem;color:var(--text,#180F08);line-height:1.5;font-weight:500;min-height:64px}
  .qb-recipe-title{font-family:var(--font-serif,serif);font-size:1.1rem;color:#2B1005;margin-bottom:16px}
  .qb-nav{display:flex;gap:10px;margin-top:20px;justify-content:space-between;align-items:center}
  ```

- [ ] **Step 3: Commit**

  ```bash
  git add src/main/resources/public/index.html
  git commit -m "feat(web): add brew timer and quickbrew CSS to index.html"
  ```

---

## Task 6: Add `page-timer` HTML and update nav links

**Files:**
- Modify: `src/main/resources/public/index.html`

- [ ] **Step 1: Add `page-timer` section after `page-map`**

  Find the closing `</div>` of `page-map` (around line 722 — it's followed by the `<!-- MODALS -->` comment block). Insert the following HTML between `</div>` (end of page-map) and the `<!-- MODALS -->` comment:

  ```html
  <div class="page" id="page-timer">
    <h1 class="timer-page-title">⏱️ Brew Timer</h1>
    <p class="timer-page-sub">Step-by-step guidance with a ratio calculator for your perfect cup</p>

    <div id="tmr-recipe-banner" style="display:none;background:rgba(168,98,30,.08);border:1.5px solid rgba(168,98,30,.3);border-radius:14px;padding:12px 16px;margin-bottom:20px;font-size:.875rem;color:#2B1005">
      <strong id="tmr-recipe-title"></strong> — recipe loaded. Steps updated below.
      <button onclick="tmr_clearRecipe()" style="float:right;border:none;background:none;cursor:pointer;color:var(--muted)">✕</button>
    </div>

    <div class="method-grid" id="tmr-method-grid"></div>

    <div class="ratio-card">
      <h3>⚖️ Ratio Calculator</h3>
      <div class="ratio-inputs">
        <div class="ratio-field">
          <div class="ratio-label">Coffee</div>
          <input class="ratio-input" id="tmr-coffee-g" type="number" min="1" max="500" value="20" step="0.5"/>
          <div class="ratio-unit">grams</div>
        </div>
        <div class="ratio-sep">
          <div class="ratio-sep-line">:</div>
          <div class="ratio-sep-label" id="tmr-ratio-display">1 : 15</div>
        </div>
        <div class="ratio-field">
          <div class="ratio-label">Water</div>
          <input class="ratio-input linked" id="tmr-water-ml" type="number" min="1" max="3000" value="300" step="5"/>
          <div class="ratio-unit">ml</div>
        </div>
      </div>
      <div class="ratio-presets" id="tmr-ratio-presets"></div>
    </div>

    <div class="done-banner" id="tmr-done-banner">
      <div class="icon">☕</div>
      <h2>Your brew is ready!</h2>
      <p id="tmr-done-msg">Enjoy every sip.</p>
    </div>

    <div class="timer-card">
      <div class="timer-ring-wrap">
        <svg class="timer-svg" width="180" height="180" viewBox="0 0 180 180">
          <circle class="timer-track" cx="90" cy="90" r="82"/>
          <circle class="timer-arc" id="tmr-arc" cx="90" cy="90" r="82"
            stroke-dasharray="515.2" stroke-dashoffset="0"/>
        </svg>
        <div class="timer-display">
          <div class="timer-time" id="tmr-display">0:00</div>
          <div class="timer-phase" id="tmr-phase">Ready</div>
          <div class="timer-total-disp-txt" id="tmr-total-disp"></div>
        </div>
      </div>
      <div class="timer-controls">
        <button class="timer-btn primary" id="tmr-btn-start" onclick="tmr_startTimer()">▶ Start</button>
        <button class="timer-btn secondary" id="tmr-btn-reset" onclick="tmr_resetTimer()">↺ Reset</button>
      </div>
      <ul class="steps-list" id="tmr-steps-list"></ul>
    </div>
  </div>
  ```

- [ ] **Step 2: Update desktop navbar Timer link**

  Find line ~489:
  ```html
  <a class="nav-link" href="brew-timer.html" style="text-decoration:none">⏱️ Timer</a>
  ```
  Replace with:
  ```html
  <button class="nav-link" onclick="showPage('timer')" data-i18n="timer">⏱️ Timer</button>
  ```

- [ ] **Step 3: Update mobile nav Timer link**

  Find line ~537:
  ```html
  <a class="mobile-nav-link" href="brew-timer.html" style="text-decoration:none;display:block">⏱️ Brew Timer</a>
  ```
  Replace with:
  ```html
  <button class="mobile-nav-link" onclick="toggleMobileNav();showPage('timer')">⏱️ Brew Timer</button>
  ```

- [ ] **Step 4: Verify in browser**

  Open `index.html` in a browser. Click Timer in the nav — it should switch to the Timer tab within the SPA (no page navigation). The method grid and ratio calculator should be visible (but non-functional until JS is added in Task 7).

- [ ] **Step 5: Commit**

  ```bash
  git add src/main/resources/public/index.html
  git commit -m "feat(web): add page-timer section and update nav links from brew-timer.html"
  ```

---

## Task 7: Add timer JS to index.html

**Files:**
- Modify: `src/main/resources/public/index.html` (JS section, near end of file before `</script>`)

- [ ] **Step 1: Find the JS section end**

  In `index.html`, find the last `</script>` tag (near the end of the file, around line 3330+). Insert the following block just before `</script>`.

  All state variables are prefixed `_tmr_` to avoid collisions with any existing index.html globals. Functions used in `onclick` attributes are prefixed `tmr_`.

  ```javascript
  // ═══════════════════════════════════════════════════════════════════════════
  // BREW TIMER MODULE
  // ═══════════════════════════════════════════════════════════════════════════

  const TMR_METHODS = [
    {
      id: 'v60', name: 'V60', icon: '🔺', time: '3–4 min',
      defaultRatio: 15, ratioPresets: [{label:'1:15',r:15},{label:'1:16',r:16},{label:'1:17',r:17}],
      steps: [
        { t: 0,   label: 'Rinse & preheat', note: 'Rinse filter with hot water, discard' },
        { t: 15,  label: 'Bloom', note: '2× coffee weight of water (e.g. 40 ml), wait 30 s' },
        { t: 45,  label: '1st Pour', note: 'Pour to 120 ml in slow spirals' },
        { t: 90,  label: '2nd Pour', note: 'Pour to total water weight in spirals' },
        { t: 150, label: 'Drain', note: 'Let coffee drain fully' },
        { t: 220, label: 'Done ☕', note: 'Remove dripper, enjoy!' }
      ]
    },
    {
      id: 'espresso', name: 'Espresso', icon: '☕', time: '25–30 s',
      defaultRatio: 2.5, ratioPresets: [{label:'1:2',r:2},{label:'1:2.5',r:2.5},{label:'1:3',r:3}],
      steps: [
        { t: 0,  label: 'Dose & tamp', note: 'Grind fine, tamp level at 30 lbs' },
        { t: 5,  label: 'Pre-infusion', note: '3–4 s low-pressure pre-infuse' },
        { t: 10, label: 'Extract', note: 'Full pressure — target golden-brown crema' },
        { t: 35, label: 'Done ☕', note: 'Pull shot, taste for balance' }
      ]
    },
    {
      id: 'frenchpress', name: 'French Press', icon: '🫖', time: '4 min',
      defaultRatio: 12, ratioPresets: [{label:'1:10',r:10},{label:'1:12',r:12},{label:'1:15',r:15}],
      steps: [
        { t: 0,   label: 'Add grounds', note: 'Coarse grind — like raw sugar' },
        { t: 15,  label: 'Bloom pour', note: 'Add 2× coffee weight water, stir gently' },
        { t: 45,  label: 'Full pour', note: 'Add remaining water' },
        { t: 60,  label: 'Place lid', note: 'Put lid on, do NOT press yet' },
        { t: 240, label: 'Press & pour', note: 'Press slowly, pour immediately to stop brewing' },
        { t: 280, label: 'Done ☕', note: 'Enjoy! Leave dregs in press.' }
      ]
    },
    {
      id: 'aeropress', name: 'AeroPress', icon: '🧪', time: '2 min',
      defaultRatio: 13, ratioPresets: [{label:'1:10',r:10},{label:'1:13',r:13},{label:'1:16',r:16}],
      steps: [
        { t: 0,  label: 'Setup', note: 'Insert filter, rinse, place on cup (inverted)' },
        { t: 15, label: 'Add coffee', note: 'Medium-fine grind, 15–20 g' },
        { t: 25, label: 'Add water', note: 'Fully saturate grounds with hot water (~80°C)' },
        { t: 35, label: 'Stir', note: 'Stir 10 seconds, attach plunger' },
        { t: 55, label: 'Steep', note: 'Wait 45–60 seconds' },
        { t: 100, label: 'Press', note: 'Flip onto cup, press in 20–30 seconds' },
        { t: 120, label: 'Done ☕', note: 'Dilute to taste or enjoy as is!' }
      ]
    },
    {
      id: 'moka', name: 'Moka Pot', icon: '🏺', time: '5 min',
      defaultRatio: 7, ratioPresets: [{label:'1:7',r:7},{label:'1:8',r:8},{label:'1:9',r:9}],
      steps: [
        { t: 0,   label: 'Fill bottom', note: 'Fill base with hot water to valve line' },
        { t: 30,  label: 'Add coffee', note: 'Fine grind, fill basket level (don\'t tamp)' },
        { t: 45,  label: 'Heat', note: 'Medium-low heat, lid open' },
        { t: 180, label: 'Watch', note: 'Gurgling? Reduce heat — stop at blond stream' },
        { t: 280, label: 'Cool base', note: 'Run cold water on base to stop extraction' },
        { t: 300, label: 'Done ☕', note: 'Pour and enjoy concentrated coffee!' }
      ]
    },
    {
      id: 'coldbrew', name: 'Cold Brew', icon: '🧊', time: '12–18 h',
      defaultRatio: 6, ratioPresets: [{label:'1:5',r:5},{label:'1:6',r:6},{label:'1:8',r:8}],
      steps: [
        { t: 0,     label: 'Coarse grind', note: 'Like very coarse sea salt' },
        { t: 30,    label: 'Combine', note: 'Mix coffee + cold water in jar, stir' },
        { t: 60,    label: 'Cover', note: 'Seal jar, refrigerate' },
        { t: 43200, label: 'Strain', note: 'After 12 h, strain through filter into bottle' },
        { t: 43260, label: 'Done ☕', note: 'Dilute 1:1 with water or milk. Keeps 2 weeks.' }
      ]
    }
  ];

  let _tmr_currentMethod  = TMR_METHODS[0];
  let _tmr_coffeeG        = 20;
  let _tmr_waterMl        = 300;
  let _tmr_interval       = null;
  let _tmr_elapsed        = 0;
  let _tmr_totalSec       = 0;
  let _tmr_running        = false;
  let _tmr_recipeSteps    = null; // non-null when recipe loaded
  const TMR_CIRCUM        = 515.2;

  function tmr_buildMethodGrid() {
    const grid = document.getElementById('tmr-method-grid');
    if (!grid) return;
    grid.innerHTML = TMR_METHODS.map(m => `
      <button class="method-btn${m.id === _tmr_currentMethod.id ? ' selected' : ''}"
        onclick="tmr_selectMethod('${m.id}')">
        <span class="method-icon">${m.icon}</span>
        <span class="method-name">${m.name}</span>
        <span class="method-time">${m.time}</span>
      </button>`).join('');
  }

  function tmr_selectMethod(id) {
    _tmr_currentMethod = TMR_METHODS.find(m => m.id === id);
    _tmr_recipeSteps = null;
    document.getElementById('tmr-recipe-banner').style.display = 'none';
    tmr_resetTimer();
    tmr_buildMethodGrid();
    tmr_buildRatioPresets();
    tmr_updateRatioDisplay();
    tmr_buildSteps();
    const preset = _tmr_currentMethod.defaultRatio;
    _tmr_waterMl = Math.round(_tmr_coffeeG * preset);
    document.getElementById('tmr-water-ml').value = _tmr_waterMl;
    tmr_updateRatioDisplay();
  }

  function tmr_buildRatioPresets() {
    const el = document.getElementById('tmr-ratio-presets');
    if (!el) return;
    el.innerHTML = _tmr_currentMethod.ratioPresets.map(p => `
      <button class="ratio-preset${Math.abs(_tmr_waterMl/_tmr_coffeeG - p.r) < 0.1 ? ' active' : ''}"
        onclick="tmr_applyPreset(${p.r})">${p.label}</button>`).join('');
  }

  function tmr_applyPreset(r) {
    _tmr_waterMl = Math.round(_tmr_coffeeG * r);
    document.getElementById('tmr-water-ml').value = _tmr_waterMl;
    tmr_updateRatioDisplay();
    tmr_buildRatioPresets();
  }

  function tmr_updateRatioDisplay() {
    const c = parseFloat(document.getElementById('tmr-coffee-g')?.value) || _tmr_coffeeG;
    const w = parseFloat(document.getElementById('tmr-water-ml')?.value) || _tmr_waterMl;
    const el = document.getElementById('tmr-ratio-display');
    if (el) el.textContent = `1 : ${(w/c).toFixed(1)}`;
  }

  function tmr_bindRatioInputs() {
    const coffeeEl = document.getElementById('tmr-coffee-g');
    const waterEl  = document.getElementById('tmr-water-ml');
    if (!coffeeEl || !waterEl) return;
    coffeeEl.addEventListener('input', function() {
      const prev = _tmr_coffeeG || 1;
      _tmr_coffeeG = parseFloat(this.value) || 1;
      _tmr_waterMl = Math.round(_tmr_coffeeG * (_tmr_waterMl / prev));
      waterEl.value = _tmr_waterMl;
      tmr_updateRatioDisplay();
      tmr_buildRatioPresets();
    });
    waterEl.addEventListener('input', function() {
      _tmr_waterMl = parseFloat(this.value) || 1;
      tmr_updateRatioDisplay();
      tmr_buildRatioPresets();
    });
  }

  function tmr_getSteps() {
    return _tmr_recipeSteps || _tmr_currentMethod.steps;
  }

  function tmr_buildSteps() {
    const steps = tmr_getSteps();
    _tmr_totalSec = steps[steps.length - 1].t;
    const ul = document.getElementById('tmr-steps-list');
    if (!ul) return;
    ul.innerHTML = steps.map((s, i) => `
      <li class="step-item${i === 0 ? ' active' : ''}" id="tmr-step-${i}">
        <div class="step-bullet">${i === 0 ? '▶' : i + 1}</div>
        <div>
          <div class="step-text"><strong>${s.label}</strong>${s.note ? ' — ' + s.note : ''}</div>
        </div>
        <div class="step-time-lbl">${tmr_formatTime(s.t)}</div>
      </li>`).join('');
  }

  function tmr_startTimer() {
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission();
    }
    if (_tmr_running) {
      clearInterval(_tmr_interval);
      _tmr_running = false;
      document.getElementById('tmr-btn-start').textContent = '▶ Resume';
      return;
    }
    if (_tmr_totalSec > 3600) {
      const banner = document.getElementById('tmr-done-banner');
      banner.classList.add('show');
      document.getElementById('tmr-done-msg').textContent =
        'This brew takes ~' + Math.round(_tmr_totalSec / 3600) + 'h. Follow the steps below.';
      return;
    }
    _tmr_running = true;
    document.getElementById('tmr-btn-start').textContent = '⏸ Pause';
    document.getElementById('tmr-done-banner').classList.remove('show');
    _tmr_interval = setInterval(() => {
      _tmr_elapsed++;
      tmr_updateTimerDisplay();
      tmr_updateSteps();
      if (_tmr_elapsed >= _tmr_totalSec) {
        clearInterval(_tmr_interval);
        _tmr_running = false;
        document.getElementById('tmr-btn-start').disabled = true;
        document.getElementById('tmr-btn-start').textContent = '✓ Done';
        tmr_showDone();
      }
    }, 1000);
  }

  function tmr_resetTimer() {
    clearInterval(_tmr_interval);
    _tmr_running = false; _tmr_elapsed = 0;
    const startBtn = document.getElementById('tmr-btn-start');
    if (startBtn) { startBtn.textContent = '▶ Start'; startBtn.disabled = false; }
    const banner = document.getElementById('tmr-done-banner');
    if (banner) banner.classList.remove('show');
    tmr_updateTimerDisplay();
    tmr_buildSteps();
  }

  function tmr_updateTimerDisplay() {
    const steps  = tmr_getSteps();
    const last   = steps[steps.length - 1].t;
    const prog   = last > 0 ? Math.min(_tmr_elapsed / last, 1) : 0;
    const offset = TMR_CIRCUM * (1 - prog);
    const arc    = document.getElementById('tmr-arc');
    if (arc) { arc.style.strokeDashoffset = offset; arc.classList.toggle('done', prog >= 1); }
    const dispEl = document.getElementById('tmr-display');
    if (dispEl) dispEl.textContent = tmr_formatTime(_tmr_elapsed);
    let phase = steps[0].label;
    for (let i = steps.length - 1; i >= 0; i--) {
      if (_tmr_elapsed >= steps[i].t) { phase = steps[i].label; break; }
    }
    const phaseEl = document.getElementById('tmr-phase');
    if (phaseEl) phaseEl.textContent = phase;
    const totalEl = document.getElementById('tmr-total-disp');
    if (totalEl) totalEl.textContent = `${tmr_formatTime(_tmr_elapsed)} / ${tmr_formatTime(last)}`;
  }

  function tmr_updateSteps() {
    const steps = tmr_getSteps();
    steps.forEach((s, i) => {
      const el   = document.getElementById(`tmr-step-${i}`);
      if (!el) return;
      const next = steps[i + 1];
      const done   = next ? _tmr_elapsed > next.t : _tmr_elapsed >= s.t;
      const active = _tmr_elapsed >= s.t && (next ? _tmr_elapsed < next.t : true);
      el.classList.toggle('active', active);
      el.classList.toggle('done',   done);
      const bullet = el.querySelector('.step-bullet');
      if (done)        bullet.textContent = '✓';
      else if (active) bullet.textContent = '▶';
      else             bullet.textContent = i + 1;
      if (active) el.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    });
  }

  function tmr_showDone() {
    const banner = document.getElementById('tmr-done-banner');
    if (banner) banner.classList.add('show');
    const msg = document.getElementById('tmr-done-msg');
    if (msg) msg.textContent =
      `${_tmr_currentMethod.name} with ${_tmr_coffeeG}g coffee & ${_tmr_waterMl}ml water. Enjoy! ☕`;
    if ('Notification' in window && Notification.permission === 'granted') {
      new Notification('Borgol ☕', { body: `Your ${_tmr_currentMethod.name} is ready!` });
    }
  }

  function tmr_formatTime(secs) {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  function tmr_init() {
    tmr_buildMethodGrid();
    tmr_buildRatioPresets();
    tmr_updateRatioDisplay();
    tmr_buildSteps();
    tmr_updateTimerDisplay();
    tmr_bindRatioInputs();
  }

  // ── Recipe → Timer handoff ───────────────────────────────────────────────────
  function loadRecipeIntoTimer(recipe) {
    const instructions = recipe.instructions || recipe.steps || '';
    const lines = instructions.split('\n').map(l => l.trim()).filter(l => l.length > 0);
    if (lines.length === 0) {
      // No instructions — just show method selection
      _tmr_recipeSteps = null;
    } else {
      // Convert lines into timed steps spaced 30 s apart (simple heuristic)
      const durSec = (recipe.brewTime || 5) * 60;
      const interval = Math.floor(durSec / Math.max(lines.length, 1));
      _tmr_recipeSteps = lines.map((l, i) => ({
        t: i * interval,
        label: l.replace(/^\d+[\.\)]\s*/, ''), // strip leading "1." or "1)"
        note: ''
      }));
      // Ensure last step is at durSec
      _tmr_recipeSteps[_tmr_recipeSteps.length - 1].t = durSec;
    }
    // Show banner with recipe name
    const banner = document.getElementById('tmr-recipe-banner');
    const title  = document.getElementById('tmr-recipe-title');
    if (banner && title) {
      title.textContent = recipe.title || 'Recipe';
      banner.style.display = 'block';
    }
    tmr_resetTimer();
  }

  function tmr_clearRecipe() {
    _tmr_recipeSteps = null;
    const banner = document.getElementById('tmr-recipe-banner');
    if (banner) banner.style.display = 'none';
    tmr_resetTimer();
  }

  // Initialize timer when the timer page is first shown
  const _tmr_origShowPage = typeof showPage === 'function' ? showPage : null;
  // Hook into page transitions — init on first visit to timer tab
  let _tmr_initialized = false;
  document.addEventListener('DOMContentLoaded', () => {
    // Also init immediately in case DOMContentLoaded already fired
    const orig = window.showPage;
    if (orig) {
      window.showPage = function(name, ...args) {
        orig(name, ...args);
        if (name === 'timer' && !_tmr_initialized) {
          _tmr_initialized = true;
          tmr_init();
        }
      };
    }
  });
  ```

- [ ] **Step 2: Find existing `useInTimer` / timer navigation calls and update them**

  Search for any calls to `brew-timer.html` in the JS section of index.html:
  ```bash
  grep -n "brew-timer\|useInTimer\|Use in Timer" src/main/resources/public/index.html
  ```
  If any JS calls `window.location` or `href` to `brew-timer.html`, replace with:
  ```javascript
  loadRecipeIntoTimer(recipe);
  showPage('timer');
  ```

- [ ] **Step 3: Verify timer works in browser**

  Open `index.html`. Navigate to Timer tab. The method grid should render. Select a method — ratio presets should update. Click Start — timer should count down, steps highlight, ring animates. Click Reset — resets to 0.

- [ ] **Step 4: Commit**

  ```bash
  git add src/main/resources/public/index.html
  git commit -m "feat(web): embed brew timer JS module in index.html with tmr_ scoped state"
  ```

---

## Task 8: QuickBrewOverlay modal in web

**Files:**
- Modify: `src/main/resources/public/index.html`

- [ ] **Step 1: Add QuickBrew modal HTML**

  Find the `<!-- MODALS -->` section in index.html. After the last existing modal's closing `</div>` (before `</div>` that closes the modals section or before `<script>`), add:

  ```html
  <!-- QuickBrew Overlay -->
  <div class="modal-overlay" id="quick-brew-modal" onclick="if(event.target===this)closeModal('quick-brew-modal')">
    <div class="modal" style="max-width:480px">
      <div class="modal-header">
        <div class="modal-title">⚡ Quick Brew</div>
        <button class="modal-close" onclick="closeModal('quick-brew-modal')">✕</button>
      </div>
      <div class="qb-progress"><div class="qb-progress-bar" id="qb-bar"></div></div>
      <div class="qb-recipe-title" id="qb-title"></div>
      <div class="qb-step-num" id="qb-step-num"></div>
      <div class="qb-step-text" id="qb-step-text"></div>
      <div class="qb-nav">
        <button class="btn btn-secondary" id="qb-prev" onclick="qb_prev()">← Prev</button>
        <button class="btn btn-secondary" id="qb-next" onclick="qb_next()">Next →</button>
      </div>
      <div style="margin-top:16px;text-align:center">
        <button class="btn btn-primary" onclick="qb_openInTimer()">⏱️ Start Full Timer</button>
      </div>
    </div>
  </div>
  ```

- [ ] **Step 2: Add QuickBrew JS**

  In the JS section (just before `</script>` or at the end of the Brew Timer module block), add:

  ```javascript
  // ═══════════════════════════════════════════════════════════════════════════
  // QUICKBREW OVERLAY MODULE
  // ═══════════════════════════════════════════════════════════════════════════
  let _qb_recipe = null;
  let _qb_steps  = [];
  let _qb_idx    = 0;

  function openQuickBrew(recipe) {
    _qb_recipe = recipe;
    const instructions = recipe.instructions || recipe.steps || '';
    _qb_steps = instructions.split('\n').map(l => l.trim()).filter(l => l.length > 0);
    if (_qb_steps.length === 0) _qb_steps = ['Brew for ' + (recipe.brewTime || 5) + ' minutes.'];
    _qb_idx = 0;
    document.getElementById('qb-title').textContent = recipe.title || 'Recipe';
    qb_render();
    openModal('quick-brew-modal');
  }

  function qb_render() {
    const total = _qb_steps.length;
    const pct   = total > 1 ? (_qb_idx / (total - 1)) * 100 : 100;
    document.getElementById('qb-bar').style.width = pct + '%';
    document.getElementById('qb-step-num').textContent = `Step ${_qb_idx + 1} of ${total}`;
    document.getElementById('qb-step-text').textContent =
      _qb_steps[_qb_idx].replace(/^\d+[\.\)]\s*/, '');
    document.getElementById('qb-prev').disabled = _qb_idx === 0;
    document.getElementById('qb-next').disabled = _qb_idx === total - 1;
  }

  function qb_prev() { if (_qb_idx > 0) { _qb_idx--; qb_render(); } }
  function qb_next() { if (_qb_idx < _qb_steps.length - 1) { _qb_idx++; qb_render(); } }

  function qb_openInTimer() {
    closeModal('quick-brew-modal');
    if (_qb_recipe) { loadRecipeIntoTimer(_qb_recipe); showPage('timer'); }
  }
  ```

- [ ] **Step 3: Add "Easy Make" button to recipe cards**

  Search for the recipe card render function (e.g. `renderRecipeCard` or `buildRecipeHtml`) in index.html JS. Find where the "Use in Timer" button is rendered and add an "Easy Make" button next to it:

  ```html
  <button onclick="openQuickBrew(recipe)" title="Step-by-step guide" style="border:none;background:none;cursor:pointer;font-size:13px;color:var(--caramel);padding:5px 8px;border-radius:8px;transition:.15s" onmouseover="this.style.background='rgba(168,98,30,.1)'" onmouseout="this.style.background='none'">⚡ Easy Make</button>
  ```

  Find the exact spot by searching for the existing "Use in Timer" button render string in the JS:
  ```bash
  grep -n "Use in Timer\|useInTimer\|timer" src/main/resources/public/index.html | grep -v "css\|toastTimer\|brew-timer.html"
  ```

- [ ] **Step 4: Verify in browser**

  Open `index.html`. On the Feed or Recipes page, find a recipe card. Click "Easy Make" — the QuickBrew modal should open showing the first instruction step with a progress bar. Click Next/Prev to navigate steps. Click "Start Full Timer" — modal closes, Timer tab opens with the recipe steps loaded.

- [ ] **Step 5: Commit**

  ```bash
  git add src/main/resources/public/index.html
  git commit -m "feat(web): add QuickBrewOverlay modal with step navigation and timer handoff"
  ```

---

## Task 9: Final verification

- [ ] **Step 1: Build the full project**

  ```bash
  ./mvnw package -q -DskipTests
  ```
  Expected: `BUILD SUCCESS`, JAR produced in `target/`.

- [ ] **Step 2: Run and verify seed data**

  Start the app (or use Railway deployment). Check:
  - `GET /api/learn-articles` — should include 6 Bean articles and 1 Drinks article
  - `GET /api/cafes` — should list 8 UB cafe entries with lat/lng
  - Web Map pane → should show cafe pins in Ulaanbaatar

- [ ] **Step 3: Verify timer parity**

  Open both `index.html` (Timer tab) and `brew-timer.html` side-by-side. Both should:
  - Show the same 6 method buttons
  - Ratio calculator updates bidirectionally
  - Timer counts down, ring animates, steps highlight

- [ ] **Step 4: Verify recipe → timer flow**

  On Feed page: find a recipe with instructions. Click "Use in Timer" — Timer tab opens with recipe steps. Click "Easy Make" — QuickBrew modal opens with step navigation. Click "Start Full Timer" from QuickBrew modal — Timer tab opens with recipe steps.

- [ ] **Step 5: Final commit**

  ```bash
  git add -A
  git commit -m "feat: complete parity sprint — timer SPA tab, QuickBrew overlay, seed data"
  ```
