// src/main/resources/public/js/journal.js
// Brew journal, bean bags, stats, CSV export.

// ── Radar / Spider Chart ──────────────────────────────────────────────────────
function buildRadarSVG(values, size, showLabels) {
  const labels  = ['Aroma','Flavor','Acidity','Body','Sweet','Finish'];
  const n       = labels.length;
  // Add padding for labels when shown
  const pad     = showLabels ? size * 0.18 : 0;
  const cx      = size / 2 + pad, cy = size / 2 + pad;
  const totalSize = size + pad * 2;
  const r       = size * 0.33;
  const angleOffset = -Math.PI / 2;
  const pt = (i, scale) => {
    const a = angleOffset + (2 * Math.PI * i) / n;
    return [cx + Math.cos(a) * r * scale, cy + Math.sin(a) * r * scale];
  };
  // Grid lines
  let grid = '';
  for (let lvl = 1; lvl <= 5; lvl++) {
    const pts = Array.from({length:n}, (_,i) => pt(i, lvl/5)).map(p=>p.join(',')).join(' ');
    grid += `<polygon points="${pts}" fill="none" stroke="#E8D8C4" stroke-width="${lvl===5?1.5:0.8}"/>`;
  }
  // Axis lines
  let axes = '';
  for (let i = 0; i < n; i++) {
    const [ex, ey] = pt(i, 1);
    axes += `<line x1="${cx}" y1="${cy}" x2="${ex}" y2="${ey}" stroke="#D0C0A8" stroke-width="0.8"/>`;
  }
  // Data polygon
  const dataStr = values.map((v,i) => pt(i, v/10).join(',')).join(' ');
  const poly = `<polygon points="${dataStr}" fill="rgba(198,134,66,.35)" stroke="#C68642" stroke-width="1.8" stroke-linejoin="round"/>`;
  // Data dots
  const dots = values.map((v,i) => { const [x,y]=pt(i,v/10); return `<circle cx="${x}" cy="${y}" r="2.5" fill="#C68642"/>`; }).join('');
  // Labels
  let lbls = '';
  if (showLabels) {
    labels.forEach((lbl,i) => {
      const [lx,ly] = pt(i, 1.28);
      const anchor  = lx < cx - 2 ? 'end' : lx > cx + 2 ? 'start' : 'middle';
      lbls += `<text x="${lx.toFixed(1)}" y="${(ly+4).toFixed(1)}" text-anchor="${anchor}" font-size="10" font-weight="700" fill="#9B8B6E">${lbl}</text>`;
      const [vx,vy] = pt(i, (values[i]/10)*0.72 + 0.05);
      if (values[i] > 0) lbls += `<text x="${vx.toFixed(1)}" y="${(vy+4).toFixed(1)}" text-anchor="middle" font-size="9" font-weight="800" fill="#3D1E0F">${values[i]}</text>`;
    });
  }
  const w = showLabels ? totalSize : size;
  const style = showLabels ? 'max-width:100%;height:auto;' : '';
  return `<svg width="${w}" height="${w}" viewBox="0 0 ${w} ${w}" class="radar-labels" style="${style}">${grid}${axes}${poly}${dots}${lbls}</svg>`;
}

// ── Journal ───────────────────────────────────────────────────────────────────
let JOURNAL_ENTRIES = [];
let currentJournalEntry = null;

async function loadJournal() {
  const grid = document.getElementById('journal-grid');
  if (!ME) {
    grid.innerHTML = `<div class="empty-state"><div class="empty-icon">📓</div><div class="empty-msg">Log in to keep your personal brew journal</div><button class="btn btn-primary" onclick="openModal('login-modal')">Log In</button></div>`;
    return;
  }
  grid.innerHTML = '<div class="loading"><div class="spinner"></div> Loading journal…</div>';
  try {
    JOURNAL_ENTRIES = await api('/api/journal');
    renderJournalGrid();
    rcUpdate();
  } catch(e) { grid.innerHTML = `<div class="empty-state"><div class="empty-icon">⚠️</div><div class="empty-msg">${esc(e.message)}</div></div>`; }
}

