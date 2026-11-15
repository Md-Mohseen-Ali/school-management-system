import { useEffect, useMemo, useState } from 'react';
import { apiRequest, decodeJwt, extractToken, readRoles, rowsFrom } from './api.js';
import { AttendanceWorkspace, ExaminationsWorkspace, FeesWorkspace, AuditWorkspace, StudentResultsWorkspace, StudentAttendanceWorkspace, StudentFeesWorkspace, GuardianAttendanceWorkspace, TeacherClassReportsWorkspace } from './OperationalWorkspaces.jsx';
import { AcademicWorkspace } from './AcademicWorkspace.jsx';
import { PeopleWorkspace } from './PeopleWorkspace.jsx';

const NAV = [
  { id: 'overview', label: 'Overview', group: 'Workspace', icon: '◫', path: null, roles: ['ADMIN', 'TEACHER', 'GUARDIAN', 'STUDENT'] },
  { id: 'students', label: 'Students', group: 'Academic', icon: '♙', path: '/api/v1/academic/students', roles: ['ADMIN'] },
  { id: 'classes', label: 'Classes', group: 'Academic', icon: '▦', path: '/api/v1/academic/classes', roles: ['ADMIN'] },
  { id: 'sections', label: 'Sections', group: 'Academic', icon: '▤', path: '/api/v1/academic/sections', roles: ['ADMIN'] },
  { id: 'subjects', label: 'Subjects', group: 'Academic', icon: '▧', path: '/api/v1/academic/subjects', roles: ['ADMIN'] },
  { id: 'enrollments', label: 'Enrollments', group: 'Academic', icon: '⇄', path: '/api/v1/academic/enrollments', roles: ['ADMIN'] },
  { id: 'assignments', label: 'Teacher assignments', group: 'Academic', icon: '▣', path: '/api/v1/academic/teacher-assignments', roles: ['ADMIN'] },
  { id: 'guardians', label: 'Family links', group: 'Academic', icon: '♧', path: '/api/v1/academic/student-guardians', roles: ['ADMIN'] },
  { id: 'people', label: 'People directory', group: 'Academic', icon: '♙', path: '/api/v1/identity/teachers', roles: ['ADMIN'] },
  { id: 'attendance', label: 'Attendance', group: 'Operations', icon: '✓', path: '/api/v1/attendance/sessions', roles: ['ADMIN', 'TEACHER'] },
  { id: 'examinations', label: 'Examinations', group: 'Operations', icon: '✎', path: '/api/v1/examinations', roles: ['ADMIN', 'TEACHER'] },
  { id: 'classreports', label: 'Class reports', group: 'Operations', icon: '▥', path: '/api/v1/attendance/reports/sections', roles: ['TEACHER'] },
  { id: 'fees', label: 'Fee invoices', group: 'Finance', icon: '₹', path: '/api/v1/fees/invoices', roles: ['ADMIN', 'GUARDIAN'] },
  { id: 'reportcard', label: 'Student report cards', group: 'Family', icon: '▤', path: null, roles: ['GUARDIAN'] },
  { id: 'familyattendance', label: 'Student attendance', group: 'Family', icon: '✓', path: '/api/v1/guardian/students', roles: ['GUARDIAN'] },
  { id: 'myattendance', label: 'My attendance', group: 'Learning', icon: '✓', path: '/api/v1/student/attendance/summary', roles: ['STUDENT'] },
  { id: 'myfees', label: 'My fees', group: 'Learning', icon: '₹', path: '/api/v1/student/fees/invoices', roles: ['STUDENT'] },
  { id: 'myresults', label: 'My results', group: 'Learning', icon: '▤', path: '/api/v1/student/examinations', roles: ['STUDENT'] },
  { id: 'audit', label: 'Audit trail', group: 'Governance', icon: '◷', path: '/api/v1/audit-logs', roles: ['ADMIN'] },
  { id: 'profile', label: 'My profile', group: 'Account', icon: '◎', path: null, roles: ['ADMIN', 'TEACHER', 'GUARDIAN', 'STUDENT'] },
];

