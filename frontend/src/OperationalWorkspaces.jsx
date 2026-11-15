import { useCallback, useEffect, useMemo, useState } from 'react';
import { apiRequest, rowsFrom } from './api.js';

const today = () => new Date().toISOString().slice(0, 10);
const money = (value) => value == null ? '—' : new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(Number(value));
const pretty = (value) => String(value || '—').replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
const get = (obj, ...keys) => { for (const key of keys) { const value = key.split('.').reduce((acc, part) => acc?.[part], obj); if (value != null) return value; } return undefined; };
const messageFrom = (err) => err?.payload?.message || err?.message || 'Something went wrong. Please try again.';

function Notice({ type = 'success', children, onClose }) {
  return <div className={`ops-notice ${type}`} role="status"><span>{type === 'success' ? '✓' : '!'}</span><div>{children}</div>{onClose && <button onClick={onClose} aria-label="Dismiss">×</button>}</div>;
}
function Empty({ title, description, action }) {
  return <div className="ops-empty"><div className="ops-empty-icon">▤</div><h3>{title}</h3><p>{description}</p>{action}</div>;
}
function SectionTitle({ eyebrow, title, description, action }) {
  return <div className="ops-title"><div><span className="eyebrow">{eyebrow}</span><h2>{title}</h2><p>{description}</p></div>{action}</div>;
}
function Badge({ value }) {
  const key = String(value || 'UNKNOWN').toUpperCase();
  const klass = ['DRAFT', 'PENDING', 'OVERDUE', 'ABSENT', 'FAILED', 'CANCELLED'].includes(key) ? 'warn' : ['LOCKED', 'PUBLISHED', 'COMPLETED', 'PAID', 'PRESENT', 'ISSUED', 'SUBMITTED'].includes(key) ? 'good' : 'neutral';
  return <span className={`ops-badge ${klass}`}>{pretty(value)}</span>;
}
function Modal({ title, subtitle, children, onClose }) {
  return <div className="ops-modal-backdrop" role="presentation" onMouseDown={(e) => { if (e.target === e.currentTarget) onClose(); }}><section className="ops-modal" role="dialog" aria-modal="true"><div className="ops-modal-head"><div><h3>{title}</h3>{subtitle && <p>{subtitle}</p>}</div><button className="ops-close" onClick={onClose} aria-label="Close">×</button></div>{children}</section></div>;
}