function renderJournalGrid() {
  const grid = document.getElementById('journal-grid');
  if (!JOURNAL_ENTRIES.length) {
    grid.innerHTML = `<div class="empty-state" style="grid-column:1/-1"><div class="empty-icon">📓</div><div class="empty-msg">No brew entries yet. Start logging your brews!</div><button class="btn btn-primary" onclick="openNewJournalModal()">+ New Entry</button></div>`;
    return;
  }
  grid.innerHTML = JOURNAL_ENTRIES.map(e => {
    const vals = [e.ratingAroma, e.ratingFlavor, e.ratingAcidity, e.ratingBody, e.ratingSweetness, e.ratingFinish];
    const avg  = (vals.reduce((a,b)=>a+b,0)/vals.length).toFixed(1);
    return `
    <div class="card journal-card card-hover" onclick="openJournalDetail(${e.id})">
      <div class="journal-card-top">
        <div class="journal-radar">${buildRadarSVG(vals, 90, false)}</div>
        <div class="journal-info">
          <div class="journal-bean">${esc(e.coffeeBean||'Unknown Bean')}</div>
          <div class="journal-meta">${esc(e.origin||'')}${e.origin&&e.roastLevel?' · ':''}${esc(e.roastLevel||'')}</div>
          <div class="journal-tags">
            ${e.brewMethod?`<span class="journal-tag">${esc(e.brewMethod)}</span>`:''}
            ${e.grindSize?`<span class="journal-tag">${esc(e.grindSize)}</span>`:''}
            <span class="journal-tag" style="background:var(--cream);color:var(--caramel)">⭐ ${avg}</span>
          </div>
        </div>
      </div>
      ${e.notes?`<div class="journal-notes">${esc(e.notes)}</div>`:''}
      <div style="display:flex;justify-content:space-between;align-items:center;padding:8px 0 0;border-top:1px solid var(--border);margin-top:8px">
        <div style="display:flex;align-items:center;gap:5px;background:var(--cream);border:1px solid var(--border);border-radius:8px;padding:3px 9px">
          <span style="font-size:12px">📅</span>
          <span style="font-size:11px;font-weight:700;color:var(--muted)" title="${e.createdAt ? new Date(e.createdAt).toLocaleString() : ''}">${timeAgo(e.createdAt)}</span>
        </div>
        <div style="display:flex;gap:6px" onclick="event.stopPropagation()">
          <button class="btn btn-sm btn-secondary" style="padding:3px 8px;font-size:11px" onclick="openEditJournal(${e.id})">Edit</button>
          <button class="btn btn-sm btn-danger" style="padding:3px 8px;font-size:11px" onclick="deleteJournalEntry(${e.id})">Del</button>
        </div>
      </div>
    </div>`;
  }).join('');
}

function openNewJournalModal() {
  if (!ME) { openModal('login-modal'); return; }
  document.getElementById('journal-edit-id').value = '';
  document.getElementById('journal-modal-title').textContent = '📓 New Brew Entry';
  ['j-bean','j-origin','j-notes'].forEach(id => document.getElementById(id).value='');
  document.getElementById('j-roast').value = 'MEDIUM';
  document.getElementById('j-method').value = 'Pour Over (V60)';
  document.getElementById('j-grind').value = 'Medium-Fine';
  document.getElementById('j-temp').value  = '93';
  document.getElementById('j-dose').value  = '18';
  document.getElementById('j-yield').value = '36';
  document.getElementById('j-time').value  = '210';
  ['aroma','flavor','acidity','body','sweetness','finish'].forEach(k => {
    document.getElementById('j-'+k).value = 7;
    document.getElementById('jv-'+k).textContent = 7;
  });
  document.getElementById('j-weather').value = '';
  document.getElementById('j-weather-display').textContent = 'Weather not fetched';
  updateJournalRadar();
  openModal('journal-modal');
}

