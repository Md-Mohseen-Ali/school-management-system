const assert = require("node:assert/strict");
const { chromium } = require(process.env.RUNNER_TEMP + "/pw/node_modules/playwright");
const origin = process.env.FRONTEND_URL || "http://localhost:5173";
const errors = [];

async function signIn(browser, email, password, role) {
  const context = await browser.newContext();
  const page = await context.newPage();
  page.on("pageerror", error => errors.push(role + ": " + error.message));
  await page.goto(origin, {waitUntil:"domcontentloaded",timeout:30000});
  await page.locator('input[type="email"]').fill(email);
  await page.locator('input[type="password"]').fill(password);
  await page.getByRole("button",{name:/Sign in/}).click();
  await page.locator(".sidebar").waitFor({state:"visible",timeout:30000});
  await page.getByText(email,{exact:true}).waitFor({state:"visible",timeout:15000});
  process.stdout.write("PASS | " + role + " signs in through the browser UI\n");
  return {context,page};
}
async function navigate(page,label) {
  const button = page.locator(".nav-item").filter({hasText:label});
  assert.equal(await button.count(),1,"Expected one navigation item: "+label);
  await button.click();
  await page.getByRole("heading",{level:1,name:label}).waitFor({state:"visible",timeout:15000});
  process.stdout.write("PASS | UI opens " + label + " workspace\n");
}
async function hasNoUiError(page,label) {
  const count = await page.locator(".alert.error").count();
  assert.equal(count,0,label+" shows an error alert");
}
(async()=>{
  const browser = await chromium.launch({headless:true});
  try {
    const admin = await signIn(browser,"admin@school.demo","Admin@2026School","Admin");
    const adminNav = await admin.page.locator(".nav-item").allTextContents();
    for (const item of ["Students","Classes","Sections","Subjects","Enrollments","Teacher assignments","Family links","Attendance","Examinations","Fee invoices","Audit trail"]) {
      assert(adminNav.some(x=>x.includes(item)),"Admin navigation missing "+item);
    }
    for (const label of ["Students","Classes","Sections","Subjects","Enrollments","Teacher assignments","Family links"]) {
      await navigate(admin.page,label);
      await hasNoUiError(admin.page,label);
    }
    await navigate(admin.page,"Attendance");
    await admin.page.locator(".ops-search input").fill("E2E");
    await admin.page.locator(".ops-session-card").first().waitFor({state:"visible",timeout:15000});
    await hasNoUiError(admin.page,"Admin attendance workspace");
    await navigate(admin.page,"Examinations");
    await admin.page.getByText(/E2E Results/).first().waitFor({state:"visible",timeout:15000});
    await hasNoUiError(admin.page,"Admin examination workspace");
    await navigate(admin.page,"Fee invoices");
    await admin.page.locator(".ops-search input").fill("E2E-FEE-");
    await admin.page.locator(".ops-table").getByText(/E2E-FEE-/).first().waitFor({state:"visible",timeout:15000});
    await hasNoUiError(admin.page,"Admin finance workspace");
    await navigate(admin.page,"Audit trail");
    await hasNoUiError(admin.page,"Admin audit workspace");
    await admin.context.close();
    process.stdout.write("PASS | Admin UI navigation and data workspaces\n");

    const teacher = await signIn(browser,"teacher@school.demo","Teacher@2026School","Teacher");
    const teacherNav = await teacher.page.locator(".nav-item").allTextContents();
    assert(!teacherNav.some(x=>x.includes("Students")),"Teacher is shown admin-only Students navigation");
    await navigate(teacher.page,"Attendance");
    await teacher.page.locator(".ops-search input").fill("E2E");
    await teacher.page.locator(".ops-session-card").first().waitFor({state:"visible",timeout:15000});
    const registerButton = teacher.page.locator(".ops-session-card").first().getByRole("button",{name:/View register|Take attendance/});
    await registerButton.click();
    await teacher.page.locator(".ops-stat-row").waitFor({state:"visible",timeout:15000});
    await hasNoUiError(teacher.page,"Teacher attendance register");
    await navigate(teacher.page,"Examinations");
    await teacher.page.getByText(/E2E Results/).first().waitFor({state:"visible",timeout:15000});
    await hasNoUiError(teacher.page,"Teacher examination workspace");
    await teacher.context.close();
    process.stdout.write("PASS | Teacher UI navigation and assigned workspaces\n");

    const guardian = await signIn(browser,"guardian1@school.demo","Guardian@2026School","Guardian");
    const guardianNav = await guardian.page.locator(".nav-item").allTextContents();
    assert(!guardianNav.some(x=>x.includes("Students") && !x.includes("Student report cards")),"Guardian is shown admin-only student directory");
    await navigate(guardian.page,"Student report cards");
    await guardian.page.getByLabel("Student").waitFor({state:"visible",timeout:15000});
    const guardianExam = guardian.page.getByLabel("Published examination");
    const guardianExamId = await guardianExam.locator("option").evaluateAll(opts => opts.find(o => o.textContent.includes("E2E Results"))?.value || "");
    assert(guardianExamId,"Guardian UI did not load the published E2E examination");
    await guardianExam.selectOption(guardianExamId);
    await guardian.page.getByRole("button",{name:/View report card/}).click();
    await guardian.page.getByText("PERCENTAGE",{exact:true}).waitFor({state:"visible",timeout:15000});
    await hasNoUiError(guardian.page,"Guardian report card workspace");
    await navigate(guardian.page,"Fee invoices");
    await guardian.page.locator(".ops-search input").fill("E2E-FEE-");
    const guardianInvoiceRow = guardian.page.locator(".ops-table tbody tr").filter({hasText:"E2E-FEE-"}).first();
    await guardianInvoiceRow.waitFor({state:"visible",timeout:15000});
    await guardianInvoiceRow.getByRole("button",{name:"Payment history"}).click();
    await guardian.page.getByRole("heading",{name:"Payment history"}).waitFor({state:"visible",timeout:15000});
    await hasNoUiError(guardian.page,"Guardian fee workspace");
    await guardian.context.close();
    process.stdout.write("PASS | Guardian UI linked report card and fee history\n");

    const student = await signIn(browser,"student1@school.demo","Student@2026School","Student");
    const studentNav = await student.page.locator(".nav-item").allTextContents();
    assert(!studentNav.some(x=>x.includes("Students")),"Student is shown admin-only student directory");
    await navigate(student.page,"My results");
    const studentExam = student.page.getByLabel("Published examination");
    const studentExamId = await studentExam.locator("option").evaluateAll(opts => opts.find(o => o.textContent.includes("E2E Results"))?.value || "");
    assert(studentExamId,"Student UI did not load the published E2E examination");
    await studentExam.selectOption(studentExamId);
    await student.page.getByRole("button",{name:/View my report card/}).click();
    await student.page.getByText("PERCENTAGE",{exact:true}).waitFor({state:"visible",timeout:15000});
    await hasNoUiError(student.page,"Student results workspace");
    await student.context.close();
    process.stdout.write("PASS | Student UI personal report card\n");

    assert.equal(errors.length,0,"Browser JavaScript errors: "+errors.join(" | "));
    process.stdout.write("PASS | No uncaught browser JavaScript exceptions\n");
  } finally {
    await browser.close();
  }
})().catch(error=>{
  process.stderr.write("FAIL | Browser E2E | "+(error && error.message ? error.message : String(error))+"\n");
  process.exitCode=1;
});
