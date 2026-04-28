// src/main/resources/public/js/app.js
// App shell: i18n, dark mode, state, navigation, boot.

// ── i18n / Settings ───────────────────────────────────────────────────────────
const LANGS = {
  en: {
    feed:'🏠 Feed',recipes:'📖 Recipes',cafes:'☕ Cafes',explore:'🔭 Explore',
    journal:'📓 Journal',learn:'🎓 Learn',map:'🗺️ Map',
    myProfile:'👤 My Profile',newRecipe:'✍️ New Recipe',logOut:'🚪 Log Out',
    logIn:'🔑 Log In',signUp:'📝 Sign Up',settings:'⚙️ Settings',
    searchPlaceholder:'Search…',exportPDF:'📄 Export PDF',exportCSV:'📊 Export CSV',
    newEntry:'+ New Entry',
  },
  mn: {
    feed:'🏠 Мэдээлэл',recipes:'📖 Жорууд',cafes:'☕ Кофе газар',explore:'🔭 Хайх',
    journal:'📓 Журнал',learn:'🎓 Суралцах',map:'🗺️ Газрын зураг',
    myProfile:'👤 Профайл',newRecipe:'✍️ Шинэ жор',logOut:'🚪 Гарах',
    logIn:'🔑 Нэвтрэх',signUp:'📝 Бүртгүүлэх',settings:'⚙️ Тохиргоо',
    searchPlaceholder:'Хайх…',exportPDF:'📄 PDF экспорт',exportCSV:'📊 CSV экспорт',
    newEntry:'+ Шинэ тэмдэглэл',
  }
};
let LANG      = localStorage.getItem('borgol_lang') || 'en';
let TEMP_UNIT = localStorage.getItem('borgol_temp') || 'C';

function t(key) { return (LANGS[LANG]||LANGS.en)[key] || LANGS.en[key] || key; }

function applyLang() {
  document.querySelectorAll('[data-i18n]').forEach(el => { el.textContent = t(el.dataset.i18n); });
  document.querySelectorAll('[data-i18n-ph]').forEach(el => { el.placeholder = t(el.dataset.i18nPh); });
  document.documentElement.lang = LANG;
}

function openSettings() {
  document.querySelectorAll('.lang-btn').forEach(b => b.classList.toggle('active', b.dataset.lang === LANG));
  document.querySelectorAll('.temp-btn').forEach(b => b.classList.toggle('active', b.dataset.temp === TEMP_UNIT));
  openModal('settings-modal');
}

function saveLang(lang) {
  LANG = lang;
  localStorage.setItem('borgol_lang', lang);
  document.querySelectorAll('.lang-btn').forEach(b => b.classList.toggle('active', b.dataset.lang === lang));
  applyLang();
}

function saveTempUnit(unit) {
  TEMP_UNIT = unit;
  localStorage.setItem('borgol_temp', unit);
  document.querySelectorAll('.temp-btn').forEach(b => b.classList.toggle('active', b.dataset.temp === unit));
}

// Convert °C → °F display when needed
function fmtTemp(c) {
  if (TEMP_UNIT === 'F') return Math.round(c * 9/5 + 32) + '°F';
  return c + '°C';
}

// ── Dark mode ──────────────────────────────────────────────────────────────
function applyTheme(dark) {
  document.body.classList.toggle('dark', dark);
  document.body.classList.toggle('light', !dark);
  localStorage.setItem('borgol_dark', dark ? '1' : '0');
  const btn = document.getElementById('darkToggleBtn');
  if (btn) btn.textContent = dark ? '☀️' : '🌙';
}
function toggleDarkMode() { applyTheme(!document.body.classList.contains('dark')); }
(function initTheme() {
  const saved = localStorage.getItem('borgol_dark');
  if (saved !== null) {
    applyTheme(saved === '1');
  } else {
    // Follow system preference and keep JS state in sync
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    applyTheme(prefersDark);
    // Listen for system changes (only when user hasn't manually set a preference)
    window.matchMedia('(prefers-color-scheme: dark)')
      .addEventListener('change', e => {
        if (localStorage.getItem('borgol_dark') === null) applyTheme(e.matches);
      });
  }
})();

