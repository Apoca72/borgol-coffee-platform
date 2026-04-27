# Borgol Features Roadmap — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` or `superpowers:subagent-driven-development` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement 8 new features across the Borgol coffee platform — quick UX wins, journal enhancements, social features, and gamification.

**Architecture:** All features follow the existing pattern: new DB tables are added idempotently in `BorgolRepository.initSchema()`, service methods in `BorgolService`, REST endpoints in `BorgolApiServer`, and UI in `src/main/resources/public/index.html` (single-file frontend).

**Tech Stack:** Java 21, Javalin 6, PostgreSQL/H2, vanilla JS, CSS custom properties. Chart.js (CDN) added for stats charts. Open-Meteo API (free, no key) for weather.

---

## Feature Map

| # | Feature | Backend | Frontend | Complexity |
|---|---------|---------|---------|-----------|
| 1 | Dark mode system-preference auto-follow | None | JS only | ⭐ |
| 2 | Brew ratio calculator panel | None | JS+HTML | ⭐⭐ |
| 3 | Bean/bag tracker | New table + API | New page section | ⭐⭐⭐ |
| 4 | Weather logging in journal | Schema change + API | Journal form | ⭐⭐ |
| 5 | Monthly brew stats | New query endpoints | Chart.js charts | ⭐⭐⭐ |
| 6 | Recipe collections | New tables + API | New UI section | ⭐⭐⭐ |
| 7 | Cafe check-ins | New table + API | Cafe detail UI | ⭐⭐ |
| 8 | Achievement badges | New table + logic | Profile + toast | ⭐⭐⭐⭐ |

---

## Key File Reference

```
src/main/java/borgol/
  core/
    domain/                         ← Add new domain classes here
      BeanBag.java                  ← NEW (Feature 3)
      CafeCheckin.java              ← NEW (Feature 7)
      Achievement.java              ← NEW (Feature 8)
    application/
      BorgolService.java            ← Add service methods for each feature
  infrastructure/
    persistence/
      BorgolRepository.java         ← Add SQL: initSchema() + new methods
  ui/
    web/
      BorgolApiServer.java          ← Add REST routes + handlers

src/main/resources/public/
  index.html                        ← All frontend: CSS + HTML + JS (3969 lines)
```

**index.html sections to know:**
- CSS: lines 1–490
- HTML body: lines 490–1380
- JS functions: lines 1380–3969
- `initTheme()`: line 1427
- `renderJournalGrid()`: line 2559
- `openJournalDetail()`: line 2695
- Journal modal HTML: line 1107
- Timer functions `tmr_*`: line 3425+

---

## Phase 1 — Quick Wins (No Backend)

### Feature 1: Dark Mode System-Preference Auto-Follow

**Problem:** `initTheme()` doesn't set the toggle button icon when the browser is in system dark mode on first load — CSS auto-applies but JS state is wrong.

**File:** `src/main/resources/public/index.html` — line ~1427

- [ ] **Step 1: Replace `initTheme` IIFE**

Find this block (around line 1427):
```js
(function initTheme() {
  const saved = localStorage.getItem('borgol_dark');
  if (saved !== null) applyTheme(saved === '1');
  // else: CSS @media query auto-applies, no JS class needed
})();
```

Replace with:
```js
(function initTheme() {
  const saved = localStorage.getItem('borgol_dark');
  if (saved !== null) {
    applyTheme(saved === '1');
  } else {
    // Follow system preference and keep JS state in sync
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    applyTheme(prefersDark);
    // Listen for system changes
    window.matchMedia('(prefers-color-scheme: dark)')
      .addEventListener('change', e => {
        if (localStorage.getItem('borgol_dark') === null) applyTheme(e.matches);
      });
  }
})();
```

- [ ] **Step 2: Commit**
```bash
git add src/main/resources/public/index.html
git commit -m "fix(ui): sync dark mode toggle button with system preference on load"
git push github main
```

---

### Feature 2: Brew Ratio Calculator Panel

**What:** A standalone panel in the Journal page (above the journal grid) with sliders for Dose, Ratio, and Yield — live calculation. No backend needed.

**File:** `src/main/resources/public/index.html`

- [ ] **Step 1: Add CSS for ratio calculator**

Find `.journal-grid{display:grid...}` (around line 444) and add after it:
```css
/* ── Ratio Calculator ──────────────────────────────────────────────────────── */
.ratio-calc{background:var(--surface);border:1px solid var(--border);border-radius:var(--radius);padding:18px 22px;margin-bottom:20px}
.ratio-calc-title{font-size:13px;font-weight:800;color:var(--muted);text-transform:uppercase;letter-spacing:.6px;margin-bottom:14px}
.ratio-row{display:flex;align-items:center;gap:12px;margin-bottom:10px}
.ratio-lbl{font-size:12px;font-weight:700;color:var(--muted);width:52px;flex-shrink:0}
.ratio-slider{flex:1;accent-color:var(--caramel)}
.ratio-val{font-size:14px;font-weight:800;color:var(--roast);width:54px;text-align:right}
.ratio-result{display:flex;gap:12px;margin-top:14px;padding-top:12px;border-top:1px solid var(--border);flex-wrap:wrap}
.ratio-chip{background:var(--cream);border:1px solid var(--border);border-radius:10px;padding:6px 14px;font-size:12px;font-weight:700;color:var(--roast);text-align:center}
.ratio-chip span{display:block;font-size:10px;font-weight:400;color:var(--muted);margin-top:1px}
```

- [ ] **Step 2: Add HTML panel inside `#page-journal`**

Find `<div id="page-journal">` (around line 785) and insert the ratio panel right after the section-header div and before `<div id="journal-grid"`:
```html
  <!-- Ratio Calculator -->
  <div class="ratio-calc" id="ratio-calc">
    <div class="ratio-calc-title">⚖️ Brew Ratio Calculator</div>
    <div class="ratio-row">
      <span class="ratio-lbl">Dose</span>
      <input class="ratio-slider" type="range" id="rc-dose" min="8" max="40" value="18" step="0.5" oninput="rcUpdate()">
      <span class="ratio-val"><span id="rc-dose-val">18</span>g</span>
    </div>
    <div class="ratio-row">
      <span class="ratio-lbl">Ratio</span>
      <input class="ratio-slider" type="range" id="rc-ratio" min="1" max="20" value="2" step="0.1" oninput="rcUpdate()">
      <span class="ratio-val">1:<span id="rc-ratio-val">2.0</span></span>
    </div>
    <div class="ratio-result" id="rc-result"></div>
  </div>
```

- [ ] **Step 3: Add JS function**

Add before or after `renderJournalGrid` (line ~2559):
```js
function rcUpdate() {
  const dose  = parseFloat(document.getElementById('rc-dose').value);
  const ratio = parseFloat(document.getElementById('rc-ratio').value);
  const yield_ = +(dose * ratio).toFixed(1);
  const tds    = +(1.35 / ratio * 100).toFixed(2); // simplified TDS estimate
  document.getElementById('rc-dose-val').textContent  = dose;
  document.getElementById('rc-ratio-val').textContent = ratio.toFixed(1);
  document.getElementById('rc-result').innerHTML = `
    <div class="ratio-chip">${yield_}g<span>Yield / Water</span></div>
    <div class="ratio-chip">${(dose+yield_).toFixed(1)}g<span>Total Weight</span></div>
    <div class="ratio-chip">~${tds}%<span>Est. TDS</span></div>
    <div class="ratio-chip">${ratio >= 15 ? 'Filter' : ratio >= 8 ? 'Espresso+' : 'Espresso'}<span>Style</span></div>
  `;
}
```

- [ ] **Step 4: Call `rcUpdate()` on journal page load**

Find `showPage` function and inside the `journal` case (look for `JOURNAL_ENTRIES = await api('/api/journal')`), add `rcUpdate();` after the existing call:
```js
// inside the journal page load block:
rcUpdate();
```

- [ ] **Step 5: Commit**
```bash
git add src/main/resources/public/index.html
git commit -m "feat(ui): brew ratio calculator panel on journal page"
git push github main
```

---

## Phase 2 — Journal Enhancements

### Feature 3: Bean/Bag Tracker