function openEditJournal(id) {
  const e = JOURNAL_ENTRIES.find(x=>x.id===id);
  if (!e) return;
  document.getElementById('journal-edit-id').value = e.id;
  document.getElementById('journal-modal-title').textContent = '✏️ Edit Brew Entry';
  document.getElementById('j-bean').value   = e.coffeeBean || '';
  document.getElementById('j-origin').value = e.origin || '';
  document.getElementById('j-roast').value  = e.roastLevel || 'MEDIUM';
  document.getElementById('j-method').value = e.brewMethod || 'Pour Over (V60)';
  document.getElementById('j-grind').value  = e.grindSize || 'Medium-Fine';
  document.getElementById('j-temp').value   = e.waterTempC || 93;
  document.getElementById('j-dose').value   = e.doseGrams || 18;
  document.getElementById('j-yield').value  = e.yieldGrams || 36;
  document.getElementById('j-time').value   = e.brewTimeSec || 210;
  document.getElementById('j-notes').value  = e.notes || '';
  document.getElementById('j-weather').value = e.weatherData || '';
  try {
    const w = e.weatherData ? JSON.parse(e.weatherData) : null;
    document.getElementById('j-weather-display').textContent = w
      ? `${w.condition} · ${w.temp}°C · ${w.humidity}% humidity`
      : 'Weather not fetched';
  } catch { document.getElementById('j-weather-display').textContent = 'Weather not fetched'; }
  const map = {aroma:e.ratingAroma, flavor:e.ratingFlavor, acidity:e.ratingAcidity,
               body:e.ratingBody, sweetness:e.ratingSweetness, finish:e.ratingFinish};
  Object.entries(map).forEach(([k,v]) => {
    document.getElementById('j-'+k).value = v;
    document.getElementById('jv-'+k).textContent = v;
  });
  updateJournalRadar();
  openModal('journal-modal');
}

function updateJournalRadar() {
  const keys = ['aroma','flavor','acidity','body','sweetness','finish'];
  const vals  = keys.map(k => {
    const v = parseInt(document.getElementById('j-'+k).value);
    document.getElementById('jv-'+k).textContent = v;
    return v;
  });
  document.getElementById('journal-radar-preview').innerHTML = buildRadarSVG(vals, 160, true);
}

async function saveJournalEntry() {
  if (!ME) return;
  const editId = document.getElementById('journal-edit-id').value;
  const body = {
    coffeeBean: document.getElementById('j-bean').value.trim(),
    origin:     document.getElementById('j-origin').value.trim(),
    roastLevel: document.getElementById('j-roast').value,
    brewMethod: document.getElementById('j-method').value,
    grindSize:  document.getElementById('j-grind').value,
    waterTempC: parseInt(document.getElementById('j-temp').value),
    doseGrams:  parseFloat(document.getElementById('j-dose').value),
    yieldGrams: parseFloat(document.getElementById('j-yield').value),
    brewTimeSec:parseInt(document.getElementById('j-time').value),
    ratingAroma:    parseInt(document.getElementById('j-aroma').value),
    ratingFlavor:   parseInt(document.getElementById('j-flavor').value),
    ratingAcidity:  parseInt(document.getElementById('j-acidity').value),
    ratingBody:     parseInt(document.getElementById('j-body').value),
    ratingSweetness:parseInt(document.getElementById('j-sweetness').value),
    ratingFinish:   parseInt(document.getElementById('j-finish').value),
    notes: document.getElementById('j-notes').value.trim(),
    weatherData: document.getElementById('j-weather').value.trim(),
  };
  try {
    if (editId) {
      await api('/api/journal/'+editId, {method:'PUT', body:JSON.stringify(body)});
      toast('Entry updated!', 'ok');
    } else {
      await api('/api/journal', {method:'POST', body:JSON.stringify(body)});
      toast('Brew entry logged!', 'ok');
    }
    closeModal('journal-modal');
    loadJournal();
    checkAchievements();
  } catch(e) { toast(e.message, 'err'); }
}

async function fetchJournalWeather() {
  const display = document.getElementById('j-weather-display');
  if (!navigator.geolocation) { display.textContent = 'Geolocation not supported'; return; }
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
      const condition = wmo[c.weather_code] || wmo[Math.floor(c.weather_code/10)*10] || 'Unknown';
      const weather = {temp: Math.round(c.temperature_2m), humidity: c.relative_humidity_2m, condition};
      document.getElementById('j-weather').value = JSON.stringify(weather);
      display.textContent = `${condition} · ${weather.temp}°C · ${weather.humidity}% humidity`;
      display.style.color = 'var(--roast)';
    } catch(e) { display.textContent = 'Weather unavailable'; display.style.color = ''; }
  }, () => { display.textContent = 'Location denied'; display.style.color = ''; });
}

async function deleteJournalEntry(id) {
  if (!confirm('Delete this brew entry?')) return;
  try {
    await api('/api/journal/'+id, {method:'DELETE'});
    toast('Entry deleted', 'info');
    loadJournal();
  } catch(e) { toast(e.message, 'err'); }
}

