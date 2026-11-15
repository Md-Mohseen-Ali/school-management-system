import {api, expect, login, check, indiaDate, report} from "./e2e-common.mjs";

const makePass = role => ["School",role,"E2E","Q7","x9","!"].join("-");
async function run() {
  const tag = Date.now().toString();
  const admin = await login("admin@school.demo","Admin@2026School","Admin");
  const tEmail = "teacher-e2e-"+tag+"@example.com";
  const tPass = makePass(tag);
  const teacher = await expect("Admin provisions new teacher",
    await api(admin,"/api/v1/identity/accounts","POST",{
      role:"TEACHER",fullName:"E2E Teacher "+tag,email:tEmail,
      phone:"9000000011",initialPassword:tPass,
      employeeCode:"E2E-TCH-"+tag,hireDate:indiaDate()
    }),201);
  check("Teacher response excludes credential values",
    teacher.email===tEmail && teacher.role==="TEACHER" &&
    !("initialPassword" in teacher) && !("passwordHash" in teacher));
  const teacherToken = await login(tEmail,tPass,"New teacher");
  const teacherMe = await expect("New teacher retrieves profile",
    await api(teacherToken,"/api/v1/auth/me"),200);
  check("New teacher profile contains employee code",
    teacherMe.profile?.employeeCode==="E2E-TCH-"+tag);

  const gEmail = "guardian-e2e-"+tag+"@example.com";
  const gPass = makePass("Guardian"+tag);
  const guardian = await expect("Admin provisions new guardian",
    await api(admin,"/api/v1/identity/accounts","POST",{
      role:"GUARDIAN",fullName:"E2E Guardian "+tag,email:gEmail,
      phone:"9000000012",initialPassword:gPass,alternatePhone:"9000000013"
    }),201);
  check("Guardian response excludes credential values",
    guardian.email===gEmail && guardian.role==="GUARDIAN" &&
    !("initialPassword" in guardian) && !("passwordHash" in guardian));
  const guardianToken = await login(gEmail,gPass,"New guardian");
  const guardianMe = await expect("New guardian retrieves profile",
    await api(guardianToken,"/api/v1/auth/me"),200);
  check("New guardian profile starts with no linked students",
    guardianMe.profile?.type==="GUARDIAN" &&
    guardianMe.profile.linkedStudents.length===0);

  const sEmail = "student-e2e-"+tag+"@example.com";
  const sPass = makePass("Student"+tag);
  const created = await expect("Admin creates student with portal credentials",
    await api(admin,"/api/v1/academic/students","POST",{
      admissionNumber:"E2E-ADM-"+tag,firstName:"E2E",lastName:"Student",
      admissionDate:indiaDate(),email:sEmail,phone:"9000000014",
      initialPassword:sPass
    }),201);
  check("New student record confirms portal access",
    created.email===sEmail && created.portalAccessEnabled===true);
  const studentToken = await login(sEmail,sPass,"New student");
  const student = await expect("New student retrieves personal profile",
    await api(studentToken,"/api/v1/student/profile"),200);
  check("Student account is linked to the new academic record",
    String(student.studentId)===String(created.id) &&
    student.admissionNumber==="E2E-ADM-"+tag);
}
run().catch(e=>check("Account provisioning E2E exception",false,e?.message||String(e))).finally(report);
