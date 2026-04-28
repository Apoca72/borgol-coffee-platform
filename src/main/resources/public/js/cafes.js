// src/main/resources/public/js/cafes.js
// Cafes, map, nearby, ratings, check-ins.

// ── Cafes ─────────────────────────────────────────────────────────────────────
async function loadCafes() {
  const search = document.getElementById('cafe-search').value;
  const dist   = cafeDistFilter === 'ALL' ? '' : cafeDistFilter;
  const url    = `/api/cafes?search=${encodeURIComponent(search||'')}&district=${dist}`;
  document.getElementById('cafes-grid').innerHTML = '<div class="loading"><div class="spinner"></div></div>';
  try {
    const cafes = await api(url);
    document.getElementById('cafes-grid').innerHTML = cafes.length
      ? cafes.map(c => renderCafeCard(c)).join('')
      : emptyState('☕','No cafes found');
  } catch (e) { toast(e.message, 'err'); }
}

function renderCafeCard(c) {
  const img = c.imageUrl || `https://picsum.photos/seed/cafe${c.id}/400/200`;
  return `
    <div class="card cafe-card card-hover" onclick="openCafeDetail(${c.id})">
      <div class="cafe-img"><img src="${img}" alt="${esc(c.name)}" onerror="this.src='https://picsum.photos/seed/c${c.id+20}/400/200'" loading="lazy"/></div>
      <div class="cafe-body">
        <div class="cafe-name">${esc(c.name)}</div>
        <div class="cafe-addr">📍 ${esc(c.district ? c.district + ', ' : '')}${esc(c.city||'')}</div>
        <div style="display:flex;align-items:center;gap:10px">
          <div class="stars">${renderStars(c.avgRating)}</div>
          <span class="rating-text">${c.avgRating>0?c.avgRating.toFixed(1)+' · ':''} ${c.ratingCount} review${c.ratingCount!==1?'s':''}</span>
        </div>
        ${c.hours ? `<div style="font-size:12px;color:var(--muted);margin-top:6px">🕐 ${esc(c.hours)}</div>` : ''}
      </div>
    </div>`;
}

function renderStars(avg) {
  return [1,2,3,4,5].map(i => {
    const cls = i <= Math.floor(avg) ? 'on' : (i - avg < 1 && i - avg > 0 ? 'half' : '');
    return `<span class="star ${cls}">★</span>`;
  }).join('');
}

function setCafeFilter(btn) {
  document.querySelectorAll('[data-dist]').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
  cafeDistFilter = btn.dataset.dist;
  loadCafes();
}

