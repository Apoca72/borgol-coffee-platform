// src/main/resources/public/js/recipes.js
// Recipes, feed, collections, equipment, reports, hashtags.

// ── Recipes ───────────────────────────────────────────────────────────────────
async function loadRecipes() {
  const search  = document.getElementById('recipe-search').value;
  const isSort  = drinkFilter === 'TRENDING';
  const dt      = isSort ? '' : (drinkFilter === 'ALL' ? '' : drinkFilter);
  const sort    = isSort ? 'TRENDING' : 'LATEST';
  const url     = `/api/recipes?search=${encodeURIComponent(search||'')}&drinkType=${dt}&sort=${sort}`;
  document.getElementById('recipes-grid').innerHTML = '<div class="loading"><div class="spinner"></div></div>';
  try {
    const recipes = await api(url);
    document.getElementById('recipes-grid').innerHTML =
      recipes.length ? recipes.map(r => renderRecipeCard(r)).join('') : emptyState('☕','No recipes found');
  } catch (e) { toast(e.message, 'err'); }
}

function renderRecipeCard(r) {
  const dtLabel = DRINK_LABELS[r.drinkType] || r.drinkType;
  const img     = r.imageUrl || `https://picsum.photos/seed/${r.id}/400/200`;
  return `
    <div class="card recipe-card card-hover" onclick="openRecipeDetail(${r.id})">
      <div class="recipe-img">
        <img src="${img}" alt="${esc(r.title)}" onerror="this.src='https://picsum.photos/seed/${r.id+5}/400/200'" loading="lazy"/>
        <span class="recipe-badge dt-${r.drinkType}">${dtLabel}</span>
      </div>
      <div class="recipe-body">
        <div class="recipe-title">${esc(r.title)}</div>
        <div class="recipe-author">by ${esc(r.authorUsername)}</div>
        <div class="recipe-meta">
          ${r.brewTime ? `<span>⏱️ ${r.brewTime}min</span>` : ''}
          ${r.difficulty ? `<span>${diffIcon(r.difficulty)} ${cap(r.difficulty)}</span>` : ''}
        </div>
        ${r.flavorTags?.length ? `<div class="flavor-tags">${r.flavorTags.slice(0,3).map(t=>`<span class="flavor-tag" onclick="event.stopPropagation();openHashtagPage('${t.toLowerCase()}')">#${t.toLowerCase()}</span>`).join('')}</div>` : ''}
      </div>
      <div class="recipe-actions" onclick="event.stopPropagation()">
        <button class="like-btn${r.likedByCurrentUser?' liked':''}" onclick="toggleLike(${r.id},this)">
          ${r.likedByCurrentUser?'❤️':'🤍'} <span class="like-count">${r.likesCount||0}</span>
        </button>
        <span class="comment-count">💬 ${r.commentCount||0}</span>
        <button class="save-btn${r.savedByCurrentUser?' saved':''}" onclick="toggleSave(${r.id},this)" title="${r.savedByCurrentUser?'Unsave':'Save'}">
          ${r.savedByCurrentUser?'🔖':'🔖'}<span style="font-size:11px">${r.savedByCurrentUser?'Saved':''}</span>
        </button>
        <button class="report-btn" onclick="openReportModal('recipe',${r.id})" title="Report">⚑</button>
        <button onclick="openQuickBrew({id:${r.id},title:${JSON.stringify(r.title||'')},instructions:${JSON.stringify(r.instructions||'')},brewTime:${r.brewTime||5}})" title="Step-by-step guide" style="border:none;background:none;cursor:pointer;font-size:13px;color:var(--caramel,#A8621E);padding:5px 8px;border-radius:8px;transition:.15s" onmouseover="this.style.background='rgba(168,98,30,.1)'" onmouseout="this.style.background='none'">⚡ Easy Make</button>
        ${ME ? `<button onclick="openAddToCollection(${r.id})" title="Save to collection" style="border:none;background:none;cursor:pointer;font-size:14px;color:var(--muted);padding:5px 8px;border-radius:8px;transition:.15s" onmouseover="this.style.color='var(--roast)'" onmouseout="this.style.color='var(--muted)'">📚</button>` : ''}
      </div>
    </div>`;
}