function openJournalDetail(id) {
  const e = JOURNAL_ENTRIES.find(x=>x.id===id);
  if (!e) return;
  currentJournalEntry = e;
  document.getElementById('journal-detail-title').textContent = esc(e.coffeeBean||'Brew Entry');
  const vals = [e.ratingAroma, e.ratingFlavor, e.ratingAcidity, e.ratingBody, e.ratingSweetness, e.ratingFinish];
  const params = [
    ['Brew Method', e.brewMethod], ['Origin', e.origin], ['Roast Level', e.roastLevel],
    ['Grind Size', e.grindSize], ['Water Temp', e.waterTempC ? e.waterTempC+'°C' : '—'],
    ['Dose', e.doseGrams ? e.doseGrams+'g' : '—'], ['Yield', e.yieldGrams ? e.yieldGrams+'g' : '—'],
    ['Brew Time', e.brewTimeSec ? Math.floor(e.brewTimeSec/60)+'m '+(e.brewTimeSec%60)+'s' : '—'],
  ].filter(([,v])=>v);
  document.getElementById('journal-detail-content').innerHTML = `
    <div style="display:flex;gap:24px;align-items:flex-start;flex-wrap:wrap;margin-bottom:20px">
      <div style="flex:1;min-width:200px">
        <div class="section-label" style="margin-bottom:10px">Brew Parameters</div>
        <div class="journal-detail-grid">
          ${params.map(([k,v])=>`<div class="journal-detail-row"><span class="journal-detail-key">${k}</span><span class="journal-detail-val">${esc(String(v))}</span></div>`).join('')}
        </div>
      </div>
      <div style="min-width:200px;flex:1">
        <div class="section-label" style="margin-bottom:10px">Flavor Profile</div>
        <div class="radar-wrap">${buildRadarSVG(vals, 220, true)}</div>
      </div>
    </div>
    ${e.notes?`<div class="section-label">Tasting Notes</div><p style="font-size:14px;line-height:1.7;color:var(--text);margin-top:8px">${esc(e.notes)}</p>`:''}
    ${e.weatherData ? (() => { try { const w = JSON.parse(e.weatherData); return `<div style="display:flex;align-items:center;gap:8px;margin-top:12px;padding:8px 12px;background:var(--cream);border-radius:8px;font-size:13px;color:var(--muted)">🌤 <strong style="color:var(--roast)">${esc(w.condition)}</strong> · ${esc(String(w.temp))}°C · ${esc(String(w.humidity))}% humidity</div>`; } catch { return ''; } })() : ''}
    <div style="display:flex;align-items:center;gap:8px;margin-top:20px;padding-top:14px;border-top:1px solid var(--border)">
      <span style="font-size:16px">📅</span>
      <div>
        <div style="font-size:12px;font-weight:800;color:var(--muted);text-transform:uppercase;letter-spacing:.5px">Added</div>
        <div style="font-size:14px;font-weight:700;color:var(--roast)">${e.createdAt ? new Date(e.createdAt).toLocaleDateString(undefined,{year:'numeric',month:'long',day:'numeric'}) : '—'}</div>
        <div style="font-size:11px;color:var(--muted)">${e.createdAt ? new Date(e.createdAt).toLocaleTimeString(undefined,{hour:'2-digit',minute:'2-digit'}) : ''} · ${timeAgo(e.createdAt)}</div>
      </div>
    </div>`;
  openModal('journal-detail-modal');
}