// ── State ─────────────────────────────────────────────────────────────────────
let TOKEN         = localStorage.getItem('borgol_token') || null;
let ME            = null;
let drinkFilter   = 'ALL';
let cafeDistFilter = 'ALL';
let viewingUserId  = null;
let currentProfileTab = 'recipes';

const FLAVORS = ['BITTER','SWEET','SOUR','FRUITY','NUTTY','CHOCOLATEY','FLORAL','SPICY','EARTHY','SMOKY','CARAMEL','VANILLA'];
const DRINK_LABELS = {
  ESPRESSO:'Espresso',LATTE:'Latte',CAPPUCCINO:'Cappuccino',AMERICANO:'Americano',
  COLD_BREW:'Cold Brew',POUR_OVER:'Pour Over',FRENCH_PRESS:'French Press',
  TEA:'Tea',SMOOTHIE:'Smoothie',OTHER:'Other'
};

// ── Mobile nav ────────────────────────────────────────────────────────────────
function toggleMobileNav() {
  document.getElementById('mobile-nav').classList.toggle('open');
}

// ── Pages ─────────────────────────────────────────────────────────────────────
function showPage(name, userId=null) {
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
  const page = document.getElementById('page-' + name);
  if (page) page.classList.add('active');
  const link = document.querySelector(`.nav-link[onclick*="'${name}'"]`);
  if (link) link.classList.add('active');

  if      (name === 'feed')    loadFeed();
  else if (name === 'recipes') loadRecipes();
  else if (name === 'cafes')   loadCafes();
  else if (name === 'explore') loadTopUsers();
  else if (name === 'journal') loadJournal();
  else if (name === 'beans')        { if (TOKEN) loadBeans(); else openModal('login-modal'); }
  else if (name === 'collections')  { if (TOKEN) loadCollections(); else openModal('login-modal'); }
  else if (name === 'learn')   loadLearn();
  else if (name === 'map')     loadMap();
  else if (name === 'profile') loadProfile(userId || (ME && ME.id));
}

// ── Feed ──────────────────────────────────────────────────────────────────────
async function loadFeed() {
  if (!ME) {
    document.getElementById('feed-content').innerHTML = `
      <div class="auth-hero">
        <h1>☕ Welcome to Borgol</h1>
        <p>Your coffee community — discover recipes, find cafes, and connect with baristas across Mongolia.</p>
        <div style="display:flex;gap:12px;justify-content:center">
          <button class="btn btn-primary" style="font-size:16px;padding:12px 28px" onclick="openModal('register-modal')">Join Now</button>
          <button class="btn" style="background:rgba(255,255,255,.2);color:#fff;font-size:16px;padding:12px 28px" onclick="openModal('login-modal')">Log In</button>
        </div>
      </div>
      <div class="section-title">🔥 Trending Now</div>
    `;
    try {
      const trending = await api('/api/recipes?sort=TRENDING');
      if (Array.isArray(trending)) {
        document.getElementById('feed-content').innerHTML += trending.map(r => renderRecipeCard(r)).join('');
      }
    } catch (err) { console.error('Trending load failed:', err); }
    loadTrending();
    loadSuggestPeople();
    return;
  }
  document.getElementById('feed-content').innerHTML = '<div class="loading"><div class="spinner"></div> Loading feed…</div>';
  try {
    const feed = await api('/api/feed');
    if (!feed.length) {
      document.getElementById('feed-content').innerHTML = `
        <div class="empty-state">
          <div class="empty-icon">☕</div>
          <div class="empty-msg">Your feed is empty — follow people to see their recipes!</div>
          <button class="btn btn-primary" onclick="showPage('explore')">Find People</button>
        </div>
        <div class="section-title" style="margin-top:32px">🔥 Trending</div>
      `;
      const trending = await api('/api/recipes?sort=TRENDING');
      document.getElementById('feed-content').innerHTML += trending.map(r => renderFeedCard(r)).join('');
    } else {
      document.getElementById('feed-content').innerHTML = feed.map(r => renderFeedCard(r)).join('');
    }
    loadTrending();
    loadSuggestPeople();
    loadQuickStats();
  } catch (e) { toast(e.message, 'err'); }
}

