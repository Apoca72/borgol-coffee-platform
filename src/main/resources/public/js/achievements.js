// src/main/resources/public/js/achievements.js
// Achievement badges.

// ── Achievements ──────────────────────────────────────────────────────────────

async function checkAchievements() {
  if (!ME) return;
  try {
    const newBadges = await api('/api/achievements/check', {method:'POST', body:'{}'});
    if (!newBadges || !newBadges.length) return;
    for (const badgeId of newBadges) {
      await new Promise(r => setTimeout(r, 400));
      toast('🏅 Achievement unlocked!', 'ok');
    }
  } catch(e) { /* silent — achievements are non-critical */ }
}

