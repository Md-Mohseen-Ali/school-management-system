import {api, expect, login, rows, need, check, indiaDate, report} from "./e2e-common.mjs";

const created = {studentId:null, sectionId:null, attendanceSessionId:null};
async function run() {
  const date = indiaDate();
  const historicalDate = (() => {
    const parts = new Intl.DateTimeFormat("en-GB", {
      timeZone:"Asia/Kolkata", year:"numeric", month:"2-digit", day:"2-digit"
    }).formatToParts(new Date(Date.now() - 5 * 86400000));
    const value = Object.fromEntries(parts.map(part => [part.type, part.value]));
    return value.year + "-" + value.month + "-" + value.day;
  })();
  const tag = Date.now().toString();
  const admin = await login("admin@school.demo","Admin@2026School","Admin");
  const teacher = await login("teacher@school.demo","Teacher@2026School","Teacher");
  const students = await expect("Admin reads student directory",
    await api(admin,"/api/v1/academic/students?search=ADM-2026-001&page=0&size=100"),200);
  const student = rows(students).find(x=>x.admissionNumber==="ADM-2026-001");
  need(student && student.id,"Seed student ADM-2026-001 not found");
  created.studentId = student.id;

  const sections = rows(await expect("Teacher sees assigned sections",
    await api(teacher,"/api/v1/teacher/roster/sections"),200));
  let section = null;
  let roster = [];
  for (const candidate of sections) {
    const r = await api(teacher,"/api/v1/teacher/roster?sectionId="+candidate.id+"&attendanceDate="+date);
    if (r.status !== 200) continue;
    const list = rows(r.data);
    if (list.some(x=>String(x.studentId)===String(student.id))) {
      section = candidate; roster = list; break;
    }
  }
  need(section && roster.length,"No assigned section with seeded student on "+date);
  created.sectionId = section.id;
  check("Teacher roster includes seeded student",true,section.className+" "+section.sectionName+"; "+roster.length+" enrolled");

  const body = {sectionId:section.id,attendanceDate:date,sessionCode:"E2E"+tag.slice(-12),notes:"E2E test "+tag};
  const session = await expect("Teacher creates attendance session",
    await api(teacher,"/api/v1/attendance/sessions","POST",body),200);
  need(session && session.id,"Attendance session response missing ID");
  created.attendanceSessionId = session.id;
  check("New attendance session starts in DRAFT",session.status==="DRAFT");
  await expect("Duplicate session for same section/date/code rejected",
    await api(teacher,"/api/v1/attendance/sessions","POST",body),409);
  await expect("Incomplete attendance cannot be submitted",
    await api(teacher,"/api/v1/attendance/sessions/"+session.id+"/submit","POST"),409);
  await expect("Duplicate student IDs in batch rejected",
    await api(teacher,"/api/v1/attendance/sessions/"+session.id+"/records","PUT",{
      records:[
        {studentId:roster[0].studentId,attendanceStatus:"PRESENT"},
        {studentId:roster[0].studentId,attendanceStatus:"PRESENT"}
      ]
    }),400);
  const records = {records:roster.map(x=>({
    studentId:x.studentId,attendanceStatus:"PRESENT",remarks:"E2E attendance"
  }))};
  const saved = await expect("Teacher saves attendance for entire roster",
    await api(teacher,"/api/v1/attendance/sessions/"+session.id+"/records","PUT",records),200);
  check("Saved attendance count matches roster",rows(saved).length===roster.length,
    rows(saved).length+" / "+roster.length);
  const readback = await expect("Admin reads persisted attendance",
    await api(admin,"/api/v1/attendance/sessions/"+session.id+"/records"),200);
  check("Attendance records persist for all students",rows(readback).length===roster.length);
  const submitted = await expect("Teacher submits attendance",
    await api(teacher,"/api/v1/attendance/sessions/"+session.id+"/submit","POST"),200);
  check("Attendance enters SUBMITTED state",submitted.status==="SUBMITTED");
  await expect("Teacher cannot lock submitted attendance",
    await api(teacher,"/api/v1/attendance/sessions/"+session.id+"/lock","POST"),403);
  const locked = await expect("Admin locks submitted attendance",
    await api(admin,"/api/v1/attendance/sessions/"+session.id+"/lock","POST"),200);
  check("Attendance enters LOCKED state",locked.status==="LOCKED");
  await expect("Locked records cannot be edited",
    await api(teacher,"/api/v1/attendance/sessions/"+session.id+"/records","PUT",records),409);
  const sessions = await expect("Teacher views own attendance sessions",
    await api(teacher,"/api/v1/attendance/sessions?sectionId="+section.id+"&attendanceDate="+date),200);
  check("New session appears in teacher's session list",
    rows(sessions).some(x=>String(x.id)===String(session.id)));
  const auditEvents = rows(await expect("Admin reads attendance audit events",
    await api(admin,"/api/v1/audit-logs?page=0&size=100&sort=occurredAt,desc"),200));
  const sessionAuditActions = auditEvents
    .filter(x=>String(x.entityId)===String(session.id))
    .map(x=>x.action);
  check("Audit trail records attendance create, mark, submit and lock",
    ["ATTENDANCE_SESSION_CREATED","ATTENDANCE_RECORDS_SAVED",
     "ATTENDANCE_SESSION_SUBMITTED","ATTENDANCE_SESSION_LOCKED"]
      .every(action=>sessionAuditActions.includes(action)),
    sessionAuditActions.join(", "));
  // Create an enrollment that is valid for a past attendance date, then end
  // it today. The administrator's historical register must retain that student.
  const historicalStudent = await expect("Admin creates historical-roster test student",
    await api(admin,"/api/v1/academic/students","POST",{
      admissionNumber:"E2E-HISTORY-"+tag,firstName:"Historical",lastName:"Student",
      admissionDate:historicalDate,email:"history-"+tag+"@example.com",
      initialPassword:"History@2026School"
    }),201);
  const historicalEnrollment = await expect("Admin enrolls student before historical register date",
    await api(admin,"/api/v1/academic/enrollments","POST",{
      studentId:historicalStudent.id,sectionId:section.id,startDate:historicalDate
    }),200);
  const historicalSession = await expect("Teacher creates historical attendance session",
    await api(teacher,"/api/v1/attendance/sessions","POST",{
      sectionId:section.id,attendanceDate:historicalDate,
      sessionCode:"HIST"+tag.slice(-12),notes:"Historical roster regression test"
    }),200);
  await expect("Admin ends the historical test enrollment",
    await api(admin,"/api/v1/academic/enrollments/"+historicalEnrollment.id+"/end","PATCH"),200);
  const historicalRoster = rows(await expect("Admin queries roster on the historical date",
    await api(admin,"/api/v1/academic/enrollments?sectionId="+section.id+"&attendanceDate="+historicalDate+"&page=0&size=100"),200));
  check("Historical register retains a student whose enrollment later ended",
    historicalRoster.some(x=>String(x.studentId)===String(historicalStudent.id)));
  const activeRoster = rows(await expect("Admin queries currently active section enrollment",
    await api(admin,"/api/v1/academic/enrollments?sectionId="+section.id+"&active=true&page=0&size=100"),200));
  check("Ended student is excluded from the current active roster",
    !activeRoster.some(x=>String(x.studentId)===String(historicalStudent.id)));
  check("Historical attendance session is retained for register review",Boolean(historicalSession.id));


  process.stdout.write("RETAINED ATTENDANCE TEST DATA: "+JSON.stringify(created)+"\n");
}
run().catch(e=>check("Attendance E2E runner exception",false,e && e.message ? e.message : String(e))).finally(report);
