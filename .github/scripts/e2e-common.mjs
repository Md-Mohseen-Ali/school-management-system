export const BASE = (process.env.API_BASE_URL || "http://localhost:8080").replace(/\/$/, "");
export const FRONTEND = (process.env.FRONTEND_URL || "http://localhost:5173").replace(/\/$/, "");
export const results = [];
export let failures = 0;

export function check(name, ok, detail = "") {
  results.push({name, ok: Boolean(ok), detail});
  if (!ok) failures++;
  process.stdout.write((ok ? "PASS" : "FAIL") + " | " + name + (detail ? " | " + detail : "") + "\n");
}
export function need(condition, message) {
  if (!condition) throw new Error(message);
}
export function rows(value) {
  if (Array.isArray(value)) return value;
  if (!value || typeof value !== "object") return [];
  for (const key of ["content","items","results","records"]) if (Array.isArray(value[key])) return value[key];
  return value.data ? rows(value.data) : [];
}
export async function call(path, options = {}) {
  const response = await fetch(BASE + path, {
    ...options, signal: AbortSignal.timeout(25000),
    headers: {Accept:"application/json", ...(options.headers || {})}
  });
  const raw = await response.text();
  let data = null;
  try { data = raw ? JSON.parse(raw) : null; } catch { data = raw; }
  return {status:response.status, data, raw, headers:response.headers};
}
export async function api(token, path, method = "GET", body, extraHeaders = {}) {
  const headers = {...extraHeaders};
  if (token) headers.Authorization = "Bearer " + token;
  const options = {method, headers};
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
    options.body = JSON.stringify(body);
  }
  return call(path, options);
}
export async function expect(name, response, status) {
  const ok = response.status === status;
  check(name, ok, "expected HTTP " + status + ", got " + response.status);
  need(ok, name + " failed");
  return response.data;
}
export async function login(email, password, role) {
  const payload = await expect(role + " login", await api(null,"/api/v1/auth/login","POST",{email,password}), 200);
  need(payload && typeof payload.accessToken === "string", role + " login had no access token");
  return payload.accessToken;
}
export function indiaDate() {
  const p = new Intl.DateTimeFormat("en-GB",{timeZone:"Asia/Kolkata",year:"numeric",month:"2-digit",day:"2-digit"}).formatToParts(new Date());
  const v = Object.fromEntries(p.map(x=>[x.type,x.value]));
  return v.year+"-"+v.month+"-"+v.day;
}
export function report() {
  const passed = results.filter(x=>x.ok).length;
  process.stdout.write("\nRESULT: "+passed+" passed, "+failures+" failed, "+results.length+" checks\n");
  if (failures) process.exitCode = 1;
}