function renderFeedCard(r) {
  const dtLabel = DRINK_LABELS[r.drinkType] || r.drinkType;
  const ago     = timeAgo(r.createdAt);
  const img     = r.imageUrl || `https://picsum.photos/seed/${r.id}/600/300`;
  return `
    <div class="card feed-card card-hover">
      <div class="feed-header">
        ${avatarHtml(r.authorUsername, null, 38)}
        <div class="feed-user-info">
          <div class="feed-username" onclick="showPage('profile',${r.authorId})">${esc(r.authorUsername)}</div>
          <div class="feed-time">${ago} · ${dtLabel}</div>
        </div>
        <span class="recipe-badge dt-${r.drinkType}" style="position:static;font-size:10px">${dtLabel}</span>
      </div>
      <img src="${img}" class="feed-img" alt="${esc(r.title)}" onerror="this.src='https://picsum.photos/seed/${r.id+10}/600/300'" loading="lazy"/>
      <div class="feed-body">
        <div class="feed-title" onclick="openRecipeDetail(${r.id})">${esc(r.title)}</div>
        ${r.description ? `<div class="feed-desc">${esc(r.description).substring(0,120)}${r.description.length>120?'…':''}</div>` : ''}
        ${r.flavorTags?.length ? `<div class="flavor-tags">${r.flavorTags.map(t=>`<span class="flavor-tag" onclick="openHashtagPage('${t.toLowerCase()}')">#${t.toLowerCase()}</span>`).join('')}</div>` : ''}
      </div>
      <div class="feed-footer">
        <button class="like-btn${r.likedByCurrentUser?' liked':''}" onclick="toggleLike(${r.id},this)">
          ${r.likedByCurrentUser?'❤️':'🤍'} <span class="like-count">${r.likesCount||0}</span>
        </button>
        <span class="comment-count" onclick="openRecipeDetail(${r.id})">💬 ${r.commentCount||0}</span>
        <button class="save-btn${r.savedByCurrentUser?' saved':''}" onclick="toggleSave(${r.id},this)" title="Save">🔖</button>
        <button class="report-btn" onclick="openReportModal('recipe',${r.id})" title="Report">⚑</button>
        <button onclick="openQuickBrew({id:${r.id},title:${JSON.stringify(r.title||'')},instructions:${JSON.stringify(r.instructions||'')},brewTime:${r.brewTime||5}})" title="Step-by-step guide" style="border:none;background:none;cursor:pointer;font-size:13px;color:var(--caramel,#A8621E);padding:5px 8px;border-radius:8px;transition:.15s" onmouseover="this.style.background='rgba(168,98,30,.1)'" onmouseout="this.style.background='none'">⚡ Easy Make</button>
        <button class="btn btn-sm btn-secondary" onclick="openRecipeDetail(${r.id})" style="margin-left:auto">View Recipe</button>
      </div>
    </div>`;
}

async function loadTrending() {
  try {
    const trending = await api('/api/recipes?sort=TRENDING');
    const top5 = trending.slice(0, 5);
    document.getElementById('trending-list').innerHTML = top5.map((r,i) => `
      <div class="trending-item" onclick="openRecipeDetail(${r.id})">
        <div class="trending-rank">${i+1}</div>
        <div>
          <div class="trending-title">${esc(r.title)}</div>
          <div class="trending-likes">❤️ ${r.likesCount} · ${DRINK_LABELS[r.drinkType]||r.drinkType}</div>
        </div>
      </div>`).join('');
  } catch (e) {}
}

