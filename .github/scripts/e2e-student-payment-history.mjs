import {api, expect, login, rows, check, report} from "./e2e-common.mjs";

async function run() {
  const student = await login("student1@school.demo","Student@2026School","Student");
  const invoicePage = await expect("Student invoice directory",
    await api(student,"/api/v1/student/fees/invoices?page=0&size=100"),200);
  const invoices = rows(invoicePage);
  if (!invoices.length) {
    check("Student has an issued invoice available for payment history",false);
    return;
  }
  const invoice = invoices[0];
  const payments = await expect("Student payment history for own invoice",
    await api(student,"/api/v1/student/fees/invoices/"+invoice.id+"/payments"),200);
  check("Payment history rows belong to selected invoice",
    rows(payments).every(item=>String(item.invoiceId)===String(invoice.id)));
  check("Student payment history is read-only",
    await api(student,"/api/v1/student/fees/invoices/"+invoice.id+"/payments","POST",{}) .then(x=>x.status===405 || x.status===403));
}
run().catch(e=>check("Student payment history E2E exception",false,e?.message||String(e))).finally(report);