function setDrinkFilter(btn) {
  document.querySelectorAll('[data-dt]').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
  drinkFilter = btn.dataset.dt;
  loadRecipes();
}

// ── Recipe detail ─────────────────────────────────────────────────────────────
async function openRecipeDetail(id) {
  openModal('recipe-detail-modal');
  document.getElementById('recipe-detail-content').innerHTML = '<div class="loading"><div class="spinner"></div></div>';
  try {
    const [r, comments] = await Promise.all([
      api(`/api/recipes/${id}`),
      api(`/api/recipes/${id}/comments`)
    ]);
    document.getElementById('recipe-detail-title').textContent = r.title;

    const img         = r.imageUrl || `https://picsum.photos/seed/${r.id}/800/400`;
    const dtLabel     = DRINK_LABELS[r.drinkType] || r.drinkType;
    const isMine      = ME && ME.id === r.authorId;
    const ingredients = (r.ingredients||'').split('\n').filter(l=>l.trim());
    const steps       = (r.instructions||'').split('\n').filter(l=>l.trim());

    document.getElementById('recipe-detail-content').innerHTML = `
      <img src="${img}" class="detail-img" alt="${esc(r.title)}" onerror="this.src='https://picsum.photos/seed/${r.id+5}/800/400'" loading="lazy"/>
      <div class="detail-body">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px">
          <div>
            <div class="detail-title">${esc(r.title)}</div>
            <div style="font-size:13px;color:var(--muted)">by <span style="cursor:pointer;color:var(--caramel);font-weight:700" onclick="closeModal('recipe-detail-modal');showPage('profile',${r.authorId})">${esc(r.authorUsername)}</span></div>
          </div>
          ${isMine ? `<div style="display:flex;gap:6px">
            <button class="btn btn-sm btn-secondary" onclick="openEditRecipe(${r.id})">✏️ Edit</button>
            <button class="btn btn-sm btn-danger" onclick="deleteRecipe(${r.id})">🗑</button>
          </div>` : ''}
        </div>
        <div class="detail-meta">
          <span class="meta-pill recipe-badge dt-${r.drinkType}" style="position:static">${dtLabel}</span>
          ${r.brewTime ? `<span class="meta-pill">⏱️ ${r.brewTime} min</span>` : ''}
          ${r.difficulty ? `<span class="meta-pill">${diffIcon(r.difficulty)} ${cap(r.difficulty)}</span>` : ''}
          ${r.flavorTags?.length ? `<span class="meta-pill">🏷️ ${r.flavorTags.join(', ')}</span>` : ''}
        </div>
        ${r.description ? `<p style="font-size:14px;line-height:1.7;color:#555;margin-bottom:20px">${esc(r.description)}</p>` : ''}

        ${ingredients.length ? `
        <div style="margin-bottom:20px">
          <div class="section-label">Ingredients</div>
          <ul class="ingredients-list">${ingredients.map(i=>`<li>${esc(i)}</li>`).join('')}</ul>
        </div>` : ''}

        ${steps.length ? `
        <div style="margin-bottom:20px">
          <div class="section-label">Instructions</div>
          ${steps.map((s,i)=>`
            <div class="step">
              <div class="step-num">${i+1}</div>
              <div class="step-text">${esc(s)}</div>
            </div>`).join('')}
        </div>` : ''}

        <div style="display:flex;align-items:center;gap:16px;padding:12px 0;border-top:1px solid var(--border);border-bottom:1px solid var(--border);margin-bottom:16px">
          <button class="like-btn${r.likedByCurrentUser?' liked':''}" id="detail-like-btn" onclick="toggleLike(${r.id},this)">
            ${r.likedByCurrentUser?'❤️':'🤍'} <span class="like-count">${r.likesCount||0}</span> likes
          </button>
          <button class="save-btn${r.savedByCurrentUser?' saved':''}" id="detail-save-btn" onclick="toggleSave(${r.id},this)">
            🔖 ${r.savedByCurrentUser?'Saved':'Save'}
          </button>
          <button class="report-btn" style="opacity:1;margin-left:auto" onclick="openReportModal('recipe',${r.id})">⚑ Report</button>
        </div>

        <div class="comments-section">
          <div class="section-label">Comments (${comments.length})</div>
          <div id="comments-list-${r.id}">
            ${comments.length ? comments.map(c=>renderComment(c)).join('') : '<p style="color:var(--muted);font-size:13px">No comments yet. Be the first!</p>'}
          </div>
          ${ME ? `
          <div class="comment-input">
            <div class="avatar" style="width:32px;height:32px;font-size:13px;flex-shrink:0">${ME.username[0].toUpperCase()}</div>
            <input id="new-comment-${r.id}" type="text" placeholder="Add a comment…"
              onkeydown="if(event.key==='Enter')postComment(${r.id})"/>
            <button class="btn btn-sm btn-primary" onclick="postComment(${r.id})">Post</button>
          </div>` : `<p style="font-size:13px;color:var(--muted);margin-top:12px"><a href="#" onclick="closeModal('recipe-detail-modal');openModal('login-modal')" style="color:var(--caramel);font-weight:700">Log in</a> to comment.</p>`}
        </div>
      </div>`;
  } catch (e) {
    document.getElementById('recipe-detail-content').innerHTML = `<div class="modal-body"><p style="color:var(--danger)">${esc(e.message)}</p></div>`;
  }
}