async function loadSuggestPeople() {
  try {
    const users = await api('/api/users');
    const el = document.getElementById('suggest-people');
    if (!el) return;
    // Show users that the current user is not already following (and not themselves)
    const candidates = users.filter(u => {
      if (ME && u.id === ME.id) return false;
      if (ME && u.isFollowing) return false;
      return true;
    }).slice(0, 5);
    if (!candidates.length) {
      el.innerHTML = '<p style="font-size:13px;color:var(--muted)">No suggestions right now.</p>';
      return;
    }
    el.innerHTML = candidates.map(u => `
      <div class="suggest-item">
        ${avatarHtml(u.username, null, 32)}
        <div class="suggest-info">
          <div class="suggest-name" onclick="showPage('profile',${u.id})">${esc(u.username)}</div>
          <div class="suggest-meta">${u.recipeCount} recipes · ${u.followerCount} followers</div>
        </div>
        <button class="btn-follow-sm${u.isFollowing?' following':''}" id="sfbtn-${u.id}"
          onclick="quickFollow(${u.id},this)">
          ${u.isFollowing ? '✓' : '+ Follow'}
        </button>
      </div>`).join('');
  } catch (e) {}
}

async function quickFollow(userId, btn) {
  if (!ME) { openModal('login-modal'); return; }
  const isFollowing = btn.classList.contains('following');
  try {
    if (isFollowing) {
      await api(`/api/users/${userId}/follow`, {method:'DELETE'});
      btn.className = 'btn-follow-sm';
      btn.textContent = '+ Follow';
    } else {
      await api(`/api/users/${userId}/follow`, {method:'POST'});
      btn.className = 'btn-follow-sm following';
      btn.textContent = '✓';
    }
  } catch (e) { toast(e.message, 'err'); }
}

async function loadQuickStats() {
  try {
    const [recipes, cafes] = await Promise.all([api('/api/recipes'), api('/api/cafes')]);
    document.getElementById('quick-stats').innerHTML = `
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;text-align:center">
        <div><div style="font-size:22px;font-weight:800;color:var(--roast)">${recipes.length}</div><div style="font-size:11px;color:var(--muted);text-transform:uppercase">Recipes</div></div>
        <div><div style="font-size:22px;font-weight:800;color:var(--roast)">${cafes.length}</div><div style="font-size:11px;color:var(--muted);text-transform:uppercase">Cafes</div></div>
      </div>`;
  } catch (e) {}
}

// ── Explore ───────────────────────────────────────────────────────────────────
async function loadTopUsers() {
  const el = document.getElementById('explore-results');
  if (!el) return;
  // Only show spinner if not already populated
  if (!el.querySelector('.user-grid')) {
    el.innerHTML = '<div class="loading"><div class="spinner"></div> Loading members…</div>';
  }
  try {
    const users = await api('/api/users');
    el.innerHTML = users.length
      ? `<div style="margin-bottom:16px;font-size:13px;color:var(--muted)">${users.length} members in the community</div>
         <div class="user-grid" style="display:grid;grid-template-columns:repeat(auto-fill,minmax(260px,1fr));gap:16px">
           ${users.map(u => renderUserCard(u)).join('')}
         </div>`
      : emptyState('👥', 'No members yet — be the first!');
  } catch (e) { el.innerHTML = emptyState('⚠️', 'Could not load members'); }
}

async function searchUsers() {
  const q  = document.getElementById('explore-search').value.trim();
  const el = document.getElementById('explore-results');
  if (!q) { loadTopUsers(); return; }
  try {
    const users = await api(`/api/users/search?q=${encodeURIComponent(q)}`);
    el.innerHTML = users.length
      ? `<div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(260px,1fr));gap:16px">
          ${users.map(u => renderUserCard(u)).join('')}
         </div>`
      : emptyState('🔍', `No users found for "${esc(q)}"`);
  } catch (e) { toast(e.message, 'err'); }
}