const TITLES = {
  overview: ['Good to see you', 'A clear view of your school operations.'],
  students: ['Students', 'Student directory and academic records.'],
  classes: ['Classes', 'Manage class structures and academic years.'],
  sections: ['Sections', 'Organize students into class sections.'],
  subjects: ['Subjects', 'Maintain the school subject catalogue.'],
  enrollments: ['Enrollments', 'Track student placement by academic year.'],
  assignments: ['Teacher assignments', 'Manage class and subject responsibilities.'],
  guardians: ['Family links', 'Connect students with their authorized guardians.'],
  people: ['People directory', 'Create and review teacher and guardian accounts.'],
  attendance: ['Attendance', 'Review attendance sessions and status.'],
  examinations: ['Examinations', 'Manage exams, marks and results.'],
  classreports: ['Class reports', 'Review attendance trends for your assigned sections.'],
  fees: ['Fee invoices', 'Review invoices, payments and balances.'],
  audit: ['Audit trail', 'Trace important administrative actions.'],
  reportcard: ['Student report cards', 'View examination results for students linked to your account.'],
  familyattendance: ['Student attendance', 'Review attendance for students linked to your guardian account.'],
  myattendance: ['My attendance', 'Track your attendance rate and review class registers.'],
  myfees: ['My fees', 'Review your issued invoices and outstanding balance.'],
  myresults: ['My results', 'View your own published examination results.'],
  profile: ['My profile', 'Your account details, school role and linked academic information.'],
  profile: ['Your profile', 'Review your signed-in account and session details.'],
};

const DEMO_LOGINS = [
  { role: 'Administrator', email: 'admin@school.demo', password: 'Admin@2026School' },
  { role: 'Teacher', email: 'teacher@school.demo', password: 'Teacher@2026School' },
  { role: 'Guardian', email: 'guardian1@school.demo', password: 'Guardian@2026School' },
  { role: 'Student', email: 'student1@school.demo', password: 'Student@2026School' },
];

function initials(value = 'School User') {
  return value.split(/[\s.@_-]+/).filter(Boolean).slice(0, 2).map((part) => part[0].toUpperCase()).join('') || 'SU';
}

function friendlyRole(roles) {
  return roles[0] ? roles[0][0] + roles[0].slice(1).toLowerCase() : 'User';
}

function Login({ onLogin }) {
  const [email, setEmail] = useState('admin@school.demo');
  const [password, setPassword] = useState('Admin@2026School');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError('');
    try {
      const response = await apiRequest('/api/v1/auth/login', {
        method: 'POST', body: { email, password },
      });
      const token = extractToken(response);
      if (!token) throw new Error('Login succeeded but no JWT token was found in the response. Check the login response contract.');
      onLogin(token, response);
    } catch (err) {
      setError(err.message || 'Unable to sign in. Check that the backend is running.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="login-screen">
      <section className="login-brand">
        <div className="brand-lockup"><div className="brand-mark">N</div><div><strong>Northstar</strong><span> SCHOOL MANAGEMENT</span></div></div>
        <div className="brand-message">
          <div className="eyebrow light">SCHOOL OPERATIONS, IN SYNC</div>
          <h1>More clarity.<br /><em>Less admin.</em></h1>
          <p>A connected workspace for academic operations, attendance, examinations and fee management.</p>
          <div className="brand-points"><span>✦ Academic records</span><span>✦ Attendance insights</span><span>✦ Secure family access</span></div>
        </div>
        <div className="login-footnote">ALGIONS AI LABS · FULL-STACK PROJECT</div>
      </section>
      <section className="login-panel">
        <div className="login-form-wrap">
          <div className="mobile-lockup brand-lockup"><div className="brand-mark">N</div><div><strong>Northstar</strong><span> SCHOOL MANAGEMENT</span></div></div>
          <div className="eyebrow">WELCOME BACK</div>
          <h2>Sign in to your portal</h2>
          <p className="muted">Use your assigned school account to continue.</p>
          <form onSubmit={submit} className="login-form">
            <label>Email address<input type="email" autoComplete="username" value={email} onChange={(e) => setEmail(e.target.value)} required /></label>
            <label>Password<input type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} required /></label>
            {error && <div className="alert error">{error}</div>}
            <button className="button primary full" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'} <span>→</span></button>
          </form>
          <details className="demo-accounts"><summary>Demo account credentials</summary><div className="demo-list">{DEMO_LOGINS.map((item) => <button type="button" key={item.role} onClick={() => { setEmail(item.email); setPassword(item.password); }}><b>{item.role}</b><span>{item.email}</span></button>)}</div><small>Dummy records only. Use these accounts for assessment demonstration.</small></details>
        </div>
      </section>
    </main>
  );
}