function renderComment(c) {
  return `
    <div class="comment-item">
      ${avatarHtml(c.authorUsername, null, 32)}
      <div class="comment-body">
        <div class="comment-author" onclick="closeModal('recipe-detail-modal');showPage('profile',${c.authorId})">${esc(c.authorUsername)}</div>
        <div class="comment-text">${esc(c.content)}</div>
        <div class="comment-time">${timeAgo(c.createdAt)}</div>
      </div>
    </div>`;
}

async function postComment(recipeId) {
  const input   = document.getElementById(`new-comment-${recipeId}`);
  const content = input.value.trim();
  if (!content) return;
  try {
    const comment = await api(`/api/recipes/${recipeId}/comments`, {method:'POST',body:JSON.stringify({content})});
    const list    = document.getElementById(`comments-list-${recipeId}`);
    if (!list.children.length || list.firstElementChild?.tagName === 'P') list.innerHTML = '';
    list.innerHTML += renderComment(comment);
    input.value = '';
    toast('Comment added!', 'ok');
  } catch (e) { toast(e.message, 'err'); }
}

// ── Like toggle ───────────────────────────────────────────────────────────────
async function toggleLike(recipeId, btn) {
  if (!ME) { openModal('login-modal'); return; }
  try {
    const result = await api(`/api/recipes/${recipeId}/like`, {method:'POST'});
    document.querySelectorAll('.like-btn').forEach(b => {
      if (b.getAttribute('onclick')?.includes(recipeId)) {
        b.className = `like-btn${result.liked?' liked':''}`;
        const isDetail = b.id === 'detail-like-btn';
        b.innerHTML = `${result.liked?'❤️':'🤍'} <span class="like-count">${result.likesCount}</span>${isDetail?' likes':''}`;
      }
    });
  } catch (e) { toast(e.message, 'err'); }
}

