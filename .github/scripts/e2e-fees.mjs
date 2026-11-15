import {api, expect, login, rows, need, check, indiaDate, report} from "./e2e-common.mjs";

const retained = {studentId:null, feeInvoiceId:null, invoiceNumber:null, paymentIds:[]};
async function run() {
  const date = indiaDate();
  const tag = Date.now().toString();
  const admin = await login("admin@school.demo","Admin@2026School","Admin");
  const guardian = await login("guardian1@school.demo","Guardian@2026School","Guardian");
  const guardian2 = await login("guardian2@school.demo","Guardian2@2026School","Guardian 2");
  const students = await expect("Admin reads student directory",
    await api(admin,"/api/v1/academic/students?search=ADM-2026-001&page=0&size=100"),200);
  const student = rows(students).find(x=>x.admissionNumber==="ADM-2026-001");
  need(student && student.id,"Seed student ADM-2026-001 not found");
  retained.studentId = student.id;

  const number = "E2E-FEE-"+tag;
  retained.invoiceNumber = number;
  const created = await expect("Admin creates invoice draft",
    await api(admin,"/api/v1/fees/invoices","POST",{
      invoiceNumber:number,studentId:student.id,academicYear:"2026-2027",
      issueDate:date,dueDate:date
    }),200);
  need(created && created.id,"Invoice response missing ID");
  retained.feeInvoiceId = created.id;
  check("Invoice begins in DRAFT status",created.status==="DRAFT");
  await expect("Duplicate invoice number rejected",
    await api(admin,"/api/v1/fees/invoices","POST",{
      invoiceNumber:number,studentId:student.id,academicYear:"2026-2027",
      issueDate:date,dueDate:date
    }),409);

  await expect("Admin adds item to draft invoice",
    await api(admin,"/api/v1/fees/invoices/"+created.id+"/items","POST",{
      description:"E2E simulated school fee "+tag,amount:100
    }),200);
  const issued = await expect("Admin issues itemized invoice",
    await api(admin,"/api/v1/fees/invoices/"+created.id+"/issue","POST"),200);
  check("Invoice moves to ISSUED status",issued.status==="ISSUED");
  check("Invoice total equals 100",Number(issued.totalAmount)===100,
    "total "+issued.totalAmount);

  const guardianInvoice = await expect("Linked guardian reads issued invoice",
    await api(guardian,"/api/v1/fees/invoices/"+created.id),200);
  check("Guardian sees the correct student on invoice",
    String(guardianInvoice.studentId)===String(student.id));
  await expect("Unlinked guardian cannot read invoice",
    await api(guardian2,"/api/v1/fees/invoices/"+created.id),404);
  await expect("Unlinked guardian cannot submit payment",
    await api(guardian2,"/api/v1/fees/invoices/"+created.id+"/payments","POST",{
      amount:1,paymentMethod:"SIMULATED",idempotencyKey:"e2e-denied-"+tag
    }),404);

  await expect("Cash payment rejected by simulated-only policy",
    await api(admin,"/api/v1/fees/invoices/"+created.id+"/payments","POST",{
      amount:100,paymentMethod:"CASH",idempotencyKey:"e2e-cash-"+tag
    }),400);

  const firstKey = "e2e-first-"+tag;
  const first = await expect("Guardian records first simulated payment",
    await api(guardian,"/api/v1/fees/invoices/"+created.id+"/payments","POST",{
      amount:40,paymentMethod:"SIMULATED",idempotencyKey:firstKey
    }),200);
  need(first && first.id,"First payment response missing ID");
  retained.paymentIds.push(first.id);
  check("Payment method and status are SIMULATED/SUCCEEDED",
    first.paymentMethod==="SIMULATED" && first.paymentStatus==="SUCCEEDED");

  const retry = await expect("Retry with same idempotency key returns success",
    await api(guardian,"/api/v1/fees/invoices/"+created.id+"/payments","POST",{
      amount:40,paymentMethod:"SIMULATED",idempotencyKey:firstKey
    }),200);
  check("Idempotent retry reuses the original payment",
    String(retry.id)===String(first.id));

  await expect("Same idempotency key cannot represent a different amount",
    await api(guardian,"/api/v1/fees/invoices/"+created.id+"/payments","POST",{
      amount:41,paymentMethod:"SIMULATED",idempotencyKey:firstKey
    }),409);
  await expect("Payment above outstanding balance is rejected",
    await api(guardian,"/api/v1/fees/invoices/"+created.id+"/payments","POST",{
      amount:61,paymentMethod:"SIMULATED",idempotencyKey:"e2e-overpay-"+tag
    }),409);

  const second = await expect("Guardian pays the remaining simulated balance",
    await api(guardian,"/api/v1/fees/invoices/"+created.id+"/payments","POST",{
      amount:60,paymentMethod:"SIMULATED",idempotencyKey:"e2e-second-"+tag
    }),200);
  need(second && second.id,"Second payment response missing ID");
  retained.paymentIds.push(second.id);
  check("Second payment is simulated and successful",
    second.paymentMethod==="SIMULATED" && second.paymentStatus==="SUCCEEDED");

  const history = await expect("Guardian retrieves invoice payment history",
    await api(guardian,"/api/v1/fees/invoices/"+created.id+"/payments"),200);
  const historyRows = rows(history);
  check("Payment history contains two unique successful payments",
    historyRows.length===2 && new Set(historyRows.map(x=>String(x.id))).size===2 &&
    historyRows.every(x=>x.paymentStatus==="SUCCEEDED" && x.paymentMethod==="SIMULATED"));

  const finalInvoice = await expect("Admin reads invoice ledger after settlement",
    await api(admin,"/api/v1/fees/invoices/"+created.id),200);
  check("Invoice ledger reconciles total/paid/balance",
    Number(finalInvoice.totalAmount)===100 &&
    Number(finalInvoice.amountPaid)===100 &&
    Number(finalInvoice.balance)===0,
    "total="+finalInvoice.totalAmount+", paid="+finalInvoice.amountPaid+", balance="+finalInvoice.balance);
  await expect("Paid invoice cannot be cancelled",
    await api(admin,"/api/v1/fees/invoices/"+created.id+"/cancel","POST"),409);

  const adminList = await expect("Admin fee register loads",
    await api(admin,"/api/v1/fees/invoices?studentId="+student.id+"&page=0&size=100"),200);
  check("Admin invoice register includes E2E invoice",
    rows(adminList).some(x=>String(x.id)===String(created.id)));
  const guardianList = await expect("Guardian invoice list loads",
    await api(guardian,"/api/v1/fees/invoices?page=0&size=100"),200);
  check("Guardian invoice list includes linked issued invoice",
    rows(guardianList).some(x=>String(x.id)===String(created.id)));
  const otherGuardianList = await expect("Second guardian invoice list loads",
    await api(guardian2,"/api/v1/fees/invoices?page=0&size=100"),200);
  check("Second guardian invoice list excludes unrelated invoice",
    !rows(otherGuardianList).some(x=>String(x.id)===String(created.id)));

  const audit = await expect("Admin audit endpoint loads after financial operations",
    await api(admin,"/api/v1/audit-logs"),200);
  check("Audit trail records invoice or payment E2E activity",
    rows(audit).some(x=>String(x.entityId)===String(created.id) ||
      retained.paymentIds.some(id=>String(x.entityId)===String(id))));
  process.stdout.write("RETAINED FEE E2E DATA: "+JSON.stringify(retained)+"\n");
}
run().catch(e=>check("Fee E2E runner exception",false,e && e.message ? e.message : String(e))).finally(report);
