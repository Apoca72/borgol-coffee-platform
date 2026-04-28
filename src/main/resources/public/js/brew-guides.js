// src/main/resources/public/js/brew-guides.js
// Brew guides and learn articles.

// ── Learn: Brew Guides ────────────────────────────────────────────────────────
let BREW_GUIDES = [];
let LEARN_ARTICLES = [];
let learnTabActive = 'guides';

async function loadLearn() {
  if (!BREW_GUIDES.length) loadBrewGuides();
  if (!LEARN_ARTICLES.length) loadLearnArticles();
}

async function loadBrewGuides() {
  const grid = document.getElementById('brew-guides-grid');
  try {
    BREW_GUIDES = await api('/api/brew-guides');
    grid.innerHTML = BREW_GUIDES.map(g => `
      <div class="card guide-card card-hover" onclick="openBrewGuide(${g.id})">
        <div class="guide-card-inner">
          <div class="guide-icon">${g.icon}</div>
          <div class="guide-name">${esc(g.methodName)}</div>
          <div class="guide-desc">${esc(g.description)}</div>
          <div class="guide-meta">
            <span class="guide-pill">⏱ ${g.brewTimeMin < 60 ? g.brewTimeMin+'m' : Math.floor(g.brewTimeMin/60)+'h'}</span>
            <span class="guide-pill">${diffLabel(g.difficulty)}</span>
          </div>
        </div>
      </div>`).join('');
  } catch(e) { grid.innerHTML = `<div class="empty-state"><div class="empty-msg">Failed to load guides</div></div>`; }
}

async function loadLearnArticles() {
  const grid = document.getElementById('learn-articles-grid');
  try {
    LEARN_ARTICLES = await api('/api/learn');
    grid.innerHTML = LEARN_ARTICLES.map(a => {
      const preview = a.content.replace(/##[^\n]*/g,'').replace(/\n+/g,' ').trim().substring(0,100);
      return `
      <div class="card article-card card-hover" onclick="openLearnArticle(${a.id})">
        <div class="article-icon">${a.icon}</div>
        <div class="article-cat">${esc(a.category)}</div>
        <div class="article-title">${esc(a.title)}</div>
        <div class="article-preview">${esc(preview)}…</div>
        <div class="article-footer">
          <span>📖 ${a.readTimeMin} min read</span>
        </div>
      </div>`;
    }).join('');
  } catch(e) { grid.innerHTML = `<div class="empty-state"><div class="empty-msg">Failed to load articles</div></div>`; }
}

function learnTab(tab, btn) {
  learnTabActive = tab;
  document.querySelectorAll('.learn-tab-content').forEach(el => el.classList.remove('active'));
  document.getElementById('learn-'+tab).classList.add('active');
  btn.closest('.tab-bar').querySelectorAll('.tab-btn').forEach(b=>b.classList.remove('active'));
  btn.classList.add('active');
  if (tab==='guides' && !BREW_GUIDES.length) loadBrewGuides();
  if (tab==='articles' && !LEARN_ARTICLES.length) loadLearnArticles();
}

function openBrewGuide(id) {
  const g = BREW_GUIDES.find(x=>x.id===id);
  if (!g) return;
  document.getElementById('brew-guide-title').textContent = g.icon + ' ' + g.methodName;
  const params = g.parameters.split('\n').filter(Boolean).map(p => {
    const [k,...vs] = p.split(':');
    return `<div class="guide-param">${esc(k)}<span>${esc(vs.join(':').trim())}</span></div>`;
  });
  const steps = g.steps.split('\n').filter(Boolean).map((s,i) => {
    const text = s.replace(/^\d+\.\s*/,'');
    return `<div class="guide-step"><div class="guide-step-num">${i+1}</div><div class="step-text">${esc(text)}</div></div>`;
  });
  document.getElementById('brew-guide-content').innerHTML = `
    <div class="guide-detail-header">
      <p style="color:#555;font-size:14px;line-height:1.6;margin-bottom:16px">${esc(g.description)}</p>
      <div style="display:flex;gap:10px;margin-bottom:16px;flex-wrap:wrap">
        <span class="guide-pill">⏱ ${g.brewTimeMin < 60 ? g.brewTimeMin+' min' : Math.floor(g.brewTimeMin/60)+' hr'}</span>
        <span class="guide-pill">${diffLabel(g.difficulty)}</span>
      </div>
      <div class="section-label" style="margin-bottom:10px">Parameters</div>
      <div class="guide-params">${params.join('')}</div>
      <div class="section-label" style="margin-bottom:12px">Steps</div>
      ${steps.join('')}
    </div>
    <div style="height:16px"></div>`;
  openModal('brew-guide-modal');
}

function openLearnArticle(id) {
  const a = LEARN_ARTICLES.find(x=>x.id===id);
  if (!a) return;
  document.getElementById('learn-article-title').textContent = a.icon + ' ' + a.title;
  // Convert ## headings and newlines to HTML
  let html = esc(a.content)
    .replace(/## ([^\n]+)/g, '</p><h2>$1</h2><p>')
    .replace(/\n\n+/g, '</p><p>')
    .replace(/\n/g, '<br/>');
  html = '<p>' + html + '</p>';
  document.getElementById('learn-article-content').innerHTML = `
    <div class="article-content">
      <div style="display:flex;gap:8px;font-size:12px;color:var(--muted);margin-bottom:16px">
        <span style="background:var(--cream);padding:3px 10px;border-radius:10px;font-weight:700">${esc(a.category)}</span>
        <span>📖 ${a.readTimeMin} min read</span>
      </div>
      ${html}
    </div>`;
  openModal('learn-article-modal');
}

function diffLabel(d) {
  return d==='BEGINNER' ? '🟢 Beginner' : d==='ADVANCED' ? '🔴 Advanced' : '🟡 Intermediate';
}