// ── Recipe photo picker ───────────────────────────────────────────────────────
function recipePhotoChosen(input) {
  if (input.files && input.files[0]) compressRecipePhoto(input.files[0]);
}
function recipeDragOver(e) {
  e.preventDefault();
  document.getElementById('r-photo-drop').classList.add('drag-over');
}
function recipeDragLeave(e) {
  document.getElementById('r-photo-drop').classList.remove('drag-over');
}
function recipeDrop(e) {
  e.preventDefault();
  document.getElementById('r-photo-drop').classList.remove('drag-over');
  const file = e.dataTransfer.files[0];
  if (file && file.type.startsWith('image/')) compressRecipePhoto(file);
}
function recipeUrlTyped(input) {
  if (input.value.trim()) recipePhotoClear(true); // clear file pick if URL typed
}
function recipePhotoClear(urlOnly) {
  if (!urlOnly) {
    document.getElementById('r-imageurl').value = '';
    document.getElementById('r-photo-file').value = '';
  }
  document.getElementById('r-photo-preview-wrap').style.display = 'none';
  document.getElementById('r-photo-preview').src = '';
  document.getElementById('r-photo-drop')._base64 = null;
}
function compressRecipePhoto(file) {
  const compressEl = document.getElementById('r-photo-compressing');
  compressEl.style.display = 'block';
  document.getElementById('r-imageurl').value = ''; // clear URL field
  const reader = new FileReader();
  reader.onload = e => {
    const img = new Image();
    img.onload = () => {
      const MAX = 900;
      let w = img.width, h = img.height;
      if (w > MAX) { h = Math.round(h * MAX / w); w = MAX; }
      const canvas = document.createElement('canvas');
      canvas.width = w; canvas.height = h;
      canvas.getContext('2d').drawImage(img, 0, 0, w, h);
      const base64 = canvas.toDataURL('image/jpeg', 0.78);
      document.getElementById('r-photo-drop')._base64 = base64;
      document.getElementById('r-photo-preview').src = base64;
      document.getElementById('r-photo-preview-wrap').style.display = 'block';
      compressEl.style.display = 'none';
    };
    img.src = e.target.result;
  };
  reader.readAsDataURL(file);
}

// ── New/Edit recipe ───────────────────────────────────────────────────────────
function openNewRecipeModal() {
  if (!ME) { openModal('login-modal'); return; }
  buildFlavorPicker('r-flavor-picker', []);
  document.getElementById('recipe-edit-id').value   = '';
  document.getElementById('recipe-modal-title').textContent = '✍️ Share a Recipe';
  document.getElementById('r-title').value          = '';
  document.getElementById('r-description').value    = '';
  document.getElementById('r-drinktype').value      = 'POUR_OVER';
  document.getElementById('r-brewtime').value       = '5';
  document.getElementById('r-difficulty').value     = 'MEDIUM';
  document.getElementById('r-ingredients').value    = '';
  document.getElementById('r-instructions').value   = '';
  recipePhotoClear();
  openModal('new-recipe-modal');
}

async function openEditRecipe(recipeId) {
  try {
    const r = await api(`/api/recipes/${recipeId}`);
    buildFlavorPicker('r-flavor-picker', r.flavorTags || []);
    document.getElementById('recipe-edit-id').value   = recipeId;
    document.getElementById('recipe-modal-title').textContent = '✏️ Edit Recipe';
    document.getElementById('r-title').value          = r.title;
    document.getElementById('r-description').value    = r.description || '';
    document.getElementById('r-drinktype').value      = r.drinkType || 'POUR_OVER';
    document.getElementById('r-brewtime').value       = r.brewTime || 5;
    document.getElementById('r-difficulty').value     = r.difficulty || 'MEDIUM';
    document.getElementById('r-ingredients').value    = r.ingredients || '';
    document.getElementById('r-instructions').value   = r.instructions || '';
    recipePhotoClear();
    // Show existing image in preview if it's a URL (not base64)
    if (r.imageUrl && !r.imageUrl.startsWith('data:')) {
      document.getElementById('r-imageurl').value = r.imageUrl;
    } else if (r.imageUrl) {
      document.getElementById('r-photo-preview').src = r.imageUrl;
      document.getElementById('r-photo-preview-wrap').style.display = 'block';
      document.getElementById('r-photo-drop')._base64 = r.imageUrl;
    }
    closeModal('recipe-detail-modal');
    openModal('new-recipe-modal');
  } catch (e) { toast(e.message, 'err'); }
}