function printJournalEntry() {
  if (!currentJournalEntry) return;
  const e = currentJournalEntry;
  const vals = [e.ratingAroma, e.ratingFlavor, e.ratingAcidity, e.ratingBody, e.ratingSweetness, e.ratingFinish];
  const labels = ['Aroma','Flavor','Acidity','Body','Sweetness','Finish'];
  const radarSvg = buildRadarSVG(vals, 220, true);
  const paramRows = [
    ['Coffee Bean', e.coffeeBean], ['Origin', e.origin], ['Roast Level', e.roastLevel],
    ['Brew Method', e.brewMethod], ['Grind Size', e.grindSize],
    ['Water Temp', e.waterTempC?e.waterTempC+'°C':'—'],
    ['Dose', e.doseGrams?e.doseGrams+'g':'—'], ['Yield', e.yieldGrams?e.yieldGrams+'g':'—'],
    ['Brew Time', e.brewTimeSec?Math.floor(e.brewTimeSec/60)+'m '+(e.brewTimeSec%60)+'s':'—'],
  ].filter(([,v])=>v&&v!=='—');
  const ratingRows = labels.map((l,i)=>`<tr><td>${l}</td><td><strong>${vals[i]}/10</strong></td></tr>`).join('');
  const win = window.open('','_blank','width=700,height=900');
  win.document.write(`<!DOCTYPE html><html><head><title>Brew Log — ${e.coffeeBean||'Entry'}</title>
  <style>body{font-family:-apple-system,sans-serif;padding:40px;color:#1A1108;max-width:620px;margin:0 auto}
  h1{font-size:24px;color:#3D1E0F;border-bottom:2px solid #C68642;padding-bottom:10px;margin-bottom:20px}
  .two{display:grid;grid-template-columns:1fr 1fr;gap:24px;margin-bottom:24px}
  table{width:100%;border-collapse:collapse}td{padding:6px 4px;border-bottom:1px solid #E8D8C4;font-size:13px}
  td:first-child{color:#9B8B6E;font-weight:600}
  .notes{background:#FAF6EF;border:1px solid #E8D8C4;border-radius:8px;padding:14px;font-size:13px;line-height:1.7;margin-top:16px}
  .foot{margin-top:32px;font-size:11px;color:#999;text-align:center}
  @media print{@page{margin:20mm}button{display:none}}
  </style></head><body>
  <h1>☕ Brew Log: ${e.coffeeBean||'Entry'}</h1>
  <div class="two">
    <div><h3 style="font-size:14px;color:#9B8B6E;text-transform:uppercase;letter-spacing:.5px;margin-bottom:10px">Brew Parameters</h3>
    <table>${paramRows.map(([k,v])=>`<tr><td>${k}</td><td><strong>${v}</strong></td></tr>`).join('')}</table></div>
    <div><h3 style="font-size:14px;color:#9B8B6E;text-transform:uppercase;letter-spacing:.5px;margin-bottom:10px">Flavor Profile</h3>
    <table>${ratingRows}</table></div>
  </div>
  <div style="text-align:center;margin:16px 0">${radarSvg}</div>
  ${e.notes?`<div class="notes"><strong>Tasting Notes:</strong><br/>${e.notes}</div>`:''}
  <div class="foot">Borgol Coffee Platform · Logged ${new Date(e.createdAt).toLocaleDateString()}</div>
  <br/><button onclick="window.print()" style="padding:10px 24px;background:#3D1E0F;color:#fff;border:none;border-radius:8px;cursor:pointer;font-size:14px">🖨️ Print / Save as PDF</button>
  </body></html>`);
  win.document.close();
}

async function shareJournalEntry() {
  if (!currentJournalEntry) return;
  const e = currentJournalEntry;
  const text = `☕ ${e.coffeeBean||'Coffee'} Brew Log\n` +
    `Method: ${e.brewMethod||'—'} · Origin: ${e.origin||'—'}\n` +
    `Aroma:${e.ratingAroma} Flavor:${e.ratingFlavor} Acidity:${e.ratingAcidity} Body:${e.ratingBody} Sweetness:${e.ratingSweetness} Finish:${e.ratingFinish}\n` +
    (e.notes ? `Notes: ${e.notes}` : '') +
    `\nvia Borgol Coffee Platform`;
  try {
    if (navigator.share) {
      await navigator.share({ title: 'Brew Log — '+e.coffeeBean, text });
    } else {
      await navigator.clipboard.writeText(text);
      toast('Brew details copied to clipboard!', 'ok');
    }
  } catch(err) { if (err.name !== 'AbortError') toast('Could not share', 'err'); }
}