async function openCafeDetail(id) {
  openModal('cafe-detail-modal');
  document.getElementById('cafe-detail-content').innerHTML = '<div class="loading"><div class="spinner"></div></div>';
  try {
    const c = await api(`/api/cafes/${id}`);
    document.getElementById('cafe-detail-title').textContent = c.name;
    const img = c.imageUrl || `https://picsum.photos/seed/cafe${c.id}/800/300`;

    document.getElementById('cafe-detail-content').innerHTML = `
      <img src="${img}" class="detail-img" alt="${esc(c.name)}" onerror="this.src='https://picsum.photos/seed/c${c.id+20}/800/300'" loading="lazy"/>
      <div class="detail-body">
        <div class="detail-title">${esc(c.name)}</div>
        <div class="detail-meta">
          ${c.address ? `<span class="meta-pill">📍 ${esc(c.address)}</span>` : ''}
          ${c.district ? `<span class="meta-pill">🏙️ ${esc(c.district)}</span>` : ''}
          ${c.phone ? `<span class="meta-pill">📞 ${esc(c.phone)}</span>` : ''}
          ${c.hours ? `<span class="meta-pill">🕐 ${esc(c.hours)}</span>` : ''}
        </div>
        <div style="display:flex;align-items:center;gap:12px;margin-bottom:16px">
          <div class="stars" style="font-size:20px">${renderStars(c.avgRating)}</div>
          <span style="font-size:18px;font-weight:800;color:var(--roast)">${c.avgRating>0?c.avgRating.toFixed(1):'-'}</span>
          <span style="color:var(--muted);font-size:14px">(${c.ratingCount} reviews)</span>
        </div>
        ${c.description ? `<p style="font-size:14px;line-height:1.7;color:#555;margin-bottom:20px">${esc(c.description)}</p>` : ''}
        ${c.submittedByUsername ? `<p style="font-size:12px;color:var(--muted);margin-bottom:16px">Added by <span style="cursor:pointer;color:var(--caramel);font-weight:600" onclick="closeModal('cafe-detail-modal');showPage('profile',${c.submittedBy})">${esc(c.submittedByUsername)}</span></p>` : ''}

        ${ME ? `
        <div style="border-top:1px solid var(--border);padding-top:16px;margin-top:16px">
          <div class="section-label">Rate This Cafe</div>
          <div id="rate-stars-${c.id}" style="display:flex;gap:6px;font-size:32px;cursor:pointer;margin-bottom:8px">
            ${[1,2,3,4,5].map(i=>`<span onclick="setRating(${c.id},${i})" data-rating="${i}" style="color:${i<=(c.currentUserRating||0)?'#F5A623':'#ddd'};transition:.15s"
              onmouseover="hoverStars(${c.id},${i})" onmouseout="unhoverStars(${c.id},${c.currentUserRating||0})">★</span>`).join('')}
          </div>
          <textarea id="rate-review-${c.id}" style="width:100%;padding:8px;border:1.5px solid var(--border);border-radius:8px;font-size:13px;resize:vertical;min-height:60px;margin-bottom:8px;font-family:inherit" placeholder="Write a review (optional)…">${esc(c.currentUserReview||'')}</textarea>
          <button class="btn btn-primary btn-sm" onclick="submitRating(${c.id})" id="rate-btn-${c.id}">
            ${c.currentUserRating ? 'Update Rating' : 'Submit Rating'}
          </button>
        </div>` : `<p style="font-size:13px;color:var(--muted)"><a href="#" onclick="closeModal('cafe-detail-modal');openModal('login-modal')" style="color:var(--caramel);font-weight:700">Log in</a> to rate this cafe.</p>`}
        <div style="margin-top:20px;padding-top:16px;border-top:1px solid var(--border)">
          <div style="font-weight:800;font-size:12px;color:var(--muted);text-transform:uppercase;letter-spacing:.5px;margin-bottom:10px">📍 Check-ins</div>
          <div id="cafe-checkins-list-${c.id}" style="margin-bottom:10px"><div class="loading"><div class="spinner"></div></div></div>
          ${ME ? `<div style="display:flex;gap:8px">
            <input id="checkin-note-${c.id}" placeholder="Add a note… (optional)" style="flex:1;padding:8px 12px;border:1.5px solid var(--border);border-radius:8px;font-size:13px;outline:none;font-family:inherit;background:var(--milk);color:var(--text)"/>
            <button class="btn btn-primary" style="font-size:12px;padding:6px 14px" onclick="submitCheckin(${c.id})">Check In 📍</button>
          </div>` : ''}
        </div>
      </div>`;

    document._cafeRatings = document._cafeRatings || {};
    document._cafeRatings[c.id] = c.currentUserRating || 0;
    loadCafeCheckins(c.id);
  } catch (e) {
    document.getElementById('cafe-detail-content').innerHTML = `<div class="modal-body"><p style="color:var(--danger)">${esc(e.message)}</p></div>`;
  }
}

function hoverStars(cafeId, rating) {
  document.querySelectorAll(`#rate-stars-${cafeId} span`).forEach((s,i) => {
    s.style.color = i < rating ? '#F5A623' : '#ddd';
  });
}
function unhoverStars(cafeId, current) {
  document.querySelectorAll(`#rate-stars-${cafeId} span`).forEach((s,i) => {
    s.style.color = i < current ? '#F5A623' : '#ddd';
  });
}
function setRating(cafeId, rating) {
  document._cafeRatings = document._cafeRatings || {};
  document._cafeRatings[cafeId] = rating;
  hoverStars(cafeId, rating);
}

async function submitRating(cafeId) {
  const rating = (document._cafeRatings || {})[cafeId] || 0;
  if (!rating) { toast('Please select a star rating', 'err'); return; }
  const review = document.getElementById(`rate-review-${cafeId}`)?.value || '';
  try {
    await api(`/api/cafes/${cafeId}/rate`, {method:'POST', body:JSON.stringify({rating, review})});
    closeModal('cafe-detail-modal');
    toast('Rating submitted!', 'ok');
    if (document.getElementById('page-cafes').classList.contains('active')) loadCafes();
  } catch (e) { toast(e.message, 'err'); }
}

