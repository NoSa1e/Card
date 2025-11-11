const base = import.meta.env.VITE_API_BASE ?? '/api';   // 개발에선 '/api'
export async function jget(p){ const r = await fetch(base + p); return r.json(); }
export async function jpost(p, opts={}) {
  const r = await fetch(base + p, { method:'POST', ...opts });
  return r.json();
}