function renderUserCard(u) {
  return `
    <div class="card card-hover" style="padding:16px;cursor:pointer" onclick="showPage('profile',${u.id})">
      <div style="display:flex;gap:12px;align-items:center;margin-bottom:${u.bio||u.flavorPrefs?.length?'10px':'0'}">
        ${avatarHtml(u.username, u.avatarUrl, 50)}
        <div style="flex:1;min-width:0">
          <div style="font-size:15px;font-weight:700">${esc(u.username)}</div>
          <span class="profile-level level-${u.expertiseLevel}" style="font-size:10px">${cap(u.expertiseLevel)}</span>
          <div style="font-size:12px;color:var(--muted);margin-top:2px">${u.recipeCount} recipes · ${u.followerCount} followers</div>
        </div>
      </div>
      ${u.bio ? `<p style="font-size:13px;color:#555;line-height:1.4">${esc(u.bio).substring(0,80)}${u.bio.length>80?'…':''}</p>` : ''}
      ${u.flavorPrefs?.length ? `<div class="flavor-tags" style="margin-top:8px">${u.flavorPrefs.slice(0,3).map(f=>`<span class="flavor-tag">${f.toLowerCase()}</span>`).join('')}</div>` : ''}
    </div>`;
}

async function globalSearch(q) {
  if (!q) return;
  showPage('explore');
  document.getElementById('explore-search').value = q;
  await searchUsers();
}

// ── Modal helpers ─────────────────────────────────────────────────────────────
function openModal(id) {
  document.getElementById(id).classList.add('open');
}
function closeModal(id) {
  document.getElementById(id).classList.remove('open');
}
function switchModal(from, to) {
  closeModal(from);
  openModal(to);
}

document.querySelectorAll('.modal-overlay').forEach(overlay => {
  overlay.addEventListener('click', e => {
    if (e.target === overlay) overlay.classList.remove('open');
  });
});

document.addEventListener('keydown', e => {
  if (e.key === 'Escape') {
    document.querySelectorAll('.modal-overlay.open').forEach(m => m.classList.remove('open'));
    document.getElementById('mobile-nav').classList.remove('open');
  }
});

// ── Toast ─────────────────────────────────────────────────────────────────────
let toastTimer;
function toast(msg, type='ok') {
  const el = document.getElementById('toast');
  clearTimeout(toastTimer);
  const icon = type==='ok'?'✓':type==='err'?'✕':'ℹ';
  el.innerHTML = `<span>${icon}</span> ${esc(msg)}`;
  el.className = `toast ${type} show`;
  toastTimer = setTimeout(() => el.className = 'toast', 3500);
}

