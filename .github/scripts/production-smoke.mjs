const base = (process.env.API_BASE_URL || "https://school-management-system-z7gg.onrender.com").replace(/\/$/, "");
const frontendOrigin = "https://algions-school-management.vercel.app";
const results = [];
let failures = 0;

function record(name, ok, detail = "") {
  results.push({ name, ok, detail });
  if (!ok) failures++;
  process.stdout.write(`${ok ? "PASS" : "FAIL"} | ${name}${detail ? " | " + detail : ""}\n`);
}

async function call(path, options = {}) {
  return fetch(base + path, {
    ...options,
    signal: AbortSignal.timeout(20000),
    headers: { ...(options.headers || {}) }
  });
}

async function readJson(response) {
  try { return await response.json(); } catch { return null; }
}
async function collectJsBundle(firstUrl, firstText) {
  const chunks = [firstText];
  const seen = new Set([firstUrl]);
  const queue = [];
  const enqueue = text => {
    for (const match of text.matchAll(/["']([^"']+\\.js(?:\\?[^"']*)?)["']/g)) {
      queue.push(match[1]);
    }
  };
  enqueue(firstText);
  while (queue.length > 0 && seen.size < 30) {
    const ref = queue.shift();
    let url;
    try { url = new URL(ref, firstUrl).toString(); } catch { continue; }
    if (seen.has(url)) continue;
    seen.add(url);
    try {
      const response = await fetch(url, { signal: AbortSignal.timeout(12000) });
      if (!response.ok) continue;
      const text = await response.text();
      chunks.push(text);
      enqueue(text);
    } catch {}
  }
  return chunks.join("\\n");
}

async function login(email, password, roleLabel) {
  const response = await call("/api/v1/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json", "Accept": "application/json" },
    body: JSON.stringify({ email, password })
  });
  const payload = await readJson(response);
  const token = payload?.accessToken;
  record(roleLabel + " demo login", response.status === 200 && typeof token === "string" && token.length > 20,
    "HTTP " + response.status);
  return response.status === 200 && typeof token === "string" ? token : null;
}

async function expectedStatus(name, path, token, expected) {
  const response = await call(path, {
    headers: { "Authorization": "Bearer " + token, "Accept": "application/json" }
  });
  record(name, response.status === expected, "expected HTTP " + expected + ", got " + response.status);
  return { response, payload: await readJson(response) };
}