async function saveRecipe() {
  if (!ME) return;
  const id   = document.getElementById('recipe-edit-id').value;
  const body = {
    title:        document.getElementById('r-title').value.trim(),
    description:  document.getElementById('r-description').value.trim(),
    drinkType:    document.getElementById('r-drinktype').value,
    brewTime:     parseInt(document.getElementById('r-brewtime').value) || 0,
    difficulty:   document.getElementById('r-difficulty').value,
    ingredients:  document.getElementById('r-ingredients').value.trim(),
    instructions: document.getElementById('r-instructions').value.trim(),
    flavorTags:   getSelectedFlavors('r-flavor-picker'),
    imageUrl:     document.getElementById('r-photo-drop')._base64 ||
                  document.getElementById('r-imageurl').value.trim()
  };
  try {
    if (id) {
      await api(`/api/recipes/${id}`, {method:'PUT',body:JSON.stringify(body)});
      toast('Recipe updated!', 'ok');
    } else {
      await api('/api/recipes', {method:'POST',body:JSON.stringify(body)});
      toast('Recipe shared!', 'ok');
    }
    closeModal('new-recipe-modal');
    loadRecipes();
    if (document.getElementById('page-feed').classList.contains('active')) loadFeed();
  } catch (e) { toast(e.message, 'err'); }
}

async function deleteRecipe(recipeId) {
  if (!confirm('Delete this recipe? This cannot be undone.')) return;
  try {
    await api(`/api/recipes/${recipeId}`, {method:'DELETE'});
    closeModal('recipe-detail-modal');
    toast('Recipe deleted', 'ok');
    loadRecipes();
  } catch (e) { toast(e.message, 'err'); }
}

// ── Flavor picker ─────────────────────────────────────────────────────────────
function buildFlavorPicker(containerId, selectedFlavors) {
  const container = document.getElementById(containerId);
  container.innerHTML = FLAVORS.map(f => `
    <span class="flavor-opt${selectedFlavors.includes(f)?' selected':''}" onclick="this.classList.toggle('selected')">${f.toLowerCase()}</span>
  `).join('');
}

function getSelectedFlavors(containerId) {
  return Array.from(document.querySelectorAll(`#${containerId} .flavor-opt.selected`))
    .map(el => el.textContent.toUpperCase().trim());
}

// ── Equipment ─────────────────────────────────────────────────────────────────
const EQ_ICONS = {GRINDER:'☕',BREWER:'🫗',KETTLE:'♨️',SCALE:'⚖️',TAMPER:'🔩',MACHINE:'🤖',OTHER:'📦'};

async function loadEquipment() {
  if (!ME) return '<div class="empty-state"><div class="empty-icon">🔧</div><div class="empty-msg">Log in to track your equipment</div></div>';
  try {
    const list = await api('/api/equipment');
    if (!list.length) {
      return `<div class="empty-state" style="grid-column:1/-1">
        <div class="empty-icon">🔧</div>
        <div class="empty-msg">No equipment tracked yet. Add your grinder, brewer, and more!</div>
        <button class="btn btn-primary" onclick="openModal('add-equipment-modal')">+ Add Equipment</button>
      </div>`;
    }
    return `<div style="display:flex;justify-content:flex-end;margin-bottom:12px">
      <button class="btn btn-primary" onclick="openModal('add-equipment-modal')">+ Add Equipment</button>
    </div>
    <div class="equipment-grid">
      ${list.map(eq => `
        <div class="equipment-card">
          <button class="equipment-del" onclick="deleteEquipment(${eq.id})" title="Remove">🗑️</button>
          <span class="eq-cat-icon">${EQ_ICONS[eq.category]||'📦'}</span>
          <div class="equipment-cat-badge">${eq.category}</div>
          <div class="equipment-name">${esc(eq.name)}</div>
          ${eq.brand ? `<div class="equipment-brand">${esc(eq.brand)}</div>` : ''}
          ${eq.notes ? `<div class="equipment-notes">${esc(eq.notes)}</div>` : ''}
        </div>`).join('')}
    </div>`;
  } catch(e) {
    return `<div class="empty-state"><div class="empty-msg">⚠️ ${esc(e.message)}</div></div>`;
  }
}

