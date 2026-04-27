// src/main/resources/public/js/auth.js
// Authentication: login, register, logout, token, profile, notifications.

// ── Auth ──────────────────────────────────────────────────────────────────────
async function doLogin() {
  try {
    const email    = document.getElementById('login-email').value.trim();
    const password = document.getElementById('login-password').value;
    const result   = await api('/api/auth/login', {method:'POST',body:JSON.stringify({email,password})});
    TOKEN = result.token;
    ME    = result.user;
    localStorage.setItem('borgol_token', TOKEN);
    closeModal('login-modal');
    updateNavUser();
    toast('Welcome back, ' + ME.username + '!', 'ok');
    showPage('feed');
  } catch (e) { toast(e.message, 'err'); }
}

async function doRegister() {
  try {
    const username = document.getElementById('reg-username').value.trim();
    const email    = document.getElementById('reg-email').value.trim();
    const password = document.getElementById('reg-password').value;
    const result   = await api('/api/auth/register', {method:'POST',body:JSON.stringify({username,email,password})});
    TOKEN = result.token;
    ME    = result.user;
    localStorage.setItem('borgol_token', TOKEN);
    closeModal('register-modal');
    updateNavUser();
    toast('Welcome to Borgol, ' + ME.username + '!', 'ok');
    showPage('feed');
  } catch (e) { toast(e.message, 'err'); }
}

function logout() {
  TOKEN = null;
  ME    = null;
  localStorage.removeItem('borgol_token');
  updateNavUser();
  showPage('recipes');
  toast('Logged out', 'info');
}

async function loadMe() {
  if (!TOKEN) return;
  try {
    ME = await api('/api/auth/me');
    updateNavUser();
  } catch (e) {
    TOKEN = null;
    localStorage.removeItem('borgol_token');
  }
}

function updateNavUser() {
  const authBtns  = document.getElementById('auth-buttons');
  const userMenu  = document.getElementById('user-menu');
  const mobileAuth = document.getElementById('mobile-auth-section');
  const mobileUser = document.getElementById('mobile-user-section');
  if (ME) {
    authBtns.style.display  = 'none';
    userMenu.style.display  = 'flex';
    mobileAuth.style.display = 'none';
    mobileUser.style.display = 'block';
    document.getElementById('notif-wrap').style.display = 'block';
    document.getElementById('nav-username').textContent = ME.username;
    // Show admin link based on isAdmin flag (fallback to id===1 if isAdmin not yet in API response)
    const isAdmin = ME.isAdmin ?? (ME.id === 1);
    const adminLink = document.getElementById('nav-admin-link');
    if (adminLink) adminLink.style.display = isAdmin ? 'inline-flex' : 'none';
    const mobileAdminLink = document.getElementById('mobile-admin-link');
    if (mobileAdminLink) mobileAdminLink.style.display = isAdmin ? 'block' : 'none';
    const avatarEl = document.getElementById('nav-avatar');
    if (ME.avatarUrl) {
      avatarEl.innerHTML = `<img src="${esc(ME.avatarUrl)}" alt="${ME.username[0].toUpperCase()}" onerror="this.parentElement.textContent='${ME.username[0].toUpperCase()}'"/>`;
    } else {
      avatarEl.textContent = ME.username[0].toUpperCase();
    }
    document.getElementById('btn-new-recipe').style.display = 'flex';
    document.getElementById('btn-new-cafe').style.display   = 'flex';
  } else {
    authBtns.style.display  = 'flex';
    userMenu.style.display  = 'none';
    mobileAuth.style.display = 'block';
    mobileUser.style.display = 'none';
    document.getElementById('btn-new-recipe').style.display = 'none';
    document.getElementById('btn-new-cafe').style.display   = 'none';
  }
}

