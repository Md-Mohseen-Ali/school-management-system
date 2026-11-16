import { useCallback, useEffect, useState } from 'react';
import { apiRequest, rowsFrom } from './api.js';

const display = value => value == null || value === '' ? '—' : String(value);
const formatDate = value => value ? new Date(value).toLocaleString('en-IN') : '—';
const messageFrom = error => error?.payload?.message || error?.message || 'The request could not be completed.';

export function PublicAccountRequest({ onBack }) {
  const [role, setRole] = useState('STUDENT');
  const [form, setForm] = useState({
    fullName:'', firstName:'', lastName:'', email:'', phone:'',
    employeeCode:'', hireDate:'', alternatePhone:'',
    admissionNumber:'', dateOfBirth:'', studentAdmissionNumber:'', relationship:''
  });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [submitted, setSubmitted] = useState(null);
  const update = (key, value) => setForm(current => ({...current, [key]:value}));

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError('');
    const body = {
      requestedRole: role,
      fullName: role === 'STUDENT' ? [form.firstName,form.lastName].filter(Boolean).join(' ') : form.fullName,
      email: form.email.trim(),
      phone: form.phone.trim() || null
    };
    if (role === 'TEACHER') {
      body.employeeCode = form.employeeCode.trim();
      body.hireDate = form.hireDate || null;
    } else if (role === 'GUARDIAN') {
      body.alternatePhone = form.alternatePhone.trim() || null;
      body.studentAdmissionNumber = form.studentAdmissionNumber.trim();
      body.relationship = form.relationship.trim();
    } else {
      body.admissionNumber = form.admissionNumber.trim();
      body.firstName = form.firstName.trim();
      body.lastName = form.lastName.trim();
      body.dateOfBirth = form.dateOfBirth || null;
    }
    try {
      const response = await apiRequest('/api/v1/account-requests', {method:'POST',body});
      setSubmitted(response?.data || response);
    } catch (e) {
      setError(messageFrom(e));
    } finally {
      setBusy(false);
    }
  }

  return <main className="access-public-screen">
    <section className="access-public-card">
      <div className="brand-lockup access-public-brand"><div className="brand-mark">N</div><div><strong>Northstar</strong><span> SCHOOL MANAGEMENT</span></div></div>
      {submitted ? <>
        <div className="access-success-mark">✓</div>
        <div className="eyebrow">REQUEST SUBMITTED</div>
        <h1>We have received your request.</h1>
        <p className="access-intro">Your portal account has not been created yet. The school administrator must verify your details and approve the request.</p>
        <div className="access-reference"><span>REQUEST REFERENCE</span><b>#{submitted.requestId}</b><small>Keep this reference and contact the school office for an update.</small></div>
        <button className="button primary full" onClick={onBack}>Back to sign in</button>
      </> : <>
        <div className="eyebrow">PORTAL ACCESS</div>
        <h1>Request an account</h1>
        <p className="access-intro">Submit your details for school verification. A login is created only after an administrator approves the request.</p>
        <form className="access-form" onSubmit={submit}>
          <label>Account type<select value={role} onChange={e=>setRole(e.target.value)}>
            <option value="STUDENT">Student</option><option value="GUARDIAN">Guardian / Parent</option><option value="TEACHER">Teacher</option>
          </select></label>
          {role !== 'STUDENT' && <label>Full name<input value={form.fullName} onChange={e=>update('fullName',e.target.value)} maxLength="120" required /></label>}
          {role === 'STUDENT' && <div className="access-form-two">
            <label>First name<input value={form.firstName} onChange={e=>update('firstName',e.target.value)} maxLength="80" required /></label>
            <label>Last name<input value={form.lastName} onChange={e=>update('lastName',e.target.value)} maxLength="80" required /></label>
          </div>}
          <div className="access-form-two">
            <label>Email address<input type="email" value={form.email} onChange={e=>update('email',e.target.value)} maxLength="180" required /></label>
            <label>Phone number<input type="tel" value={form.phone} onChange={e=>update('phone',e.target.value)} maxLength="25" /></label>
          </div>
          {role === 'TEACHER' && <>
            <label>Employee code<input value={form.employeeCode} onChange={e=>update('employeeCode',e.target.value)} maxLength="30" required /></label>
            <label>Hire date (if known)<input type="date" value={form.hireDate} onChange={e=>update('hireDate',e.target.value)} /></label>
          </>}
          {role === 'GUARDIAN' && <>
            <label>Student admission number<input value={form.studentAdmissionNumber} onChange={e=>update('studentAdmissionNumber',e.target.value)} maxLength="40" required /></label>
            <div className="access-form-two">
              <label>Relationship to student<input value={form.relationship} onChange={e=>update('relationship',e.target.value)} maxLength="30" placeholder="Parent, guardian…" required /></label>
              <label>Alternate phone<input type="tel" value={form.alternatePhone} onChange={e=>update('alternatePhone',e.target.value)} maxLength="25" /></label>
            </div>
          </>}
          {role === 'STUDENT' && <>
            <label>Admission number<input value={form.admissionNumber} onChange={e=>update('admissionNumber',e.target.value)} maxLength="40" required /></label>
            <label>Date of birth (optional)<input type="date" value={form.dateOfBirth} onChange={e=>update('dateOfBirth',e.target.value)} /></label>
          </>}
          {error && <div className="ops-notice error" role="alert">{error}</div>}
          <div className="access-form-actions"><button type="button" className="button outline" onClick={onBack}>Back</button><button className="button primary" disabled={busy}>{busy?'Submitting…':'Submit request'}</button></div>
        </form>
        <p className="access-privacy-note">Do not enter a password here. The school verifies your details and sets initial credentials after approval.</p>
      </>}
    </section>
  </main>;
}