// ── Utilities ─────────────────────────────────────────────────────────────────
function esc(s) {
  return String(s||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
}

function cap(s) {
  if (!s) return '';
  return s[0].toUpperCase() + s.slice(1).toLowerCase();
}

function diffIcon(d) {
  return d==='EASY'?'🟢':d==='MEDIUM'?'🟡':'🔴';
}

function timeAgo(ts) {
  if (!ts) return '';
  const date = new Date(ts.replace('T',' ').replace(/\.\d+$/,''));
  const diff = Math.floor((Date.now() - date.getTime()) / 1000);
  if (diff < 60)    return 'just now';
  if (diff < 3600)  return Math.floor(diff/60) + 'm ago';
  if (diff < 86400) return Math.floor(diff/3600) + 'h ago';
  return Math.floor(diff/86400) + 'd ago';
}

function emptyState(icon, msg) {
  return `<div class="empty-state"><div class="empty-icon">${icon}</div><div class="empty-msg">${msg}</div></div>`;
}

function avatarHtml(username, avatarUrl, size=36) {
  const letter = (username||'?')[0].toUpperCase();
  const sz = `width:${size}px;height:${size}px;font-size:${Math.round(size*0.42)}px;flex-shrink:0`;
  if (avatarUrl) {
    return `<img src="${esc(avatarUrl)}" class="avatar" style="${sz}" alt="${letter}" onerror="this.outerHTML='<div class=&quot;avatar&quot; style=&quot;${sz}&quot;>${letter}</div>'"/>`;
  }
  return `<div class="avatar" style="${sz}">${letter}</div>`;
}

// Debounce
const _timers = {};
function debounce(fn, ms) {
  return (...args) => {
    clearTimeout(_timers[fn.name]);
    _timers[fn.name] = setTimeout(() => fn(...args), ms);
  };
}

// ── Brew Ratio Calculator ──────────────────────────────────────────────────────
function rcUpdate() {
  const dose  = parseFloat(document.getElementById('rc-dose').value);
  const ratio = parseFloat(document.getElementById('rc-ratio').value);
  const yield_ = +(dose * ratio).toFixed(1);
  const tds    = +(1.35 / ratio * 100).toFixed(2);
  document.getElementById('rc-dose-val').textContent  = dose;
  document.getElementById('rc-ratio-val').textContent = ratio.toFixed(1);
  document.getElementById('rc-result').innerHTML = `
    <div class="ratio-chip">${yield_}g<span>Yield / Water</span></div>
    <div class="ratio-chip">${(dose+yield_).toFixed(1)}g<span>Total Weight</span></div>
    <div class="ratio-chip">~${tds}%<span>Est. TDS</span></div>
    <div class="ratio-chip">${ratio >= 15 ? 'Filter' : ratio >= 8 ? 'Espresso+' : 'Espresso'}<span>Style</span></div>
  `;
}

// ── Redis Pub/Sub SSE мэдэгдлийн захиалга ─────────────────────────────────────
// setInterval polling-г Server-Sent Events-р солино.
// EventSource custom header дэмждэггүй тул token-г query param-аар дамжуулна.
let _sseSource = null;
function subscribeNotifications() {
  if (!TOKEN || !ME) return;
  if (_sseSource) { _sseSource.close(); _sseSource = null; }
  const url = `/api/notifications/stream?token=${encodeURIComponent(TOKEN)}`;
  const es = new EventSource(url);
  _sseSource = es;
  es.onmessage = e => {
    try {
      const event = JSON.parse(e.data);
      toast(event.message || 'New notification', 'info');
      pollNotifCount(); // badge шинэчлэнэ
    } catch (_) {}
  };
  es.onerror = () => {
    es.close();
    _sseSource = null;
    // 5 секундын дараа дахин холбогдоно
    setTimeout(subscribeNotifications, 5000);
  };
}

// ── Boot ──────────────────────────────────────────────────────────────────────
(async () => {
  applyLang();
  buildFlavorPicker('r-flavor-picker', []);
  buildFlavorPicker('p-flavor-picker', []);
  await loadMe();
  updateNavUser();
  showPage(ME ? 'feed' : 'recipes');
  if (ME) {
    pollNotifCount();          // анхны badge тоо
    subscribeNotifications();  // Redis Pub/Sub SSE захиалга (polling-г орлоно)
  }
})();

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
let _tmr_recipeSteps    = null;
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
    _tmr_recipeSteps = null;
  } else {
    const durSec = (recipe.brewTime || 5) * 60;
    const interval = Math.floor(durSec / Math.max(lines.length, 1));
    _tmr_recipeSteps = lines.map((l, i) => ({
      t: i * interval,
      label: l.replace(/^\d+[\.\)]\s*/, ''),
      note: ''
    }));
    _tmr_recipeSteps[_tmr_recipeSteps.length - 1].t = durSec;
  }
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