async function saveEquipment() {
  const name = document.getElementById('eq-name').value.trim();
  if (!name) { toast('Name is required','err'); return; }
  const body = {
    category: document.getElementById('eq-category').value,
    name,
    brand: document.getElementById('eq-brand').value.trim(),
    notes: document.getElementById('eq-notes').value.trim(),
  };
  try {
    await api('/api/equipment', {method:'POST', body:JSON.stringify(body), headers:{'Content-Type':'application/json'}});
    closeModal('add-equipment-modal');
    toast('Equipment added!');
    // Refresh equipment tab if active
    if (currentProfileTab === 'equipment') {
      const content = document.getElementById('profile-tab-content');
      content.innerHTML = await loadEquipment();
    }
  } catch(e) { toast(e.message,'err'); }
}

async function deleteEquipment(id) {
  if (!confirm('Remove this equipment?')) return;
  try {
    await api(`/api/equipment/${id}`, {method:'DELETE'});
    toast('Removed');
    if (currentProfileTab === 'equipment') {
      const content = document.getElementById('profile-tab-content');
      content.innerHTML = await loadEquipment();
    }
  } catch(e) { toast(e.message,'err'); }
}

// ── Recipe Collections ────────────────────────────────────────────────────────

let COLLECTIONS = [];

async function loadCollections() {
  const grid = document.getElementById('collections-grid');
  try {
    COLLECTIONS = await api('/api/collections');
    renderCollectionsGrid();
  } catch(e) {
    grid.innerHTML = `<div class="empty-state" style="grid-column:1/-1"><div class="empty-icon">⚠️</div><div class="empty-msg">${esc(e.message)}</div></div>`;
  }
}

function renderCollectionsGrid() {
  const grid = document.getElementById('collections-grid');
  if (!COLLECTIONS.length) {
    grid.innerHTML = `<div class="empty-state" style="grid-column:1/-1"><div class="empty-icon">📚</div><div class="empty-msg">No collections yet. Create your first one!</div><button class="btn btn-primary" onclick="openNewCollectionModal()">+ New Collection</button></div>`;
    return;
  }
  grid.innerHTML = COLLECTIONS.map(c => `
    <div class="card card-hover" style="padding:16px;cursor:pointer" onclick="openCollectionDetail(${c.id})">
      <div style="font-size:28px;margin-bottom:8px">📚</div>
      <div style="font-weight:800;font-size:15px;margin-bottom:4px">${esc(c.name)}</div>
      ${c.description ? `<div style="font-size:12px;color:var(--muted);margin-bottom:8px">${esc(c.description)}</div>` : ''}
      <div style="display:flex;justify-content:space-between;align-items:center;padding-top:8px;border-top:1px solid var(--border);font-size:12px;color:var(--muted)">
        <span>📖 ${c.recipeCount} recipe${c.recipeCount!==1?'s':''}</span>
        <button class="btn btn-sm btn-danger" style="padding:3px 8px;font-size:11px" onclick="event.stopPropagation();deleteCollectionConfirm(${c.id})">Delete</button>
      </div>
    </div>`).join('');
}

function openNewCollectionModal() {
  document.getElementById('col-name').value = '';
  document.getElementById('col-desc').value = '';
  openModal('collection-new-modal');
}

async function saveCollection() {
  const name = document.getElementById('col-name').value.trim();
  if (!name) { toast('Collection name is required', 'err'); return; }
  try {
    await api('/api/collections', {method:'POST', body:JSON.stringify({name, description: document.getElementById('col-desc').value.trim(), isPublic: true})});
    closeModal('collection-new-modal');
    await loadCollections();
    toast('Collection created!');
  } catch(e) { toast(e.message, 'err'); }
}