**What:** A new "My Beans" section where users log coffee bags they own/have tried: name, roaster, origin, roast date, remaining grams, rating.

#### 3a — Backend

**Files:**
- Create: `src/main/java/borgol/core/domain/BeanBag.java`
- Modify: `BorgolRepository.java` — `initSchema()` + CRUD methods
- Modify: `BorgolService.java` — service methods
- Modify: `BorgolApiServer.java` — REST routes

- [ ] **Step 1: Create domain class**

Create `src/main/java/borgol/core/domain/BeanBag.java`:
```java
package borgol.core.domain;

public class BeanBag {
    private int id;
    private int userId;
    private String name;
    private String roaster;
    private String origin;
    private String roastLevel;   // LIGHT, MEDIUM, MEDIUM-DARK, DARK
    private String roastDate;    // ISO date string, nullable
    private double remainingGrams;
    private int rating;          // 1-5, 0 = unrated
    private String notes;
    private String createdAt;

    // Getters/setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRoaster() { return roaster; }
    public void setRoaster(String roaster) { this.roaster = roaster; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getRoastLevel() { return roastLevel; }
    public void setRoastLevel(String roastLevel) { this.roastLevel = roastLevel; }
    public String getRoastDate() { return roastDate; }
    public void setRoastDate(String roastDate) { this.roastDate = roastDate; }
    public double getRemainingGrams() { return remainingGrams; }
    public void setRemainingGrams(double g) { this.remainingGrams = g; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 2: Add DB table in `BorgolRepository.initSchema()`**

Inside `initSchema()`, after the last `s.execute(...)` block (around line 290), add:
```java
s.execute("""
    CREATE TABLE IF NOT EXISTS bean_bags (
        id              INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
        user_id         INT          NOT NULL REFERENCES borgol_users(id) ON DELETE CASCADE,
        name            VARCHAR(120) NOT NULL,
        roaster         VARCHAR(100) DEFAULT '',
        origin          VARCHAR(100) DEFAULT '',
        roast_level     VARCHAR(20)  DEFAULT 'MEDIUM',
        roast_date      DATE,
        remaining_grams NUMERIC(7,1) DEFAULT 0,
        rating          INT          DEFAULT 0,
        notes           VARCHAR(500) DEFAULT '',
        created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
    )""");
```

- [ ] **Step 3: Add CRUD methods to `BorgolRepository`**

Add these methods at the end of `BorgolRepository.java` (before the last `}`):
```java
// ── Bean Bag operations ───────────────────────────────────────────────────

public List<BeanBag> getBeanBags(int userId) {
    String sql = "SELECT * FROM bean_bags WHERE user_id=? ORDER BY created_at DESC";
    List<BeanBag> list = new ArrayList<>();
    try (PreparedStatement ps = conn().prepareStatement(sql)) {
        ps.setInt(1, userId);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapBeanBag(rs));
        }
    } catch (SQLException e) { throw new RuntimeException(e); }
    return list;
}

public BeanBag createBeanBag(BeanBag b) {
    String sql = """
        INSERT INTO bean_bags (user_id, name, roaster, origin, roast_level,
          roast_date, remaining_grams, rating, notes)
        VALUES (?,?,?,?,?,?,?,?,?)""";
    try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        ps.setInt(1, b.getUserId());
        ps.setString(2, nvl(b.getName()));
        ps.setString(3, nvl(b.getRoaster()));
        ps.setString(4, nvl(b.getOrigin()));
        ps.setString(5, nvl(b.getRoastLevel()));
        ps.setObject(6, b.getRoastDate() != null && !b.getRoastDate().isBlank()
            ? java.sql.Date.valueOf(b.getRoastDate()) : null);
        ps.setDouble(7, b.getRemainingGrams());
        ps.setInt(8, b.getRating());
        ps.setString(9, nvl(b.getNotes()));
        ps.executeUpdate();
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys.next()) b.setId(keys.getInt(1));
        }
    } catch (SQLException e) { throw new RuntimeException(e); }
    return b;
}

public BeanBag updateBeanBag(BeanBag b) {
    String sql = """
        UPDATE bean_bags SET name=?, roaster=?, origin=?, roast_level=?,
          roast_date=?, remaining_grams=?, rating=?, notes=?
        WHERE id=? AND user_id=?""";
    try (PreparedStatement ps = conn().prepareStatement(sql)) {
        ps.setString(1, nvl(b.getName()));
        ps.setString(2, nvl(b.getRoaster()));
        ps.setString(3, nvl(b.getOrigin()));
        ps.setString(4, nvl(b.getRoastLevel()));
        ps.setObject(5, b.getRoastDate() != null && !b.getRoastDate().isBlank()
            ? java.sql.Date.valueOf(b.getRoastDate()) : null);
        ps.setDouble(6, b.getRemainingGrams());
        ps.setInt(7, b.getRating());
        ps.setString(8, nvl(b.getNotes()));
        ps.setInt(9, b.getId());
        ps.setInt(10, b.getUserId());
        ps.executeUpdate();
    } catch (SQLException e) { throw new RuntimeException(e); }
    return b;
}

public void deleteBeanBag(int id, int userId) {
    try (PreparedStatement ps = conn().prepareStatement(
            "DELETE FROM bean_bags WHERE id=? AND user_id=?")) {
        ps.setInt(1, id); ps.setInt(2, userId);
        ps.executeUpdate();
    } catch (SQLException e) { throw new RuntimeException(e); }
}

private BeanBag mapBeanBag(ResultSet rs) throws SQLException {
    BeanBag b = new BeanBag();
    b.setId(rs.getInt("id"));
    b.setUserId(rs.getInt("user_id"));
    b.setName(nullToEmpty(rs.getString("name")));
    b.setRoaster(nullToEmpty(rs.getString("roaster")));
    b.setOrigin(nullToEmpty(rs.getString("origin")));
    b.setRoastLevel(nullToEmpty(rs.getString("roast_level")));
    java.sql.Date d = rs.getDate("roast_date");
    b.setRoastDate(d != null ? d.toString() : "");
    b.setRemainingGrams(rs.getDouble("remaining_grams"));
    b.setRating(rs.getInt("rating"));
    b.setNotes(nullToEmpty(rs.getString("notes")));
    Timestamp ts = rs.getTimestamp("created_at");
    if (ts != null) b.setCreatedAt(ts.toLocalDateTime().toString());
    return b;
}
```

- [ ] **Step 4: Add service methods to `BorgolService.java`**

Add at end of `BorgolService` (before last `}`):
```java
public List<BeanBag> getBeanBags(int userId) {
    return repo.getBeanBags(userId);
}

public BeanBag createBeanBag(int userId, String name, String roaster, String origin,
        String roastLevel, String roastDate, double remainingGrams, int rating, String notes) {
    BeanBag b = new BeanBag();
    b.setUserId(userId);
    b.setName(name);
    b.setRoaster(roaster != null ? roaster : "");
    b.setOrigin(origin != null ? origin : "");
    b.setRoastLevel(roastLevel != null ? roastLevel : "MEDIUM");
    b.setRoastDate(roastDate);
    b.setRemainingGrams(remainingGrams);
    b.setRating(Math.max(0, Math.min(5, rating)));
    b.setNotes(notes != null ? notes : "");
    return repo.createBeanBag(b);
}

public BeanBag updateBeanBag(int id, int userId, String name, String roaster, String origin,
        String roastLevel, String roastDate, double remainingGrams, int rating, String notes) {
    BeanBag b = new BeanBag();
    b.setId(id);
    b.setUserId(userId);
    b.setName(name);
    b.setRoaster(roaster != null ? roaster : "");
    b.setOrigin(origin != null ? origin : "");
    b.setRoastLevel(roastLevel != null ? roastLevel : "MEDIUM");
    b.setRoastDate(roastDate);
    b.setRemainingGrams(remainingGrams);
    b.setRating(Math.max(0, Math.min(5, rating)));
    b.setNotes(notes != null ? notes : "");
    return repo.updateBeanBag(b);
}