// ── Profile ───────────────────────────────────────────────────────────────────
async function loadProfile(userId) {
  if (!userId) {
    document.getElementById('profile-header').innerHTML = `
      <div class="empty-state" style="width:100%">
        <div class="empty-icon">☕</div>
        <div class="empty-msg">Log in to view your profile</div>
        <button class="btn btn-primary" onclick="openModal('login-modal')">Log In</button>
      </div>`;
    document.getElementById('profile-tab-content').innerHTML = '';
    return;
  }
  viewingUserId = userId;
  currentProfileTab = 'recipes';
  // Reset tabs
  document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
  document.getElementById('tab-recipes').classList.add('active');

  document.getElementById('profile-header').innerHTML = '<div class="loading"><div class="spinner"></div></div>';
  document.getElementById('profile-tab-content').innerHTML = '<div class="loading"><div class="spinner"></div></div>';
  try {
    const [user, recipes, achievements] = await Promise.all([
      api(`/api/users/${userId}`),
      api(`/api/users/${userId}/recipes`),
      ME && ME.id === userId ? api('/api/achievements').catch(()=>[]) : Promise.resolve([])
    ]);
    const isMe        = ME && ME.id === userId;
    const isFollowing = user.isFollowing;

    document.getElementById('profile-header').innerHTML = `
      <div class="profile-avatar">
        ${user.avatarUrl
          ? `<img src="${esc(user.avatarUrl)}" alt="${esc(user.username)}" onerror="this.outerHTML='<span style=font-size:32px>${user.username[0].toUpperCase()}</span>'">`
          : user.username[0].toUpperCase()}
      </div>
      <div class="profile-info">
        <div class="profile-name">${esc(user.username)}</div>
        <span class="profile-level level-${user.expertiseLevel}">${cap(user.expertiseLevel)}</span>
        <div class="profile-bio">${esc(user.bio || 'No bio yet.')}</div>
        ${user.flavorPrefs?.length ? `<div class="flavor-tags" style="margin-bottom:12px">${user.flavorPrefs.map(f=>`<span class="flavor-tag">${f.toLowerCase()}</span>`).join('')}</div>` : ''}
        <div class="profile-stats">
          <div class="stat-item" onclick="profileTab('recipes',document.getElementById('tab-recipes'))">
            <div class="stat-num">${user.recipeCount}</div><div class="stat-lbl">Recipes</div>
          </div>
          <div class="stat-item" onclick="openUserListModal('followers',${userId})">
            <div class="stat-num">${user.followerCount}</div><div class="stat-lbl">Followers</div>
          </div>
          <div class="stat-item" onclick="openUserListModal('following',${userId})">
            <div class="stat-num">${user.followingCount}</div><div class="stat-lbl">Following</div>
          </div>
        </div>
      </div>
      <div style="display:flex;flex-direction:column;gap:8px;align-self:flex-start">
        ${isMe ? `
          <button class="btn btn-secondary" onclick="openEditProfile()">✏️ Edit Profile</button>
          <button class="btn btn-secondary btn-sm" onclick="logout()" style="color:var(--danger)">Log Out</button>
        ` : ME ? `
          <button class="btn ${isFollowing ? 'btn-secondary' : 'btn-primary'}" id="follow-btn" onclick="toggleFollow(${userId})">
            ${isFollowing ? '✓ Following' : '+ Follow'}
          </button>
        ` : ''}
      </div>`;

    // Badges section (only shown on own profile)
    if (achievements.length) {
      const earned = achievements.filter(a => a.earned);
      const badgeHtml = earned.length
        ? earned.map(a => `<div title="${esc(a.name)}: ${esc(a.description)}" style="display:flex;flex-direction:column;align-items:center;gap:3px;padding:8px 10px;background:var(--cream);border:1px solid var(--border);border-radius:10px;min-width:54px;cursor:default">
            <span style="font-size:22px">${a.icon}</span>
            <span style="font-size:10px;font-weight:700;color:var(--muted);text-align:center;line-height:1.2">${esc(a.name)}</span>
          </div>`).join('')
        : `<span style="font-size:13px;color:var(--muted)">No badges yet — keep brewing!</span>`;
      document.getElementById('profile-header').innerHTML += `
        <div style="padding:14px 0 0;border-top:1px solid var(--border);margin-top:14px;width:100%">
          <div style="font-size:11px;font-weight:800;color:var(--muted);text-transform:uppercase;letter-spacing:.5px;margin-bottom:10px">🏅 Achievements</div>
          <div style="display:flex;flex-wrap:wrap;gap:8px">${badgeHtml}</div>
        </div>`;
    }

    document.getElementById('profile-tab-content').innerHTML =
      recipes.length ? recipes.map(r => renderRecipeCard(r)).join('') : emptyState('📖', 'No recipes yet');
  } catch (e) {
    document.getElementById('profile-header').innerHTML = `<p style="color:var(--danger)">${esc(e.message)}</p>`;
  }
}