function Sidebar({ active, setActive, roles, user, onLogout }) {
  const visible = NAV.filter((item) => item.roles.some((role) => roles.includes(role)));
  const groups = [...new Set(visible.map((item) => item.group))];
  return <aside className="sidebar">
    <div className="brand-lockup side-brand"><div className="brand-mark">N</div><div><strong>Northstar</strong><span> SCHOOL MANAGEMENT</span></div></div>
    <div className="school-switch"><div className="school-crest">NS</div><div><b>Northstar Academy</b><span>Academic year 2026–27</span></div><span className="chevron">⌄</span></div>
    <nav className="nav-list">
      {groups.map((group) => <div className="nav-group" key={group}><div className="nav-caption">{group}</div>{visible.filter((item) => item.group === group).map((item) => <button key={item.id} className={`nav-item ${active === item.id ? 'selected' : ''}`} onClick={() => setActive(item.id)}><span className="nav-icon">{item.icon}</span>{item.label}{active === item.id && <span className="nav-active-dot" />}</button>)}</div>)}
    </nav>
    <div className="sidebar-bottom">
      <div className="help-card"><div className="help-symbol">?</div><b>Need a hand?</b><span>School operations workspace</span></div>
      <div className="user-row"><div className="avatar">{initials(user?.email || 'School User')}</div><div className="user-meta"><b>{user?.email || 'School user'}</b><span>{friendlyRole(roles)}</span></div><button className="icon-button logout" title="Sign out" onClick={onLogout}>↗</button></div>
    </div>
  </aside>;
}

function MetricCard({ label, value, note, icon, tint }) {
  return <article className="metric-card"><div className={`metric-icon ${tint}`}>{icon}</div><div className="metric-label">{label}</div><div className="metric-value">{value}</div><div className="metric-note">{note}</div></article>;
}