public void deleteBeanBag(int id, int userId) {
    repo.deleteBeanBag(id, userId);
}
```

- [ ] **Step 5: Add API routes to `BorgolApiServer.java`**

In `registerRoutes()`, add after the equipment routes:
```java
// Bean Bags
app.get   ("/api/beans",      this::getBeanBags);
app.post  ("/api/beans",      this::createBeanBag);
app.put   ("/api/beans/{id}", this::updateBeanBag);
app.delete("/api/beans/{id}", this::deleteBeanBag);
```

Add handler methods:
```java
private void getBeanBags(Context ctx) {
    Integer userId = authRequired(ctx);
    if (userId == null) return;
    ctx.json(borgol.getBeanBags(userId));
}

private void createBeanBag(Context ctx) {
    Integer userId = authRequired(ctx);
    if (userId == null) return;
    var req = ctx.bodyAsClass(BeanBagReq.class);
    ctx.status(201).json(borgol.createBeanBag(userId, req.name, req.roaster, req.origin,
        req.roastLevel, req.roastDate, req.remainingGrams, req.rating, req.notes));
}

private void updateBeanBag(Context ctx) {
    Integer userId = authRequired(ctx);
    if (userId == null) return;
    var req = ctx.bodyAsClass(BeanBagReq.class);
    ctx.json(borgol.updateBeanBag(intParam(ctx, "id"), userId, req.name, req.roaster,
        req.origin, req.roastLevel, req.roastDate, req.remainingGrams, req.rating, req.notes));
}

private void deleteBeanBag(Context ctx) {
    Integer userId = authRequired(ctx);
    if (userId == null) return;
    borgol.deleteBeanBag(intParam(ctx, "id"), userId);
    ctx.status(204);
}
```

Add request POJO inside `BorgolApiServer`:
```java
public static class BeanBagReq {
    public String name        = "";
    public String roaster     = "";
    public String origin      = "";
    public String roastLevel  = "MEDIUM";
    public String roastDate   = "";
    public double remainingGrams = 0;
    public int    rating      = 0;
    public String notes       = "";
}
```

- [ ] **Step 6: Commit backend**
```bash
git add src/main/java/borgol/
git commit -m "feat(beans): bean bag tracker backend — table, repo, service, API"
git push github main
```

#### 3b — Frontend

- [ ] **Step 7: Add "Beans" nav link in `index.html`**

Find the nav links section (around line 600) and add:
```html
<button class="nav-link" onclick="showPage('beans')">🫘 My Beans</button>
```
Also add to mobile nav if present.

- [ ] **Step 8: Add beans page HTML**

After `</div><!-- /page-journal -->`, add:
```html
<div class="page" id="page-beans">
  <div class="page-header">
    <div class="section-title">🫘 My Bean Collection</div>
    <div style="display:flex;gap:8px">
      <button class="btn btn-primary" onclick="openNewBeanModal()">+ Add Bean</button>
    </div>
  </div>
  <div id="beans-grid" class="recipe-grid">
    <div class="loading"><div class="spinner"></div> Loading beans…</div>
  </div>
</div>
```

- [ ] **Step 9: Add bean modal HTML**

After the journal modal closing tag, add:
```html
<!-- Bean Bag Modal -->
<div class="modal-overlay" id="bean-modal">
  <div class="modal modal-md">
    <div class="modal-header">
      <div class="modal-title" id="bean-modal-title">🫘 Add Bean</div>
      <button class="modal-close" onclick="closeModal('bean-modal')">✕</button>
    </div>
    <div class="modal-body">
      <input type="hidden" id="bean-edit-id"/>
      <div class="form-row">
        <div class="field"><label>Bean Name *</label><input id="bean-name" placeholder="e.g. Yirgacheffe Natural"/></div>
        <div class="field"><label>Roaster</label><input id="bean-roaster" placeholder="e.g. Blue Bottle"/></div>
      </div>
      <div class="form-row">
        <div class="field"><label>Origin</label><input id="bean-origin" placeholder="e.g. Ethiopia"/></div>
        <div class="field"><label>Roast Level</label>
          <select id="bean-roast-level">
            <option value="LIGHT">Light</option>
            <option value="MEDIUM" selected>Medium</option>
            <option value="MEDIUM-DARK">Medium-Dark</option>
            <option value="DARK">Dark</option>
          </select>
        </div>
      </div>
      <div class="form-row">
        <div class="field"><label>Roast Date</label><input type="date" id="bean-roast-date"/></div>
        <div class="field"><label>Remaining (g)</label><input type="number" id="bean-remaining" value="250" min="0" max="5000"/></div>
      </div>
      <div class="field"><label>Your Rating (0=unrated)</label>
        <input type="range" id="bean-rating" min="0" max="5" value="0" step="1" oninput="document.getElementById('bean-rating-val').textContent=this.value"/>
        <span id="bean-rating-val">0</span>/5
      </div>
      <div class="field"><label>Notes</label><textarea id="bean-notes" rows="2" placeholder="Tasting notes, where you got it…"></textarea></div>
    </div>
    <div class="modal-footer">
      <button class="btn btn-secondary" onclick="closeModal('bean-modal')">Cancel</button>
      <button class="btn btn-primary" onclick="saveBeanBag()">Save</button>
    </div>
  </div>
</div>
```

- [ ] **Step 10: Add JS for beans page**

Add these JS functions (anywhere after the journal functions):
```js
let BEAN_BAGS = [];

async function loadBeans() {
  const grid = document.getElementById('beans-grid');
  try {
    BEAN_BAGS = await api('/api/beans');
    renderBeanGrid();
  } catch(e) {
    grid.innerHTML = `<div class="empty-state"><div class="empty-icon">⚠️</div><div class="empty-msg">${esc(e.message)}</div></div>`;
  }
}