async function profileTab(name, btn) {
  currentProfileTab = name;
  document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
  btn.classList.add('active');
  const el = document.getElementById('profile-tab-content');
  el.innerHTML = '<div class="loading"><div class="spinner"></div></div>';
  el.className = name === 'following' || name === 'followers'
    ? '' : 'recipe-grid';
  try {
    if (name === 'recipes') {
      const items = await api(`/api/users/${viewingUserId}/recipes`);
      el.innerHTML = items.length ? items.map(r => renderRecipeCard(r)).join('') : emptyState('📖','No recipes yet');
    } else if (name === 'liked') {
      const items = await api(`/api/users/${viewingUserId}/liked`);
      el.innerHTML = items.length ? items.map(r => renderRecipeCard(r)).join('') : emptyState('❤️','No liked recipes yet');
    } else if (name === 'following') {
      const items = await api(`/api/users/${viewingUserId}/following`);
      el.innerHTML = items.length
        ? `<div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(240px,1fr));gap:12px">${items.map(u => renderUserCard(u)).join('')}</div>`
        : emptyState('👥','Not following anyone yet');
    } else if (name === 'followers') {
      const items = await api(`/api/users/${viewingUserId}/followers`);
      el.innerHTML = items.length
        ? `<div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(240px,1fr));gap:12px">${items.map(u => renderUserCard(u)).join('')}</div>`
        : emptyState('👥','No followers yet');
    } else if (name === 'equipment') {
      el.className = '';
      el.innerHTML = await loadEquipment();
    }
  } catch (e) { el.innerHTML = emptyState('⚠️', e.message); }
}

async function openUserListModal(type, userId) {
  const title = type === 'followers' ? 'Followers' : 'Following';
  document.getElementById('user-list-title').textContent = title;
  document.getElementById('user-list-content').innerHTML = '<div class="loading"><div class="spinner"></div></div>';
  openModal('user-list-modal');
  try {
    const users = await api(`/api/users/${userId}/${type}`);
    document.getElementById('user-list-content').innerHTML = users.length
      ? users.map(u => `
          <div class="user-list-item" onclick="closeModal('user-list-modal');showPage('profile',${u.id})">
            ${avatarHtml(u.username, u.avatarUrl, 40)}
            <div>
              <div class="user-list-name">${esc(u.username)}</div>
              <div class="user-list-meta">${u.recipeCount} recipes · ${u.followerCount} followers</div>
            </div>
          </div>`).join('')
      : `<p style="color:var(--muted);font-size:14px;text-align:center;padding:20px 0">No ${title.toLowerCase()} yet.</p>`;
  } catch (e) { document.getElementById('user-list-content').innerHTML = `<p style="color:var(--danger)">${esc(e.message)}</p>`; }
}

async function toggleFollow(userId) {
  if (!ME) { openModal('login-modal'); return; }
  const btn = document.getElementById('follow-btn');
  const isFollowing = btn.textContent.trim().startsWith('✓');
  try {
    if (isFollowing) {
      await api(`/api/users/${userId}/follow`, {method:'DELETE'});
      btn.textContent = '+ Follow';
      btn.className   = 'btn btn-primary';
      toast('Unfollowed', 'info');
    } else {
      await api(`/api/users/${userId}/follow`, {method:'POST'});
      btn.textContent = '✓ Following';
      btn.className   = 'btn btn-secondary';
      toast('Following!', 'ok');
    }
    loadProfile(userId);
  } catch (e) { toast(e.message, 'err'); }
}

