import {api, expect, login, rows, need, check, indiaDate, report} from "./e2e-common.mjs";

async function run() {
  const date = indiaDate();
  const fromDate = new Date(Date.now()-90*86400000).toISOString().slice(0,10);
  const admin = await login("admin@school.demo","Admin@2026School","Admin");
  const teacher = await login("teacher@school.demo","Teacher@2026School","Teacher");
  const guardian = await login("guardian1@school.demo","Guardian@2026School","Guardian");
  const guardian2 = await login("guardian2@school.demo","Guardian2@2026School","Guardian 2");
  const student = await login("student1@school.demo","Student@2026School","Student");

  const profiles = await Promise.all([
    api(admin,"/api/v1/auth/me"),
    api(teacher,"/api/v1/auth/me"),
    api(guardian,"/api/v1/auth/me"),
    api(student,"/api/v1/auth/me")
  ]);
  for (const [label,response,role] of [
    ["Admin",profiles[0],"ADMIN"],["Teacher",profiles[1],"TEACHER"],
    ["Guardian",profiles[2],"GUARDIAN"],["Student",profiles[3],"STUDENT"]
  ]) {
    const data = await expect(label+" detailed profile endpoint",response,200);
    check(label+" profile identifies role-specific details",data.profile?.type===role);
  }
  const studentProfile = profiles[3].data.profile;
  check("Student profile includes admission, class and section",
    studentProfile.admissionNumber==="ADM-2026-001" &&
    studentProfile.className==="Grade 10" && studentProfile.sectionName==="A");

  const personal = await expect("Student self-service profile",
    await api(student,"/api/v1/student/profile"),200);
  check("Student profile is resolved from current login",
    personal.email==="student1@school.demo" && personal.admissionNumber==="ADM-2026-001");

  const attendance = await expect("Student attendance summary",
    await api(student,"/api/v1/student/attendance/summary?fromDate="+fromDate+"&toDate="+date),200);
  check("Student sees own attendance record count",Number(attendance.totalRecords)>=1 &&
    attendance.admissionNumber==="ADM-2026-001");
  const history = await expect("Student attendance history",
    await api(student,"/api/v1/student/attendance/history?fromDate="+fromDate+"&toDate="+date),200);
  check("Student history includes only finalized sessions",
    rows(history).length>=1 && rows(history).every(x=>["SUBMITTED","LOCKED"].includes(x.sessionStatus)));

  const studentInvoices = await expect("Student issued invoice list",
    await api(student,"/api/v1/student/fees/invoices?page=0&size=100"),200);
  check("Student sees only invoices for own student record",
    rows(studentInvoices).every(x=>String(x.studentId)===String(personal.studentId)));
  if(rows(studentInvoices).length) {
    const invoice = rows(studentInvoices)[0];
    const detail = await expect("Student invoice detail",
      await api(student,"/api/v1/student/fees/invoices/"+invoice.id),200);
    check("Student invoice detail cannot switch student identity",
      String(detail.studentId)===String(personal.studentId));
  }

  const linked = await expect("Guardian-linked students",
    await api(guardian,"/api/v1/guardian/students"),200);
  const ownChild = rows(linked).find(x=>x.admissionNumber==="ADM-2026-001");
  need(ownChild,"Seed guardian has no linked student ADM-2026-001");
  const guardianAttendance = await expect("Guardian attendance summary for linked child",
    await api(guardian,"/api/v1/guardian/students/"+ownChild.id+"/attendance/summary?fromDate="+fromDate+"&toDate="+date),200);
  check("Guardian summary identifies linked child",
    guardianAttendance.admissionNumber==="ADM-2026-001");
  const guardianHistory = await expect("Guardian attendance history for linked child",
    await api(guardian,"/api/v1/guardian/students/"+ownChild.id+"/attendance/history?fromDate="+fromDate+"&toDate="+date),200);
  check("Guardian sees finalized attendance records",
    rows(guardianHistory).length>=1 && rows(guardianHistory).every(x=>["SUBMITTED","LOCKED"].includes(x.sessionStatus)));

  const otherLinked = await expect("Second guardian linked-student list",
    await api(guardian2,"/api/v1/guardian/students"),200);
  need(rows(otherLinked).length>0,"Second guardian has no seeded student to test against");
  await expect("Guardian cannot access an unrelated student's attendance",
    await api(guardian2,"/api/v1/guardian/students/"+ownChild.id+"/attendance/summary?fromDate="+fromDate+"&toDate="+date),404);
  await expect("Guardian role cannot call student self-service endpoint",
    await api(guardian,"/api/v1/student/profile"),403);
  await expect("Student role cannot call teacher directory",
    await api(student,"/api/v1/identity/teachers"),403);

  const sections = rows(await expect("Teacher assigned sections",
    await api(teacher,"/api/v1/teacher/roster/sections"),200));
  need(sections.length>0,"Seed teacher has no sections");
  const first = sections[0];
  const classReport = await expect("Teacher class attendance report for assigned section",
    await api(teacher,"/api/v1/attendance/reports/sections/"+first.id+"?fromDate="+fromDate+"&toDate="+date),200);
  check("Teacher report returns student-level aggregates",Array.isArray(classReport.students));
  await expect("Teacher cannot report on unassigned section",
    await api(teacher,"/api/v1/attendance/reports/sections/999999?fromDate="+fromDate+"&toDate="+date),403);
}
run().catch(e=>check("Self-service E2E exception",false,e?.message||String(e))).finally(report);