// Hook showPage to initialize timer on first visit
let _tmr_initialized = false;
(function() {
  const _orig_showPage = window.showPage;
  if (typeof _orig_showPage === 'function') {
    window.showPage = function(name, ...args) {
      _orig_showPage(name, ...args);
      if (name === 'timer' && !_tmr_initialized) {
        _tmr_initialized = true;
        tmr_init();
      }
    };
  }
})();

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
(function() {
  const HISTORY_KEY = 'bean_history';
  let messages = [];   // {role, content}
  let streaming = false;

  // Restore history from sessionStorage
  try {
    const saved = sessionStorage.getItem(HISTORY_KEY);
    if (saved) messages = JSON.parse(saved);
  } catch {}

  window.beanToggle = function() {
    const panel = document.getElementById('bean-panel');
    if (panel.classList.contains('hidden')) {
      panel.classList.remove('hidden');
      document.getElementById('bean-unread').style.display = 'none';
      renderHistory();
      document.getElementById('bean-input').focus();
    } else {
      panel.classList.add('hidden');
    }
  };

  function renderHistory() {
    const box = document.getElementById('bean-messages');
    // Keep greeting, then append history
    const greeting = box.querySelector('.bean-msg.assistant');
    box.innerHTML = '';
    if (!messages.length) {
      box.appendChild(greeting);
    } else {
      messages.forEach(m => appendMsg(m.role, m.content));
    }
    scrollBean();
  }

  function appendMsg(role, text, isTyping = false) {
    const box = document.getElementById('bean-messages');
    const div = document.createElement('div');
    div.className = `bean-msg ${role}${isTyping ? ' typing' : ''}`;
    div.textContent = text;
    box.appendChild(div);
    scrollBean();
    return div;
  }

  function scrollBean() {
    const box = document.getElementById('bean-messages');
    box.scrollTop = box.scrollHeight;
  }

  window.beanSend = async function() {
    if (streaming) return;
    const input = document.getElementById('bean-input');
    const text = input.value.trim();
    if (!text) return;

    input.value = '';
    input.style.height = 'auto';
    document.getElementById('bean-send').disabled = true;

    messages.push({ role: 'user', content: text });
    appendMsg('user', text);
    sessionStorage.setItem(HISTORY_KEY, JSON.stringify(messages));

    // Typing indicator
    const typingDiv = appendMsg('assistant', '…', true);
    streaming = true;

    try {
      const token = localStorage.getItem('token');
      const headers = { 'Content-Type': 'application/json' };
      if (token) headers['Authorization'] = `Bearer ${token}`;

      const res = await fetch('/api/bean/chat', {
        method: 'POST',
        headers,
        body: JSON.stringify({ messages })
      });

      if (!res.ok) {
        const err = await res.json().catch(() => ({ error: 'Error' }));
        typingDiv.remove();
        appendMsg('assistant', err.error || 'Something went wrong.');
        streaming = false;
        document.getElementById('bean-send').disabled = false;
        return;
      }

      // Stream SSE
      const reader = res.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';
      let reply = '';
      typingDiv.classList.remove('typing');
      typingDiv.textContent = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop();
        for (const line of lines) {
          if (!line.startsWith('data: ')) continue;
          const data = line.slice(6).trim();
          if (data === '[DONE]') break;
          try {
            const chunk = JSON.parse(data);
            reply += chunk;
            typingDiv.textContent = reply;
            scrollBean();
          } catch {}
        }
      }

      if (reply) {
        messages.push({ role: 'assistant', content: reply });
        sessionStorage.setItem(HISTORY_KEY, JSON.stringify(messages));
      }

      // Show unread dot if panel is hidden
      if (document.getElementById('bean-panel').classList.contains('hidden')) {
        document.getElementById('bean-unread').style.display = 'block';
      }
    } catch (e) {
      typingDiv.remove();
      appendMsg('assistant', 'Sorry, I couldn\'t connect right now. Try again!');
    }

    streaming = false;
    document.getElementById('bean-send').disabled = false;
    document.getElementById('bean-input').focus();
  };
})();