async function deleteCollectionConfirm(id) {
  if (!confirm('Delete this collection?')) return;
  try {
    await api(`/api/collections/${id}`, {method:'DELETE'});
    await loadCollections();
    toast('Deleted');
  } catch(e) { toast(e.message, 'err'); }
}

async function openCollectionDetail(id) {
  const col = COLLECTIONS.find(c => c.id === id);
  if (col) document.getElementById('col-detail-title').textContent = `📚 ${col.name}`;
  const content = document.getElementById('col-detail-content');
  content.innerHTML = '<div class="loading"><div class="spinner"></div></div>';
  openModal('collection-detail-modal');
  try {
    const recipes = await api(`/api/collections/${id}/recipes`);
    if (!recipes.length) {
      content.innerHTML = `<div class="empty-state" style="grid-column:1/-1"><div class="empty-icon">📖</div><div class="empty-msg">No recipes in this collection yet.</div></div>`;
      return;
    }
    content.innerHTML = recipes.map(r => `
      <div class="card card-hover" style="padding:12px;cursor:pointer" onclick="closeModal('collection-detail-modal');openRecipeDetail(${r.id})">
        <div style="font-weight:700;font-size:14px;margin-bottom:4px">${esc(r.title)}</div>
        <div style="font-size:12px;color:var(--muted)">by ${esc(r.username)}</div>
        <div style="display:flex;justify-content:flex-end;margin-top:8px">
          <button class="btn btn-sm btn-danger" style="padding:3px 8px;font-size:11px" onclick="event.stopPropagation();removeFromCollection(${id},${r.id})">Remove</button>
        </div>
      </div>`).join('');
  } catch(e) { content.innerHTML = `<p style="color:var(--danger)">${esc(e.message)}</p>`; }
}

async function removeFromCollection(collectionId, recipeId) {
  try {
    await api(`/api/collections/${collectionId}/recipes/${recipeId}`, {method:'DELETE'});
    await openCollectionDetail(collectionId);
    toast('Removed from collection');
  } catch(e) { toast(e.message, 'err'); }
}

async function openAddToCollection(recipeId) {
  document.getElementById('atc-recipe-id').value = recipeId;
  const list = document.getElementById('atc-collections-list');
  list.innerHTML = '<div class="loading"><div class="spinner"></div></div>';
  openModal('add-to-col-modal');
  try {
    const cols = await api('/api/collections');
    if (!cols.length) {
      list.innerHTML = `<div style="font-size:13px;color:var(--muted)">No collections yet. <button class="btn btn-sm btn-primary" onclick="closeModal('add-to-col-modal');openNewCollectionModal()">Create one</button></div>`;
      return;
    }
    list.innerHTML = cols.map(c => `
      <div style="display:flex;align-items:center;justify-content:space-between;padding:8px 0;border-bottom:1px solid var(--border)">
        <span style="font-size:13px;font-weight:600">${esc(c.name)} <span style="font-weight:400;color:var(--muted)">(${c.recipeCount})</span></span>
        <button class="btn btn-sm btn-primary" style="font-size:11px;padding:3px 10px" onclick="addToCollection(${c.id},${recipeId})">Add</button>
      </div>`).join('');
  } catch(e) { list.innerHTML = `<p style="color:var(--danger)">${esc(e.message)}</p>`; }
}

async function addToCollection(collectionId, recipeId) {
  try {
    await api(`/api/collections/${collectionId}/recipes`, {method:'POST', body:JSON.stringify({recipeId})});
    closeModal('add-to-col-modal');
    toast('Added to collection! 📚');
    COLLECTIONS = [];  // invalidate cache
  } catch(e) { toast(e.message, 'err'); }
}

