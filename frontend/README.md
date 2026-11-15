ALGIONS School Management System — Frontend package

Requirements:
- Node.js 20.19+ or 22.12+ recommended (Node 24 is fine)
- Backend running at http://localhost:8080

Setup on Windows PowerShell:
1. Extract this frontend folder into the project root so the structure is:
   school-management-system\frontend\package.json
   school-management-system\frontend\src\...
2. Open PowerShell in the frontend folder:
   cd C:\Users\HP\workspace\school-management-system\frontend
3. Install dependencies:
   npm install
4. Start the UI:
   npm run dev
5. Open:
   http://localhost:5173

Vite proxies /api and /actuator calls to http://localhost:8080 in local development.
The frontend stores the JWT in sessionStorage and clears it when signing out.

For production, configure VITE_API_BASE_URL with the backend origin before building.
Run `npm run build` to create the production bundle in frontend\dist.

Notes:
- This is a functional API-connected MVP shell. It displays data returned by the existing backend endpoints.
- The API Action panel is provided for POST/PUT/PATCH/DELETE operations where the caller knows the existing API contract.
- Role-based navigation is a usability feature only. Backend authorization remains the security boundary.
- Review the login response token property and /api/v1/auth/me role payload against your actual backend. The token extractor supports common accessToken/access_token/token/jwt response shapes.
- Demo accounts are the seeded test accounts in the assessment environment. Replace or remove them before using real data.