function dateOffset(days) {
  const value = new Date();
  value.setDate(value.getDate() - days);
  return new Date(value.getTime() - value.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
}
function formatCurrency(value) {
  return new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR',maximumFractionDigits:0}).format(Number(value)||0);
}
function Overview({ token, roles, user, setActive }) {
  const [stats, setStats] = useState({});
  const [portal, setPortal] = useState({});
  const [health, setHealth] = useState('Checking');
  const [recent, setRecent] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      const admin = roles.includes('ADMIN');
      const teacher = roles.includes('TEACHER');
      const guardian = roles.includes('GUARDIAN');
      const student = roles.includes('STUDENT');
      const jobs = admin ? [
        ['students', '/api/v1/academic/students'], ['classes', '/api/v1/academic/classes'],
        ['sessions', '/api/v1/attendance/sessions'], ['invoices', '/api/v1/fees/invoices'],
      ] : teacher ? [
        ['sessions', '/api/v1/attendance/sessions'], ['exams', '/api/v1/teacher/examinations'],
      ] : guardian ? [['invoices', '/api/v1/fees/invoices']] : [['exams', '/api/v1/student/examinations']];
      const values = await Promise.all(jobs.map(async ([key, path]) => {
        try {
          const response = await apiRequest(path + (path.includes('?') ? '&' : '?') + 'size=1', { token });
          return [key, { count: response?.totalElements ?? rowsFrom(response).length }];
        } catch { return [key, null]; }
      }));
      let extra = {};
      if (student) {
        try {
          const fromDate = dateOffset(90);
          const toDate = new Date().toISOString().slice(0,10);
          const results = await Promise.all([
            apiRequest('/api/v1/student/profile', { token }),
            apiRequest('/api/v1/student/attendance/summary?fromDate='+fromDate+'&toDate='+toDate,{token}),
            apiRequest('/api/v1/student/fees/invoices?page=0&size=100',{token})
          ]);
          const profileData = results[0];
          const attendance = results[1];
          const invoices = rowsFrom(results[2]);
          extra = {
            name: [profileData.firstName,profileData.lastName].filter(Boolean).join(' '),
            attendance: attendance.attendancePercentage == null ? 0 : attendance.attendancePercentage,
            outstanding: invoices.reduce((sum,x)=>sum+Number(x.balance||0),0),
            className: [profileData.className,profileData.sectionName].filter(Boolean).join(' · ')
          };
        } catch { extra = {}; }
      } else if (guardian) {
        try {
          const results = await Promise.all([
            apiRequest('/api/v1/guardian/students',{token}),
            apiRequest('/api/v1/fees/invoices?page=0&size=100',{token})
          ]);
          extra = {
            children: rowsFrom(results[0]).length,
            outstanding: rowsFrom(results[1]).reduce((sum,x)=>sum+Number(x.balance||0),0)
          };
        } catch { extra = {}; }
      } else if (teacher) {
        try {
          const sections = await apiRequest('/api/v1/teacher/roster/sections',{token});
          extra = { sections: rowsFrom(sections).length };
        } catch { extra = {}; }
      }
      let audit = [];
      if (admin) {
        try { audit = rowsFrom(await apiRequest('/api/v1/audit-logs', { token })).slice(0, 5); } catch { audit = []; }
      }
      let status = 'Unavailable';
      try {
        const h = await apiRequest('/actuator/health');
        status = h?.status || 'Available';
      } catch { status = 'Unavailable'; }
      if (!cancelled) {
        setStats(Object.fromEntries(values));
        setPortal(extra);
        setRecent(audit);
        setHealth(status);
        setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, [token, roles]);

  const count = (key) => stats[key] == null ? '—' : stats[key].count;
  const cards = roles.includes('ADMIN') ? [
    ['Students', count('students'), 'Student directory', '♙', 'violet'],
    ['Classes', count('classes'), 'Academic structure', '▦', 'blue'],
    ['Attendance sessions', count('sessions'), 'Recorded registers', '✓', 'green'],
    ['Fee invoices', count('invoices'), 'Financial records', '₹', 'amber'],
  ] : roles.includes('TEACHER') ? [
    ['Assigned sections', loading ? '…' : (portal.sections ?? '—'), 'Your teaching responsibility', '▦', 'blue'],
    ['Attendance sessions', count('sessions'), 'Your recorded registers', '✓', 'green'],
    ['Examinations', count('exams'), 'Assessment workspace', '✎', 'violet'],
  ] : roles.includes('GUARDIAN') ? [
    ['Linked students', loading ? '…' : (portal.children ?? '—'), 'Family access', '♧', 'blue'],
    ['Fee balance', loading ? '…' : formatCurrency(portal.outstanding), 'Across issued invoices', '₹', 'amber'],
  ] : [
    ['Published examinations', count('exams'), 'Available results', '▤', 'blue'],
    ['Attendance rate', loading ? '…' : (portal.attendance == null ? '—' : portal.attendance+'%'), 'Last 90 days', '✓', 'green'],
    ['Outstanding fees', loading ? '…' : formatCurrency(portal.outstanding), 'Issued invoice balance', '₹', 'amber'],
  ];

  const dayLabel = new Intl.DateTimeFormat('en-IN',{weekday:'long',day:'2-digit',month:'long',year:'numeric'}).format(new Date()).toUpperCase();
  const greeting = portal.name || user?.fullName || (user?.email ? user.email.split('@')[0] : 'there');

  return <>
    <div className="welcome-strip"><div><span className="eyebrow">{dayLabel}</span><h2>{roles.includes('ADMIN') ? 'School at a glance' : 'Welcome, ' + greeting}</h2><p>{portal.className ? portal.className+' · ' : ''}Here is your school workspace at a glance.</p></div><div className="health-pill"><span className={'status-dot ' + (health.toLowerCase() === 'up' || health.toLowerCase() === 'available' ? 'online' : '')} />API {health}</div></div>
    <div className={'metrics-grid ' + (cards.length === 2 ? 'two' : '')}>{cards.map((card) => <MetricCard key={card[0]} label={card[0]} value={loading ? '…' : card[1]} note={card[2]} icon={card[3]} tint={card[4]} />)}</div>
    <div className="content-grid">
      <section className="panel quick-panel"><div className="panel-heading"><div><div className="eyebrow">QUICK ACCESS</div><h3>Your workspace</h3></div><span className="soft-tag">{friendlyRole(roles)} view</span></div><div className="quick-grid">
        {NAV.filter((x) => x.id !== 'overview' && x.id !== 'profile' && x.roles.some((r) => roles.includes(r))).slice(0, 6).map((item) => <button key={item.id} className="quick-link" onClick={() => setActive(item.id)}><span>{item.icon}</span><div><b>{item.label}</b><small>Open workspace</small></div><i>↗</i></button>)}
      </div></section>
      <section className="panel activity-panel"><div className="panel-heading"><div><div className="eyebrow">SYSTEM ACTIVITY</div><h3>Recent audit events</h3></div><span className="soft-tag">Latest 5</span></div>{roles.includes('ADMIN') ? (recent.length ? <div className="activity-list">{recent.map((item, index) => <div className="activity-item" key={item.id || index}><span className="activity-dot" /><div><b>{item.action || item.actionType || 'Business event'}</b><small>{item.entityType || 'Record'} · #{item.entityId ?? '—'}</small></div><time>{item.occurredAt ? new Date(item.occurredAt).toLocaleTimeString([], {hour:'2-digit',minute:'2-digit'}) : 'Logged'}</time></div>)}</div> : <EmptyState compact text={loading ? 'Loading activity…' : 'No audit events could be loaded.'} />) : <div className="quiet-note"><div className="quiet-icon">◷</div><b>Activity is private</b><p>{roles.includes('STUDENT') ? 'Use your learning workspace to review attendance, fee status and published results.' : roles.includes('GUARDIAN') ? 'Use Family access to review only students linked to your account.' : 'Administrative audit events are visible to authorized administrators only.'}</p></div>}</section>
    </div>
  </>;
}

function EmptyState({ text = 'No records found.', compact = false }) {
  return <div className={`empty-state ${compact ? 'compact' : ''}`}><div className="empty-icon">⌕</div><b>{text}</b><span>Try refreshing or check your access permissions.</span></div>;
}

function DataWorkspace({ item, token }) {
  const [payload, setPayload] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [query, setQuery] = useState('');
  useEffect(() => { setPayload(null); setError(''); }, [item.id, item.path]);

  async function runRequest() {
    setBusy(true); setError('');
    try {
      const result = await apiRequest(item.path, { token });
      setPayload(result);
    } catch (err) {
      setError(`${err.status ? `${err.status} · ` : ''}${err.message}`);
      setPayload(err.payload || null);
    } finally { setBusy(false); }
  }

  useEffect(() => { runRequest(); }, [item.path, token]);

  const rows = useMemo(() => rowsFrom(payload), [payload]);
  const filtered = useMemo(() => {
    if (!query.trim()) return rows;
    const q = query.toLowerCase();
    return rows.filter((row) => JSON.stringify(row).toLowerCase().includes(q));
  }, [rows, query]);
  const columns = useMemo(() => rows.length ? Object.keys(rows[0]).slice(0, 6) : [], [rows]);

  return <section className="workspace-page">
    <div className="workspace-toolbar"><div className="record-count">{rows.length ? `${filtered.length} of ${rows.length} records` : 'Live API data'}</div><div className="toolbar-actions"><button className="button primary" onClick={() => runRequest()} disabled={busy}>{busy ? <><span className="spinner"/> Loading</> : '↻ Refresh data'}</button></div></div>
    {error && <div className="alert error"><b>API request failed</b><span>{error}</span></div>}
    <div className="panel data-panel"><div className="data-header"><div className="search-box"><span>⌕</span><input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search records…" /></div></div>
      {rows.length > 0 ? <div className="table-scroll"><table><thead><tr>{columns.map((key) => <th key={key}>{key.replace(/([A-Z])/g, ' $1')}</th>)}</tr></thead><tbody>{filtered.map((row, index) => <tr key={row.id ?? row.studentId ?? index}>{columns.map((key) => <td key={key}>{formatCell(row[key])}</td>)}</tr>)}</tbody></table></div> : <EmptyState text={busy ? 'Loading records…' : error ? 'Records are unavailable for this account.' : 'No records returned by this endpoint.'} />}
    </div>
  </section>;
}

function formatCell(value) {
  if (value == null || value === '') return '—';
  if (typeof value === 'object') return Array.isArray(value) ? `${value.length} items` : JSON.stringify(value);
  if (typeof value === 'boolean') return value ? 'Yes' : 'No';
  return String(value).length > 70 ? `${String(value).slice(0, 67)}…` : String(value);
}


function GuardianReportCard({ token }) {
  const [students, setStudents] = useState([]);
  const [exams, setExams] = useState([]);
  const [studentId, setStudentId] = useState('');
  const [examId, setExamId] = useState('');
  const [result, setResult] = useState(null);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let live = true;
    setLoading(true);
    Promise.all([
      apiRequest('/api/v1/guardian/students', { token }),
      apiRequest('/api/v1/guardian/examinations', { token }),
    ]).then(([studentData, examData]) => {
      if (!live) return;
      const linked = rowsFrom(studentData);
      const published = rowsFrom(examData);
      setStudents(linked);
      setExams(published);
      setStudentId(linked[0]?.id ? String(linked[0].id) : '');
      setExamId(published[0]?.id ? String(published[0].id) : '');
      setError('');
    }).catch((e) => { if (live) setError(e.message || 'Could not load linked students and published examinations.'); })
      .finally(() => { if (live) setLoading(false); });
    return () => { live = false; };
  }, [token]);

  async function lookup(event) {
    event.preventDefault();
    if (!examId || !studentId) return;
    setBusy(true); setError(''); setResult(null);
    try {
      const data = await apiRequest('/api/v1/guardian/examinations/' + encodeURIComponent(examId) + '/students/' + encodeURIComponent(studentId) + '/report-card', { token });
      setResult(data);
    } catch (err) {
      setError(err.message || 'Report card could not be retrieved.');
      setResult(null);
    } finally { setBusy(false); }
  }

  return <section className="panel report-lookup">
    <div className="panel-heading"><div><div className="eyebrow">FAMILY ACCESS</div><h3>Linked student report card</h3></div><span className="soft-tag">Guardian only</span></div>
    <p className="report-intro">Only students linked to your guardian account and examinations with published results are available here.</p>
    {loading ? <EmptyState compact text="Loading your linked students and published results…"/> : students.length && exams.length ? <form className="lookup-form" onSubmit={lookup}>
      <label>Student<select value={studentId} onChange={(e) => { setStudentId(e.target.value); setResult(null); }} required>{students.map((student) => <option key={student.id} value={student.id}>{student.admissionNumber} · {student.firstName} {student.lastName}</option>)}</select></label>
      <label>Published examination<select value={examId} onChange={(e) => { setExamId(e.target.value); setResult(null); }} required>{exams.map((exam) => <option key={exam.id} value={exam.id}>{exam.examName} · {exam.academicYear}</option>)}</select></label>
      <button className="button primary" disabled={busy}>{busy ? 'Retrieving…' : 'View report card →'}</button>
    </form> : <EmptyState compact text={students.length ? 'No published examinations are available for your linked students yet.' : 'No students are linked to this guardian account.'}/>}
    {error && <div className="alert error"><b>Report card unavailable</b><span>{error}</span></div>}
    {result && <div className="report-result"><div className="result-summary"><div><span>STUDENT</span><b>{result.studentName || '—'}</b></div><div><span>PERCENTAGE</span><b>{result.percentage ?? '—'}{result.percentage != null ? '%' : ''}</b></div><div><span>GRADE</span><b>{result.grade || '—'}</b></div><div><span>RESULT</span><b>{result.passed ? 'Passed' : 'Not passed'}</b></div></div><div className="ops-table-wrap"><table className="ops-table"><thead><tr><th>Subject</th><th>Marks</th><th>Maximum</th><th>Pass marks</th><th>Result</th></tr></thead><tbody>{(result.subjects || []).map((subject) => <tr key={subject.examinationSubjectId}><td>{subject.subjectName}</td><td>{subject.marksObtained}</td><td>{subject.maxMarks}</td><td>{subject.passMarks}</td><td><span className={'ops-badge ' + (subject.passed ? 'good' : 'warn')}>{subject.passed ? 'Passed' : 'Failed'}</span></td></tr>)}</tbody></table></div></div>}
  </section>;
}

function ProfilePage({ user, roles, token }) {
  const [profile, setProfile] = useState(user || null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let alive = true;
    apiRequest('/api/v1/auth/me', { token })
      .then(value => { if (alive) { setProfile(value?.data || value); setError(''); } })
      .catch(e => { if (alive) setError(e.message || 'Account details could not be loaded.'); })
      .finally(() => { if (alive) setLoading(false); });
    return () => { alive = false; };
  }, [token]);

  const details = profile?.profile || {};
  const displayName = profile?.fullName || user?.fullName || user?.email || 'School user';
  const initialsText = initials(displayName);
  const roleLabel = roles.map(friendlyRole).join(' · ') || 'User';
  const field = (label, value) => <div className="profile-detail" key={label}><span>{label}</span><b>{value == null || value === '' ? 'Not provided' : String(value)}</b></div>;

  return <section className="profile-page">
    <div className="profile-hero">
      <div className="profile-hero-main">
        <div className="profile-avatar-large">{initialsText}</div>
        <div><span className="eyebrow light">ACCOUNT PROFILE</span><h2>{displayName}</h2><p>{profile?.email || user?.email || 'Email unavailable'}</p><div className="profile-role-tags">{roles.map(role=><span key={role}>{friendlyRole([role])}</span>)}</div></div>
      </div>
      <div className="profile-hero-status"><span className="profile-status-dot"/>Account {profile?.enabled === false ? 'disabled' : 'active'}</div>
    </div>

    {error && <div className="ops-notice warning"><span>!</span><div>{error}</div></div>}
    {loading ? <div className="ops-loading">Loading account details…</div> : <>
      <section className="profile-section">
        <div className="profile-section-heading"><div><span className="eyebrow">PERSONAL INFORMATION</span><h3>Account details</h3></div><span className="profile-readonly">Read only</span></div>
        <div className="profile-details-grid">
          {field('Full name',profile?.fullName)}
          {field('Email address',profile?.email)}
          {field('Phone number',profile?.phone)}
          {field('Account ID',profile?.id)}
          {field('Role',roleLabel)}
          {field('Account status',profile?.enabled === false ? 'Disabled' : 'Active')}
        </div>
      </section>

      {details.type === 'STUDENT' && <section className="profile-section">
        <div className="profile-section-heading"><div><span className="eyebrow">ACADEMIC IDENTITY</span><h3>Student details</h3></div><span className="profile-readonly">Student portal</span></div>
        <div className="profile-details-grid">
          {field('Admission number',details.admissionNumber)}
          {field('Student status',details.studentStatus)}
          {field('Date of birth',details.dateOfBirth)}
          {field('Admission date',details.admissionDate)}
          {field('Class',details.className)}
          {field('Section',details.sectionName)}
          {field('Academic year',details.academicYear)}
          {field('Enrollment start',details.enrollmentStartDate)}
        </div>
      </section>}

      {details.type === 'TEACHER' && <section className="profile-section">
        <div className="profile-section-heading"><div><span className="eyebrow">STAFF IDENTITY</span><h3>Teaching assignments</h3></div><span className="profile-readonly">Teacher portal</span></div>
        <div className="profile-details-grid">
          {field('Employee code',details.employeeCode)}
          {field('Hire date',details.hireDate)}
          {field('Teacher status',details.active === false ? 'Inactive' : 'Active')}
          {field('Assigned sections',(details.assignedSections||[]).length)}
        </div>
        {(details.assignedSections||[]).length > 0 && <div className="profile-assignment-list">{details.assignedSections.map((item,index)=><div key={item.sectionId+'-'+item.subjectName+'-'+index}><b>{item.className} · {item.sectionName}</b><span>{item.subjectName} · {item.academicYear}</span></div>)}</div>}
      </section>}

      {details.type === 'GUARDIAN' && <section className="profile-section">
        <div className="profile-section-heading"><div><span className="eyebrow">FAMILY IDENTITY</span><h3>Linked students</h3></div><span className="profile-readonly">Guardian portal</span></div>
        <div className="profile-details-grid">{field('Guardian ID',details.guardianId)}{field('Alternate phone',details.alternatePhone)}{field('Linked students',(details.linkedStudents||[]).length)}</div>
        {(details.linkedStudents||[]).length > 0 ? <div className="profile-linked-list">{details.linkedStudents.map(student=><div key={student.studentId}><div className="profile-linked-avatar">{initials(student.studentName)}</div><div><b>{student.studentName}</b><span>{student.admissionNumber} · {student.relationship}{student.primary?' · Primary contact':''}</span></div></div>)}</div> : <div className="profile-empty">No students are currently linked to this guardian account.</div>}
      </section>}

      {details.type === 'ADMIN' && <section className="profile-section">
        <div className="profile-section-heading"><div><span className="eyebrow">ADMINISTRATIVE ACCESS</span><h3>School administration</h3></div><span className="profile-readonly">Admin portal</span></div>
        <p className="profile-helper">This account can manage academic records, people, operational workflows, fee invoices and the audit trail. Changes are enforced by backend authorization.</p>
      </section>}

      <section className="profile-security-note"><span>◈</span><div><b>Account security</b><p>Your session uses a signed bearer token. The profile page never displays passwords, token values or raw API responses. For corrections to account information, contact the school administrator.</p></div></section>
    </>}
  </section>;
}

export default function App() {
  const [token, setToken] = useState(() => sessionStorage.getItem('school.jwt') || '');
  const [loginPayload, setLoginPayload] = useState(null);
  const [profile, setProfile] = useState(null);
  const [active, setActive] = useState('overview');
  const tokenRoles = useMemo(() => token ? readRoles(decodeJwt(token)) : [], [token]);
  const [profileRoles, setProfileRoles] = useState([]);
  const roles = profileRoles.length ? profileRoles : tokenRoles;
  const currentItem = active === 'profile' ? { id: 'profile', roles: ['ADMIN', 'TEACHER', 'GUARDIAN', 'STUDENT'], path: null } : (NAV.find((item) => item.id === active) || NAV[0]);

  useEffect(() => {
    if (!token) return;
    sessionStorage.setItem('school.jwt', token);
    apiRequest('/api/v1/auth/me', { token }).then((value) => {
      setProfile(value?.data || value);
      const found = readRoles(value);
      if (found.length) setProfileRoles(found);
    }).catch(() => setProfile((old) => old || loginPayload || null));
  }, [token, loginPayload]);

  function login(jwt, response) {
    sessionStorage.setItem('school.jwt', jwt);
    setLoginPayload(response);
    setProfile(response?.data || response);
    setToken(jwt);
  }

  function logout() {
    sessionStorage.removeItem('school.jwt');
    setToken(''); setProfile(null); setProfileRoles([]); setLoginPayload(null); setActive('overview');
  }

  const title = TITLES[currentItem.id] || ['Workspace', 'School operations workspace.'];
  const menuRoles = roles;
  const effectiveItem = currentItem.roles.some((role) => menuRoles.includes(role)) ? currentItem : NAV[0];
  useEffect(() => { if (active !== effectiveItem.id) setActive(effectiveItem.id); }, [effectiveItem.id, active]);

  if (!token) return <Login onLogin={login} />;

  return <div className="app-shell">
    <Sidebar active={active} setActive={setActive} roles={menuRoles} user={profile} onLogout={logout}/>
    <main className="main-area">
      <header className="topbar"><div className="breadcrumb"><span>Workspace</span><b>/</b><strong>{title[0]}</strong></div><div className="topbar-right"><div className="today-chip">◷ <span>Academic year 2026–27</span></div><button className="top-avatar" title="Profile" onClick={() => setActive('profile')}>{initials(profile?.email)}</button></div></header>
      <div className="page-content"><div className="page-heading"><div><div className="eyebrow">NORTHSTAR ACADEMY · MANAGEMENT PORTAL</div><h1>{title[0]}</h1><p>{title[1]}</p></div>{active !== 'overview' && active !== 'profile' && <span className="live-badge"><span/> Connected workspace</span>}</div>
        {active === 'overview' ? <Overview token={token} roles={menuRoles} user={profile} setActive={setActive}/> : active === 'profile' ? <ProfilePage user={profile} roles={menuRoles} token={token}/> : active === 'reportcard' ? <GuardianReportCard token={token}/> : active === 'familyattendance' ? <GuardianAttendanceWorkspace token={token}/> : active === 'myattendance' ? <StudentAttendanceWorkspace token={token}/> : active === 'myfees' ? <StudentFeesWorkspace token={token}/> : active === 'myresults' ? <StudentResultsWorkspace token={token}/> : active === 'classreports' ? <TeacherClassReportsWorkspace token={token}/> : active === 'attendance' ? <AttendanceWorkspace token={token} roles={menuRoles}/> : active === 'examinations' ? <ExaminationsWorkspace token={token} roles={menuRoles}/> : active === 'fees' ? <FeesWorkspace token={token} roles={menuRoles}/> : active === 'people' ? <PeopleWorkspace token={token}/> : active === 'audit' ? <AuditWorkspace token={token}/> : ['students','classes','sections','subjects','enrollments','assignments','guardians'].includes(active) ? <AcademicWorkspace key={active} item={effectiveItem} token={token}/> : <DataWorkspace key={active} item={effectiveItem} token={token}/>} 
        <footer className="page-footer"><span>Northstar School Management</span><span>Secure session · Role-based access enforced by backend</span></footer>
      </div>
    </main>
  </div>;
}