export function AccountRequestsWorkspace({ token }) {
  const [status, setStatus] = useState('PENDING');
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState(null);
  const [review, setReview] = useState(null);
  const [initialPassword, setInitialPassword] = useState('');
  const [reviewNote, setReviewNote] = useState('');
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const query = status ? '?status='+encodeURIComponent(status) : '';
      setRequests(rowsFrom(await apiRequest('/api/v1/account-requests'+query,{token})));
    } catch(e) {
      setError(messageFrom(e));
    } finally {
      setLoading(false);
    }
  },[status,token]);

  useEffect(()=>{load();},[load]);

  function openReview(request, action) {
    setReview({request,action});
    setInitialPassword('');
    setReviewNote('');
    setNotice(null);
  }

  async function submitReview(event) {
    event.preventDefault();
    if (!review) return;
    setBusy(true);
    setNotice(null);
    try {
      const endpoint = '/api/v1/account-requests/'+review.request.id+'/'+review.action;
      const body = review.action === 'approve'
        ? {initialPassword,reviewNote:reviewNote.trim() || null}
        : {reviewNote:reviewNote.trim()};
      await apiRequest(endpoint,{method:'POST',token,body});
      setReview(null);
      setNotice({type:'success',text:review.action==='approve'
        ? 'Request approved and the account has been provisioned. Share the initial password securely.'
        : 'Request rejected. No login account was created.'});
      await load();
    } catch(e) {
      setNotice({type:'error',text:messageFrom(e)});
    } finally {
      setBusy(false);
    }
  }

  return <section className="access-admin-workspace">
    {notice && <div className={'ops-notice '+notice.type} role="status">{notice.text}<button onClick={()=>setNotice(null)}>×</button></div>}
    <div className="ops-title"><div><span className="eyebrow">IDENTITY AND ACCESS</span><h2>Account requests</h2><p>Review identity details before creating a teacher, guardian or student login.</p></div><button className="ops-button secondary" onClick={load} disabled={loading}>↻ Refresh</button></div>
    <div className="access-admin-toolbar">
      <div className="ops-tabs" role="tablist" aria-label="Request status">
        {[['PENDING','Pending'],['APPROVED','Approved'],['REJECTED','Rejected'],['','All']].map(([value,label])=><button key={label} role="tab" aria-selected={status===value} className={status===value?'active':''} onClick={()=>setStatus(value)}>{label}</button>)}
      </div>
      <span className="access-record-count">{requests.length} {status ? status.toLowerCase() : 'total'} request(s)</span>
    </div>
    {error && <div className="ops-notice error" role="alert">{error}</div>}
    {loading ? <div className="ops-loading">Loading account requests…</div> : requests.length ? <div className="access-request-list">
      {requests.map(request=><article className="access-request-card" key={request.id}>
        <div className="access-request-head">
          <div><span className="access-request-role">{request.requestedRole}</span><h3>{request.fullName}</h3><p>{request.email} · {request.phone || 'No phone provided'}</p></div>
          <span className={'access-request-status '+String(request.status).toLowerCase()}>{request.status}</span>
        </div>
        <div className="access-request-details">
          {request.requestedRole==='TEACHER' ? <><div><span>Employee code</span><b>{display(request.employeeCode)}</b></div><div><span>Hire date</span><b>{display(request.hireDate)}</b></div></> : request.requestedRole==='GUARDIAN' ? <><div><span>Student admission</span><b>{display(request.studentAdmissionNumber)}</b></div><div><span>Relationship</span><b>{display(request.relationship)}</b></div><div><span>Alternate phone</span><b>{display(request.alternatePhone)}</b></div></> : <><div><span>Admission number</span><b>{display(request.admissionNumber)}</b></div><div><span>Date of birth</span><b>{display(request.dateOfBirth)}</b></div><div><span>Applicant name</span><b>{display([request.firstName,request.lastName].filter(Boolean).join(' '))}</b></div></>}
          <div><span>Submitted</span><b>{formatDate(request.createdAt)}</b></div>
        </div>
        {request.reviewNote && <p className="access-review-note">Review note: {request.reviewNote}</p>}
        {request.status==='PENDING' && <div className="access-request-actions"><button className="ops-button secondary" onClick={()=>openReview(request,'reject')}>Reject</button><button className="ops-button primary" onClick={()=>openReview(request,'approve')}>Verify & approve</button></div>}
      </article>)}
    </div> : <div className="ops-empty"><div className="ops-empty-icon">✉</div><h3>No {status ? status.toLowerCase() : ''} requests</h3><p>New public requests will appear here for administrator review.</p></div>}
    {review && <div className="ops-modal-backdrop" role="presentation" onMouseDown={e=>{if(e.target===e.currentTarget&&!busy)setReview(null);}}>
      <section className="ops-modal access-review-modal" role="dialog" aria-modal="true" aria-labelledby="access-review-title">
        <div className="ops-modal-head"><div><h3 id="access-review-title">{review.action==='approve'?'Approve and provision account':'Reject account request'}</h3><p>{review.request.requestedRole} · {review.request.fullName} · {review.request.email}</p></div><button className="ops-close" onClick={()=>setReview(null)} disabled={busy} aria-label="Close">×</button></div>
        <form className="ops-form" onSubmit={submitReview}>
          {review.action==='approve' ? <label>Initial password<input type="password" value={initialPassword} onChange={e=>setInitialPassword(e.target.value)} minLength="12" maxLength="72" autoComplete="new-password" required/><small className="ops-muted">At least 12 characters. The password is hashed and is not included in the response.</small></label> : null}
          <label>{review.action==='approve'?'Administrator note (optional)':'Reason for rejection'}<textarea value={reviewNote} onChange={e=>setReviewNote(e.target.value)} maxLength="500" rows="3" required={review.action==='reject'} /></label>
          <div className="ops-form-footer"><button type="button" className="ops-button secondary" onClick={()=>setReview(null)} disabled={busy}>Cancel</button><button className="ops-button primary" disabled={busy || (review.action==='approve'&&initialPassword.length<12)}>{busy?'Saving…':review.action==='approve'?'Approve request':'Reject request'}</button></div>
        </form>
      </section>
    </div>}
  </section>;
}