try {
  const frontend = await fetch(frontendOrigin + "/", { signal: AbortSignal.timeout(20000) });
  const frontendHtml = await frontend.text();
  const scriptTags = frontendHtml.split("<script").slice(1).map(part => "<script" + part.slice(0, part.indexOf(">") + 1));
  const scriptPaths = scriptTags.flatMap(tag => {
    const marker = tag.indexOf("src=");
    if (marker < 0) return [];
    const tail = tag.slice(marker + 4).trimStart();
    const quote = tail[0];
    const endQuote = tail.indexOf(quote, 1);
    return (quote === "'" || quote === '"') && endQuote > 0 ? [tail.slice(1, endQuote)] : [];
  });
  const hasRoot = frontendHtml.includes('id="root"') || frontendHtml.includes("id='root'");
  record("Vercel frontend page", frontend.status === 200 && hasRoot && scriptPaths.length > 0,
    "HTTP " + frontend.status + ", content-type " + (frontend.headers.get("content-type") || "unknown") +
    ", app root " + (hasRoot ? "found" : "missing") + ", script tags " + scriptTags.length +
    (frontend.status === 200 && hasRoot && scriptPaths.length > 0 ? "" :
      ", response preview: " + frontendHtml.slice(0, 420).replace(/\${ch}+/g, " ")));
  if (frontend.status === 200 && scriptPaths.length > 0) {
    const assetUrl = new URL(scriptPaths[0], frontendOrigin).toString();
    const asset = await fetch(assetUrl, { signal: AbortSignal.timeout(20000) });
    const assetText = await asset.text();
    const productionAssetSet = await collectJsBundle(assetUrl, assetText);
    record("Vercel production bundle loads", asset.status === 200, "HTTP " + asset.status);
    record("Frontend bundle targets deployed Render API", asset.status === 200 && assetText.includes(base),
      asset.status === 200 && assetText.includes(base) ? "API base URL found" : "API base URL not found in first JS bundle");
    record("Vercel bundle includes Access requests portal", asset.status === 200 && productionAssetSet.includes("Access requests"),
      asset.status === 200 && productionAssetSet.includes("Access requests") ? "Admin UI marker found" : "Admin UI marker missing");
    record("Vercel bundle includes My academics student workspace", asset.status === 200 && productionAssetSet.includes("My academics"),
      asset.status === 200 && productionAssetSet.includes("My academics") ? "Student UI marker found" : "Student UI marker missing");
    record("Vercel bundle includes public account request screen", asset.status === 200 && productionAssetSet.includes("Request an account"),
      asset.status === 200 && productionAssetSet.includes("Request an account") ? "Public request UI marker found" : "Public request UI marker missing");
    const previewOrigin = "https://algions-school-management-git-f-60837a-md-mohseen-alis-projects.vercel.app";
    const preview = await fetch(previewOrigin + "/", { signal: AbortSignal.timeout(20000) });
    const previewHtml = await preview.text();
    const previewScriptTag = previewHtml.split("<script").slice(1).map(part => "<script" + part.slice(0, part.indexOf(">") + 1)).find(tag => tag.includes("src="));
    const previewScriptMatch = previewScriptTag?.match(/src=["']([^"']+)["']/);
    record("Vercel feature preview page", preview.status === 200 && Boolean(previewScriptMatch),
      "HTTP " + preview.status);
    if (preview.status === 200 && previewScriptMatch) {
      const previewAsset = await fetch(new URL(previewScriptMatch[1], previewOrigin), { signal: AbortSignal.timeout(20000) });
      const previewText = await previewAsset.text();
      const previewAssetSet = await collectJsBundle(new URL(previewScriptMatch[1], previewOrigin).toString(), previewText);
      record("Vercel feature preview includes Access requests UI", previewAsset.status === 200 && previewAssetSet.includes("Access requests"),
        "HTTP " + previewAsset.status + (previewAssetSet.includes("Access requests") ? ", marker found" : ", marker missing"));
      record("Vercel feature preview includes My academics UI", previewAsset.status === 200 && previewAssetSet.includes("My academics"),
        "HTTP " + previewAsset.status + (previewAssetSet.includes("My academics") ? ", marker found" : ", marker missing"));
      record("Vercel feature preview includes public account request UI", previewAsset.status === 200 && previewAssetSet.includes("Request an account"),
        "HTTP " + previewAsset.status + (previewAssetSet.includes("Request an account") ? ", marker found" : ", marker missing"));
    } else {
      record("Vercel feature preview includes Access requests UI", false, "preview bundle not accessible");
      record("Vercel feature preview includes My academics UI", false, "preview bundle not accessible");
      record("Vercel feature preview includes public account request UI", false, "preview bundle not accessible");
    }
  } else {
    record("Vercel production bundle loads", false, "could not locate frontend JS bundle");
    record("Frontend bundle targets deployed Render API", false, "could not inspect frontend JS bundle");
  }

  const health = await call("/actuator/health");
  const healthJson = await readJson(health);
  record("Render actuator health", health.status === 200 && healthJson?.status === "UP",
    "HTTP " + health.status + ", status " + (healthJson?.status ?? "unavailable"));

  const cors = await call("/api/v1/auth/login", {
    method: "OPTIONS",
    headers: {
      "Origin": frontendOrigin,
      "Access-Control-Request-Method": "POST",
      "Access-Control-Request-Headers": "authorization,content-type"
    }
  });
  const allowOrigin = cors.headers.get("access-control-allow-origin");
  const allowMethods = (cors.headers.get("access-control-allow-methods") || "").toUpperCase();
  const allowHeaders = (cors.headers.get("access-control-allow-headers") || "").toLowerCase();
  record("Vercel CORS preflight", cors.status >= 200 && cors.status < 300 &&
    allowOrigin === frontendOrigin && allowMethods.includes("POST") &&
    allowHeaders.includes("authorization") && allowHeaders.includes("content-type"),
    "HTTP " + cors.status + ", exact origin " + (allowOrigin === frontendOrigin ? "matched" : "not matched"));

  const unauth = await call("/api/v1/auth/me");
  record("Unauthenticated protected endpoint is rejected", unauth.status === 401,
    "expected HTTP 401, got " + unauth.status);

  const invalid = await call("/api/v1/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json", "Accept": "application/json" },
    body: JSON.stringify({ email: "smoke-test-invalid@example.invalid", password: "DefinitelyWrong!2026" })
  });
  record("Invalid credentials are rejected", invalid.status === 401,
    "expected HTTP 401, got " + invalid.status);

  const admin = await login("admin@school.demo", "Admin@2026School", "Admin");
  const teacher = await login("teacher@school.demo", "Teacher@2026School", "Teacher");
  const guardian1 = await login("guardian1@school.demo", "Guardian@2026School", "Guardian 1");
  const guardian2 = await login("guardian2@school.demo", "Guardian2@2026School", "Guardian 2");
  const student = await login("student1@school.demo", "Student@2026School", "Student");

  if (admin) {
    const me = await expectedStatus("Admin identity endpoint", "/api/v1/auth/me", admin, 200);
    const roles = me.payload?.roles || [];
    record("Admin JWT resolves ADMIN role", Array.isArray(roles) && roles.includes("ROLE_ADMIN"),
      Array.isArray(roles) && roles.includes("ROLE_ADMIN") ? "role matched" : "role not found");
    await expectedStatus("Admin can read student directory", "/api/v1/academic/students?page=0&size=1", admin, 200);
    await expectedStatus("Admin can read account access request queue (V7 migration)", "/api/v1/account-requests?status=PENDING", admin, 200);
    await expectedStatus("Admin is blocked from teacher roster", "/api/v1/teacher/roster/sections", admin, 403);
    await expectedStatus("Admin is blocked from guardian student endpoint", "/api/v1/guardian/students", admin, 403);
    await expectedStatus("Admin is blocked from student results endpoint", "/api/v1/student/examinations", admin, 403);
  }

  if (teacher) {
    const me = await expectedStatus("Teacher identity endpoint", "/api/v1/auth/me", teacher, 200);
    const roles = me.payload?.roles || [];
    record("Teacher JWT resolves TEACHER role", Array.isArray(roles) && roles.includes("ROLE_TEACHER"),
      Array.isArray(roles) && roles.includes("ROLE_TEACHER") ? "role matched" : "role not found");
    await expectedStatus("Teacher can read assigned sections", "/api/v1/teacher/roster/sections", teacher, 200);
    await expectedStatus("Teacher can read assigned examinations", "/api/v1/teacher/examinations", teacher, 200);
    await expectedStatus("Teacher is blocked from admin student directory", "/api/v1/academic/students?page=0&size=1", teacher, 403);
    await expectedStatus("Teacher is blocked from guardian students", "/api/v1/guardian/students", teacher, 403);
  }

  let guardian1Students = null;
  if (guardian1) {
    const me = await expectedStatus("Guardian 1 identity endpoint", "/api/v1/auth/me", guardian1, 200);
    const roles = me.payload?.roles || [];
    record("Guardian JWT resolves GUARDIAN role", Array.isArray(roles) && roles.includes("ROLE_GUARDIAN"),
      Array.isArray(roles) && roles.includes("ROLE_GUARDIAN") ? "role matched" : "role not found");
    const students = await expectedStatus("Guardian can read linked students", "/api/v1/guardian/students", guardian1, 200);
    guardian1Students = Array.isArray(students.payload) ? students.payload : [];
    await expectedStatus("Guardian can read published examination list", "/api/v1/guardian/examinations", guardian1, 200);
    await expectedStatus("Guardian can read fee invoices", "/api/v1/fees/invoices?page=0&size=1", guardian1, 200);
    await expectedStatus("Guardian is blocked from admin student directory", "/api/v1/academic/students?page=0&size=1", guardian1, 403);
    await expectedStatus("Guardian is blocked from teacher roster", "/api/v1/teacher/roster/sections", guardian1, 403);
    await expectedStatus("Guardian is blocked from student self-service", "/api/v1/student/examinations", guardian1, 403);
  }

  if (guardian2 && guardian1 && guardian1Students) {
    const other = await expectedStatus("Guardian 2 can read own linked students", "/api/v1/guardian/students", guardian2, 200);
    const otherStudents = Array.isArray(other.payload) ? other.payload : [];
    const ownIds = new Set(guardian1Students.map(s => String(s.id)));
    const otherOnly = otherStudents.find(s => s?.id != null && !ownIds.has(String(s.id)));
    if (otherOnly) {
      const path = "/api/v1/guardian/examinations/999999/students/" + encodeURIComponent(otherOnly.id) + "/report-card";
      await expectedStatus("Guardian cannot access another guardian's student report", path, guardian1, 404);
    } else {
      record("Guardian-to-guardian student isolation", false,
        "could not identify a distinct seeded student pair to check");
    }
  }

  if (student) {
    const me = await expectedStatus("Student identity endpoint", "/api/v1/auth/me", student, 200);
    const roles = me.payload?.roles || [];
    record("Student JWT resolves STUDENT role", Array.isArray(roles) && roles.includes("ROLE_STUDENT"),
      Array.isArray(roles) && roles.includes("ROLE_STUDENT") ? "role matched" : "role not found");
    await expectedStatus("Student can read own published examination list", "/api/v1/student/examinations", student, 200);
    await expectedStatus("Student can read own academic workspace", "/api/v1/student/academics", student, 200);
    await expectedStatus("Student is blocked from admin student directory", "/api/v1/academic/students?page=0&size=1", student, 403);
    await expectedStatus("Student is blocked from guardian student endpoint", "/api/v1/guardian/students", student, 403);
    await expectedStatus("Student is blocked from teacher roster", "/api/v1/teacher/roster/sections", student, 403);
  }
} catch (error) {
  record("Smoke test runner exception", false, String(error?.message || error));
}

process.stdout.write(`\nRESULT: ${results.filter(r => r.ok).length} passed, ${failures} failed, ${results.length} checks\n`);
if (failures > 0) process.exitCode = 1;
