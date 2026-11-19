# Deployment guide — Vercel + Render + Neon

This deployment uses three services:

- **Frontend:** React + Vite on Vercel
- **Backend:** Spring Boot 4.1.2 / Java 17 on Render as a Docker web service
- **Database:** PostgreSQL on Neon

The Vercel frontend uses `VITE_API_BASE_URL` to call the backend directly. The Spring Boot backend uses `CORS_ALLOWED_ORIGINS` to allow the exact Vercel production origin. The security-hardening release adds one backend-only `DEMO_ADMIN_PASSWORD` secret; it does not require a new Vercel variable or a new frontend domain.

## 1. Apply this deployment patch

Extract this patch into the repository root. It adds or replaces:

- `frontend/vercel.json` — Vercel SPA fallback for React client-side routes
- `backend/Dockerfile` and `backend/.dockerignore`
- `backend/src/main/resources/application.yaml` — environment-driven DB, port, connection pool and CORS settings
- `backend/src/main/java/com/schoolmanagement/security/config/SecurityConfig.java` — CORS support, preserving JWT and 401/403 handlers
- `.env.example` — local placeholders including CORS settings
- `README.md` — deployment architecture section

After applying the patch, verify locally from PowerShell:

```powershell
Set-Location "C:\Users\HP\workspace\school-management-system\backend"
.\mvnw.cmd clean compile

Set-Location ..\frontend
npm run build
```

Do not overwrite the real root `.env` file. The patch contains `.env.example`, not `.env`.

Then use GitHub Desktop to review, commit, and push the deployment changes to `main`.

## 2. Create the Neon PostgreSQL database

1. Open https://neon.tech/ and create/sign into an account.
2. Create a PostgreSQL project, for example `algions-school-management`.
3. Choose a region reasonably close to the Render backend region.
4. Open the project's **Connect** dialog and select the database and role.
5. Select the **direct** (non-pooler) connection option for the migration-capable backend connection.
6. Copy the connection details privately. Do not put them in Git or this chat.

Build these Render variables from the direct Neon connection details:

```text
DB_URL=jdbc:postgresql://DIRECT_HOST:5432/DB_NAME?sslmode=require
DB_USERNAME=NEON_ROLE
DB_PASSWORD=NEON_PASSWORD
```

Use the actual direct hostname, database, role and password from Neon. Retain `sslmode=require`. Do not use the `-pooler` hostname for the initial Flyway migration connection.

The Neon Free plan currently includes 1 GB PostgreSQL storage and 100 compute-hours per project per month. It can scale to zero when idle; the first request after inactivity may take longer. Verify current usage and plan limits in your Neon console before relying on it.

## 3. Deploy the Spring Boot backend on Render

1. Open https://dashboard.render.com/.
2. Choose **New → Web Service**.
3. Connect GitHub and select `Md-Mohseen-Ali/school-management-system`.
4. Select branch `main`.
5. Set **Root Directory** to `backend`.
6. Set the runtime/language to **Docker**.
7. Set Dockerfile path to `./Dockerfile` (and context to `.` if the form asks for a Docker context).
8. Choose the Free plan for the assessment demo, subject to Render's free-tier limitations.
9. Set Health Check Path to `/actuator/health` if available.
10. Add these environment variables in Render's Environment settings:

| Key | Value |
|---|---|
| `DB_URL` | JDBC URL using Neon's direct host and `sslmode=require` |
| `DB_USERNAME` | Neon database role |
| `DB_PASSWORD` | Neon database password |
| `JWT_SECRET` | Fresh Base64-encoded random 32-byte secret |
| `CORS_ALLOWED_ORIGINS` | Temporary value `http://localhost:5173`; replace after Vercel deploy with the exact Vercel production origin |
| `DEMO_ADMIN_PASSWORD` | A unique private password (16–72 UTF-8 bytes) for `admin@school.demo`; store only in Render's secret environment settings |

Render supplies `PORT`; the app uses `${PORT:8080}`. Do not hard-code the Render port.

Generate a fresh JWT secret locally in PowerShell and paste it directly into Render's environment settings. Do not commit or share it:

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

11. Create the Web Service and inspect deploy logs. Spring Boot must connect to Neon and Flyway must apply migrations V1–V8.
12. Copy the backend service origin, for example `https://YOUR-API-NAME.onrender.com` (origin only, no trailing slash).
13. Check `https://YOUR-API-NAME.onrender.com/actuator/health` returns `{"status":"UP"}`. An initial 502/503 can occur while a free service starts; inspect Render logs before changing code.

