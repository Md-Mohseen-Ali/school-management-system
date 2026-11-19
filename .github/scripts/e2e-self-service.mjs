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
  const adminProfile = profiles[0].data.profile;
  const teacherProfile = profiles[1].data.profile;
  const guardianProfile = profiles[2].data.profile;
  const studentProfile = profiles[3].data.profile;
  check("Admin profile describes its school-wide management areas",
    adminProfile.managementAreas?.length >= 5 && adminProfile.administrativeScope === "School-wide");
  check("Teacher profile includes staff identity and subject assignment details",
    Boolean(teacherProfile.employeeCode) &&
    teacherProfile.assignedSections?.some(x => x.subjectCode && x.subjectName && x.assignedOn));
  check("Guardian profile includes linked-child academic context",
    guardianProfile.linkedStudents?.some(x => x.studentStatus && x.className && x.sectionName));
  check("Student profile includes admission, class, section and account creation",
    studentProfile.admissionNumber==="ADM-2026-001" &&
    studentProfile.className==="Grade 10" && studentProfile.sectionName==="A" &&
    Boolean(profiles[3].data.createdAt));

  const personal = await expect("Student self-service profile",
    await api(student,"/api/v1/student/profile"),200);
  check("Student profile is resolved from current login",
    personal.email==="student1@school.demo" && personal.admissionNumber==="ADM-2026-001");

  const academics = await expect("Student academic overview",
    await api(student,"/api/v1/student/academics"),200);
  check("Student academics are scoped to the authenticated student's enrollment",
    academics.student?.admissionNumber==="ADM-2026-001" &&
    academics.student?.className==="Grade 10" && academics.student?.sectionName==="A");
  check("Student subject directory returns the active subject-teacher assignments",
    Array.isArray(academics.subjects) && academics.subjects.length>0 &&
    academics.subjects.every(x=>x.subjectCode && x.subjectName && x.teacherName));

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

  const childExams = await expect("Guardian loads published examinations for selected linked child",
    await api(guardian,"/api/v1/guardian/examinations/students/"+ownChild.id),200);
  const allGuardianExams = await expect("Guardian aggregate published examination list",
    await api(guardian,"/api/v1/guardian/examinations"),200);
  check("Student-specific exam choices include the child's E2E published exam",
    rows(childExams).some(x=>String(x.examName||"").startsWith("E2E Results")));
  check("Student-specific exam choices are a subset of guardian's published exams",
    rows(childExams).every(x=>rows(allGuardianExams).some(y=>String(y.id)===String(x.id))));
  const unrelatedExamList = await api(guardian2,"/api/v1/guardian/examinations/students/"+ownChild.id);
  await expect("Second guardian cannot load another child's examination choices",unrelatedExamList,404);

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
  const individualReport = await expect("Teacher can read an enrolled student's assigned-section summary",
    await api(teacher,"/api/v1/attendance/reports/students/"+ownChild.id+
      "?sectionId="+first.id+"&fromDate="+fromDate+"&toDate="+date),200);
  check("Teacher individual report identifies the enrolled student",
    String(individualReport.studentId)===String(ownChild.id));
  await expect("Teacher cannot report on unassigned section",
    await api(teacher,"/api/v1/attendance/reports/sections/999999?fromDate="+fromDate+"&toDate="+date),403);

  // A real student without an enrollment in the teacher's assigned section
  // must not be exposed through the individual summary endpoint.
  const unrelatedStudent = await expect("Admin creates student outside teacher's section",
    await api(admin,"/api/v1/academic/students","POST",{
      admissionNumber:"E2E-PRIVATE-"+Date.now(),firstName:"Private",lastName:"Student",
      admissionDate:date,email:"private-"+Date.now()+"@example.com",
      initialPassword:"Private@2026School"
    }),201);
  await expect("Teacher cannot inspect an unrelated student's attendance summary",
    await api(teacher,"/api/v1/attendance/reports/students/"+unrelatedStudent.id+
      "?sectionId="+first.id+"&fromDate="+fromDate+"&toDate="+date),404);

  // Historical exam discovery must remain independent of current enrollment.
  const enrollmentPage = await expect("Admin reads student's active enrollment before transfer",
    await api(admin,"/api/v1/academic/enrollments?studentId="+ownChild.id+"&active=true&page=0&size=10"),200);
  const activeEnrollment = rows(enrollmentPage)[0];
  need(activeEnrollment && activeEnrollment.id,"Student active enrollment not found for transfer regression test");
  await expect("Admin ends student's current enrollment after published exam",
    await api(admin,"/api/v1/academic/enrollments/"+activeEnrollment.id+"/end","PATCH"),200);
  const studentHistoricalExams = await expect("Student still sees exams taken before leaving the section",
    await api(student,"/api/v1/student/examinations"),200);
  check("Published historical examination remains visible to the student",
    rows(studentHistoricalExams).some(x=>String(x.examName||"").startsWith("E2E Results")));
  const guardianHistoricalExams = await expect("Guardian still sees linked child's historical exams",
    await api(guardian,"/api/v1/guardian/examinations/students/"+ownChild.id),200);
  check("Published historical examination remains visible to the linked guardian",
    rows(guardianHistoricalExams).some(x=>String(x.examName||"").startsWith("E2E Results")));

  const auditEvents = rows(await expect("Admin reads audit trail after academic changes",
    await api(admin,"/api/v1/audit-logs?page=0&size=100&sort=occurredAt,desc"),200));
  check("Audit trail captures student admission",
    auditEvents.some(x=>x.action==="STUDENT_CREATED" &&
      String(x.entityId)===String(unrelatedStudent.id)));
  check("Audit trail captures enrollment ending",
    auditEvents.some(x=>x.action==="STUDENT_ENROLLMENT_ENDED" &&
      String(x.entityId)===String(activeEnrollment.id)));

  // A future-dated enrollment must not be exposed as the student's current
  // class/section until its start date is reached.
  const futureDate = new Date(Date.now()+30*86400000).toISOString().slice(0,10);
  const futureEmail = "future-"+Date.now()+"@example.com";
  const futureStudent = await expect("Admin creates a student with a future enrollment",
    await api(admin,"/api/v1/academic/students","POST",{
      admissionNumber:"E2E-FUTURE-"+Date.now(),firstName:"Future",lastName:"Student",
      admissionDate:date,email:futureEmail,initialPassword:"Future@2026School"
    }),201);
  await expect("Admin schedules the student's future enrollment",
    await api(admin,"/api/v1/academic/enrollments","POST",{
      studentId:futureStudent.id,sectionId:first.id,startDate:futureDate
    }),201);
  const futureStudentToken=await login(futureEmail,"Future@2026School","Future student");
  const futureProfile=await expect("Future student's profile is accessible",
    await api(futureStudentToken,"/api/v1/student/profile"),200);
  const futureAcademics=await expect("Future student's academic overview is accessible",
    await api(futureStudentToken,"/api/v1/student/academics"),200);
  const futureMe=await expect("Future account profile",
    await api(futureStudentToken,"/api/v1/auth/me"),200);
  check("Future enrollment is not displayed as a current class or section",
    futureProfile.className==null && futureProfile.sectionName==null &&
    futureAcademics.student?.className==null && futureAcademics.student?.sectionName==null &&
    Array.isArray(futureAcademics.subjects) && futureAcademics.subjects.length===0);
}
run().catch(e=>check("Self-service E2E exception",false,e?.message||String(e))).finally(report);