export function AttendanceWorkspace({ token, roles = [] }) {
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeSession, setActiveSession] = useState(null);
  const [students, setStudents] = useState([]);
  const [marks, setMarks] = useState({});
  const [remarks, setRemarks] = useState({});
  const [recordsSaved, setRecordsSaved] = useState(false);
  const [rosterLoading, setRosterLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState(null);
  const [search, setSearch] = useState('');
  const [sessionForm, setSessionForm] = useState(false);
  const [sessionDraft, setSessionDraft] = useState({ sectionId: '1', attendanceDate: today(), sessionCode: 'FULL_DAY', notes: '' });
  const canMark = roles.includes('TEACHER');
  const canLock = roles.includes('ADMIN');

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try { setSessions(rowsFrom(await apiRequest('/api/v1/attendance/sessions', { token }))); }
    catch (e) { setError(messageFrom(e)); }
    finally { setLoading(false); }
  }, [token]);
  useEffect(() => { load(); }, [load]);

  async function openRoster(session) {
    setActiveSession(session); setNotice(null); setRosterLoading(true); setStudents([]); setMarks({}); setRemarks({}); setRecordsSaved(false);
    try {
      const rows = rowsFrom(await apiRequest(`/api/v1/academic/enrollments?sectionId=${encodeURIComponent(session.sectionId)}&active=true`, { token }));
      setStudents(rows);
      const initial = {};
      rows.forEach((r) => { const id = get(r, 'studentId', 'id'); initial[id] = 'PRESENT'; });
      setMarks(initial);
    } catch (e) { setNotice({ type: 'error', text: `Could not load the class register: ${messageFrom(e)}` }); }
    finally { setRosterLoading(false); }
  }

  async function saveRecords() {
    if (!activeSession || !students.length) return;
    setSaving(true); setNotice(null);
    try {
      const records = students.map((student) => {
        const id = get(student, 'studentId', 'id');
        return { studentId: Number(id), attendanceStatus: marks[id] || 'PRESENT', remarks: remarks[id] || '' };
      });
      await apiRequest(`/api/v1/attendance/sessions/${activeSession.id}/records`, { method: 'PUT', token, body: { records } });
      setRecordsSaved(true);
      setNotice({ type: 'success', text: 'Attendance saved. You can now submit this session for review.' });
      await load();
    } catch (e) { setNotice({ type: 'error', text: messageFrom(e) }); }
    finally { setSaving(false); }
  }

  async function transition(action) {
    setSaving(true); setNotice(null);
    try {
      await apiRequest(`/api/v1/attendance/sessions/${activeSession.id}/${action}`, { method: 'POST', token });
      setNotice({ type: 'success', text: action === 'submit' ? 'Attendance has been submitted.' : 'Attendance session has been locked.' });
      await load();
      setActiveSession((old) => old ? { ...old, status: action === 'submit' ? 'SUBMITTED' : 'LOCKED' } : old);
    } catch (e) { setNotice({ type: 'error', text: messageFrom(e) }); }
    finally { setSaving(false); }
  }

  async function createSession(e) {
    e.preventDefault(); setSaving(true); setNotice(null);
    try {
      const created = await apiRequest('/api/v1/attendance/sessions', { method: 'POST', token, body: { ...sessionDraft, sectionId: Number(sessionDraft.sectionId) } });
      setSessionForm(false); setNotice({ type: 'success', text: 'Attendance session created.' }); await load();
      if (created?.id) await openRoster(created);
    } catch (err) { setNotice({ type: 'error', text: messageFrom(err) }); }
    finally { setSaving(false); }
  }

  const filtered = useMemo(() => sessions.filter((s) => JSON.stringify(s).toLowerCase().includes(search.toLowerCase())), [sessions, search]);
  const presentCount = Object.values(marks).filter((v) => v === 'PRESENT').length;
  const absentCount = Object.values(marks).filter((v) => v === 'ABSENT').length;

  return <section className="ops-workspace">
    {notice && <Notice type={notice.type} onClose={() => setNotice(null)}>{notice.text}</Notice>}
    {activeSession ? <>
      <button className="ops-back" onClick={() => setActiveSession(null)}>← Back to attendance sessions</button>
      <SectionTitle eyebrow="CLASS REGISTER" title={`${activeSession.className || 'Class'} · ${activeSession.sectionName || `Section ${activeSession.sectionId}`}`} description={`${new Date(activeSession.attendanceDate + 'T00:00:00').toLocaleDateString('en-IN', { day: 'numeric', month: 'long', year: 'numeric' })} · ${pretty(activeSession.sessionCode)} · Session #${activeSession.id}`} action={<Badge value={activeSession.status}/>} />
      <div className="ops-stat-row"><div className="ops-stat"><b>{students.length}</b><span>Students</span></div><div className="ops-stat"><b>{presentCount}</b><span>Present</span></div><div className="ops-stat"><b>{absentCount}</b><span>Absent</span></div><div className="ops-stat"><b>{students.length ? Math.round(presentCount / students.length * 100) : 0}%</b><span>Present rate</span></div></div>
      <div className="ops-card"><div className="ops-card-head"><div><h3>Mark student attendance</h3><p>Choose a status for every enrolled student. Changes are saved to this session.</p></div><span className="ops-count">{students.length} enrolled</span></div>
        {rosterLoading ? <div className="ops-loading">Loading class register…</div> : students.length ? <div className="ops-roster">{students.map((s) => { const id = get(s, 'studentId', 'id'); const name = get(s, 'studentName', 'name', 'fullName') || `Student #${id}`; return <div className="ops-student-row" key={id}><div className="ops-student-avatar">{String(name).split(' ').map((x) => x[0]).slice(0,2).join('').toUpperCase()}</div><div className="ops-student-info"><b>{name}</b><small>{get(s, 'admissionNumber', 'rollNumber') || `Student ID ${id}`}</small></div><div className="ops-status-picker">{['PRESENT', 'ABSENT', 'LATE', 'EXCUSED'].map((status) => <button type="button" key={status} disabled={!canMark || activeSession.status !== 'DRAFT'} className={marks[id] === status ? `chosen ${status.toLowerCase()}` : ''} onClick={() => { setRecordsSaved(false); setMarks((old) => ({ ...old, [id]: status })); }}>{pretty(status)}</button>)}</div></div>; })}</div> : <Empty title="No enrolled students found" description="There are no active enrollments for this section. Check the class enrollment records."/>}
        <div className="ops-form-footer"><span className="ops-muted">{activeSession.status === 'DRAFT' ? 'Attendance can be edited until the session is submitted.' : 'This session is read-only.'}</span><div className="ops-actions">{canMark && activeSession.status === 'DRAFT' && <button className="ops-button primary" disabled={saving || rosterLoading || !students.length} onClick={saveRecords}>{saving ? 'Saving…' : 'Save attendance'}</button>}{canMark && activeSession.status === 'DRAFT' && <button className="ops-button secondary" disabled={saving || !students.length || !recordsSaved} onClick={() => transition('submit')}>Submit attendance →</button>}{canLock && activeSession.status === 'SUBMITTED' && <button className="ops-button primary" disabled={saving} onClick={() => transition('lock')}>Lock session</button>}</div></div>
      </div>
    </> : <>
      <SectionTitle eyebrow="DAILY OPERATIONS" title="Attendance sessions" description="Take the register, review student presence and submit completed sessions." action={canMark ? <button className="ops-button primary" onClick={() => setSessionForm(true)}>＋ New session</button> : null}/>
      <div className="ops-toolbar"><div className="ops-search"><span>⌕</span><input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search class, section or date…"/></div><button className="ops-button secondary" onClick={load} disabled={loading}>↻ Refresh</button></div>
      {error && <Notice type="error">{error}</Notice>}
      {loading ? <div className="ops-loading">Loading attendance sessions…</div> : filtered.length ? <div className="ops-session-grid">{filtered.map((s) => <article className="ops-session-card" key={s.id}><div className="ops-session-top"><span className="ops-session-symbol">✓</span><Badge value={s.status}/></div><h3>{s.className || 'Class'} · {s.sectionName || `Section ${s.sectionId}`}</h3><p>{s.attendanceDate ? new Date(s.attendanceDate + 'T00:00:00').toLocaleDateString('en-IN', { day:'numeric', month:'short', year:'numeric' }) : 'Date not set'} · {pretty(s.sessionCode)}</p><div className="ops-session-meta"><span>Teacher ID <b>{s.teacherId ?? '—'}</b></span><span>Session <b>#{s.id}</b></span></div><button className="ops-button secondary full" onClick={() => openRoster(s)}>{canMark && s.status === 'DRAFT' ? 'Take attendance →' : 'View register →'}</button></article>)}</div> : <Empty title="No attendance sessions yet" description="Create a session for an assigned class, then record attendance from the class register." action={canMark ? <button className="ops-button primary" onClick={() => setSessionForm(true)}>Create first session</button> : null}/>}
    </>}
    {sessionForm && <Modal title="Create attendance session" subtitle="Set the class and date for today's register." onClose={() => setSessionForm(false)}><form className="ops-form" onSubmit={createSession}><label>Section ID<input type="number" min="1" value={sessionDraft.sectionId} onChange={(e) => setSessionDraft({...sessionDraft, sectionId:e.target.value})} required/></label><label>Attendance date<input type="date" value={sessionDraft.attendanceDate} onChange={(e) => setSessionDraft({...sessionDraft, attendanceDate:e.target.value})} required/></label><label>Session<select value={sessionDraft.sessionCode} onChange={(e) => setSessionDraft({...sessionDraft, sessionCode:e.target.value})}><option value="FULL_DAY">Full day</option><option value="MORNING">Morning</option><option value="AFTERNOON">Afternoon</option></select></label><label>Notes<input value={sessionDraft.notes} onChange={(e) => setSessionDraft({...sessionDraft, notes:e.target.value})} placeholder="Optional note"/></label><div className="ops-form-footer"><button type="button" className="ops-button secondary" onClick={() => setSessionForm(false)}>Cancel</button><button className="ops-button primary" disabled={saving}>{saving ? 'Creating…' : 'Create session'}</button></div></form></Modal>}
  </section>;
}

export function ExaminationsWorkspace({ token, roles = [] }) {
  const [exams, setExams] = useState([]); const [loading, setLoading] = useState(true); const [error, setError] = useState(''); const [notice, setNotice] = useState(null); const [query, setQuery] = useState(''); const [showCreate, setShowCreate] = useState(false); const [saving, setSaving] = useState(false);
  const [draft, setDraft] = useState({ name: '', description: '', sectionId: '1', academicYear: '2026-2027', startDate: today(), endDate: today() });
  const admin = roles.includes('ADMIN');
  const load = useCallback(async () => { setLoading(true); setError(''); try { setExams(rowsFrom(await apiRequest('/api/v1/examinations', { token }))); } catch(e) { setError(messageFrom(e)); } finally { setLoading(false); } }, [token]);
  useEffect(() => { load(); }, [load]);
  async function create(e) { e.preventDefault(); setSaving(true); setNotice(null); try { await apiRequest('/api/v1/examinations', { method:'POST', token, body:{ ...draft, sectionId:Number(draft.sectionId) } }); setShowCreate(false); setNotice({type:'success',text:'Examination scheduled successfully.'}); await load(); } catch(err) { setNotice({type:'error',text:messageFrom(err)}); } finally { setSaving(false); } }
  const filtered = exams.filter((x) => JSON.stringify(x).toLowerCase().includes(query.toLowerCase()));
  return <section className="ops-workspace">{notice && <Notice type={notice.type} onClose={() => setNotice(null)}>{notice.text}</Notice>}<SectionTitle eyebrow="ASSESSMENT MANAGEMENT" title="Examinations" description="Schedule assessments and manage examination progress." action={admin && <button className="ops-button primary" onClick={() => setShowCreate(true)}>＋ Schedule examination</button>}/><div className="ops-toolbar"><div className="ops-search"><span>⌕</span><input value={query} onChange={(e)=>setQuery(e.target.value)} placeholder="Search examinations…"/></div><button className="ops-button secondary" onClick={load} disabled={loading}>↻ Refresh</button></div>{error && <Notice type="error">{error}</Notice>}{loading ? <div className="ops-loading">Loading examinations…</div> : filtered.length ? <div className="ops-table-wrap"><table className="ops-table"><thead><tr><th>Examination</th><th>Academic year</th><th>Section</th><th>Schedule</th><th>Status</th></tr></thead><tbody>{filtered.map((exam) => <tr key={exam.id}><td><b>{get(exam,'name','examName','title') || `Examination #${exam.id}`}</b><small>{exam.description || 'School assessment'}</small></td><td>{exam.academicYear || '2026–27'}</td><td>{get(exam,'sectionName','sectionId') || '—'}</td><td>{get(exam,'startDate','examDate') || '—'}{exam.endDate && exam.endDate !== exam.startDate ? ` – ${exam.endDate}` : ''}</td><td><Badge value={exam.status || 'SCHEDULED'}/></td></tr>)}</tbody></table></div> : <Empty title="No examinations scheduled" description="Create the first examination to organize subjects, enter marks and publish report cards." action={admin ? <button className="ops-button primary" onClick={() => setShowCreate(true)}>Schedule an examination</button> : null}/>}<div className="ops-info-strip"><span>ⓘ</span><p>Marks entry is available after an examination and its subject list have been configured. Teachers can only assess their assigned sections and subjects.</p></div>{showCreate && <Modal title="Schedule an examination" subtitle="Add the assessment details for the academic year." onClose={()=>setShowCreate(false)}><form className="ops-form" onSubmit={create}><label>Examination name<input value={draft.name} onChange={(e)=>setDraft({...draft,name:e.target.value})} placeholder="e.g. Unit Test 1" required/></label><label>Description<input value={draft.description} onChange={(e)=>setDraft({...draft,description:e.target.value})} placeholder="Optional description"/></label><div className="ops-form-grid"><label>Section ID<input type="number" min="1" value={draft.sectionId} onChange={(e)=>setDraft({...draft,sectionId:e.target.value})} required/></label><label>Academic year<input value={draft.academicYear} onChange={(e)=>setDraft({...draft,academicYear:e.target.value})} required/></label><label>Start date<input type="date" value={draft.startDate} onChange={(e)=>setDraft({...draft,startDate:e.target.value})} required/></label><label>End date<input type="date" value={draft.endDate} onChange={(e)=>setDraft({...draft,endDate:e.target.value})} min={draft.startDate} required/></label></div><div className="ops-form-footer"><button type="button" className="ops-button secondary" onClick={()=>setShowCreate(false)}>Cancel</button><button className="ops-button primary" disabled={saving}>{saving?'Saving…':'Schedule examination'}</button></div></form></Modal>}</section>;
}

export function FeesWorkspace({ token, roles = [] }) {
  const [invoices,setInvoices]=useState([]); const [loading,setLoading]=useState(true); const [error,setError]=useState(''); const [notice,setNotice]=useState(null); const [query,setQuery]=useState(''); const [showCreate,setShowCreate]=useState(false); const [paymentInvoice,setPaymentInvoice]=useState(null); const [saving,setSaving]=useState(false);
  const [draft,setDraft]=useState({studentId:'1',invoiceNumber:`INV-${new Date().getFullYear()}-003`,issueDate:today(),dueDate:today(),description:'Tuition fee',amount:'15000'}); const [payment,setPayment]=useState({amount:'',reference:''});
  const admin=roles.includes('ADMIN'); const guardian=roles.includes('GUARDIAN');
  const load=useCallback(async()=>{setLoading(true);setError('');try{setInvoices(rowsFrom(await apiRequest('/api/v1/fees/invoices',{token})));}catch(e){setError(messageFrom(e));}finally{setLoading(false);}},[token]); useEffect(()=>{load();},[load]);
  async function createInvoice(e){e.preventDefault();setSaving(true);setNotice(null);try{await apiRequest('/api/v1/fees/invoices',{method:'POST',token,body:{studentId:Number(draft.studentId),invoiceNumber:draft.invoiceNumber,issueDate:draft.issueDate,dueDate:draft.dueDate,items:[{description:draft.description,amount:Number(draft.amount)}]}});setShowCreate(false);setNotice({type:'success',text:'Fee invoice created as a draft.'});await load();}catch(err){setNotice({type:'error',text:messageFrom(err)});}finally{setSaving(false);}}
  async function issueInvoice(invoice){setSaving(true);setNotice(null);try{await apiRequest(`/api/v1/fees/invoices/${invoice.id}/issue`,{method:'POST',token});setNotice({type:'success',text:'Invoice issued successfully.'});await load();}catch(err){setNotice({type:'error',text:messageFrom(err)});}finally{setSaving(false);}}
  async function payInvoice(e){e.preventDefault();setSaving(true);setNotice(null);try{const key=payment.reference.trim() || (globalThis.crypto?.randomUUID?.() ?? `demo-${Date.now()}`);await apiRequest(`/api/v1/fees/invoices/${paymentInvoice.id}/payments`,{method:'POST',token,body:{amount:Number(payment.amount),method:'SIMULATED',idempotencyKey:key}});setPaymentInvoice(null);setNotice({type:'success',text:'Simulated payment recorded successfully.'});await load();}catch(err){setNotice({type:'error',text:messageFrom(err)});}finally{setSaving(false);}}
  const filtered=invoices.filter(x=>JSON.stringify(x).toLowerCase().includes(query.toLowerCase()));
  return <section className="ops-workspace">{notice&&<Notice type={notice.type} onClose={()=>setNotice(null)}>{notice.text}</Notice>}<SectionTitle eyebrow="SCHOOL FINANCE" title="Fee invoices" description="Issue student invoices, record simulated payments and track outstanding balances." action={admin&&<button className="ops-button primary" onClick={()=>setShowCreate(true)}>＋ Create invoice</button>}/><div className="ops-toolbar"><div className="ops-search"><span>⌕</span><input value={query} onChange={(e)=>setQuery(e.target.value)} placeholder="Search invoice or student…"/></div><button className="ops-button secondary" onClick={load} disabled={loading}>↻ Refresh</button></div>{error&&<Notice type="error">{error}</Notice>}{loading?<div className="ops-loading">Loading invoices…</div>:filtered.length?<div className="ops-table-wrap"><table className="ops-table"><thead><tr><th>Invoice</th><th>Student</th><th>Due date</th><th>Total</th><th>Paid</th><th>Balance</th><th>Status</th><th>Action</th></tr></thead><tbody>{filtered.map(inv=>{const total=get(inv,'totalAmount','total','grandTotal')??0;const paid=get(inv,'amountPaid','paidAmount','paid')??0;const balance=get(inv,'outstandingBalance','balance','remainingAmount')??(Number(total)-Number(paid));const status=inv.status || (Number(balance)<=0?'PAID':'ISSUED');return <tr key={inv.id}><td><b>{get(inv,'invoiceNumber','number')||`Invoice #${inv.id}`}</b><small>#{inv.id}</small></td><td>{get(inv,'studentName','student.name','studentId')||'—'}</td><td>{get(inv,'dueDate')||'—'}</td><td>{money(total)}</td><td>{money(paid)}</td><td><b>{money(balance)}</b></td><td><Badge value={status}/></td><td><div className="ops-inline-actions">{admin&&String(status).toUpperCase()==='DRAFT'&&<button className="ops-text-button" disabled={saving} onClick={()=>issueInvoice(inv)}>Issue</button>}{(guardian||admin)&&Number(balance)>0&&String(status).toUpperCase()!=='DRAFT'&&<button className="ops-text-button" onClick={()=>{setPaymentInvoice(inv);setPayment({amount:String(balance),reference:''});}}>Record payment</button>}</div></td></tr>})}</tbody></table></div>:<Empty title="No fee invoices yet" description="Create an invoice for a student. Issued invoices can accept simulated payments and show a live outstanding balance." action={admin?<button className="ops-button primary" onClick={()=>setShowCreate(true)}>Create first invoice</button>:null}/>}<div className="ops-info-strip"><span>ⓘ</span><p>Payments are simulated for assessment purposes. No real money is collected or transferred.</p></div>{showCreate&&<Modal title="Create fee invoice" subtitle="Add an invoice for a student and one fee item." onClose={()=>setShowCreate(false)}><form className="ops-form" onSubmit={createInvoice}><div className="ops-form-grid"><label>Student ID<input type="number" min="1" value={draft.studentId} onChange={(e)=>setDraft({...draft,studentId:e.target.value})} required/></label><label>Invoice number<input value={draft.invoiceNumber} onChange={(e)=>setDraft({...draft,invoiceNumber:e.target.value})} required/></label><label>Issue date<input type="date" value={draft.issueDate} onChange={(e)=>setDraft({...draft,issueDate:e.target.value})} required/></label><label>Due date<input type="date" value={draft.dueDate} onChange={(e)=>setDraft({...draft,dueDate:e.target.value})} min={draft.issueDate} required/></label></div><label>Fee description<input value={draft.description} onChange={(e)=>setDraft({...draft,description:e.target.value})} required/></label><label>Amount (INR)<input type="number" min="1" step="0.01" value={draft.amount} onChange={(e)=>setDraft({...draft,amount:e.target.value})} required/></label><div className="ops-form-footer"><button type="button" className="ops-button secondary" onClick={()=>setShowCreate(false)}>Cancel</button><button className="ops-button primary" disabled={saving}>{saving?'Creating…':'Create draft invoice'}</button></div></form></Modal>}{paymentInvoice&&<Modal title="Record simulated payment" subtitle={`Invoice ${paymentInvoice.invoiceNumber||`#${paymentInvoice.id}`} · ${money(get(paymentInvoice,'outstandingBalance','balance')??(Number(get(paymentInvoice,'totalAmount','total')||0)-Number(get(paymentInvoice,'amountPaid','paidAmount')||0)))}`} onClose={()=>setPaymentInvoice(null)}><form className="ops-form" onSubmit={payInvoice}><label>Payment amount (INR)<input type="number" min="0.01" step="0.01" value={payment.amount} onChange={(e)=>setPayment({...payment,amount:e.target.value})} required/></label><label>Reference (optional)<input value={payment.reference} onChange={(e)=>setPayment({...payment,reference:e.target.value})} placeholder="Leave blank to generate a unique reference"/></label><div className="ops-payment-note">✓ Simulation mode · No actual transaction will occur</div><div className="ops-form-footer"><button type="button" className="ops-button secondary" onClick={()=>setPaymentInvoice(null)}>Cancel</button><button className="ops-button primary" disabled={saving}>{saving?'Recording…':'Record payment'}</button></div></form></Modal>}</section>;
}

export function AuditWorkspace({ token }) {
  const [events,setEvents]=useState([]); const [loading,setLoading]=useState(true); const [error,setError]=useState(''); const [query,setQuery]=useState('');
  const load=useCallback(async()=>{setLoading(true);setError('');try{setEvents(rowsFrom(await apiRequest('/api/v1/audit-logs',{token})));}catch(e){setError(messageFrom(e));}finally{setLoading(false);}},[token]); useEffect(()=>{load();},[load]);
  const filtered=events.filter(x=>JSON.stringify(x).toLowerCase().includes(query.toLowerCase()));
  return <section className="ops-workspace"><SectionTitle eyebrow="GOVERNANCE & COMPLIANCE" title="Audit trail" description="A chronological record of important administrative and financial changes." action={<button className="ops-button secondary" onClick={load} disabled={loading}>↻ Refresh</button>}/><div className="ops-toolbar"><div className="ops-search"><span>⌕</span><input value={query} onChange={(e)=>setQuery(e.target.value)} placeholder="Search actions, records or users…"/></div><span className="ops-count">{events.length} recorded events</span></div>{error&&<Notice type="error">{error}</Notice>}{loading?<div className="ops-loading">Loading audit events…</div>:filtered.length?<div className="ops-audit-list">{filtered.map((event,i)=><article className="ops-audit-item" key={event.id||i}><div className="ops-audit-marker">◷</div><div className="ops-audit-content"><div className="ops-audit-heading"><b>{pretty(get(event,'action','actionType','eventType'))}</b><Badge value={get(event,'entityType','module')||'SYSTEM'}/></div><p>{get(event,'description','details','message')||`${pretty(event.entityType)} record ${event.entityId??''} updated`}</p><div className="ops-audit-meta"><span>{get(event,'actorEmail','performedBy','username')||'Authorized user'}</span><span>{get(event,'entityType','entity')||'Record'} #{get(event,'entityId','recordId')||'—'}</span><time>{event.createdAt?new Date(event.createdAt).toLocaleString('en-IN'):'Timestamp unavailable'}</time></div></div></article>)}</div>:<Empty title="No audit events recorded yet" description="Audit events appear here after tracked actions such as issuing a fee invoice or recording a payment. This page updates automatically when those actions are performed."/>}</section>;
}