## 4. Deploy the React frontend on Vercel

1. Open https://vercel.com/ and sign in with GitHub.
2. Select **Add New → Project** and import `Md-Mohseen-Ali/school-management-system`.
3. In project configuration, set **Root Directory** to `frontend` and confirm it uses that directory.
4. Framework Preset: **Vite**.
5. Build Command: `npm run build`.
6. Output Directory: `dist`.
7. Install Command: `npm install` (or leave Vercel's detected default).
8. Add this Environment Variable before deploying:

```text
VITE_API_BASE_URL=https://YOUR-API-NAME.onrender.com
```

Use the actual Render backend origin, with no trailing slash. It is a public API URL, not a secret. Do not put DB credentials, JWT signing secrets or `DEMO_ADMIN_PASSWORD` in Vercel.

9. Deploy. Vercel builds the React app from `frontend` and serves `dist`.
10. Copy the Vercel production domain, for example `https://YOUR-PROJECT.vercel.app`.

The `frontend/vercel.json` file rewrites unmatched client-side routes to `index.html`, which is needed for Vite single-page application deep links. The security-hardening update does not change the Vercel project, domain assignment, root directory or `VITE_API_BASE_URL`; keep the already-submitted production URL attached to the same Vercel project. Vercel's Vite documentation describes this SPA fallback behavior.

**Important:** Vite embeds `VITE_API_BASE_URL` into the frontend at build time. If it changes, update the Vercel environment variable and redeploy the frontend.

## 5. Set the exact production CORS origin

After Vercel creates the production domain:

1. Open the backend Web Service in Render.
2. Open **Environment**.
3. Set `CORS_ALLOWED_ORIGINS` to the exact Vercel production origin, without a trailing slash:

```text
CORS_ALLOWED_ORIGINS=https://YOUR-PROJECT.vercel.app
```

4. Save and redeploy the backend.

The backend CORS configuration allows `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`, and the `Authorization`, `Content-Type`, `Accept`, and `X-Request-ID` headers. Credentials/cookies are disabled; the frontend uses Bearer JWT authentication.

If you later add a custom Vercel domain, include that exact origin too. Vercel preview deployment domains are different origins; they will not be allowed unless explicitly added to the backend CORS list.

## 6. Verify the deployed application

1. Open the Vercel production URL in a private/incognito browser window.
2. Sign in with the assessment administrator account:
   - Email: `admin@school.demo`
   - Password: the private value configured in Render as `DEMO_ADMIN_PASSWORD` (share it with evaluators only through a private channel). The fixed password from the original seed is disabled by migration V8.
3. Check Dashboard, Students, Sections, Subjects, Enrollments, Attendance, Examinations, Fees and Audit.
4. If the first backend request takes time, wait for Render to wake and retry once.
5. If the browser reports a CORS error, verify `CORS_ALLOWED_ORIGINS` exactly matches the Vercel production origin (scheme + hostname, no path and no trailing slash), then confirm the backend redeployed.
6. If a login or data request fails, inspect Render backend logs and Vercel deployment/build logs. Do not put credentials into browser code or screenshots.

## 7. Free-tier limitations

- Render Free web services sleep after 15 minutes without inbound traffic and may take about a minute to wake. Their local filesystem is ephemeral; persistent records must stay in PostgreSQL.
- Neon Free currently includes 1 GB storage and 100 compute-hours per project per month. Its compute can scale to zero while idle.
- Free plans, quotas and product features can change. Check current provider dashboards and official documentation.
- This configuration is appropriate for an assessment/demo, not for production school records or sensitive student information.

## Official documentation

- Vercel Vite deployment: https://vercel.com/docs/frameworks/frontend/vite
- Vercel SPA rewrites: https://vercel.com/docs/frameworks/frontend/vite#using-vite-to-make-spas
- Vercel environment variables: https://vercel.com/docs/environment-variables
- Render Docker services: https://render.com/docs/docker
- Render web services and port binding: https://render.com/docs/web-services
- Render environment variables: https://render.com/docs/configure-environment-variables
- Render free limitations: https://render.com/docs/free
- Neon current Free plan: https://neon.com/blog/neon-free-plan-1-gb-per-project
- Neon connection security: https://neon.com/blog/oops-proof-your-vibe-code-with-neon-because-mistakes-happen