// ── Save (Bookmark) ────────────────────────────────────────────────────────────
async function toggleSave(recipeId, btn) {
  if (!ME) { openModal('login-modal'); return; }
  try {
    const result = await api(`/api/recipes/${recipeId}/save`, {method:'POST'});
    document.querySelectorAll('.save-btn').forEach(b => {
      if (b.getAttribute('onclick')?.includes(`toggleSave(${recipeId}`)) {
        b.className = `save-btn${result.saved?' saved':''}`;
        const isDetail = b.id === 'detail-save-btn';
        b.innerHTML = isDetail
          ? `🔖 ${result.saved?'Saved':'Save'}`
          : `🔖<span style="font-size:11px">${result.saved?'Saved':''}</span>`;
      }
    });
    toast(result.saved ? '🔖 Recipe saved' : 'Removed from saved', result.saved ? 'ok' : 'info');
  } catch(e) { toast('✗ ' + e.message, 'err'); }
}

// ── Report modal ───────────────────────────────────────────────────────────────
let _reportType = '', _reportId = 0;
function openReportModal(type, id) {
  if (!ME) { openModal('login-modal'); return; }
  _reportType = type; _reportId = id;
  document.getElementById('report-reason').value = '';
  document.getElementById('report-desc').value = '';
  openModal('report-modal');
}
async function submitReport() {
  const reason = document.getElementById('report-reason').value;
  const desc   = document.getElementById('report-desc').value;
  if (!reason) { toast('Please select a reason', 'err'); return; }
  try {
    await api('/api/report', {method:'POST', body: JSON.stringify({
      contentType: _reportType, contentId: _reportId,
      reason, description: desc
    })});
    closeModal('report-modal');
    toast('✓ Report submitted. Thank you.', 'ok');
  } catch(e) { toast('✗ ' + e.message, 'err'); }
}

// ── Hashtag pages ──────────────────────────────────────────────────────────────
async function openHashtagPage(tag) {
  try {
    const recipes = await api(`/api/hashtags/${encodeURIComponent(tag)}/recipes`);
    const followed = ME ? (await api('/api/users/me/hashtags').catch(()=>[])) : [];
    const isFollowing = followed.includes(tag);
    const container = document.getElementById('recipe-detail-content');
    openModal('recipe-detail-modal');
    container.innerHTML = `<div class="modal-header">
      <div class="modal-title">#${esc(tag)}</div>
      <div style="display:flex;gap:8px;align-items:center">
        ${ME ? `<button class="btn btn-sm ${isFollowing?'btn-secondary':'btn-primary'}" id="hashtag-follow-btn"
          onclick="toggleHashtag('${esc(tag)}',this)">${isFollowing?'✓ Following':'+ Follow #'+esc(tag)}</button>` : ''}
        <button class="modal-close" onclick="closeModal('recipe-detail-modal')">✕</button>
      </div></div>
      <div class="modal-body">
        <div style="color:var(--muted);font-size:13px;margin-bottom:16px">${recipes.length} recipe${recipes.length!==1?'s':''} tagged #${esc(tag)}</div>
        ${recipes.length ? `<div class="recipe-grid">${recipes.map(r=>renderRecipeCard(r)).join('')}</div>`
          : `<div class="empty-state"><div class="empty-icon">🏷️</div><div class="empty-msg">No recipes with #${esc(tag)} yet</div></div>`}
      </div>`;
  } catch(e) { toast('✗ ' + e.message, 'err'); }
}
async function toggleHashtag(tag, btn) {
  if (!ME) { openModal('login-modal'); return; }
  const following = btn.textContent.includes('Following');
  try {
    if (following) {
      await api(`/api/hashtags/${encodeURIComponent(tag)}/follow`, {method:'DELETE'});
      btn.className = 'btn btn-sm btn-primary';
      btn.textContent = '+ Follow #' + tag;
    } else {
      await api(`/api/hashtags/${encodeURIComponent(tag)}/follow`, {method:'POST'});
      btn.className = 'btn btn-sm btn-secondary';
      btn.textContent = '✓ Following';
    }
    toast(following ? `Unfollowed #${tag}` : `Following #${tag}!`, following ? 'info' : 'ok');
  } catch(e) { toast('✗ ' + e.message, 'err'); }
}