function renderBeanGrid() {
  const grid = document.getElementById('beans-grid');
  if (!BEAN_BAGS.length) {
    grid.innerHTML = `<div class="empty-state" style="grid-column:1/-1"><div class="empty-icon">🫘</div><div class="empty-msg">No beans logged yet. Add your first bag!</div><button class="btn btn-primary" onclick="openNewBeanModal()">+ Add Bean</button></div>`;
    return;
  }
  const roastColors = {LIGHT:'#F0E0C0',MEDIUM:'#C8904A','MEDIUM-DARK':'#8B5020',DARK:'#3D1505'};
  grid.innerHTML = BEAN_BAGS.map(b => `
    <div class="card card-hover" style="padding:16px">
      <div style="display:flex;align-items:center;gap:10px;margin-bottom:10px">
        <div style="width:36px;height:36px;border-radius:50%;background:${roastColors[b.roastLevel]||'#C8904A'};display:flex;align-items:center;justify-content:center;font-size:18px;flex-shrink:0">🫘</div>
        <div style="flex:1;min-width:0">
          <div style="font-weight:700;font-size:15px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${esc(b.name)}</div>
          <div style="font-size:11px;color:var(--muted)">${esc(b.roaster)}${b.roaster&&b.origin?' · ':''}${esc(b.origin)}</div>
        </div>
      </div>
      <div style="display:flex;gap:8px;flex-wrap:wrap;margin-bottom:10px">
        <span class="journal-tag">${esc(b.roastLevel)}</span>
        ${b.roastDate?`<span class="journal-tag">🗓 ${b.roastDate}</span>`:''}
        ${b.remainingGrams>0?`<span class="journal-tag" style="background:var(--cream);color:var(--caramel)">⚖️ ${b.remainingGrams}g left</span>`:'<span class="journal-tag" style="background:#FFEBEE;color:var(--danger)">Empty</span>'}
        ${b.rating>0?`<span class="journal-tag" style="background:var(--cream);color:var(--caramel)">⭐ ${b.rating}/5</span>`:''}
      </div>
      ${b.notes?`<div style="font-size:12px;color:var(--muted);margin-bottom:10px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${esc(b.notes)}</div>`:''}
      <div style="display:flex;justify-content:space-between;align-items:center;padding-top:8px;border-top:1px solid var(--border);font-size:11px;color:var(--muted)">
        <span>📅 ${timeAgo(b.createdAt)}</span>
        <div style="display:flex;gap:6px">
          <button class="btn btn-sm btn-secondary" style="padding:3px 8px;font-size:11px" onclick="openEditBean(${b.id})">Edit</button>
          <button class="btn btn-sm btn-danger"    style="padding:3px 8px;font-size:11px" onclick="deleteBeanBag(${b.id})">Del</button>
        </div>
      </div>
    </div>`).join('');
}

function openNewBeanModal() {
  document.getElementById('bean-edit-id').value = '';
  document.getElementById('bean-modal-title').textContent = '🫘 Add Bean';
  ['name','roaster','origin','notes'].forEach(f => document.getElementById('bean-'+f).value = '');
  document.getElementById('bean-roast-level').value = 'MEDIUM';
  document.getElementById('bean-roast-date').value = '';
  document.getElementById('bean-remaining').value = 250;
  document.getElementById('bean-rating').value = 0;
  document.getElementById('bean-rating-val').textContent = '0';
  openModal('bean-modal');
}

function openEditBean(id) {
  const b = BEAN_BAGS.find(x=>x.id===id);
  if (!b) return;
  document.getElementById('bean-edit-id').value = b.id;
  document.getElementById('bean-modal-title').textContent = '✏️ Edit Bean';
  document.getElementById('bean-name').value = b.name || '';
  document.getElementById('bean-roaster').value = b.roaster || '';
  document.getElementById('bean-origin').value = b.origin || '';
  document.getElementById('bean-roast-level').value = b.roastLevel || 'MEDIUM';
  document.getElementById('bean-roast-date').value = b.roastDate || '';
  document.getElementById('bean-remaining').value = b.remainingGrams || 0;
  document.getElementById('bean-rating').value = b.rating || 0;
  document.getElementById('bean-rating-val').textContent = b.rating || 0;
  document.getElementById('bean-notes').value = b.notes || '';
  openModal('bean-modal');
}

async function saveBeanBag() {
  const editId = document.getElementById('bean-edit-id').value;
  const body = {
    name:           document.getElementById('bean-name').value.trim(),
    roaster:        document.getElementById('bean-roaster').value.trim(),
    origin:         document.getElementById('bean-origin').value.trim(),
    roastLevel:     document.getElementById('bean-roast-level').value,
    roastDate:      document.getElementById('bean-roast-date').value,
    remainingGrams: parseFloat(document.getElementById('bean-remaining').value) || 0,
    rating:         parseInt(document.getElementById('bean-rating').value) || 0,
    notes:          document.getElementById('bean-notes').value.trim(),
  };
  if (!body.name) { toast('Bean name is required', 'err'); return; }
  try {
    if (editId) {
      await api(`/api/beans/${editId}`, 'PUT', body);
    } else {
      await api('/api/beans', 'POST', body);
    }
    closeModal('bean-modal');
    await loadBeans();
    toast('Bean saved!');
  } catch(e) { toast(e.message, 'err'); }
}

async function deleteBeanBag(id) {
  if (!confirm('Delete this bean entry?')) return;
  try {
    await api(`/api/beans/${id}`, 'DELETE');
    await loadBeans();
    toast('Deleted');
  } catch(e) { toast(e.message, 'err'); }
}
```

- [ ] **Step 11: Wire `loadBeans()` into `showPage()`**

Find the `showPage` function and add a `beans` case:
```js
case 'beans':
  if (TOKEN) await loadBeans();
  else showPage('login');
  break;
```

- [ ] **Step 12: Commit frontend**
```bash
git add src/main/resources/public/index.html
git commit -m "feat(beans): bean bag tracker frontend — nav, grid, add/edit/delete modal"
git push github main
```

---

### Feature 4: Weather Logging in Journal

**What:** When creating a journal entry, auto-fetch weather (temp °C, humidity, condition) from Open-Meteo API using the browser's geolocation. Stored as JSON in a `weather_data` column. Displayed in journal detail.

**Note:** Open-Meteo is free, no API key. URL: `https://api.open-meteo.com/v1/forecast?latitude=X&longitude=Y&current=temperature_2m,relative_humidity_2m,weather_code`

#### 4a — Backend schema change

- [ ] **Step 1: Add `weather_data` column to `brew_journal`**

In `BorgolRepository.initSchema()`, after the `brew_journal` CREATE TABLE block, add:
```java
// Add weather_data column if it doesn't exist (idempotent migration)
try {
    s.execute("ALTER TABLE brew_journal ADD COLUMN weather_data VARCHAR(200) DEFAULT ''");
} catch (Exception ignored) { /* column already exists */ }
```

- [ ] **Step 2: Update `mapJournal()` to include `weather_data`**

In `mapJournal` method, add:
```java
e.setWeatherData(nullToEmpty(rs.getString("weather_data")));
```

- [ ] **Step 3: Add `weatherData` field to `BrewJournalEntry.java`**

Add field + getter/setter:
```java
private String weatherData; // JSON: {"temp":22,"humidity":45,"condition":"Clear"}

public String getWeatherData() { return weatherData; }
public void setWeatherData(String weatherData) { this.weatherData = weatherData; }
```

- [ ] **Step 4: Update `BorgolRepository.createJournalEntry()` and `updateJournalEntry()` to include `weather_data`**

In the INSERT SQL, add `weather_data` to both column list and values, and set it via `ps.setString(N, nvl(e.getWeatherData()))`. Increment all parameter indices after it accordingly.

- [ ] **Step 5: Update `BorgolService.createJournalEntry()` and `updateJournalEntry()` signatures** to accept a `weatherData` parameter and pass it through.

- [ ] **Step 6: Update `BorgolApiServer.JournalReq`** to add:
```java
public String weatherData = "";
```
And pass it to service calls.

- [ ] **Step 7: Commit backend**
```bash
git add src/main/java/borgol/
git commit -m "feat(journal): add weather_data column for weather logging"
git push github main
```

#### 4b — Frontend

- [ ] **Step 8: Add "Fetch Weather" button to journal modal**

In the journal modal HTML (around line 1107), add a button and weather display div at the top of the form:
```html
<div style="display:flex;align-items:center;gap:8px;margin-bottom:14px;padding:10px 14px;background:var(--cream);border-radius:10px;border:1px solid var(--border)">
  <span style="font-size:18px">🌤</span>
  <span id="journal-weather-display" style="font-size:13px;color:var(--muted);flex:1">Weather not fetched</span>
  <button class="btn btn-sm btn-secondary" onclick="fetchJournalWeather()" type="button">Fetch</button>
</div>
<input type="hidden" id="journal-weather-data"/>
```

- [ ] **Step 9: Add JS weather fetch function**
```js
async function fetchJournalWeather() {
  const display = document.getElementById('journal-weather-display');
  display.textContent = 'Locating…';
  navigator.geolocation.getCurrentPosition(async pos => {
    try {
      const {latitude: lat, longitude: lng} = pos.coords;
      const url = `https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lng}&current=temperature_2m,relative_humidity_2m,weather_code`;
      const r = await fetch(url);
      const d = await r.json();
      const c = d.current;
      const wmo = {0:'Clear',1:'Mainly clear',2:'Partly cloudy',3:'Overcast',
        45:'Foggy',51:'Drizzle',61:'Rain',71:'Snow',80:'Showers',95:'Thunderstorm'};
      const condition = wmo[Math.floor(c.weather_code/10)*10] || wmo[c.weather_code] || 'Unknown';
      const weather = {temp: Math.round(c.temperature_2m), humidity: c.relative_humidity_2m, condition};
      document.getElementById('journal-weather-data').value = JSON.stringify(weather);
      display.textContent = `${condition} · ${weather.temp}°C · ${weather.humidity}% humidity`;
      display.style.color = 'var(--roast)';
    } catch(e) { display.textContent = 'Weather unavailable'; }
  }, () => { display.textContent = 'Location denied'; });
}
```

- [ ] **Step 10: Include `weatherData` when submitting journal entries**

In `saveJournalEntry()` (or however journal is saved), add:
```js
weatherData: document.getElementById('journal-weather-data').value || '',
```

- [ ] **Step 11: Display weather in journal detail view**

In `openJournalDetail()`, in the detail HTML template, add after the tasting notes:
```js
${e.weatherData ? (() => {
  try {
    const w = JSON.parse(e.weatherData);
    return `<div style="display:flex;align-items:center;gap:8px;margin-top:12px;font-size:13px;color:var(--muted)">
      🌤 <strong>${w.condition}</strong> · ${w.temp}°C · ${w.humidity}% humidity
    </div>`;
  } catch{ return ''; }
})() : ''}
```

- [ ] **Step 12: Commit**
```bash
git add src/main/resources/public/index.html
git commit -m "feat(journal): weather auto-fetch from Open-Meteo on journal entry"
git push github main
```

---

### Feature 5: Monthly Brew Stats

**What:** A "Stats" tab on the journal page with:
1. Brew methods used (bar chart via Chart.js)
2. Average rating per month (line chart)
3. Summary numbers: total entries, avg overall rating, most used method

#### 5a — Backend

- [ ] **Step 1: Add stats endpoint to `BorgolApiServer.java`**

In `registerRoutes()`:
```java
app.get("/api/journal/stats", this::getJournalStats);
```

Handler:
```java
private void getJournalStats(Context ctx) {
    Integer userId = authRequired(ctx);
    if (userId == null) return;
    ctx.json(borgol.getJournalStats(userId));
}
```

- [ ] **Step 2: Add `getJournalStats()` to `BorgolService.java`**
```java
public Map<String, Object> getJournalStats(int userId) {
    return repo.getJournalStats(userId);
}
```

- [ ] **Step 3: Add `getJournalStats()` to `BorgolRepository.java`**
```java
public Map<String, Object> getJournalStats(int userId) {
    Map<String, Object> stats = new java.util.LinkedHashMap<>();
    try (Statement s = conn().createStatement()) {

        // Total entries + avg rating
        String totalSql = "SELECT COUNT(*) AS cnt, " +
            "AVG((rating_aroma+rating_flavor+rating_acidity+rating_body+rating_sweetness+rating_finish)/6.0) AS avg_rating " +
            "FROM brew_journal WHERE user_id=" + userId;
        try (ResultSet rs = s.executeQuery(totalSql)) {
            if (rs.next()) {
                stats.put("totalEntries", rs.getInt("cnt"));
                stats.put("avgRating", rs.getDouble("avg_rating"));
            }
        }

        // Brew method counts
        String methodSql = "SELECT brew_method, COUNT(*) AS cnt FROM brew_journal " +
            "WHERE user_id=" + userId + " AND brew_method IS NOT NULL AND brew_method<>'' " +
            "GROUP BY brew_method ORDER BY cnt DESC";
        List<Map<String,Object>> methods = new ArrayList<>();
        try (ResultSet rs = s.executeQuery(methodSql)) {
            while (rs.next()) methods.add(Map.of(
                "method", rs.getString("brew_method"),
                "count",  rs.getInt("cnt")));
        }
        stats.put("brewMethods", methods);

        // Monthly avg rating (last 12 months)
        String monthlySql = "SELECT TO_CHAR(created_at,'YYYY-MM') AS month, " +
            "AVG((rating_aroma+rating_flavor+rating_acidity+rating_body+rating_sweetness+rating_finish)/6.0) AS avg_rating, " +
            "COUNT(*) AS cnt FROM brew_journal WHERE user_id=" + userId +
            " AND created_at >= NOW() - INTERVAL '12 months' " +
            "GROUP BY month ORDER BY month";
        List<Map<String,Object>> monthly = new ArrayList<>();
        try (ResultSet rs = s.executeQuery(monthlySql)) {
            while (rs.next()) monthly.add(Map.of(
                "month",     rs.getString("month"),
                "avgRating", rs.getDouble("avg_rating"),
                "count",     rs.getInt("cnt")));
        }
        stats.put("monthly", monthly);

    } catch (SQLException e) { throw new RuntimeException(e); }
    return stats;
}
```

**Note for H2 local mode:** `TO_CHAR` and `NOW()` work in H2's PostgreSQL compatibility mode. If issues arise locally, wrap in try/catch and return empty lists.

- [ ] **Step 4: Commit backend**
```bash
git add src/main/java/borgol/
git commit -m "feat(stats): journal stats API — method counts, monthly avg rating"
git push github main
```

#### 5b — Frontend

- [ ] **Step 5: Add Chart.js CDN to `<head>` in `index.html`**

After the Leaflet script tag (around line 9):
```html
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
```

- [ ] **Step 6: Add a "Stats" tab to the journal page**

After the page-header of `#page-journal`, add:
```html
<div class="tab-bar" style="margin-bottom:20px">
  <button class="tab-btn active" id="journal-tab-entries" onclick="switchJournalTab('entries',this)">Entries</button>
  <button class="tab-btn" id="journal-tab-stats"   onclick="switchJournalTab('stats',this)">📊 Stats</button>
</div>
<div id="journal-stats-panel" style="display:none">
  <div style="display:flex;gap:16px;flex-wrap:wrap;margin-bottom:24px">
    <div class="card" style="padding:18px 24px;flex:1;min-width:140px;text-align:center">
      <div style="font-size:28px;font-weight:800;color:var(--roast)" id="stat-total">—</div>
      <div style="font-size:11px;color:var(--muted);text-transform:uppercase;letter-spacing:.5px">Total Brews</div>
    </div>
    <div class="card" style="padding:18px 24px;flex:1;min-width:140px;text-align:center">
      <div style="font-size:28px;font-weight:800;color:var(--caramel)" id="stat-avg">—</div>
      <div style="font-size:11px;color:var(--muted);text-transform:uppercase;letter-spacing:.5px">Avg Rating</div>
    </div>
    <div class="card" style="padding:18px 24px;flex:1;min-width:140px;text-align:center">
      <div style="font-size:18px;font-weight:800;color:var(--roast);white-space:nowrap;overflow:hidden;text-overflow:ellipsis" id="stat-top-method">—</div>
      <div style="font-size:11px;color:var(--muted);text-transform:uppercase;letter-spacing:.5px">Top Method</div>
    </div>
  </div>
  <div style="display:grid;grid-template-columns:1fr 1fr;gap:20px;flex-wrap:wrap">
    <div class="card" style="padding:18px">
      <div style="font-size:12px;font-weight:800;color:var(--muted);text-transform:uppercase;margin-bottom:12px">Brew Methods</div>
      <canvas id="chart-methods" height="200"></canvas>
    </div>
    <div class="card" style="padding:18px">
      <div style="font-size:12px;font-weight:800;color:var(--muted);text-transform:uppercase;margin-bottom:12px">Monthly Avg Rating</div>
      <canvas id="chart-monthly" height="200"></canvas>
    </div>
  </div>
</div>
```

- [ ] **Step 7: Add JS for stats tab**
```js
let _methodChart = null, _monthlyChart = null;

function switchJournalTab(tab, btn) {
  document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
  const isStats = tab === 'stats';
  document.getElementById('ratio-calc').style.display      = isStats ? 'none' : '';
  document.getElementById('journal-grid').style.display    = isStats ? 'none' : '';
  document.getElementById('journal-stats-panel').style.display = isStats ? '' : 'none';
  if (isStats) loadJournalStats();
}

async function loadJournalStats() {
  try {
    const s = await api('/api/journal/stats');
    document.getElementById('stat-total').textContent = s.totalEntries || 0;
    document.getElementById('stat-avg').textContent   = s.avgRating ? s.avgRating.toFixed(1) : '—';
    document.getElementById('stat-top-method').textContent =
      s.brewMethods && s.brewMethods.length ? s.brewMethods[0].method : '—';

    // Methods bar chart
    if (_methodChart) _methodChart.destroy();
    _methodChart = new Chart(document.getElementById('chart-methods'), {
      type: 'bar',
      data: {
        labels: (s.brewMethods||[]).map(m => m.method),
        datasets: [{ data: (s.brewMethods||[]).map(m => m.count),
          backgroundColor: '#CB8840', borderRadius: 6 }]
      },
      options: { plugins:{legend:{display:false}}, scales:{y:{beginAtZero:true,ticks:{stepSize:1}}} }
    });

    // Monthly line chart
    if (_monthlyChart) _monthlyChart.destroy();
    _monthlyChart = new Chart(document.getElementById('chart-monthly'), {
      type: 'line',
      data: {
        labels: (s.monthly||[]).map(m => m.month),
        datasets: [{ data: (s.monthly||[]).map(m => +m.avgRating.toFixed(2)),
          borderColor: '#CB8840', backgroundColor: 'rgba(203,136,64,.12)',
          fill: true, tension: 0.4, pointRadius: 4 }]
      },
      options: { plugins:{legend:{display:false}},
        scales:{y:{beginAtZero:false,min:0,max:10,ticks:{stepSize:2}}} }
    });
  } catch(e) { console.error('Stats error', e); }
}
```

- [ ] **Step 8: Commit**
```bash
git add src/main/resources/public/index.html
git commit -m "feat(stats): monthly brew stats tab with Chart.js bar and line charts"
git push github main
```

---

## Phase 3 — Social Features

### Feature 6: Recipe Collections

**What:** Users can create named collections of recipes ("Weekend Brews", "Espresso Classics") and add/remove recipes. Collections are shareable (public/private flag).

#### 6a — Backend

- [ ] **Step 1: Add tables to `initSchema()`**
```java
s.execute("""
    CREATE TABLE IF NOT EXISTS recipe_collections (
        id          INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
        user_id     INT          NOT NULL REFERENCES borgol_users(id) ON DELETE CASCADE,
        name        VARCHAR(100) NOT NULL,
        description VARCHAR(500) DEFAULT '',
        is_public   BOOLEAN      DEFAULT TRUE,
        created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
    )""");
s.execute("""
    CREATE TABLE IF NOT EXISTS collection_recipes (
        collection_id INT NOT NULL REFERENCES recipe_collections(id) ON DELETE CASCADE,
        recipe_id     INT NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
        added_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (collection_id, recipe_id)
    )""");
```

- [ ] **Step 2: Add repository methods for collections**

Add to `BorgolRepository.java`:
```java
public List<Map<String,Object>> getCollections(int userId, int currentUserId) {
    String sql = "SELECT c.*, u.username, " +
        "(SELECT COUNT(*) FROM collection_recipes cr WHERE cr.collection_id=c.id) AS recipe_count " +
        "FROM recipe_collections c JOIN borgol_users u ON u.id=c.user_id " +
        "WHERE c.user_id=? OR (c.is_public=TRUE AND ?<>0) ORDER BY c.created_at DESC";
    List<Map<String,Object>> list = new ArrayList<>();
    try (PreparedStatement ps = conn().prepareStatement(sql)) {
        ps.setInt(1, userId); ps.setInt(2, currentUserId);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String,Object> m = new java.util.LinkedHashMap<>();
                m.put("id", rs.getInt("id"));
                m.put("userId", rs.getInt("user_id"));
                m.put("username", rs.getString("username"));
                m.put("name", rs.getString("name"));
                m.put("description", rs.getString("description"));
                m.put("isPublic", rs.getBoolean("is_public"));
                m.put("recipeCount", rs.getInt("recipe_count"));
                Timestamp ts = rs.getTimestamp("created_at");
                m.put("createdAt", ts != null ? ts.toLocalDateTime().toString() : "");
                list.add(m);
            }
        }
    } catch (SQLException e) { throw new RuntimeException(e); }
    return list;
}

public Map<String,Object> createCollection(int userId, String name, String description, boolean isPublic) {
    try (PreparedStatement ps = conn().prepareStatement(
            "INSERT INTO recipe_collections (user_id, name, description, is_public) VALUES (?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS)) {
        ps.setInt(1, userId); ps.setString(2, name);
        ps.setString(3, nvl(description)); ps.setBoolean(4, isPublic);
        ps.executeUpdate();
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys.next()) return Map.of("id", keys.getInt(1), "name", name);
        }
    } catch (SQLException e) { throw new RuntimeException(e); }
    return Map.of();
}

public void addRecipeToCollection(int collectionId, int recipeId, int userId) {
    // Verify ownership
    try (PreparedStatement ps = conn().prepareStatement(
            "SELECT id FROM recipe_collections WHERE id=? AND user_id=?")) {
        ps.setInt(1, collectionId); ps.setInt(2, userId);
        try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new IllegalArgumentException("Collection not found");
        }
    } catch (SQLException e) { throw new RuntimeException(e); }
    try (PreparedStatement ps = conn().prepareStatement(
            "INSERT INTO collection_recipes (collection_id, recipe_id) VALUES (?,?) ON CONFLICT DO NOTHING")) {
        ps.setInt(1, collectionId); ps.setInt(2, recipeId); ps.executeUpdate();
    } catch (SQLException e) { throw new RuntimeException(e); }
}

public void removeRecipeFromCollection(int collectionId, int recipeId, int userId) {
    // Verify ownership
    try (PreparedStatement ps = conn().prepareStatement(
            "SELECT id FROM recipe_collections WHERE id=? AND user_id=?")) {
        ps.setInt(1, collectionId); ps.setInt(2, userId);
        try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new IllegalArgumentException("Collection not found");
        }
    } catch (SQLException e) { throw new RuntimeException(e); }
    try (PreparedStatement ps = conn().prepareStatement(
            "DELETE FROM collection_recipes WHERE collection_id=? AND recipe_id=?")) {
        ps.setInt(1, collectionId); ps.setInt(2, recipeId); ps.executeUpdate();
    } catch (SQLException e) { throw new RuntimeException(e); }
}

public List<Map<String,Object>> getCollectionRecipes(int collectionId, int currentUserId) {
    String sql = "SELECT r.*, u.username, u.avatar_url, " +
        "(SELECT COUNT(*) FROM recipe_likes l WHERE l.recipe_id=r.id) AS likes_count " +
        "FROM collection_recipes cr JOIN recipes r ON r.id=cr.recipe_id " +
        "JOIN borgol_users u ON u.id=r.author_id WHERE cr.collection_id=? ORDER BY cr.added_at DESC";
    List<Map<String,Object>> list = new ArrayList<>();
    try (PreparedStatement ps = conn().prepareStatement(sql)) {
        ps.setInt(1, collectionId);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String,Object> m = new java.util.LinkedHashMap<>();
                m.put("id", rs.getInt("id"));
                m.put("title", rs.getString("title"));
                m.put("drinkType", rs.getString("drink_type"));
                m.put("difficulty", rs.getString("difficulty"));
                m.put("imageUrl", rs.getString("image_url"));
                m.put("username", rs.getString("username"));
                m.put("likesCount", rs.getInt("likes_count"));
                list.add(m);
            }
        }
    } catch (SQLException e) { throw new RuntimeException(e); }
    return list;
}

public void deleteCollection(int id, int userId) {
    try (PreparedStatement ps = conn().prepareStatement(
            "DELETE FROM recipe_collections WHERE id=? AND user_id=?")) {
        ps.setInt(1, id); ps.setInt(2, userId); ps.executeUpdate();
    } catch (SQLException e) { throw new RuntimeException(e); }
}
```

- [ ] **Step 3: Add service + API routes** (follow same pattern as Bean Bags above)

Service wraps repo calls. API routes:
```
GET    /api/collections?userId=X      — list collections for a user
POST   /api/collections               — create [auth]
DELETE /api/collections/{id}          — delete [auth]
GET    /api/collections/{id}/recipes  — list recipes in collection
POST   /api/collections/{id}/recipes  — add recipe {recipeId} [auth]
DELETE /api/collections/{id}/recipes/{recipeId} — remove [auth]
```

- [ ] **Step 4: Frontend** — Add "Collections" page and "Add to Collection" button on recipe cards (follow same HTML/JS pattern as Bean Bags above).

- [ ] **Step 5: Commit**
```bash
git add src/main/java/borgol/ src/main/resources/public/index.html
git commit -m "feat(collections): recipe collections — create, add/remove recipes, view"
git push github main
```

---

### Feature 7: Cafe Check-ins

**What:** Users can "check in" at a cafe with a timestamp. The cafe detail view shows recent check-ins.

#### 7a — Backend

- [ ] **Step 1: Add table**
```java
s.execute("""
    CREATE TABLE IF NOT EXISTS cafe_checkins (
        id         INT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
        cafe_id    INT NOT NULL REFERENCES cafes(id) ON DELETE CASCADE,
        user_id    INT NOT NULL REFERENCES borgol_users(id) ON DELETE CASCADE,
        note       VARCHAR(200) DEFAULT '',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )""");
```

- [ ] **Step 2: Add repository methods**
```java
public Map<String,Object> checkIn(int cafeId, int userId, String note) {
    try (PreparedStatement ps = conn().prepareStatement(
            "INSERT INTO cafe_checkins (cafe_id, user_id, note) VALUES (?,?,?)",
            Statement.RETURN_GENERATED_KEYS)) {
        ps.setInt(1, cafeId); ps.setInt(2, userId); ps.setString(3, nvl(note));
        ps.executeUpdate();
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys.next()) return Map.of("id", keys.getInt(1), "message", "Checked in!");
        }
    } catch (SQLException e) { throw new RuntimeException(e); }
    return Map.of();
}

public List<Map<String,Object>> getCheckins(int cafeId) {
    String sql = "SELECT ci.*, u.username, u.avatar_url FROM cafe_checkins ci " +
        "JOIN borgol_users u ON u.id=ci.user_id WHERE ci.cafe_id=? ORDER BY ci.created_at DESC LIMIT 20";
    List<Map<String,Object>> list = new ArrayList<>();
    try (PreparedStatement ps = conn().prepareStatement(sql)) {
        ps.setInt(1, cafeId);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String,Object> m = new java.util.LinkedHashMap<>();
                m.put("id", rs.getInt("id"));
                m.put("username", rs.getString("username"));
                m.put("avatarUrl", rs.getString("avatar_url"));
                m.put("note", rs.getString("note"));
                Timestamp ts = rs.getTimestamp("created_at");
                m.put("createdAt", ts != null ? ts.toLocalDateTime().toString() : "");
                list.add(m);
            }
        }
    } catch (SQLException e) { throw new RuntimeException(e); }
    return list;
}
```

- [ ] **Step 3: API routes**
```java
app.post("/api/cafes/{id}/checkin",   this::cafeCheckin);
app.get ("/api/cafes/{id}/checkins",  this::getCafeCheckins);
```

Handlers:
```java
private void cafeCheckin(Context ctx) {
    Integer userId = authRequired(ctx);
    if (userId == null) return;
    var req = ctx.bodyAsClass(CheckinReq.class);
    ctx.status(201).json(borgol.checkIn(intParam(ctx,"id"), userId, req.note));
}
private void getCafeCheckins(Context ctx) {
    ctx.json(borgol.getCheckins(intParam(ctx,"id")));
}
// POJO:
public static class CheckinReq { public String note = ""; }
```

#### 7b — Frontend

- [ ] **Step 4: In the cafe detail modal**, add a "Check In" button and recent check-ins list.

Find where cafe details are rendered (look for `openCafeDetail` function in index.html ~line 2100) and add:
```js
// At the end of cafe detail content:
`<div style="margin-top:20px;padding-top:16px;border-top:1px solid var(--border)">
  <div style="font-weight:800;font-size:13px;color:var(--muted);text-transform:uppercase;margin-bottom:10px">Recent Check-ins</div>
  <div id="cafe-checkins-list" style="margin-bottom:12px"><div class="loading"><div class="spinner"></div></div></div>
  ${TOKEN ? `<div style="display:flex;gap:8px">
    <input id="checkin-note" placeholder="Add a note… (optional)" style="flex:1;padding:8px 12px;border:1.5px solid var(--border);border-radius:8px;font-size:13px;outline:none;font-family:inherit;background:var(--milk);color:var(--text)"/>
    <button class="btn btn-primary btn-sm" onclick="submitCheckin(${c.id})">Check In 📍</button>
  </div>` : ''}
</div>`
```

Add JS:
```js
async function loadCafeCheckins(cafeId) {
  const list = document.getElementById('cafe-checkins-list');
  const data = await api(`/api/cafes/${cafeId}/checkins`);
  if (!data.length) { list.innerHTML = '<div style="font-size:12px;color:var(--muted)">No check-ins yet. Be the first!</div>'; return; }
  list.innerHTML = data.map(ci => `
    <div style="display:flex;gap:8px;align-items:center;padding:6px 0;border-bottom:1px solid var(--border)">
      ${avatarHtml(ci.username, ci.avatarUrl, 28)}
      <div style="flex:1;min-width:0">
        <span style="font-weight:700;font-size:13px">${esc(ci.username)}</span>
        ${ci.note?`<span style="font-size:12px;color:var(--muted)"> · ${esc(ci.note)}</span>`:''}
        <div style="font-size:11px;color:var(--muted)">${timeAgo(ci.createdAt)}</div>
      </div>
    </div>`).join('');
}

async function submitCheckin(cafeId) {
  const note = document.getElementById('checkin-note').value.trim();
  try {
    await api(`/api/cafes/${cafeId}/checkin`, 'POST', {note});
    document.getElementById('checkin-note').value = '';
    await loadCafeCheckins(cafeId);
    toast('Checked in! 📍');
  } catch(e) { toast(e.message, 'err'); }
}
```

- [ ] **Step 5: Call `loadCafeCheckins(cafeId)` after rendering the cafe detail**

- [ ] **Step 6: Commit**
```bash
git add src/main/java/borgol/ src/main/resources/public/index.html
git commit -m "feat(cafes): cafe check-ins — check in with optional note, show recent 20"
git push github main
```

---

## Phase 4 — Gamification

### Feature 8: Achievement Badges

**What:** Users earn badges for milestones. Checked on relevant actions. Shown on profile.

**Badges:**
| ID | Name | Condition |
|---|---|---|
| `first_brew` | First Sip | Log first journal entry |
| `brew_10` | Regular | 10 journal entries |
| `brew_50` | Dedicated | 50 journal entries |
| `recipe_author` | Recipe Author | Create first recipe |
| `cafe_explorer` | Cafe Explorer | Rate 3 different cafes |
| `social_butterfly` | Social Butterfly | Follow 5 users |
| `bean_collector` | Bean Collector | Add 5 beans to tracker |
| `pour_over_pro` | Pour Over Pro | Log 5 pour-over journal entries |

#### 8a — Backend

- [ ] **Step 1: Add table**
```java
s.execute("""
    CREATE TABLE IF NOT EXISTS user_achievements (
        user_id     INT         NOT NULL REFERENCES borgol_users(id) ON DELETE CASCADE,
        badge_id    VARCHAR(30) NOT NULL,
        earned_at   TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (user_id, badge_id)
    )""");
```

- [ ] **Step 2: Add achievement service to `BorgolService.java`**
```java
private static final Map<String, String[]> BADGE_META = Map.of(
    "first_brew",      new String[]{"☕", "First Sip",        "Logged your first brew"},
    "brew_10",         new String[]{"🔥", "Regular",          "Logged 10 brews"},
    "brew_50",         new String[]{"💪", "Dedicated",        "Logged 50 brews"},
    "recipe_author",   new String[]{"📝", "Recipe Author",    "Created your first recipe"},
    "cafe_explorer",   new String[]{"🗺️", "Cafe Explorer",    "Rated 3 different cafes"},
    "social_butterfly",new String[]{"🦋", "Social Butterfly", "Followed 5 users"},
    "bean_collector",  new String[]{"🫘", "Bean Collector",   "Added 5 beans"},
    "pour_over_pro",   new String[]{"⏱️", "Pour Over Pro",    "Logged 5 pour-over entries"}
);

public List<Map<String,Object>> getAchievements(int userId) {
    return repo.getAchievements(userId, BADGE_META);
}

public List<String> checkAndAwardAchievements(int userId) {
    return repo.checkAndAwardAchievements(userId, BADGE_META);
}
```

- [ ] **Step 3: Add repository methods**
```java
public List<Map<String,Object>> getAchievements(int userId, Map<String,String[]> meta) {
    Set<String> earned = new HashSet<>();
    try (PreparedStatement ps = conn().prepareStatement(
            "SELECT badge_id FROM user_achievements WHERE user_id=?")) {
        ps.setInt(1, userId);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) earned.add(rs.getString("badge_id"));
        }
    } catch (SQLException e) { throw new RuntimeException(e); }
    return meta.entrySet().stream().map(e -> {
        Map<String,Object> m = new java.util.LinkedHashMap<>();
        String[] v = e.getValue();
        m.put("id", e.getKey());
        m.put("icon", v[0]); m.put("name", v[1]); m.put("description", v[2]);
        m.put("earned", earned.contains(e.getKey()));
        return m;
    }).collect(java.util.stream.Collectors.toList());
}

public List<String> checkAndAwardAchievements(int userId, Map<String,String[]> meta) {
    List<String> newlyEarned = new ArrayList<>();
    Set<String> alreadyHas = new HashSet<>();
    try (PreparedStatement ps = conn().prepareStatement(
            "SELECT badge_id FROM user_achievements WHERE user_id=?")) {
        ps.setInt(1, userId);
        try (ResultSet rs = ps.executeQuery()) { while (rs.next()) alreadyHas.add(rs.getString("badge_id")); }
    } catch (SQLException e) { throw new RuntimeException(e); }

    Map<String, Integer> counts = new HashMap<>();
    String[] countSqls = {
        "first_brew:SELECT COUNT(*) FROM brew_journal WHERE user_id="+userId,
        "brew_10:SELECT COUNT(*) FROM brew_journal WHERE user_id="+userId,
        "brew_50:SELECT COUNT(*) FROM brew_journal WHERE user_id="+userId,
        "recipe_author:SELECT COUNT(*) FROM recipes WHERE author_id="+userId,
        "cafe_explorer:SELECT COUNT(DISTINCT cafe_id) FROM cafe_ratings WHERE user_id="+userId,
        "social_butterfly:SELECT COUNT(*) FROM user_follows WHERE follower_id="+userId,
        "bean_collector:SELECT COUNT(*) FROM bean_bags WHERE user_id="+userId,
        "pour_over_pro:SELECT COUNT(*) FROM brew_journal WHERE user_id="+userId+" AND LOWER(brew_method) LIKE '%pour%'"
    };
    try (Statement s = conn().createStatement()) {
        for (String entry : countSqls) {
            String[] parts = entry.split(":", 2);
            try (ResultSet rs = s.executeQuery(parts[1])) {
                if (rs.next()) counts.put(parts[0], rs.getInt(1));
            }
        }
    } catch (SQLException e) { throw new RuntimeException(e); }

    Map<String, Integer> thresholds = Map.of(
        "first_brew",1, "brew_10",10, "brew_50",50, "recipe_author",1,
        "cafe_explorer",3, "social_butterfly",5, "bean_collector",5, "pour_over_pro",5);

    for (Map.Entry<String,Integer> t : thresholds.entrySet()) {
        String badgeId = t.getKey();
        if (!alreadyHas.contains(badgeId) && counts.getOrDefault(badgeId,0) >= t.getValue()) {
            try (PreparedStatement ps = conn().prepareStatement(
                    "INSERT INTO user_achievements (user_id, badge_id) VALUES (?,?) ON CONFLICT DO NOTHING")) {
                ps.setInt(1, userId); ps.setString(2, badgeId); ps.executeUpdate();
            } catch (SQLException e) { throw new RuntimeException(e); }
            newlyEarned.add(badgeId);
        }
    }
    return newlyEarned;
}
```

- [ ] **Step 4: Add API routes**
```java
app.get("/api/achievements",         this::getAchievements);
app.post("/api/achievements/check",  this::checkAchievements);
```

Handlers:
```java
private void getAchievements(Context ctx) {
    Integer userId = authRequired(ctx);
    if (userId == null) return;
    ctx.json(borgol.getAchievements(userId));
}
private void checkAchievements(Context ctx) {
    Integer userId = authRequired(ctx);
    if (userId == null) return;
    ctx.json(borgol.checkAndAwardAchievements(userId));
}
```

- [ ] **Step 5: Call achievement check after relevant actions in `BorgolApiServer`**

After `createJournalEntry`, `createRecipe`, `rateCafe`, `followUser`, `createBeanBag` — add a non-blocking fire-and-forget achievement check:
```java
// After creating journal entry:
borgol.checkAndAwardAchievements(userId); // sync is fine — fast DB read
```

- [ ] **Step 6: Commit backend**
```bash
git add src/main/java/borgol/
git commit -m "feat(achievements): badge system — 8 badges, auto-award on actions"
git push github main
```

#### 8b — Frontend

- [ ] **Step 7: Add badges section to profile page**

In the profile rendering JS (look for where profile content is built), add a badges row:
```js
// After profile stats, add:
const achievements = await api('/api/achievements');
const earnedBadges = achievements.filter(a => a.earned);
const badgeHtml = earnedBadges.length
  ? earnedBadges.map(a => `
      <div title="${esc(a.name)}: ${esc(a.description)}"
           style="display:flex;flex-direction:column;align-items:center;gap:4px;padding:8px 10px;background:var(--cream);border:1px solid var(--border);border-radius:10px;cursor:default;min-width:60px">
        <span style="font-size:22px">${a.icon}</span>
        <span style="font-size:10px;font-weight:700;color:var(--muted);text-align:center;line-height:1.2">${esc(a.name)}</span>
      </div>`).join('')
  : '<span style="font-size:13px;color:var(--muted)">No badges yet — keep brewing!</span>';

profileContent += `
  <div style="padding:16px 28px;border-top:1px solid var(--border)">
    <div style="font-size:12px;font-weight:800;color:var(--muted);text-transform:uppercase;letter-spacing:.5px;margin-bottom:10px">🏅 Achievements</div>
    <div style="display:flex;flex-wrap:wrap;gap:8px">${badgeHtml}</div>
  </div>`;
```

- [ ] **Step 8: Show toast for newly earned badges**

After actions that might trigger achievements, call:
```js
async function checkAchievements() {
  try {
    const newBadges = await api('/api/achievements/check', 'POST', {});
    for (const badgeId of newBadges) {
      // Small delay between toasts
      await new Promise(r => setTimeout(r, 400));
      toast(`🏅 Achievement unlocked!`, 'info');
    }
  } catch(e) { /* silent fail */ }
}
```

Call `checkAchievements()` after: saving journal, creating recipe, rating cafe, following user, saving bean.

- [ ] **Step 9: Commit**
```bash
git add src/main/resources/public/index.html
git commit -m "feat(achievements): badge display on profile + unlock toast notifications"
git push github main
```

---

## Execution Checklist

Use this to track overall progress:

### Phase 1 — Quick Wins
- [ ] Feature 1: Dark mode system-preference auto-follow
- [ ] Feature 2: Brew ratio calculator panel

### Phase 2 — Journal Enhancements
- [ ] Feature 3a: Bean tracker backend
- [ ] Feature 3b: Bean tracker frontend
- [ ] Feature 4a: Weather logging backend
- [ ] Feature 4b: Weather logging frontend
- [ ] Feature 5a: Monthly stats backend
- [ ] Feature 5b: Monthly stats frontend

### Phase 3 — Social
- [ ] Feature 6: Recipe collections (backend + frontend)
- [ ] Feature 7: Cafe check-ins (backend + frontend)

### Phase 4 — Gamification
- [ ] Feature 8a: Achievement badges backend
- [ ] Feature 8b: Achievement badges frontend

---

## Context for Future Sessions

**Repo:** https://github.com/Apoca72/borgol-coffee-platform  
**Remote:** `git push github main`  
**Deployed:** Render (auto-deploys on push to main)  
**Admin login:** `that.u.sir.name@gmail.com` / `Uya.5284`  
**Main frontend file:** `src/main/resources/public/index.html` (3969 lines, all CSS+HTML+JS in one file)  
**Pattern for new features:** DB table in `initSchema()` → repo methods → service methods → API route + handler in `BorgolApiServer` → JS/HTML in `index.html`  
**All imports needed in BorgolRepository:** already imported (`java.sql.*`, `java.util.*`)  
**`nvl()` helper:** already exists in BorgolRepository — converts null to empty string  
**`nullToEmpty()` helper:** already exists — same purpose for ResultSet values  
**DB compatibility note:** `ON CONFLICT DO NOTHING` works in both PostgreSQL and H2 PostgreSQL mode  
