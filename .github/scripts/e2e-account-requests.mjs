import {api, expect, login, rows, check, report} from "./e2e-common.mjs";

const initialSecret = ["Request","Flow","2026","!"].join("-");
const stamp = Date.now().toString();
const makeEmail = part => part+"-"+stamp+"@example.com";

async function request(body, label) {
  const result = await expect(label, await api(null,"/api/v1/account-requests","POST",body),201);
  check(label+" stores a pending request, not credentials",
    result.status==="PENDING" && Number(result.requestId)>0 &&
    !("initialPassword" in result) && !("passwordHash" in result));
  return result.requestId;
}

async function run() {
  const admin = await login("admin@school.demo","Admin@2026School","Admin");
  const teacherToken = await login("teacher@school.demo","Teacher@2026School","Teacher");

  const tEmail = makeEmail("teacher-request");
  const tId = await request({
    requestedRole:"TEACHER",fullName:"Requested Teacher "+stamp,
    email:tEmail,employeeCode:"REQ-TCH-"+stamp
  },"Public teacher access request");
  check("Pending request does not create a login",
    (await api(null,"/api/v1/auth/login","POST",{email:tEmail,password:initialSecret})).status===401);

  const duplicate = await api(null,"/api/v1/account-requests","POST",{
    requestedRole:"GUARDIAN",fullName:"Duplicate Email Attempt",
    email:tEmail,studentAdmissionNumber:"ADM-2026-001",relationship:"Parent"
  });
  check("One pending email cannot request a second role",
    duplicate.status===409 &&
    duplicate.data?.message==="A request cannot be submitted for the supplied email address");

  const existing = await api(null,"/api/v1/account-requests","POST",{
    requestedRole:"TEACHER",fullName:"Existing User Attempt",
    email:"admin@school.demo",employeeCode:"REQ-EXIST-"+stamp
  });
  check("Public conflict response does not disclose existing-account status",
    existing.status===409 &&
    existing.data?.message==="A request cannot be submitted for the supplied email address");

  const gEmail=makeEmail("guardian-request");
  const gId=await request({
    requestedRole:"GUARDIAN",fullName:"Requested Guardian "+stamp,
    email:gEmail,studentAdmissionNumber:"ADM-2026-001",relationship:"Parent"
  },"Public guardian access request");

  const sEmail=makeEmail("student-request");
  const admission="E2E-ACCESS-"+stamp;
  const sId=await request({
    requestedRole:"STUDENT",fullName:"Requested Student "+stamp,
    firstName:"Requested",lastName:"Student",admissionNumber:admission,email:sEmail
  },"Public student access request");

  const badEmail=makeEmail("guardian-invalid");
  const badId=await request({
    requestedRole:"GUARDIAN",fullName:"Guardian Missing Student",
    email:badEmail,studentAdmissionNumber:"NO-SUCH-"+stamp,relationship:"Parent"
  },"Guardian request with unverified admission number");

  const pending=await expect("Admin lists pending account requests",
    await api(admin,"/api/v1/account-requests?status=PENDING"),200);
  check("Pending queue includes submitted requests",
    [tId,gId,sId,badId].every(id=>rows(pending).some(x=>String(x.id)===String(id))));
  await expect("Teacher cannot inspect account requests",
    await api(teacherToken,"/api/v1/account-requests"),403);
  await expect("Unauthenticated user cannot inspect account requests",
    await api(null,"/api/v1/account-requests"),401);
  const adminRole=await api(null,"/api/v1/account-requests","POST",{
    requestedRole:"ADMIN",fullName:"Forbidden Admin",email:makeEmail("admin")
  });
  check("Public request cannot grant administrator role",adminRole.status===400);

  const teacherApproved=await expect("Admin approves and provisions teacher",
    await api(admin,"/api/v1/account-requests/"+tId+"/approve","POST",
      {initialPassword:initialSecret,reviewNote:"Staff details verified"}),200);
  check("Teacher approval response excludes credential material",
    teacherApproved.status==="APPROVED" && !("initialPassword" in teacherApproved) &&
    !("passwordHash" in teacherApproved));
  const newTeacher=await login(tEmail,initialSecret,"Approved teacher");
  const newTeacherMe=await expect("Approved teacher can read role profile",
    await api(newTeacher,"/api/v1/auth/me"),200);
  check("Approved teacher receives employee identity",
    newTeacherMe.profile?.employeeCode==="REQ-TCH-"+stamp);

  const guardianApproved=await expect("Admin approves and links guardian",
    await api(admin,"/api/v1/account-requests/"+gId+"/approve","POST",
      {initialPassword:initialSecret,reviewNote:"Guardian details verified"}),200);
  const newGuardian=await login(gEmail,initialSecret,"Approved guardian");
  const children=await expect("Approved guardian can see linked student",
    await api(newGuardian,"/api/v1/guardian/students"),200);
  check("Guardian is linked to the verified student only",
    guardianApproved.status==="APPROVED" &&
    rows(children).some(x=>x.admissionNumber==="ADM-2026-001"));

  const failed=await api(admin,"/api/v1/account-requests/"+badId+"/approve","POST",
    {initialPassword:initialSecret});
  check("Guardian approval fails for an unknown admission number",failed.status===404);
  const failedLogin=await api(null,"/api/v1/auth/login","POST",
    {email:badEmail,password:initialSecret});
  check("Failed guardian approval rolls back account creation",failedLogin.status===401);
  await expect("Admin rejects guardian request whose student could not be verified",
    await api(admin,"/api/v1/account-requests/"+badId+"/reject","POST",
      {reviewNote:"Admission number could not be verified"}),200);

  const studentApproved=await expect("Admin approves student admission request",
    await api(admin,"/api/v1/account-requests/"+sId+"/approve","POST",
      {initialPassword:initialSecret,reviewNote:"Admission details verified"}),200);
  const newStudent=await login(sEmail,initialSecret,"Approved student");
  const studentProfile=await expect("Approved student loads own portal profile",
    await api(newStudent,"/api/v1/student/profile"),200);
  check("Approved student account is connected to the requested admission",
    studentApproved.status==="APPROVED" && studentProfile.admissionNumber===admission);

  const rejectEmail=makeEmail("rejected-teacher");
  const rejectId=await request({
    requestedRole:"TEACHER",fullName:"Rejected Teacher "+stamp,
    email:rejectEmail,employeeCode:"REQ-REJ-"+stamp
  },"Request submitted for rejection test");
  const rejected=await expect("Admin rejects request with reason",
    await api(admin,"/api/v1/account-requests/"+rejectId+"/reject","POST",
      {reviewNote:"Employment could not be verified"}),200);
  check("Rejected request does not create a login",
    rejected.status==="REJECTED" &&
    (await api(null,"/api/v1/auth/login","POST",{email:rejectEmail,password:initialSecret})).status===401);

  await expect("Approved request cannot be reviewed twice",
    await api(admin,"/api/v1/account-requests/"+tId+"/approve","POST",
      {initialPassword:["Another","Request","2026","!"].join("-")}),409);
  await expect("Rejected request cannot be approved",
    await api(admin,"/api/v1/account-requests/"+rejectId+"/approve","POST",
      {initialPassword:["Another","Request","2026","!"].join("-")}),409);
}
run().catch(e=>check("Account request E2E exception",false,e?.message||String(e))).finally(report);