async function loadCafeCheckins(cafeId) {
  const el = document.getElementById(`cafe-checkins-list-${cafeId}`);
  if (!el) return;
  try {
    const data = await api(`/api/cafes/${cafeId}/checkins`);
    if (!data.length) { el.innerHTML = '<div style="font-size:12px;color:var(--muted)">No check-ins yet. Be the first!</div>'; return; }
    el.innerHTML = data.map(ci => `
      <div style="display:flex;gap:8px;align-items:center;padding:6px 0;border-bottom:1px solid var(--border)">
        ${avatarHtml(ci.username, ci.avatarUrl, 28)}
        <div style="flex:1;min-width:0">
          <span style="font-weight:700;font-size:13px">${esc(ci.username)}</span>
          ${ci.note ? `<span style="font-size:12px;color:var(--muted)"> · ${esc(ci.note)}</span>` : ''}
          <div style="font-size:11px;color:var(--muted)">${timeAgo(ci.createdAt)}</div>
        </div>
      </div>`).join('');
  } catch(e) { if (el) el.innerHTML = ''; }
}

async function submitCheckin(cafeId) {
  const input = document.getElementById(`checkin-note-${cafeId}`);
  const note = input ? input.value.trim() : '';
  try {
    await api(`/api/cafes/${cafeId}/checkin`, {method:'POST', body:JSON.stringify({note})});
    if (input) input.value = '';
    await loadCafeCheckins(cafeId);
    toast('Checked in! 📍');
  } catch(e) { toast(e.message, 'err'); }
}

async function saveCafe() {
  if (!ME) return;
  const body = {
    name:        document.getElementById('c-name').value.trim(),
    address:     document.getElementById('c-address').value.trim(),
    district:    document.getElementById('c-district').value.trim(),
    city:        document.getElementById('c-city').value.trim() || 'Ulaanbaatar',
    phone:       document.getElementById('c-phone').value.trim(),
    description: document.getElementById('c-description').value.trim(),
    hours:       document.getElementById('c-hours').value.trim(),
    imageUrl:    document.getElementById('c-imageurl').value.trim()
  };
  try {
    await api('/api/cafes', {method:'POST', body:JSON.stringify(body)});
    closeModal('new-cafe-modal');
    toast('Cafe added!', 'ok');
    loadCafes();
  } catch (e) { toast(e.message, 'err'); }
}

// ── Map ───────────────────────────────────────────────────────────────────────
let _map = null, _mapMarkers = [];
const UB_CENTER = [47.9077, 106.8832]; // Ulaanbaatar default

function loadMap() {
  const status = document.getElementById('map-status');
  status.textContent = '📍 Getting your location…';
  if (!navigator.geolocation) {
    status.textContent = 'Geolocation not supported — showing Ulaanbaatar.';
    initMap(UB_CENTER, 13);
    fetchNearbyCafes(UB_CENTER[0], UB_CENTER[1]);
    return;
  }
  navigator.geolocation.getCurrentPosition(
    pos => {
      const { latitude: lat, longitude: lng } = pos.coords;
      status.textContent = `📍 Location found — showing cafes within ${document.getElementById('map-radius').value} km`;
      initMap([lat, lng], 14);
      fetchNearbyCafes(lat, lng);
    },
    () => {
      status.textContent = '⚠️ Location denied — showing Ulaanbaatar area.';
      initMap(UB_CENTER, 13);
      fetchNearbyCafes(UB_CENTER[0], UB_CENTER[1]);
    },
    { timeout: 8000 }
  );
}

function initMap(center, zoom) {
  if (_map) { _map.setView(center, zoom); setTimeout(() => _map.invalidateSize(), 50); return; }
  _map = L.map('borgol-map').setView(center, zoom);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© <a href="https://www.openstreetmap.org/">OpenStreetMap</a> contributors',
    maxZoom: 19
  }).addTo(_map);
  // Blue dot for user position
  L.circleMarker(center, { radius: 9, color: '#1565C0', fillColor: '#42A5F5', fillOpacity: 1, weight: 2 })
    .addTo(_map).bindPopup('<b>📍 You are here</b>');
  // Allow the DOM to finish rendering before Leaflet calculates tile positions
  setTimeout(() => _map.invalidateSize(), 100);
}

