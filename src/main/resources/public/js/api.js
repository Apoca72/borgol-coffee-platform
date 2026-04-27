// src/main/resources/public/js/api.js
// API utilities — loaded first, used by all other JS files.

// ── API helper ────────────────────────────────────────────────────────────────
async function api(url, opts={}) {
  const headers = {'Content-Type':'application/json'};
  if (TOKEN) headers['Authorization'] = 'Bearer ' + TOKEN;
  const res = await fetch(url, { ...opts, headers: {...headers, ...opts.headers} });
  const data = await res.json().catch(() => null);
  if (!res.ok) throw new Error(data?.error || res.statusText || 'Request failed');
  return data;
}

