import { useCallback, useEffect, useState } from 'react';
import { apiRequest, rowsFrom } from './api.js';

const today = () => new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10);
const messageFrom = (e) => e?.payload?.message || e?.message || 'The request could not be completed.';

function Modal({ title, subtitle, onClose, children }) {
  return <div className="ops-modal-backdrop" role="presentation" onMouseDown={(e) => { if (e.target === e.currentTarget) onClose(); }}>
    <section className="ops-modal" role="dialog" aria-modal="true">
      <div className="ops-modal-head"><div><h3>{title}</h3><p>{subtitle}</p></div><button className="ops-close" onClick={onClose} aria-label="Close">×</button></div>
      {children}
    </section>
  </div>;
}

export function PeopleWorkspace({ token }) {
  const [tab, setTab] = useState('teachers');
  const [teachers, setTeachers] = useState([]);
  const [guardians, setGuardians] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState(null);
  const [showCreate, setShowCreate] = useState(false);
  const [draft, setDraft] = useState({
    fullName: '', email: '', phone: '', initialPassword: '',
    employeeCode: '', hireDate: today(), alternatePhone: ''
  });

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [teacherData, guardianData] = await Promise.all([
        apiRequest('/api/v1/identity/teachers', { token }),
        apiRequest('/api/v1/identity/guardians', { token })
      ]);
      setTeachers(rowsFrom(teacherData));
      setGuardians(rowsFrom(guardianData));
    } catch (e) {
      setError(messageFrom(e));
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => { load(); }, [load]);

  function openCreate(role) {
    setDraft({
      fullName: '', email: '', phone: '', initialPassword: '',
      employeeCode: '', hireDate: today(), alternatePhone: ''
    });
    setTab(role === 'TEACHER' ? 'teachers' : 'guardians');
    setShowCreate(true);
    setNotice(null);
  }

  async function submit(e) {
    e.preventDefault();
    setSaving(true);
    setNotice(null);
    const role = tab === 'teachers' ? 'TEACHER' : 'GUARDIAN';
    const body = {
      role,
      fullName: draft.fullName.trim(),
      email: draft.email.trim(),
      phone: draft.phone.trim() || null,
      initialPassword: draft.initialPassword
    };
    if (role === 'TEACHER') {
      body.employeeCode = draft.employeeCode.trim();
      body.hireDate = draft.hireDate || null;
    } else {
      body.alternatePhone = draft.alternatePhone.trim() || null;
    }
    try {
      await apiRequest('/api/v1/identity/accounts', { method: 'POST', token, body });
      setShowCreate(false);
      setNotice({ type: 'success', text: role === 'TEACHER'
        ? 'Teacher account created. Assign this teacher to sections and subjects next.'
        : 'Guardian account created. Link the guardian to the correct student next.' });
      await load();
    } catch (e) {
      setNotice({ type: 'error', text: messageFrom(e) });
    } finally {
      setSaving(false);
    }
  }

  const records = tab === 'teachers' ? teachers : guardians;
  return <section className="ops-workspace">
    {notice && <div className={'ops-notice ' + notice.type} role="status"><span>{notice.type === 'success' ? '✓' : '!'}</span><div>{notice.text}</div><button onClick={() => setNotice(null)}>×</button></div>}
    <div className="ops-title"><div><span className="eyebrow">PEOPLE & ACCESS</span><h2>People directory</h2><p>Create staff and family accounts, then connect them to the academic structure.</p></div><button className="ops-button primary" onClick={() => openCreate(tab === 'teachers' ? 'TEACHER' : 'GUARDIAN')}>＋ Add {tab === 'teachers' ? 'teacher' : 'guardian'}</button></div>
    <div className="ops-toolbar">
      <div className="ops-tabs" role="tablist" aria-label="People type">
        <button role="tab" aria-selected={tab === 'teachers'} className={tab === 'teachers' ? 'active' : ''} onClick={() => setTab('teachers')}>Teachers <span>{teachers.length}</span></button>
        <button role="tab" aria-selected={tab === 'guardians'} className={tab === 'guardians' ? 'active' : ''} onClick={() => setTab('guardians')}>Guardians <span>{guardians.length}</span></button>
      </div>
      <button className="ops-button secondary" onClick={load} disabled={loading}>↻ Refresh</button>
    </div>
    {error && <div className="ops-notice error"><span>!</span><div>{error}</div></div>}
    {loading ? <div className="ops-loading">Loading people directory…</div> : records.length ? <div className="ops-people-grid">
      {records.map((person) => <article className="ops-person-card" key={person.id}>
        <div className="ops-person-avatar">{String(person.fullName || person.email || '?').split(/\s+/).slice(0, 2).map(x => x[0]).join('').toUpperCase()}</div>
        <div className="ops-person-main">
          <h3>{person.fullName || 'Unnamed account'}</h3>
          <p>{person.email || 'Email not available'}</p>
          <div className="ops-person-tags">
            <span>{tab === 'teachers' ? person.employeeCode : 'Guardian account'}</span>
            <span>{tab === 'teachers' ? (person.active ? 'Active' : 'Inactive') : (person.alternatePhone || 'No alternate phone')}</span>
          </div>
          <small>{person.phone || 'No phone number'} {tab === 'teachers' && person.hireDate ? '· Joined ' + person.hireDate : ''}</small>
        </div>
      </article>)}
    </div> : <div className="ops-empty"><div className="ops-empty-icon">♙</div><h3>No {tab} registered yet</h3><p>Create an account here, then assign teachers or link guardians from Academic Administration.</p><button className="ops-button primary" onClick={() => openCreate(tab === 'teachers' ? 'TEACHER' : 'GUARDIAN')}>Add first {tab === 'teachers' ? 'teacher' : 'guardian'}</button></div>}
    <div className="ops-info-strip"><span>ⓘ</span><p>Initial passwords are stored as BCrypt hashes. Share temporary credentials directly with the account holder; never place them in this directory or in project documentation.</p></div>

    {showCreate && <Modal title={tab === 'teachers' ? 'Create teacher account' : 'Create guardian account'}
      subtitle={tab === 'teachers' ? 'A new teacher can be assigned to classes and subjects after creation.' : 'A guardian can be linked to one or more students after creation.'}
      onClose={() => setShowCreate(false)}>
      <form className="ops-form" onSubmit={submit}>
        <label>Full name<input value={draft.fullName} onChange={e => setDraft({...draft,fullName:e.target.value})} maxLength="120" required /></label>
        <label>Email address<input type="email" value={draft.email} onChange={e => setDraft({...draft,email:e.target.value})} maxLength="180" required /></label>
        <label>Phone number<input type="tel" value={draft.phone} onChange={e => setDraft({...draft,phone:e.target.value})} maxLength="25" /></label>
        {tab === 'teachers' ? <>
          <label>Employee code<input value={draft.employeeCode} onChange={e => setDraft({...draft,employeeCode:e.target.value})} maxLength="30" required /></label>
          <label>Hire date<input type="date" value={draft.hireDate} onChange={e => setDraft({...draft,hireDate:e.target.value})} /></label>
        </> : <label>Alternate phone<input type="tel" value={draft.alternatePhone} onChange={e => setDraft({...draft,alternatePhone:e.target.value})} maxLength="25" /></label>}
        <label>Initial password<input type="password" value={draft.initialPassword} onChange={e => setDraft({...draft,initialPassword:e.target.value})} minLength="12" maxLength="72" autoComplete="new-password" required /><small className="ops-muted">At least 12 characters. It cannot be recovered from the system after creation.</small></label>
        <div className="ops-form-footer"><button type="button" className="ops-button secondary" onClick={() => setShowCreate(false)}>Cancel</button><button className="ops-button primary" disabled={saving}>{saving ? 'Creating account…' : 'Create account'}</button></div>
      </form>
    </Modal>}
  </section>;
}