function openEditProfile() {
  if (!ME) return;
  document.getElementById('p-bio').value      = ME.bio || '';
  document.getElementById('p-level').value    = ME.expertiseLevel || 'BEGINNER';
  document.getElementById('p-avatarurl').value = ME.avatarUrl || '';
  buildFlavorPicker('p-flavor-picker', ME.flavorPrefs || []);
  openModal('edit-profile-modal');
}

async function saveProfile() {
  const avatarUrl = document.getElementById('p-avatarurl').value.trim();
  const body = {
    bio:           document.getElementById('p-bio').value.trim(),
    expertiseLevel:document.getElementById('p-level').value,
    flavorPrefs:   getSelectedFlavors('p-flavor-picker'),
    avatarUrl:     avatarUrl
  };
  try {
    ME = await api('/api/users/me', {method:'PUT', body:JSON.stringify(body)});
    closeModal('edit-profile-modal');
    toast('Profile updated!', 'ok');
    updateNavUser();
    loadProfile(ME.id);
  } catch (e) { toast(e.message, 'err'); }
}

// ── Notifications ──────────────────────────────────────────────────────────────
let _notifOpen = false;
async function toggleNotifPanel() {
  _notifOpen = !_notifOpen;
  document.getElementById('notif-panel').classList.toggle('open', _notifOpen);
  if (_notifOpen) await loadNotifications();
}
document.addEventListener('click', e => {
  if (_notifOpen && !document.getElementById('notif-wrap').contains(e.target)) {
    _notifOpen = false;
    document.getElementById('notif-panel').classList.remove('open');
  }
});
async function loadNotifications() {
  const list = document.getElementById('notif-list');
  try {
    const notifs = await api('/api/notifications');
    if (!notifs.length) { list.innerHTML = '<div class="notif-empty">No notifications yet</div>'; return; }
    list.innerHTML = notifs.map(n => `
      <div class="notif-item${n.isRead?'':' unread'}" onclick="onNotifClick(${n.contentId},'${n.type}')">
        <div class="notif-avatar">${n.fromAvatar ? `<img src="${esc(n.fromAvatar)}"/>` : (n.fromUsername||'?')[0].toUpperCase()}</div>
        <div class="notif-msg">
          <div class="notif-text"><strong>${esc(n.fromUsername||'Someone')}</strong> ${esc(n.message)}</div>
          <div class="notif-time">${timeAgo(n.createdAt)}</div>
        </div>
        ${n.isRead?'':'<div class="notif-dot"></div>'}
      </div>`).join('');
  } catch(e) { list.innerHTML = '<div class="notif-empty">Failed to load</div>'; }
}
async function markNotifsRead() {
  try {
    await api('/api/notifications/read', {method:'POST'});
    document.getElementById('notif-badge').style.display = 'none';
    document.querySelectorAll('.notif-item.unread').forEach(el => el.classList.remove('unread'));
    document.querySelectorAll('.notif-dot').forEach(el => el.remove());
  } catch(e) {}
}
function onNotifClick(contentId, type) {
  _notifOpen = false;
  document.getElementById('notif-panel').classList.remove('open');
  if (type === 'follow') showPage('explore');
  else if (contentId) openRecipeDetail(contentId);
}
async function pollNotifCount() {
  if (!ME) return;
  try {
    const {unread} = await api('/api/notifications/count');
    const badge = document.getElementById('notif-badge');
    if (unread > 0) {
      badge.textContent = unread > 99 ? '99+' : unread;
      badge.style.display = 'flex';
    } else {
      badge.style.display = 'none';
    }
  } catch(e) {}
}