async function exportJournalPDF() {
  if (!ME) { openModal('login-modal'); return; }
  if (!JOURNAL_ENTRIES.length) { toast('No entries to export', 'info'); return; }
  const rows = JOURNAL_ENTRIES.map(e => {
    const vals = [e.ratingAroma, e.ratingFlavor, e.ratingAcidity, e.ratingBody, e.ratingSweetness, e.ratingFinish];
    const avg  = (vals.reduce((a,b)=>a+b,0)/vals.length).toFixed(1);
    const radar = buildRadarSVG(vals, 90, false);
    return `<tr><td>${radar}</td><td><strong>${e.coffeeBean||'—'}</strong><br/><small style="color:#999">${e.origin||''}</small></td>
      <td>${e.brewMethod||'—'}</td><td>${e.roastLevel||'—'}</td>
      <td style="text-align:center"><strong style="color:#C68642;font-size:16px">${avg}</strong><br/><small>avg</small></td>
      <td style="font-size:12px;color:#666;max-width:180px">${e.notes||''}</td></tr>`;
  }).join('');
  const win = window.open('','_blank','width=900,height=700');
  win.document.write(`<!DOCTYPE html><html><head><title>Brew Journal — ${ME.username}</title>
  <style>body{font-family:-apple-system,sans-serif;padding:32px;color:#1A1108}
  h1{color:#3D1E0F;font-size:22px;border-bottom:2px solid #C68642;padding-bottom:8px;margin-bottom:20px}
  table{width:100%;border-collapse:collapse;font-size:13px}
  th{background:#F5E6C8;color:#3D1E0F;font-weight:700;padding:10px 8px;text-align:left;border-bottom:2px solid #E8D8C4}
  td{padding:8px;border-bottom:1px solid #F0E4D0;vertical-align:middle}tr:hover td{background:#FFFBF5}
  .foot{margin-top:24px;font-size:11px;color:#aaa;text-align:center}
  @media print{@page{size:landscape;margin:15mm}button{display:none}}</style></head><body>
  <h1>📓 Brew Journal — ${ME.username}</h1>
  <table><thead><tr><th>Chart</th><th>Coffee Bean</th><th>Method</th><th>Roast</th><th>Score</th><th>Notes</th></tr></thead>
  <tbody>${rows}</tbody></table>
  <div class="foot">Borgol Coffee Platform · ${JOURNAL_ENTRIES.length} entries · ${new Date().toLocaleDateString()}</div>
  <br/><button onclick="window.print()" style="padding:10px 24px;background:#3D1E0F;color:#fff;border:none;border-radius:8px;cursor:pointer">🖨️ Print / Save as PDF</button>
  </body></html>`);
  win.document.close();
}

// ── Journal CSV Export ────────────────────────────────────────────────────────
function exportJournalCSV() {
  if (!JOURNAL_ENTRIES.length) { toast('No entries to export','err'); return; }
  const cols = ['id','coffeeBean','origin','roastLevel','brewMethod','grindSize',
    'waterTempC','doseGrams','yieldGrams','brewTimeSec',
    'ratingAroma','ratingFlavor','ratingAcidity','ratingBody','ratingSweetness','ratingFinish','notes','createdAt'];
  const header = cols.join(',');
  const rows = JOURNAL_ENTRIES.map(e =>
    cols.map(c => {
      const v = e[c] ?? '';
      return typeof v === 'string' && (v.includes(',') || v.includes('"') || v.includes('\n'))
        ? `"${v.replace(/"/g,'""')}"` : v;
    }).join(',')
  );
  const csv = [header, ...rows].join('\n');
  const blob = new Blob([csv], {type:'text/csv'});
  const url  = URL.createObjectURL(blob);
  const a    = Object.assign(document.createElement('a'), {href:url, download:'borgol-journal.csv'});
  document.body.appendChild(a);
  a.click();
  setTimeout(() => { URL.revokeObjectURL(url); a.remove(); }, 1000);
  toast('CSV downloaded!');
}

// ── Journal Tab switcher ───────────────────────────────────────────────────────
let _methodChart = null, _monthlyChart = null;

function switchJournalTab(tab, btn) {
  document.querySelectorAll('#page-journal .tab-btn').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
  const isStats = tab === 'stats';
  document.getElementById('ratio-calc').style.display           = isStats ? 'none' : '';
  document.getElementById('journal-grid').style.display         = isStats ? 'none' : '';
  document.getElementById('journal-stats-panel').style.display  = isStats ? '' : 'none';
  if (isStats) loadJournalStats();
}

async function loadJournalStats() {
  try {
    const s = await api('/api/journal/stats');
    document.getElementById('stat-total').textContent = s.totalEntries || 0;
    document.getElementById('stat-avg').textContent   = s.avgRating ? s.avgRating.toFixed(1) : '—';
    document.getElementById('stat-top-method').textContent =
      s.brewMethods && s.brewMethods.length ? s.brewMethods[0].method : '—';
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

// ── Bean Bag Tracker ───────────────────────────────────────────────────────────
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
    checkAchievements();
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