async function fetchNearbyCafes(lat, lng) {
  const radius = document.getElementById('map-radius').value;
  const status = document.getElementById('map-status');
  // Clear old markers
  _mapMarkers.forEach(m => m.remove());
  _mapMarkers = [];
  document.getElementById('map-cafe-list').innerHTML = '<div class="loading"><div class="spinner"></div> Searching…</div>';

  try {
    const res = await fetch(`/api/cafes/nearby?lat=${lat}&lng=${lng}&radius=${radius}`);
    if (!res.ok) throw new Error(await res.text());
    const cafes = await res.json();
    if (!Array.isArray(cafes)) throw new Error('Invalid response');

    if (!cafes.length) {
      status.textContent = `No cafes found within ${radius} km. Try increasing the radius.`;
      document.getElementById('map-cafe-list').innerHTML = '';
      return;
    }
    status.textContent = `Found ${cafes.length} cafe${cafes.length>1?'s':''} within ${radius} km`;

    // Custom coffee-cup icon
    const coffeeIcon = L.divIcon({
      html: '<div style="font-size:28px;line-height:1;filter:drop-shadow(0 2px 3px rgba(0,0,0,.4))">☕</div>',
      className: '', iconSize: [32, 32], iconAnchor: [16, 28], popupAnchor: [0, -30]
    });

    cafes.forEach(c => {
      const stars = '★'.repeat(Math.round(c.avgRating)) + '☆'.repeat(5 - Math.round(c.avgRating));
      const popup = L.popup({ maxWidth: 260 }).setContent(`
        <div style="font-family:system-ui,sans-serif">
          <div style="font-size:16px;font-weight:700;margin-bottom:4px">${esc(c.name)}</div>
          <div style="color:#795548;font-size:13px;margin-bottom:6px">${esc(c.address)}</div>
          <div style="color:#F57F17;font-size:15px;margin-bottom:2px">${stars}</div>
          <div style="font-size:12px;color:#666;margin-bottom:8px">${c.avgRating.toFixed(1)} / 5 &bull; ${c.ratingCount} review${c.ratingCount!==1?'s':''}</div>
          ${c.hours ? `<div style="font-size:12px;color:#555;margin-bottom:8px">🕐 ${esc(c.hours)}</div>` : ''}
          <button onclick="closeMapPopup();openCafeDetail(${c.id})" style="width:100%;padding:7px;background:#5D4037;color:#fff;border:none;border-radius:8px;font-size:13px;font-weight:600;cursor:pointer">
            View Details & Menu →
          </button>
        </div>`);
      const marker = L.marker([c.lat, c.lng], { icon: coffeeIcon }).addTo(_map).bindPopup(popup);
      _mapMarkers.push(marker);
    });

    // Fit map to all markers + user location
    const bounds = L.latLngBounds(_mapMarkers.map(m => m.getLatLng()));
    bounds.extend([lat, lng]);
    _map.fitBounds(bounds, { padding: [40, 40], maxZoom: 15 });

    // List below map
    document.getElementById('map-cafe-list').innerHTML = `
      <div class="section-title" style="margin-bottom:12px">Nearby Cafes</div>
      <div style="display:flex;flex-direction:column;gap:10px">
        ${cafes.map(c => `
          <div class="card" style="display:flex;gap:14px;align-items:center;padding:14px 16px;cursor:pointer"
               onclick="if(_map&&${c.lat})_map.setView([${c.lat},${c.lng}],16)">
            <div style="font-size:32px">☕</div>
            <div style="flex:1;min-width:0">
              <div style="font-weight:700;font-size:15px">${esc(c.name)}</div>
              <div style="font-size:12px;color:var(--muted)">${esc(c.address)}</div>
              <div style="color:#F57F17;font-size:13px;margin-top:3px">
                ${'★'.repeat(Math.round(c.avgRating))}${'☆'.repeat(5-Math.round(c.avgRating))}
                <span style="color:var(--muted);font-size:12px"> ${c.avgRating.toFixed(1)} (${c.ratingCount})</span>
              </div>
            </div>
            <div style="font-size:20px;color:var(--muted)">›</div>
          </div>`).join('')}
      </div>`;
  } catch (err) {
    console.error('Map fetch failed:', err);
    status.textContent = 'Failed to load nearby cafes.';
    document.getElementById('map-cafe-list').innerHTML = '<div class="empty-state"><div class="empty-icon">⚠️</div><div class="empty-msg">Could not load nearby cafes. Please try again.</div></div>';
  }
}

function refreshMap() {
  if (!_map) return;
  const center = _map.getCenter();
  fetchNearbyCafes(center.lat, center.lng);
}

function locateMe() {
  if (!_map) { loadMap(); return; }
  document.getElementById('map-status').textContent = '📍 Getting your location…';
  navigator.geolocation.getCurrentPosition(
    pos => {
      const { latitude: lat, longitude: lng } = pos.coords;
      _map.setView([lat, lng], 14);
      fetchNearbyCafes(lat, lng);
    },
    () => { document.getElementById('map-status').textContent = '⚠️ Could not get location.'; },
    { timeout: 8000 }
  );
}

function closeMapPopup() { if (_map) _map.closePopup(); }

