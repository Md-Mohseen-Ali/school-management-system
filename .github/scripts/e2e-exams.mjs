import {api, expect, login, rows, need, check, indiaDate, report} from "./e2e-common.mjs";

const retained = {studentId:null, sectionId:null, examinationId:null};
async function run() {
  const date = indiaDate();
  const tag = Date.now().toString();
  const admin = await login("admin@school.demo","Admin@2026School","Admin");
  const teacher = await login("teacher@school.demo","Teacher@2026School","Teacher");
  const guardian = await login("guardian1@school.demo","Guardian@2026School","Guardian");
  const guardian2 = await login("guardian2@school.demo","Guardian2@2026School","Guardian 2");
  const studentToken = await login("student1@school.demo","Student@2026School","Student");

  const directory = await expect("Admin reads student directory",
    await api(admin,"/api/v1/academic/students?search=ADM-2026-001&page=0&size=100"),200);
  const student = rows(directory).find(x=>x.admissionNumber==="ADM-2026-001");
  need(student && student.id,"Seed student ADM-2026-001 not found");
  retained.studentId = student.id;

  const guardianStudents = await expect("Guardian reads linked student list",
    await api(guardian,"/api/v1/guardian/students"),200);
  check("Guardian is linked to the seeded student",
    rows(guardianStudents).some(x=>String(x.id)===String(student.id)));
  const otherGuardianStudents = await expect("Second guardian reads own students",
    await api(guardian2,"/api/v1/guardian/students"),200);
  check("Other guardian does not see student's private record",
    !rows(otherGuardianStudents).some(x=>String(x.id)===String(student.id)));

  const teacherSections = rows(await expect("Teacher reads assigned sections",
    await api(teacher,"/api/v1/teacher/roster/sections"),200));
  let section = null, roster = [];
  for (const candidate of teacherSections) {
    const response = await api(teacher,"/api/v1/teacher/roster?sectionId="+candidate.id+"&attendanceDate="+date);
    if (response.status !== 200) continue;
    const list = rows(response.data);
    if (list.some(x=>String(x.studentId)===String(student.id))) {
      section=candidate; roster=list; break;
    }
  }
  need(section && roster.length,"No assigned section with the seeded student on "+date);
  retained.sectionId = section.id;
  const assignmentPage = await expect("Admin reads active section assignments",
    await api(admin,"/api/v1/academic/teacher-assignments?sectionId="+section.id+"&active=true&page=0&size=100"),200);
  const assignment = rows(assignmentPage).find(x=>x.active && x.subjectId);
  need(assignment,"No active subject assignment for chosen section");
  const year = section.academicYear || "2026-2027";

  let gradePayload = await expect("Admin reads academic-year grading rules",
    await api(admin,"/api/v1/examinations/grading-scales?academicYear="+encodeURIComponent(year)),200);
  let grades = rows(gradePayload);
  if (grades.length === 0) {
    await expect("Creates clearly marked grade band only when no rules exist",
      await api(admin,"/api/v1/examinations/grading-scales","POST",{
        academicYear:year,grade:"E2E",minPercentage:0,maxPercentage:100,
        remarks:"E2E local test fixture; no grading scale existed"
      }),200);
    gradePayload = await expect("Reloads grading rules",
      await api(admin,"/api/v1/examinations/grading-scales?academicYear="+encodeURIComponent(year)),200);
    grades = rows(gradePayload);
  }
  const ordered = [...grades].sort((a,b)=>Number(a.minPercentage)-Number(b.minPercentage));
  let cursor = 0, coverage = ordered.length>0;
  for (const g of ordered) {
    if (Math.abs(Number(g.minPercentage)-cursor)>0.0001) coverage=false;
    cursor = Math.round((Number(g.maxPercentage)+0.01)*100)/100;
  }
  if (Math.abs(cursor-100.01)>0.0001) coverage=false;
  check("Grading scale covers 0–100 without gaps",coverage,
    ordered.map(g=>g.grade+":"+g.minPercentage+"-"+g.maxPercentage).join(", "));
  need(coverage,"Grading bands contain gaps or overlaps; existing rules left untouched");

  const examName = "E2E Results "+tag;
  const exam = await expect("Admin creates examination",
    await api(admin,"/api/v1/examinations","POST",{
      sectionId:section.id,examName,academicYear:year,startDate:date,endDate:date
    }),200);
  need(exam && exam.id,"Exam response missing ID");
  retained.examinationId = exam.id;
  check("Exam begins in DRAFT state",exam.status==="DRAFT");
  await expect("Duplicate exam name in same section/year is rejected",
    await api(admin,"/api/v1/examinations","POST",{
      sectionId:section.id,examName,academicYear:year,startDate:date,endDate:date
    }),409);

  const examSubject = await expect("Admin configures assigned subject",
    await api(admin,"/api/v1/examinations/"+exam.id+"/subjects","POST",{
      subjectId:assignment.subjectId,examinationDate:date,maxMarks:100,passMarks:40
    }),200);
  need(examSubject && examSubject.id,"Exam subject response missing ID");
  await expect("Duplicate subject in one exam is blocked",
    await api(admin,"/api/v1/examinations/"+exam.id+"/subjects","POST",{
      subjectId:assignment.subjectId,examinationDate:date,maxMarks:100,passMarks:40
    }),409);
  const scheduled = await expect("Admin schedules exam",
    await api(admin,"/api/v1/examinations/"+exam.id+"/schedule","POST"),200);
  check("Exam transitions to SCHEDULED",scheduled.status==="SCHEDULED");

  const teacherExams = await expect("Teacher sees exams for assigned subjects",
    await api(teacher,"/api/v1/teacher/examinations"),200);
  check("Teacher list includes new exam",rows(teacherExams).some(x=>String(x.id)===String(exam.id)));
  const teacherSubjects = await expect("Teacher sees own assigned exam subjects",
    await api(teacher,"/api/v1/teacher/examinations/"+exam.id+"/subjects"),200);
  check("Teacher can view configured subject",
    rows(teacherSubjects).some(x=>String(x.id)===String(examSubject.id)));

  await expect("Marks above maximum are rejected",
    await api(teacher,"/api/v1/examinations/subjects/"+examSubject.id+"/marks","PUT",{
      records:[{studentId:roster[0].studentId,marksObtained:101,attendanceStatus:"PRESENT"}]
    }),400);
  await expect("Absent student cannot receive positive marks",
    await api(teacher,"/api/v1/examinations/subjects/"+examSubject.id+"/marks","PUT",{
      records:[{studentId:roster[0].studentId,marksObtained:1,attendanceStatus:"ABSENT"}]
    }),400);
  await expect("Duplicate student IDs in marks batch are rejected",
    await api(teacher,"/api/v1/examinations/subjects/"+examSubject.id+"/marks","PUT",{
      records:[
        {studentId:roster[0].studentId,marksObtained:80,attendanceStatus:"PRESENT"},
        {studentId:roster[0].studentId,marksObtained:80,attendanceStatus:"PRESENT"}
      ]
    }),400);

  const marks = await expect("Teacher saves marks for entire enrolled roster",
    await api(teacher,"/api/v1/examinations/subjects/"+examSubject.id+"/marks","PUT",{
      records:roster.map(x=>({studentId:x.studentId,marksObtained:80,attendanceStatus:"PRESENT",remarks:"E2E marks"}))
    }),200);
  check("Mark row count equals roster count",rows(marks).length===roster.length);
  check("Every saved mark is passing",rows(marks).every(x=>x.passed===true));

  const studentBefore = await expect("Student reads published exam list before publish",
    await api(studentToken,"/api/v1/student/examinations"),200);
  check("Scheduled exam is hidden from student",
    !rows(studentBefore).some(x=>String(x.id)===String(exam.id)));
  const guardianBefore = await expect("Guardian reads published exam list before publish",
    await api(guardian,"/api/v1/guardian/examinations"),200);
  check("Scheduled exam is hidden from guardian",
    !rows(guardianBefore).some(x=>String(x.id)===String(exam.id)));

  const complete = await expect("Admin completes exam after every student has marks",
    await api(admin,"/api/v1/examinations/"+exam.id+"/complete","POST"),200);
  check("Exam transitions to COMPLETED",complete.status==="COMPLETED");
  const published = await expect("Admin publishes completed results",
    await api(admin,"/api/v1/examinations/"+exam.id+"/publish","POST"),200);
  check("Exam transitions to PUBLISHED",published.status==="PUBLISHED");

  const reportCard = await expect("Admin reads report card",
    await api(admin,"/api/v1/examinations/"+exam.id+"/report-cards/"+student.id),200);
  check("Report percentage calculated correctly",Number(reportCard.percentage)===80,
    "percentage "+reportCard.percentage);
  check("Grade matches configured percentage band",
    grades.some(g=>g.grade===reportCard.grade &&
      Number(reportCard.percentage)>=Number(g.minPercentage) &&
      Number(reportCard.percentage)<=Number(g.maxPercentage)),
    "grade "+reportCard.grade);
  check("Overall result is passing",reportCard.passed===true);

  const studentExams = await expect("Student sees published exam",
    await api(studentToken,"/api/v1/student/examinations"),200);
  check("Student portal lists published exam",
    rows(studentExams).some(x=>String(x.id)===String(exam.id)));
  const studentReport = await expect("Student opens personal report card",
    await api(studentToken,"/api/v1/student/examinations/"+exam.id+"/report-card"),200);
  check("Student report card resolves from authenticated account",
    String(studentReport.studentId)===String(student.id));

  const guardianExams = await expect("Guardian sees published exam",
    await api(guardian,"/api/v1/guardian/examinations"),200);
  check("Guardian portal lists published exam",
    rows(guardianExams).some(x=>String(x.id)===String(exam.id)));
  const guardianReport = await expect("Guardian opens linked child report card",
    await api(guardian,"/api/v1/guardian/examinations/"+exam.id+"/students/"+student.id+"/report-card"),200);
  check("Guardian report card refers to linked child",
    String(guardianReport.studentId)===String(student.id));
  await expect("Unlinked guardian is denied report-card access",
    await api(guardian2,"/api/v1/guardian/examinations/"+exam.id+"/students/"+student.id+"/report-card"),404);

  const auditEvents = rows(await expect("Admin reads examination audit events",
    await api(admin,"/api/v1/audit-logs?page=0&size=100&sort=occurredAt,desc"),200));
  const examAuditActions = auditEvents
    .filter(x=>String(x.entityId)===String(exam.id))
    .map(x=>x.action);
  check("Audit trail records examination creation, scheduling, completion and publication",
    ["EXAMINATION_CREATED","EXAMINATION_SCHEDULED",
     "EXAMINATION_COMPLETED","EXAMINATION_PUBLISHED"]
      .every(action=>examAuditActions.includes(action)),
    examAuditActions.join(", "));
  check("Audit trail records marks submitted for the configured subject",
    auditEvents.some(x=>x.action==="EXAMINATION_MARKS_SAVED" &&
      String(x.entityId)===String(examSubject.id)));

  process.stdout.write("RETAINED EXAMINATION TEST DATA: "+JSON.stringify(retained)+"\n");
}
run().catch(e=>check("Examination E2E runner exception",false,e && e.message ? e.message : String(e))).finally(report);
