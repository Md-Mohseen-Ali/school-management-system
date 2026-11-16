import { useCallback, useEffect, useMemo, useState } from 'react';
import { apiRequest, rowsFrom } from './api.js';

const today = () => { const now = new Date(); return new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 10); };
const money = (value) => value == null ? '—' : new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(Number(value));
const pretty = (value) => String(value || '—').replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
const get = (obj, ...keys) => { for (const key of keys) { const value = key.split('.').reduce((acc, part) => acc?.[part], obj); if (value != null) return value; } return undefined; };
const messageFrom = (err) => err?.payload?.message || err?.message || 'Something went wrong. Please try again.';
const makeKey = () => globalThis.crypto?.randomUUID?.() || ('demo-' + Date.now() + '-' + Math.random().toString(36).slice(2));

function Notice({ type = 'success', children, onClose }) {
  return <div className={'ops-notice ' + type} role="status"><span>{type === 'success' ? '✓' : '!'}</span><div>{children}</div>{onClose && <button onClick={onClose} aria-label="Dismiss">×</button>}</div>;
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
  return <span className={'ops-badge ' + klass}>{pretty(value)}</span>;
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
  const [assignedSections, setAssignedSections] = useState([]);
  const [sessionDraft, setSessionDraft] = useState({ sectionId: '', attendanceDate: today(), sessionCode: 'FULL_DAY', notes: '' });
  const canMark = roles.includes('TEACHER');
  const canLock = roles.includes('ADMIN');

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try { setSessions(rowsFrom(await apiRequest('/api/v1/attendance/sessions', { token }))); }
    catch (e) { setError(messageFrom(e)); }
    finally { setLoading(false); }
  }, [token]);
  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    if (!canMark) return;
    apiRequest('/api/v1/teacher/roster/sections', { token })
      .then((data) => {
        const options = rowsFrom(data);
        setAssignedSections(options);
        setSessionDraft((old) => ({ ...old, sectionId: old.sectionId || (options[0]?.id ? String(options[0].id) : '') }));
      })
      .catch((e) => setNotice({ type: 'error', text: 'Could not load your assigned sections: ' + messageFrom(e) }));
  }, [canMark, token]);

  async function openRoster(session) {
    setActiveSession(session); setNotice(null); setRosterLoading(true); setStudents([]); setMarks({}); setRemarks({}); setRecordsSaved(false);
    try {
      const rosterPath = canMark
        ? '/api/v1/teacher/roster?sectionId=' + encodeURIComponent(session.sectionId) + '&attendanceDate=' + encodeURIComponent(session.attendanceDate)
        : '/api/v1/academic/enrollments?sectionId=' + encodeURIComponent(session.sectionId) + '&attendanceDate=' + encodeURIComponent(session.attendanceDate) + '&size=100';
      const [roster, existing] = await Promise.all([
        apiRequest(rosterPath, { token }),
        apiRequest('/api/v1/attendance/sessions/' + session.id + '/records', { token }),
      ]);
      const rosterRows = rowsFrom(roster);
      const savedRows = rowsFrom(existing);
      const savedByStudent = Object.fromEntries(savedRows.map((record) => [record.studentId, record]));
      const initialMarks = {};
      const initialRemarks = {};
      rosterRows.forEach((student) => {
        const studentId = get(student, 'studentId', 'id');
        const saved = savedByStudent[studentId];
        initialMarks[studentId] = saved?.attendanceStatus || 'PRESENT';
        initialRemarks[studentId] = saved?.remarks || '';
      });
      setStudents(rosterRows);
      setMarks(initialMarks);
      setRemarks(initialRemarks);
      setRecordsSaved(rosterRows.length > 0 && savedRows.length === rosterRows.length);
    } catch (e) {
      setNotice({ type: 'error', text: 'Could not load the class register: ' + messageFrom(e) });
    } finally { setRosterLoading(false); }
  }

  async function saveRecords() {
    if (!activeSession || !students.length) return;
    setSaving(true); setNotice(null);
    try {
      const records = students.map((student) => {
        const id = get(student, 'studentId', 'id');
        return { studentId: Number(id), attendanceStatus: marks[id] || 'PRESENT', remarks: remarks[id] || '' };
      });
      const saved = await apiRequest('/api/v1/attendance/sessions/' + activeSession.id + '/records', { method: 'PUT', token, body: { records } });
      const savedRows = rowsFrom(saved);
      setRecordsSaved(true);
      if (savedRows.length) {
        setMarks(Object.fromEntries(savedRows.map((x) => [x.studentId, x.attendanceStatus])));
        setRemarks(Object.fromEntries(savedRows.map((x) => [x.studentId, x.remarks || ''])));
      }
      setNotice({ type: 'success', text: 'Attendance saved successfully. Review the register, then submit it.' });
      await load();
    } catch (e) { setNotice({ type: 'error', text: messageFrom(e) }); }
    finally { setSaving(false); }
  }

  async function transition(action) {
    setSaving(true); setNotice(null);
    try {
      const updated = await apiRequest('/api/v1/attendance/sessions/' + activeSession.id + '/' + action, { method: 'POST', token });
      setNotice({ type: 'success', text: action === 'submit' ? 'Attendance has been submitted.' : 'Attendance session has been locked.' });
      await load();
      setActiveSession((old) => old ? { ...old, ...(updated || {}), status: action === 'submit' ? 'SUBMITTED' : 'LOCKED' } : old);
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
  const dateLabel = activeSession?.attendanceDate ? new Date(activeSession.attendanceDate + 'T00:00:00').toLocaleDateString('en-IN', { day:'numeric', month:'long', year:'numeric' }) : '';

  return <section className="ops-workspace">
    {notice && <Notice type={notice.type} onClose={() => setNotice(null)}>{notice.text}</Notice>}
    {activeSession ? <>
      <button className="ops-back" onClick={() => setActiveSession(null)}>← Back to attendance sessions</button>
      <SectionTitle eyebrow="CLASS REGISTER" title={(activeSession.className || 'Class') + ' · ' + (activeSession.sectionName || ('Section ' + activeSession.sectionId))} description={dateLabel + ' · ' + pretty(activeSession.sessionCode) + ' · Session #' + activeSession.id} action={<Badge value={activeSession.status}/>}/>
      <div className="ops-stat-row"><div className="ops-stat"><b>{students.length}</b><span>Students</span></div><div className="ops-stat"><b>{presentCount}</b><span>Present</span></div><div className="ops-stat"><b>{absentCount}</b><span>Absent</span></div><div className="ops-stat"><b>{students.length ? Math.round(presentCount / students.length * 100) : 0}%</b><span>Present rate</span></div></div>
      <div className="ops-card"><div className="ops-card-head"><div><h3>Mark student attendance</h3><p>Saved attendance is loaded when you reopen a session. Changes remain editable until submission.</p></div><span className="ops-count">{students.length} enrolled</span></div>
        {rosterLoading ? <div className="ops-loading">Loading class register…</div> : students.length ? <div className="ops-roster">{students.map((student) => {
          const id = get(student, 'studentId', 'id');
          const name = get(student, 'studentName', 'name', 'fullName') || ('Student #' + id);
          return <div className="ops-student-row" key={id}>
            <div className="ops-student-avatar">{String(name).split(' ').map((x) => x[0]).slice(0,2).join('').toUpperCase()}</div>
            <div className="ops-student-info"><b>{name}</b><small>{get(student, 'admissionNumber', 'rollNumber') || ('Student ID ' + id)}</small></div>
            <div className="ops-status-picker">{['PRESENT','ABSENT','LATE','EXCUSED'].map((status) => <button type="button" key={status} disabled={!canMark || activeSession.status !== 'DRAFT'} className={marks[id] === status ? 'chosen ' + status.toLowerCase() : ''} onClick={() => { setRecordsSaved(false); setMarks((old) => ({...old,[id]:status})); }}>{pretty(status)}</button>)}</div>
            <input className="ops-remarks-input" aria-label={'Remarks for ' + name} placeholder="Optional remarks" value={remarks[id] || ''} disabled={!canMark || activeSession.status !== 'DRAFT'} onChange={(e) => setRemarks((old) => ({...old,[id]:e.target.value}))}/>
          </div>;
        })}</div> : <Empty title="No enrolled students found" description="There are no students enrolled in this section on the attendance date. Check the enrollment records."/>}
        <div className="ops-form-footer"><span className="ops-muted">{activeSession.status === 'DRAFT' ? 'All enrolled students must have a saved attendance record before submission.' : 'This session is read-only.'}</span><div className="ops-actions">
          {canMark && activeSession.status === 'DRAFT' && <button className="ops-button primary" disabled={saving || rosterLoading || !students.length} onClick={saveRecords}>{saving ? 'Saving…' : 'Save attendance'}</button>}
          {canMark && activeSession.status === 'DRAFT' && <button className="ops-button secondary" disabled={saving || !students.length || !recordsSaved} onClick={() => transition('submit')}>Submit attendance →</button>}
          {canLock && activeSession.status === 'SUBMITTED' && <button className="ops-button primary" disabled={saving} onClick={() => transition('lock')}>Lock session</button>}
        </div></div>
      </div>
    </> : <>
      <SectionTitle eyebrow="DAILY OPERATIONS" title="Attendance sessions" description="Take the register, review student presence and submit completed sessions." action={canMark ? <button className="ops-button primary" onClick={() => setSessionForm(true)}>＋ New session</button> : null}/>
      <div className="ops-toolbar"><div className="ops-search"><span>⌕</span><input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search class, section or date…"/></div><button className="ops-button secondary" onClick={load} disabled={loading}>↻ Refresh</button></div>
      {error && <Notice type="error">{error}</Notice>}
      {loading ? <div className="ops-loading">Loading attendance sessions…</div> : filtered.length ? <div className="ops-session-grid">{filtered.map((session) => <article className="ops-session-card" key={session.id}><div className="ops-session-top"><span className="ops-session-symbol">✓</span><Badge value={session.status}/></div><h3>{session.className || 'Class'} · {session.sectionName || ('Section ' + session.sectionId)}</h3><p>{session.attendanceDate ? new Date(session.attendanceDate + 'T00:00:00').toLocaleDateString('en-IN', { day:'numeric', month:'short', year:'numeric' }) : 'Date not set'} · {pretty(session.sessionCode)}</p><div className="ops-session-meta"><span>Teacher ID <b>{session.teacherId ?? '—'}</b></span><span>Session <b>#{session.id}</b></span></div><button className="ops-button secondary full" onClick={() => openRoster(session)}>{canMark && session.status === 'DRAFT' ? 'Take attendance →' : 'View register →'}</button></article>)}</div> : <Empty title="No attendance sessions yet" description="Create a session for an assigned class, then record attendance from the class register." action={canMark ? <button className="ops-button primary" onClick={() => setSessionForm(true)}>Create first session</button> : null}/>}
    </>}
    {sessionForm && <Modal title="Create attendance session" subtitle="Select an assigned section and create the register." onClose={() => setSessionForm(false)}><form className="ops-form" onSubmit={createSession}>
      <label>Assigned section<select value={sessionDraft.sectionId} onChange={(e) => setSessionDraft({...sessionDraft,sectionId:e.target.value})} required><option value="">Select a section</option>{assignedSections.map((s) => <option key={s.id} value={s.id}>{s.className} · {s.sectionName} · {s.academicYear}</option>)}</select></label>
      <label>Attendance date<input type="date" value={sessionDraft.attendanceDate} onChange={(e) => setSessionDraft({...sessionDraft,attendanceDate:e.target.value})} required/></label>
      <label>Session<select value={sessionDraft.sessionCode} onChange={(e) => setSessionDraft({...sessionDraft,sessionCode:e.target.value})}><option value="FULL_DAY">Full day</option><option value="MORNING">Morning</option><option value="AFTERNOON">Afternoon</option></select></label>
      <label>Notes<input value={sessionDraft.notes} onChange={(e) => setSessionDraft({...sessionDraft,notes:e.target.value})} maxLength="255" placeholder="Optional note"/></label>
      <div className="ops-form-footer"><button type="button" className="ops-button secondary" onClick={() => setSessionForm(false)}>Cancel</button><button className="ops-button primary" disabled={saving || !assignedSections.length}>{saving ? 'Creating…' : 'Create session'}</button></div>
    </form></Modal>}
  </section>;
}

export function ExaminationsWorkspace({ token, roles = [] }) {
  const admin = roles.includes('ADMIN');
  const [exams,setExams] = useState([]);
  const [loading,setLoading] = useState(true);
  const [saving,setSaving] = useState(false);
  const [error,setError] = useState('');
  const [notice,setNotice] = useState(null);
  const [query,setQuery] = useState('');
  const [sections,setSections] = useState([]);
  const [subjects,setSubjects] = useState([]);
  const [showCreate,setShowCreate] = useState(false);
  const [selectedExam,setSelectedExam] = useState(null);
  const [examSubjects,setExamSubjects] = useState([]);
  const [subjectDraft,setSubjectDraft] = useState({subjectId:'',examinationDate:today(),maxMarks:'100',passMarks:'35'});
  const [showSubjectForm,setShowSubjectForm] = useState(false);
  const [activeSubject,setActiveSubject] = useState(null);
  const [marksRows,setMarksRows] = useState([]);
  const [marks,setMarks] = useState({});
  const [markAttendance,setMarkAttendance] = useState({});
  const [markRemarks,setMarkRemarks] = useState({});
  const [marksLoading,setMarksLoading] = useState(false);
  const [marksSaved,setMarksSaved] = useState(false);
  const [gradingScales,setGradingScales] = useState([]);
  const [gradingLoading,setGradingLoading] = useState(false);
  const [showGrading,setShowGrading] = useState(false);
  const [gradingDraft,setGradingDraft] = useState({academicYear:'2026-2027',grade:'A',minPercentage:'80',maxPercentage:'100',remarks:''});
  const [reportPreviewExam,setReportPreviewExam] = useState(null);
  const [reportStudents,setReportStudents] = useState([]);
  const [reportStudentId,setReportStudentId] = useState('');
  const [reportCard,setReportCard] = useState(null);
  const [reportBusy,setReportBusy] = useState(false);
  const [draft,setDraft] = useState({examName:'',sectionId:'',academicYear:'2026-2027',startDate:today(),endDate:today()});

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try {
      const path = admin ? '/api/v1/examinations' : '/api/v1/teacher/examinations';
      setExams(rowsFrom(await apiRequest(path,{token})));
    } catch(e) { setError(messageFrom(e)); setExams([]); }
    finally { setLoading(false); }
  },[admin,token]);
  useEffect(()=>{load();},[load]);
  useEffect(()=>{
    if(!admin)return;
    Promise.all([
      apiRequest('/api/v1/academic/sections?size=100',{token}),
      apiRequest('/api/v1/academic/subjects?active=true&size=100',{token}),
    ]).then(([sectionData,subjectData])=>{
      setSections(rowsFrom(sectionData));
      setSubjects(rowsFrom(subjectData));
    }).catch((e)=>setNotice({type:'error',text:'Could not load section/subject options: '+messageFrom(e)}));
  },[admin,token]);

  const loadGradingScales = useCallback(async (academicYear='2026-2027') => {
    if(!admin)return;
    setGradingLoading(true);
    try { setGradingScales(rowsFrom(await apiRequest('/api/v1/examinations/grading-scales?academicYear='+encodeURIComponent(academicYear),{token}))); }
    catch(e) { setNotice({type:'error',text:'Could not load grading scale: '+messageFrom(e)}); }
    finally { setGradingLoading(false); }
  },[admin,token]);
  useEffect(()=>{if(admin)loadGradingScales('2026-2027');},[admin,loadGradingScales]);

  async function createGrade(e) {
    e.preventDefault();setSaving(true);setNotice(null);
    try {
      await apiRequest('/api/v1/examinations/grading-scales',{method:'POST',token,body:{
        academicYear:gradingDraft.academicYear,grade:gradingDraft.grade.trim().toUpperCase(),
        minPercentage:Number(gradingDraft.minPercentage),maxPercentage:Number(gradingDraft.maxPercentage),remarks:gradingDraft.remarks||null,
      }});
      setNotice({type:'success',text:'Grading band saved.'});
      await loadGradingScales(gradingDraft.academicYear);
      setShowGrading(false);
    } catch(e) { setNotice({type:'error',text:messageFrom(e)}); }
    finally { setSaving(false); }
  }

  async function openAdminReport(exam) {
    setReportPreviewExam(exam);setReportCard(null);setReportStudentId('');setReportBusy(true);
    try {
      if(!reportStudents.length) setReportStudents(rowsFrom(await apiRequest('/api/v1/academic/students?size=100',{token})));
    } catch(e) { setNotice({type:'error',text:'Could not load students for report cards: '+messageFrom(e)}); }
    finally { setReportBusy(false); }
  }

  async function loadAdminReport(e) {
    e.preventDefault();if(!reportPreviewExam||!reportStudentId)return;
    setReportBusy(true);setReportCard(null);setNotice(null);
    try {
      setReportCard(await apiRequest('/api/v1/examinations/'+reportPreviewExam.id+'/report-cards/'+reportStudentId,{token}));
    } catch(e) { setNotice({type:'error',text:messageFrom(e)}); }
    finally { setReportBusy(false); }
  }

  async function createExam(e) {
    e.preventDefault(); setSaving(true); setNotice(null);
    try {
      const created = await apiRequest('/api/v1/examinations',{method:'POST',token,body:{
        examName:draft.examName.trim(),sectionId:Number(draft.sectionId),academicYear:draft.academicYear.trim(),startDate:draft.startDate,endDate:draft.endDate,
      }});
      setShowCreate(false);
      setSelectedExam(created);
      setExamSubjects([]);
      setSubjectDraft({...subjectDraft,examinationDate:draft.startDate});
      setNotice({type:'success',text:'Examination draft created. Configure subjects before scheduling.'});
      setShowSubjectForm(true);
      await load();
    } catch(e) { setNotice({type:'error',text:messageFrom(e)}); }
    finally { setSaving(false); }
  }

  async function loadSubjects(exam) {
    setSelectedExam(exam); setActiveSubject(null); setNotice(null);
    try {
      const path = admin ? '/api/v1/examinations/' + exam.id + '/subjects' : '/api/v1/teacher/examinations/' + exam.id + '/subjects';
      setExamSubjects(rowsFrom(await apiRequest(path,{token})));
    } catch(e) { setExamSubjects([]); setNotice({type:'error',text:messageFrom(e)}); }
  }

  async function addSubject(e) {
    e.preventDefault(); if(!selectedExam)return;
    setSaving(true); setNotice(null);
    try {
      await apiRequest('/api/v1/examinations/' + selectedExam.id + '/subjects',{method:'POST',token,body:{
        subjectId:Number(subjectDraft.subjectId),examinationDate:subjectDraft.examinationDate,maxMarks:Number(subjectDraft.maxMarks),passMarks:Number(subjectDraft.passMarks),
      }});
      setShowSubjectForm(false); setNotice({type:'success',text:'Subject added to the examination.'});
      await loadSubjects(selectedExam);
    } catch(e) { setNotice({type:'error',text:messageFrom(e)}); }
    finally { setSaving(false); }
  }

  async function examTransition(id,action) {
    setSaving(true); setNotice(null);
    try {
      const updated=await apiRequest('/api/v1/examinations/'+id+'/'+action,{method:'POST',token});
      setNotice({type:'success',text:'Examination '+(action==='schedule'?'scheduled':action==='complete'?'completed':'published')+'.'});
      await load();
      if(selectedExam?.id===id) setSelectedExam(updated || selectedExam);
    } catch(e) { setNotice({type:'error',text:messageFrom(e)}); }
    finally { setSaving(false); }
  }

  async function openMarks(subject) {
    setActiveSubject(subject); setMarksLoading(true); setMarksSaved(false); setNotice(null);
    const sectionId=selectedExam.sectionId;
    const date=subject.examinationDate || selectedExam.startDate;
    try {
      const rosterPath=admin
        ? '/api/v1/academic/enrollments?sectionId='+sectionId+'&attendanceDate='+encodeURIComponent(date)+'&size=100'
        : '/api/v1/teacher/roster?sectionId='+sectionId+'&attendanceDate='+date;
      const [rosterData,existingData]=await Promise.all([
        apiRequest(rosterPath,{token}),
        apiRequest('/api/v1/examinations/subjects/'+subject.id+'/marks',{token}),
      ]);
      const roster=rowsFrom(rosterData);
      const existing=rowsFrom(existingData);
      const byStudent=Object.fromEntries(existing.map((r)=>[r.studentId,r]));
      setMarksRows(roster);
      setMarks(Object.fromEntries(roster.map((s)=>{
        const id=get(s,'studentId','id');
        return [id,byStudent[id]?.marksObtained ?? ''];
      })));
      setMarkAttendance(Object.fromEntries(roster.map((s)=>{
        const id=get(s,'studentId','id');
        return [id,byStudent[id]?.attendanceStatus || 'PRESENT'];
      })));
      setMarkRemarks(Object.fromEntries(roster.map((s)=>{
        const id=get(s,'studentId','id');
        return [id,byStudent[id]?.remarks || ''];
      })));
    } catch(e) { setNotice({type:'error',text:'Could not load students and saved marks: '+messageFrom(e)}); }
    finally { setMarksLoading(false); }
  }

  async function saveMarks(e) {
    e?.preventDefault(); if(!activeSubject)return;
    setSaving(true); setNotice(null);
    try {
      const records=marksRows.map((s)=>{
        const studentId=get(s,'studentId','id');
        return {studentId:Number(studentId),marksObtained:Number(marks[studentId]||0),attendanceStatus:markAttendance[studentId]||'PRESENT',remarks:markRemarks[studentId]||''};
      });
      await apiRequest('/api/v1/examinations/subjects/'+activeSubject.id+'/marks',{method:'PUT',token,body:{records}});
      setMarksSaved(true); setNotice({type:'success',text:'Marks saved successfully.'});
      await openMarksRefresh();
    } catch(e) { setNotice({type:'error',text:messageFrom(e)}); }
    finally { setSaving(false); }
  }

  async function openMarksRefresh() {
    if(!activeSubject)return;
    const existing=rowsFrom(await apiRequest('/api/v1/examinations/subjects/'+activeSubject.id+'/marks',{token}));
    const byStudent=Object.fromEntries(existing.map((r)=>[r.studentId,r]));
    setMarks(Object.fromEntries(marksRows.map((s)=>{const id=get(s,'studentId','id');return[id,byStudent[id]?.marksObtained??''];})));
    setMarkAttendance(Object.fromEntries(marksRows.map((s)=>{const id=get(s,'studentId','id');return[id,byStudent[id]?.attendanceStatus||'PRESENT'];})));
    setMarkRemarks(Object.fromEntries(marksRows.map((s)=>{const id=get(s,'studentId','id');return[id,byStudent[id]?.remarks||''];})));
  }

  const filtered=exams.filter((exam)=>JSON.stringify(exam).toLowerCase().includes(query.toLowerCase()));
  return <section className="ops-workspace">
    {notice&&<Notice type={notice.type} onClose={()=>setNotice(null)}>{notice.text}</Notice>}
    <SectionTitle eyebrow="ASSESSMENT MANAGEMENT" title="Examinations" description="Create assessments, configure subjects, enter marks and publish results." action={admin&&<div className="ops-actions"><button className="ops-button secondary" onClick={()=>{setShowGrading(true);loadGradingScales(gradingDraft.academicYear);}}>Grading scale</button><button className="ops-button primary" onClick={()=>setShowCreate(true)}>＋ Create examination</button></div>}/>
    <div className="ops-toolbar"><div className="ops-search"><span>⌕</span><input value={query} onChange={(e)=>setQuery(e.target.value)} placeholder="Search examinations…"/></div><button className="ops-button secondary" onClick={load} disabled={loading}>↻ Refresh</button></div>
    {error&&<Notice type="error">{error}</Notice>}
    {loading?<div className="ops-loading">Loading examinations…</div>:filtered.length?<div className="ops-table-wrap"><table className="ops-table"><thead><tr><th>Examination</th><th>Year</th><th>Section</th><th>Dates</th><th>Status</th><th>Actions</th></tr></thead><tbody>{filtered.map((exam)=><tr key={exam.id}><td><b>{exam.examName||'Exam #'+exam.id}</b></td><td>{exam.academicYear}</td><td>{exam.className} · {exam.sectionName}</td><td>{exam.startDate} – {exam.endDate}</td><td><Badge value={exam.status}/></td><td><div className="ops-inline-actions">
      <button className="ops-text-button" onClick={()=>loadSubjects(exam)}>Subjects / marks</button>
      {admin&&exam.status==='DRAFT'&&<button className="ops-text-button" disabled={saving} onClick={()=>examTransition(exam.id,'schedule')}>Schedule</button>}
      {admin&&exam.status==='SCHEDULED'&&<button className="ops-text-button" disabled={saving} onClick={()=>examTransition(exam.id,'complete')}>Complete</button>}
      {admin&&exam.status==='COMPLETED'&&<button className="ops-text-button" disabled={saving} onClick={()=>examTransition(exam.id,'publish')}>Publish</button>}
      {admin&&exam.status==='PUBLISHED'&&<button className="ops-text-button" onClick={()=>openAdminReport(exam)}>Report card</button>}
    </div></td></tr>)}</tbody></table></div>:<Empty title="No examinations scheduled" description="Create an examination, add at least one subject, then schedule it." action={admin&&<button className="ops-button primary" onClick={()=>setShowCreate(true)}>Create examination</button>}/>}
    {showGrading&&<Modal title="Grading scale" subtitle="Define non-overlapping grade bands for an academic year. These bands are used in report-card calculations." onClose={()=>setShowGrading(false)}>
      <form className="ops-form" onSubmit={createGrade}>
        <label>Academic year<input value={gradingDraft.academicYear} pattern="[0-9]{4}-[0-9]{4}" onChange={(e)=>setGradingDraft({...gradingDraft,academicYear:e.target.value})} required/></label>
        <div className="ops-form-grid"><label>Grade<input maxLength="5" value={gradingDraft.grade} onChange={(e)=>setGradingDraft({...gradingDraft,grade:e.target.value})} required/></label><label>Remarks<input maxLength="100" value={gradingDraft.remarks} onChange={(e)=>setGradingDraft({...gradingDraft,remarks:e.target.value})}/></label><label>Minimum %<input type="number" min="0" max="100" step="0.01" value={gradingDraft.minPercentage} onChange={(e)=>setGradingDraft({...gradingDraft,minPercentage:e.target.value})} required/></label><label>Maximum %<input type="number" min={gradingDraft.minPercentage} max="100" step="0.01" value={gradingDraft.maxPercentage} onChange={(e)=>setGradingDraft({...gradingDraft,maxPercentage:e.target.value})} required/></label></div>
        {gradingLoading?<div className="ops-loading">Loading existing grades…</div>:gradingScales.length?<div className="ops-table-wrap"><table className="ops-table"><thead><tr><th>Grade</th><th>Minimum</th><th>Maximum</th><th>Remarks</th></tr></thead><tbody>{gradingScales.map((g)=><tr key={g.id}><td>{g.grade}</td><td>{g.minPercentage}%</td><td>{g.maxPercentage}%</td><td>{g.remarks||'—'}</td></tr>)}</tbody></table></div>:<p className="ops-muted">No grade bands configured for this academic year.</p>}
        <div className="ops-form-footer"><button type="button" className="ops-button secondary" onClick={()=>setShowGrading(false)}>Close</button><button className="ops-button primary" disabled={saving}>{saving?'Saving…':'Add grading band'}</button></div>
      </form>
    </Modal>}
    {reportPreviewExam&&<Modal title="Examination report card" subtitle={reportPreviewExam.examName+' · '+reportPreviewExam.academicYear} onClose={()=>{setReportPreviewExam(null);setReportCard(null);}}>
      <form className="ops-form" onSubmit={loadAdminReport}>
        <label>Student<select value={reportStudentId} onChange={(e)=>setReportStudentId(e.target.value)} required><option value="">Select student</option>{reportStudents.map((s)=><option value={s.id} key={s.id}>{s.admissionNumber} · {s.firstName} {s.lastName}</option>)}</select></label>
        <div className="ops-form-footer"><button className="ops-button primary" disabled={reportBusy||!reportStudentId}>{reportBusy?'Loading…':'Generate report card'}</button></div>
      </form>
      {reportCard&&<div className="ops-card"><div className="result-summary"><div><span>STUDENT</span><b>{reportCard.studentName}</b></div><div><span>PERCENTAGE</span><b>{reportCard.percentage}%</b></div><div><span>GRADE</span><b>{reportCard.grade}</b></div><div><span>RESULT</span><b>{reportCard.passed?'Passed':'Not passed'}</b></div></div><div className="ops-table-wrap"><table className="ops-table"><thead><tr><th>Subject</th><th>Marks</th><th>Maximum</th><th>Pass</th><th>Result</th></tr></thead><tbody>{(reportCard.subjects||[]).map((s)=><tr key={s.examinationSubjectId}><td>{s.subjectName}</td><td>{s.marksObtained}</td><td>{s.maxMarks}</td><td>{s.passMarks}</td><td><Badge value={s.passed?'PASSED':'FAILED'}/></td></tr>)}</tbody></table></div></div>}
    </Modal>}
    {showCreate&&<Modal title="Create examination" subtitle="Create a draft. You will then configure its subjects before scheduling." onClose={()=>setShowCreate(false)}><form className="ops-form" onSubmit={createExam}>
      <label>Examination name<input value={draft.examName} onChange={(e)=>setDraft({...draft,examName:e.target.value})} maxLength="100" required/></label>
      <label>Section<select value={draft.sectionId} onChange={(e)=>{const s=sections.find((x)=>String(x.id)===e.target.value);setDraft({...draft,sectionId:e.target.value,academicYear:s?.academicYear||draft.academicYear});}} required><option value="">Select section</option>{sections.map((s)=><option key={s.id} value={s.id}>{s.className} · {s.sectionName} · {s.academicYear}</option>)}</select></label>
      <label>Academic year<input value={draft.academicYear} readOnly required/></label>
      <div className="ops-form-grid"><label>Start date<input type="date" value={draft.startDate} onChange={(e)=>setDraft({...draft,startDate:e.target.value})} required/></label><label>End date<input type="date" value={draft.endDate} min={draft.startDate} onChange={(e)=>setDraft({...draft,endDate:e.target.value})} required/></label></div>
      <div className="ops-form-footer"><button type="button" className="ops-button secondary" onClick={()=>setShowCreate(false)}>Cancel</button><button className="ops-button primary" disabled={saving||!sections.length}>{saving?'Creating…':'Create draft'}</button></div>
    </form></Modal>}
    {selectedExam&&!activeSubject&&<Modal title={selectedExam.examName||'Examination subjects'} subtitle={selectedExam.className+' · '+selectedExam.sectionName+' · '+selectedExam.academicYear} onClose={()=>{setSelectedExam(null);setExamSubjects([]);}}>
      <div className="ops-form">
        <div className="ops-table-wrap"><table className="ops-table"><thead><tr><th>Subject</th><th>Exam date</th><th>Maximum</th><th>Pass</th><th>Action</th></tr></thead><tbody>{examSubjects.map((s)=><tr key={s.id}><td><b>{s.subjectName}</b><small>{s.subjectCode}</small></td><td>{s.examinationDate}</td><td>{s.maxMarks}</td><td>{s.passMarks}</td><td><button className="ops-text-button" onClick={()=>openMarks(s)}>Enter marks</button></td></tr>)}</tbody></table></div>
        {admin&&selectedExam.status==='DRAFT'&&<button className="ops-button secondary" onClick={()=>{setSubjectDraft({...subjectDraft,examinationDate:selectedExam.startDate});setShowSubjectForm(true);}}>＋ Add subject</button>}
        <div className="ops-form-footer"><button className="ops-button secondary" onClick={()=>setSelectedExam(null)}>Close</button><span className="ops-muted">{examSubjects.length} configured subjects</span></div>
      </div>
    </Modal>}
    {showSubjectForm&&selectedExam&&<Modal title="Configure examination subject" subtitle="Maximum marks, pass marks and date are stored per subject." onClose={()=>setShowSubjectForm(false)}><form className="ops-form" onSubmit={addSubject}>
      <label>Subject<select value={subjectDraft.subjectId} onChange={(e)=>setSubjectDraft({...subjectDraft,subjectId:e.target.value})} required><option value="">Select subject</option>{subjects.map((s)=><option key={s.id} value={s.id}>{s.subjectCode} · {s.name}</option>)}</select></label>
      <label>Examination date<input type="date" value={subjectDraft.examinationDate} min={selectedExam.startDate} max={selectedExam.endDate} onChange={(e)=>setSubjectDraft({...subjectDraft,examinationDate:e.target.value})} required/></label>
      <div className="ops-form-grid"><label>Maximum marks<input type="number" min="0.01" step="0.01" value={subjectDraft.maxMarks} onChange={(e)=>setSubjectDraft({...subjectDraft,maxMarks:e.target.value})} required/></label><label>Passing marks<input type="number" min="0" step="0.01" max={subjectDraft.maxMarks} value={subjectDraft.passMarks} onChange={(e)=>setSubjectDraft({...subjectDraft,passMarks:e.target.value})} required/></label></div>
      <div className="ops-form-footer"><button type="button" className="ops-button secondary" onClick={()=>setShowSubjectForm(false)}>Cancel</button><button className="ops-button primary" disabled={saving||!subjects.length}>{saving?'Saving…':'Add subject'}</button></div>
    </form></Modal>}
    {activeSubject&&selectedExam&&<Modal title={'Marks · '+activeSubject.subjectName} subtitle={selectedExam.examName+' · Maximum '+activeSubject.maxMarks+' · Pass '+activeSubject.passMarks} onClose={()=>setActiveSubject(null)}>
      {marksLoading?<div className="ops-loading">Loading student marks…</div>:<form className="ops-form" onSubmit={saveMarks}>
        <div className="ops-table-wrap"><table className="ops-table"><thead><tr><th>Student</th><th>Attendance</th><th>Marks</th><th>Remarks</th></tr></thead><tbody>{marksRows.map((student)=>{const id=get(student,'studentId','id');return <tr key={id}><td><b>{get(student,'studentName','name')||('Student #'+id)}</b><small>{student.admissionNumber}</small></td><td><select value={markAttendance[id]||'PRESENT'} onChange={(e)=>setMarkAttendance({...markAttendance,[id]:e.target.value})}><option value="PRESENT">Present</option><option value="ABSENT">Absent</option></select></td><td><input aria-label={'Marks for student '+id} type="number" min="0" max={activeSubject.maxMarks} step="0.01" value={marks[id]??''} disabled={markAttendance[id]==='ABSENT'} onChange={(e)=>setMarks({...marks,[id]:e.target.value})} required={markAttendance[id]!=='ABSENT'}/></td><td><input aria-label={'Remarks for student '+id} value={markRemarks[id]||''} onChange={(e)=>setMarkRemarks({...markRemarks,[id]:e.target.value})}/></td></tr>;})}</tbody></table></div>
        <div className="ops-form-footer"><span className="ops-muted">Absent students are recorded with zero marks.</span><div className="ops-actions"><button type="button" className="ops-button secondary" onClick={()=>setActiveSubject(null)}>Close</button><button className="ops-button primary" disabled={saving||!marksRows.length}>{saving?'Saving…':'Save marks'}</button></div></div>
      </form>}
    </Modal>}
  </section>;
}

export function FeesWorkspace({ token, roles = [] }) {
  const admin=roles.includes('ADMIN');
  const guardian=roles.includes('GUARDIAN');
  const [invoices,setInvoices]=useState([]);
  const [students,setStudents]=useState([]);
  const [loading,setLoading]=useState(true);
  const [saving,setSaving]=useState(false);
  const [error,setError]=useState('');
  const [notice,setNotice]=useState(null);
  const [query,setQuery]=useState('');
  const [showCreate,setShowCreate]=useState(false);
  const [paymentInvoice,setPaymentInvoice]=useState(null);
  const [historyInvoice,setHistoryInvoice]=useState(null);
  const [payments,setPayments]=useState([]);
  const [draft,setDraft]=useState({studentId:'',invoiceNumber:'',academicYear:'2026-2027',issueDate:today(),dueDate:today(),description:'Tuition fee',amount:'15000'});
  const [payment,setPayment]=useState({amount:''});

  const load=useCallback(async()=>{
    setLoading(true);setError('');
    try{
      const data=await apiRequest('/api/v1/fees/invoices',{token});
      setInvoices(rowsFrom(data));
    }catch(e){setError(messageFrom(e));setInvoices([]);}
    finally{setLoading(false);}
  },[token]);
  useEffect(()=>{load();},[load]);
  useEffect(()=>{
    if(!admin)return;
    apiRequest('/api/v1/academic/students?size=100',{token})
      .then((data)=>setStudents(rowsFrom(data)))
      .catch((e)=>setNotice({type:'error',text:'Could not load student directory: '+messageFrom(e)}));
  },[admin,token]);

  async function createInvoice(e){
    e.preventDefault();setSaving(true);setNotice(null);
    let created=null;
    try{
      created=await apiRequest('/api/v1/fees/invoices',{method:'POST',token,body:{
        invoiceNumber:draft.invoiceNumber.trim(),studentId:Number(draft.studentId),academicYear:draft.academicYear,
        issueDate:draft.issueDate,dueDate:draft.dueDate,
      }});
      await apiRequest('/api/v1/fees/invoices/'+created.id+'/items',{method:'POST',token,body:{
        description:draft.description.trim(),amount:Number(draft.amount),
      }});
      await apiRequest('/api/v1/fees/invoices/'+created.id+'/issue',{method:'POST',token});
      setShowCreate(false);setNotice({type:'success',text:'Invoice created, itemized and issued successfully.'});
      await load();
    }catch(e){
      setNotice({type:'error',text:created ? 'Draft invoice #'+created.id+' was created, but the next step failed: '+messageFrom(e)+' You can retry from the draft invoice.' : messageFrom(e)});
      await load();
    }finally{setSaving(false);}
  }

  async function issueInvoice(invoice){
    setSaving(true);setNotice(null);
    try{await apiRequest('/api/v1/fees/invoices/'+invoice.id+'/issue',{method:'POST',token});setNotice({type:'success',text:'Invoice issued successfully.'});await load();}
    catch(e){setNotice({type:'error',text:messageFrom(e)});}
    finally{setSaving(false);}
  }

  async function cancelInvoice(invoice){
    if(!globalThis.confirm('Cancel invoice '+invoice.invoiceNumber+'?'))return;
    setSaving(true);setNotice(null);
    try{await apiRequest('/api/v1/fees/invoices/'+invoice.id+'/cancel',{method:'POST',token});setNotice({type:'success',text:'Invoice cancelled.'});await load();}
    catch(e){setNotice({type:'error',text:messageFrom(e)});}
    finally{setSaving(false);}
  }

  async function payInvoice(e){
    e.preventDefault();setSaving(true);setNotice(null);
    try{
      await apiRequest('/api/v1/fees/invoices/'+paymentInvoice.id+'/payments',{method:'POST',token,body:{
        amount:Number(payment.amount),paymentMethod:'SIMULATED',idempotencyKey:makeKey(),
      }});
      setPaymentInvoice(null);setNotice({type:'success',text:'Simulated payment recorded successfully.'});await load();
    }catch(e){setNotice({type:'error',text:messageFrom(e)});}
    finally{setSaving(false);}
  }

  async function showHistory(invoice){
    setHistoryInvoice(invoice);setPayments([]);setNotice(null);
    try{setPayments(rowsFrom(await apiRequest('/api/v1/fees/invoices/'+invoice.id+'/payments',{token})));}
    catch(e){setNotice({type:'error',text:'Payment history unavailable: '+messageFrom(e)});}
  }

  const filtered=invoices.filter((x)=>JSON.stringify(x).toLowerCase().includes(query.toLowerCase()));
  return <section className="ops-workspace">
    {notice&&<Notice type={notice.type} onClose={()=>setNotice(null)}>{notice.text}</Notice>}
    <SectionTitle eyebrow="SCHOOL FINANCE" title="Fee invoices" description="Create itemized invoices, issue them and record simulated payments." action={admin&&<button className="ops-button primary" onClick={()=>{setDraft({studentId:'',invoiceNumber:'INV-'+Date.now(),academicYear:'2026-2027',issueDate:today(),dueDate:today(),description:'Tuition fee',amount:'15000'});setShowCreate(true);}}>＋ Create invoice</button>}/>
    <div className="ops-toolbar"><div className="ops-search"><span>⌕</span><input value={query} onChange={(e)=>setQuery(e.target.value)} placeholder="Search invoice or student…"/></div><button className="ops-button secondary" onClick={load} disabled={loading}>↻ Refresh</button></div>
    {error&&<Notice type="error">{error}</Notice>}
    {loading?<div className="ops-loading">Loading invoices…</div>:filtered.length?<div className="ops-table-wrap"><table className="ops-table"><thead><tr><th>Invoice</th><th>Student</th><th>Due date</th><th>Total</th><th>Paid</th><th>Balance</th><th>Status</th><th>Actions</th></tr></thead><tbody>{filtered.map((inv)=>{
      const total=get(inv,'totalAmount')??0;const paid=get(inv,'amountPaid')??0;const balance=get(inv,'balance')??(Number(total)-Number(paid));const status=inv.status||'ISSUED';
      return <tr key={inv.id}><td><b>{inv.invoiceNumber||('Invoice #'+inv.id)}</b><small>#{inv.id} · {inv.academicYear}</small></td><td>{inv.studentName||('Student #'+inv.studentId)}</td><td>{inv.dueDate}</td><td>{money(total)}</td><td>{money(paid)}</td><td><b>{money(balance)}</b></td><td><Badge value={status}/></td><td><div className="ops-inline-actions">
        <button className="ops-text-button" onClick={()=>showHistory(inv)}>Payment history</button>
        {admin&&status==='DRAFT'&&<button className="ops-text-button" disabled={saving} onClick={()=>issueInvoice(inv)}>Issue</button>}
        {admin&&['DRAFT','ISSUED'].includes(status)&&<button className="ops-text-button danger" disabled={saving} onClick={()=>cancelInvoice(inv)}>Cancel</button>}
        {(guardian||admin)&&status==='ISSUED'&&Number(balance)>0&&<button className="ops-text-button" onClick={()=>{setPaymentInvoice(inv);setPayment({amount:String(balance)});}}>Pay</button>}
      </div></td></tr>;
    })}</tbody></table></div>:<Empty title="No fee invoices yet" description={admin?'Create an invoice, add fee items and issue it to make it payable.':'No invoices are currently linked to your account.'} action={admin&&<button className="ops-button primary" onClick={()=>setShowCreate(true)}>Create first invoice</button>}/>}
    <div className="ops-info-strip"><span>ⓘ</span><p>Payments are simulated only. No real money is collected or transferred. All roles use the same SIMULATED payment method.</p></div>
    {showCreate&&<Modal title="Create fee invoice" subtitle="Create the invoice, add its first line item and issue it." onClose={()=>setShowCreate(false)}><form className="ops-form" onSubmit={createInvoice}>
      <label>Student<select value={draft.studentId} onChange={(e)=>setDraft({...draft,studentId:e.target.value})} required><option value="">Select student</option>{students.map((s)=><option key={s.id} value={s.id}>{s.admissionNumber} · {s.firstName} {s.lastName}</option>)}</select></label>
      <label>Invoice number<input value={draft.invoiceNumber} onChange={(e)=>setDraft({...draft,invoiceNumber:e.target.value})} maxLength="40" required/></label>
      <label>Academic year<input value={draft.academicYear} onChange={(e)=>setDraft({...draft,academicYear:e.target.value})} pattern="[0-9]{4}-[0-9]{4}" required/></label>
      <div className="ops-form-grid"><label>Issue date<input type="date" value={draft.issueDate} onChange={(e)=>setDraft({...draft,issueDate:e.target.value})} required/></label><label>Due date<input type="date" min={draft.issueDate} value={draft.dueDate} onChange={(e)=>setDraft({...draft,dueDate:e.target.value})} required/></label></div>
      <label>Fee description<input value={draft.description} onChange={(e)=>setDraft({...draft,description:e.target.value})} maxLength="150" required/></label>
      <label>Amount (INR)<input type="number" min="0.01" step="0.01" value={draft.amount} onChange={(e)=>setDraft({...draft,amount:e.target.value})} required/></label>
      <div className="ops-form-footer"><button type="button" className="ops-button secondary" onClick={()=>setShowCreate(false)}>Cancel</button><button className="ops-button primary" disabled={saving||!students.length}>{saving?'Creating…':'Create and issue invoice'}</button></div>
    </form></Modal>}
    {paymentInvoice&&<Modal title="Record simulated payment" subtitle={paymentInvoice.invoiceNumber+' · Balance '+money(paymentInvoice.balance)} onClose={()=>setPaymentInvoice(null)}><form className="ops-form" onSubmit={payInvoice}>
      <label>Payment amount (INR)<input type="number" min="0.01" max={paymentInvoice.balance} step="0.01" value={payment.amount} onChange={(e)=>setPayment({...payment,amount:e.target.value})} required/></label>
      <div className="ops-payment-note">✓ SIMULATED payment · no external payment provider is contacted.</div>
      <div className="ops-form-footer"><button type="button" className="ops-button secondary" onClick={()=>setPaymentInvoice(null)}>Cancel</button><button className="ops-button primary" disabled={saving}>{saving?'Recording…':'Record payment'}</button></div>
    </form></Modal>}
    {historyInvoice&&<Modal title="Payment history" subtitle={historyInvoice.invoiceNumber+' · '+historyInvoice.studentName} onClose={()=>setHistoryInvoice(null)}>
      {payments.length?<div className="ops-table-wrap"><table className="ops-table"><thead><tr><th>Reference</th><th>Amount</th><th>Method</th><th>Status</th><th>Paid at</th></tr></thead><tbody>{payments.map((p)=><tr key={p.id}><td>{p.paymentReference}</td><td>{money(p.amount)}</td><td>{pretty(p.paymentMethod)}</td><td><Badge value={p.paymentStatus}/></td><td>{p.paidAt?new Date(p.paidAt).toLocaleString('en-IN'):'—'}</td></tr>)}</tbody></table></div>:<Empty title="No payments recorded" description="Successful simulated payments will appear here."/>}
    </Modal>}
  </section>;
}


const dateDaysAgo = (days) => {
  const value = new Date();
  value.setDate(value.getDate() - days);
  return new Date(value.getTime() - value.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
};

function AttendanceSummaryCards({ summary }) {
  if (!summary) return null;
  return <div className="ops-stat-row ops-stat-row-wide">
    <div className="ops-stat"><b>{summary.attendancePercentage ?? 0}%</b><span>Attendance rate</span></div>
    <div className="ops-stat"><b>{summary.presentCount ?? 0}</b><span>Present</span></div>
    <div className="ops-stat"><b>{summary.absentCount ?? 0}</b><span>Absent</span></div>
    <div className="ops-stat"><b>{summary.lateCount ?? 0}</b><span>Late</span></div>
    <div className="ops-stat"><b>{summary.totalRecords ?? 0}</b><span>Recorded sessions</span></div>
  </div>;
}

function AttendanceHistoryTable({ rows, emptyTitle = 'No attendance records in this period' }) {
  return rows.length ? <div className="ops-table-wrap"><table className="ops-table">
    <thead><tr><th>Date</th><th>Class</th><th>Section</th><th>Session</th><th>Attendance</th><th>Notes</th></tr></thead>
    <tbody>{rows.map((record) => <tr key={record.attendanceRecordId}>
      <td>{record.attendanceDate}</td>
      <td>{record.className}</td>
      <td>{record.sectionName}</td>
      <td>{pretty(record.sessionCode)}</td>
      <td><Badge value={record.attendanceStatus}/></td>
      <td>{record.remarks || '—'}</td>
    </tr>)}</tbody>
  </table></div> : <Empty title={emptyTitle} description="Attendance appears here after a teacher submits the class register." />;
}

export function StudentAttendanceWorkspace({ token }) {
  const [fromDate, setFromDate] = useState(() => dateDaysAgo(90));
  const [toDate, setToDate] = useState(today());
  const [summary, setSummary] = useState(null);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [applied, setApplied] = useState({ fromDate: dateDaysAgo(90), toDate: today() });

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try {
      const query = '?fromDate=' + encodeURIComponent(applied.fromDate) + '&toDate=' + encodeURIComponent(applied.toDate);
      const [summaryData, historyData] = await Promise.all([
        apiRequest('/api/v1/student/attendance/summary' + query, { token }),
        apiRequest('/api/v1/student/attendance/history' + query, { token })
      ]);
      setSummary(summaryData);
      setHistory(rowsFrom(historyData));
    } catch (e) { setError(messageFrom(e)); setSummary(null); setHistory([]); }
    finally { setLoading(false); }
  }, [token, applied]);
  useEffect(() => { load(); }, [load]);

  function applyRange(e) {
    e.preventDefault();
    if (toDate < fromDate) { setError('End date cannot be earlier than start date.'); return; }
    setApplied({ fromDate, toDate });
  }

  return <section className="ops-workspace">
    <SectionTitle eyebrow="STUDENT LIFE" title="My attendance" description="Track your attendance rate and review submitted class registers." action={<button className="ops-button secondary" onClick={load} disabled={loading}>↻ Refresh</button>}/>
    <form className="ops-filter-panel" onSubmit={applyRange}>
      <label>From date<input type="date" value={fromDate} max={toDate} onChange={e=>setFromDate(e.target.value)}/></label>
      <label>To date<input type="date" value={toDate} min={fromDate} max={today()} onChange={e=>setToDate(e.target.value)}/></label>
      <button className="ops-button primary" disabled={loading}>Apply period</button>
      <span className="ops-muted">Showing {applied.fromDate} to {applied.toDate}</span>
    </form>
    {error && <Notice type="error">{error}</Notice>}
    {loading ? <div className="ops-loading">Loading your attendance…</div> : <>
      <AttendanceSummaryCards summary={summary}/>
      <div className="ops-panel-heading"><div><span className="eyebrow">ATTENDANCE REGISTER</span><h3>Recent records</h3></div><span className="ops-count">{history.length} records</span></div>
      <AttendanceHistoryTable rows={history}/>
    </>}
  </section>;
}

export function StudentFeesWorkspace({ token }) {
  const [invoices, setInvoices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState(null);
  const [payments, setPayments] = useState([]);
  const [detailLoading, setDetailLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try {
      setInvoices(rowsFrom(await apiRequest('/api/v1/student/fees/invoices?page=0&size=100', { token })));
    } catch (e) { setError(messageFrom(e)); setInvoices([]); }
    finally { setLoading(false); }
  }, [token]);
  useEffect(() => { load(); }, [load]);

  async function openInvoice(invoice) {
    setSelected(invoice);
    setPayments([]);
    setDetailLoading(true);
    try {
      const [detail, history] = await Promise.all([
        apiRequest('/api/v1/student/fees/invoices/' + invoice.id, { token }),
        apiRequest('/api/v1/student/fees/invoices/' + invoice.id + '/payments', { token })
      ]);
      setSelected(detail);
      setPayments(rowsFrom(history));
    } catch (e) {
      setError(messageFrom(e));
    } finally {
      setDetailLoading(false);
    }
  }

  const filtered = invoices.filter(invoice => JSON.stringify(invoice).toLowerCase().includes(query.toLowerCase()));
  const totalDue = invoices.reduce((sum, invoice) => sum + Number(invoice.balance || 0), 0);
  const totalPaid = invoices.reduce((sum, invoice) => sum + Number(invoice.amountPaid || 0), 0);
  const totalBilled = invoices.reduce((sum, invoice) => sum + Number(invoice.totalAmount || 0), 0);

  return <section className="ops-workspace">
    <SectionTitle eyebrow="STUDENT FINANCE" title="My fee summary" description="Review issued invoices, payments received and the remaining balance. Payments are handled by your guardian." action={<button className="ops-button secondary" onClick={load} disabled={loading}>↻ Refresh</button>}/>
    {error && <Notice type="error">{error}</Notice>}
    {loading ? <div className="ops-loading">Loading your fee ledger…</div> : <>
      <div className="ops-stat-row ops-stat-row-wide">
        <div className="ops-stat"><b>{money(totalBilled)}</b><span>Total billed</span></div>
        <div className="ops-stat"><b>{money(totalPaid)}</b><span>Paid</span></div>
        <div className="ops-stat"><b>{money(totalDue)}</b><span>Outstanding</span></div>
        <div className="ops-stat"><b>{invoices.length}</b><span>Issued invoices</span></div>
      </div>
      <div className="ops-toolbar"><label className="ops-search"><span>⌕</span><input value={query} onChange={e=>setQuery(e.target.value)} placeholder="Search invoices…"/></label><span className="ops-count">{filtered.length} invoices</span></div>
      {filtered.length ? <div className="ops-table-wrap"><table className="ops-table">
        <thead><tr><th>Invoice</th><th>Academic year</th><th>Issued</th><th>Due date</th><th>Total</th><th>Paid</th><th>Balance</th><th>Status</th><th>Details</th></tr></thead>
        <tbody>{filtered.map(invoice=><tr key={invoice.id}>
          <td><b>{invoice.invoiceNumber}</b><small>#{invoice.id}</small></td>
          <td>{invoice.academicYear}</td><td>{invoice.issueDate}</td><td>{invoice.dueDate}</td>
          <td>{money(invoice.totalAmount)}</td><td>{money(invoice.amountPaid)}</td><td><b>{money(invoice.balance)}</b></td>
          <td><Badge value={invoice.status}/></td><td><button className="ops-text-button" onClick={()=>openInvoice(invoice)}>Invoice details</button></td>
        </tr>)}</tbody>
      </table></div> : <Empty title="No fee invoices yet" description="Issued school invoices will appear here when they are assigned to your student record."/>}
      <div className="ops-info-strip"><span>ⓘ</span><p>This is a read-only student view. Your guardian or the school office handles simulated payment recording.</p></div>
    </>}
    {selected && <Modal title={selected.invoiceNumber} subtitle={'Issued fee invoice · ' + selected.academicYear} onClose={()=>setSelected(null)}>
      <div className="ops-invoice-detail">
        {(selected.items||[]).map(item=><div className="ops-invoice-line" key={item.id}><span>{item.description}</span><b>{money(item.amount)}</b></div>)}
        <div className="ops-invoice-total"><span>Invoice total</span><b>{money(selected.totalAmount)}</b></div>
        <div className="ops-invoice-total"><span>Amount paid</span><b>{money(selected.amountPaid)}</b></div>
        <div className="ops-invoice-total balance"><span>Outstanding balance</span><b>{money(selected.balance)}</b></div>
        <div className="ops-panel-heading"><div><span className="eyebrow">PAYMENT RECORD</span><h3>Simulated payment history</h3></div><span className="ops-count">{payments.length} entries</span></div>
        {detailLoading ? <div className="ops-loading">Loading payment history…</div> : payments.length ? <div className="ops-table-wrap"><table className="ops-table"><thead><tr><th>Reference</th><th>Paid amount</th><th>Method</th><th>Status</th><th>Date</th></tr></thead><tbody>{payments.map(payment=><tr key={payment.id}><td>{payment.paymentReference}</td><td>{money(payment.amount)}</td><td>{pretty(payment.paymentMethod)}</td><td><Badge value={payment.paymentStatus}/></td><td>{payment.paidAt?new Date(payment.paidAt).toLocaleString('en-IN'):'—'}</td></tr>)}</tbody></table></div> : <Empty title="No payments recorded" description="Your guardian or the school office has not recorded a payment for this invoice yet."/>}
      </div>
    </Modal>}
  </section>;
}

export function GuardianAttendanceWorkspace({ token }) {
  const [students, setStudents] = useState([]);
  const [studentId, setStudentId] = useState('');
  const [fromDate, setFromDate] = useState(() => dateDaysAgo(90));
  const [toDate, setToDate] = useState(today());
  const [summary, setSummary] = useState(null);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [studentsLoading, setStudentsLoading] = useState(true);

  useEffect(() => {
    let alive = true;
    apiRequest('/api/v1/guardian/students', { token })
      .then(data => {
        if (!alive) return;
        const linked = rowsFrom(data);
        setStudents(linked);
        setStudentId(linked[0]?.id ? String(linked[0].id) : '');
      })
      .catch(e => { if (alive) setError(messageFrom(e)); })
      .finally(() => { if (alive) setStudentsLoading(false); });
    return () => { alive = false; };
  }, [token]);

  const load = useCallback(async () => {
    if (!studentId) { setLoading(false); setSummary(null); setHistory([]); return; }
    setLoading(true); setError('');
    const query = '?fromDate=' + encodeURIComponent(fromDate) + '&toDate=' + encodeURIComponent(toDate);
    try {
      const [summaryData, historyData] = await Promise.all([
        apiRequest('/api/v1/guardian/students/' + encodeURIComponent(studentId) + '/attendance/summary' + query, { token }),
        apiRequest('/api/v1/guardian/students/' + encodeURIComponent(studentId) + '/attendance/history' + query, { token })
      ]);
      setSummary(summaryData);
      setHistory(rowsFrom(historyData));
    } catch (e) { setError(messageFrom(e)); setSummary(null); setHistory([]); }
    finally { setLoading(false); }
  }, [token, studentId, fromDate, toDate]);
  useEffect(() => { if (!studentsLoading) load(); }, [studentsLoading, load]);

  return <section className="ops-workspace">
    <SectionTitle eyebrow="FAMILY ACCESS" title="Student attendance" description="Review attendance summaries and submitted registers for students linked to your guardian account." action={<button className="ops-button secondary" onClick={load} disabled={loading || studentsLoading}>↻ Refresh</button>}/>
    {studentsLoading ? <div className="ops-loading">Loading linked students…</div> : students.length ? <>
      <div className="ops-filter-panel">
        <label>Linked student<select value={studentId} onChange={e=>setStudentId(e.target.value)}>{students.map(s=><option value={s.id} key={s.id}>{s.admissionNumber} · {s.firstName} {s.lastName}</option>)}</select></label>
        <label>From date<input type="date" value={fromDate} max={toDate} onChange={e=>setFromDate(e.target.value)}/></label>
        <label>To date<input type="date" value={toDate} min={fromDate} max={today()} onChange={e=>setToDate(e.target.value)}/></label>
      </div>
      {error && <Notice type="error">{error}</Notice>}
      {loading ? <div className="ops-loading">Loading attendance report…</div> : <>
        <AttendanceSummaryCards summary={summary}/>
        <div className="ops-panel-heading"><div><span className="eyebrow">CLASS REGISTERS</span><h3>Attendance history</h3></div><span className="ops-count">{history.length} records</span></div>
        <AttendanceHistoryTable rows={history}/>
      </>}
    </> : <Empty title="No linked students" description="A school administrator must connect a student to this guardian account before family information is displayed."/>}
  </section>;
}

export function TeacherClassReportsWorkspace({ token }) {
  const [sections, setSections] = useState([]);
  const [sectionId, setSectionId] = useState('');
  const [fromDate, setFromDate] = useState(() => dateDaysAgo(90));
  const [toDate, setToDate] = useState(today());
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let alive = true;
    apiRequest('/api/v1/teacher/roster/sections', { token })
      .then(data => {
        if (!alive) return;
        const assigned = rowsFrom(data);
        setSections(assigned);
        setSectionId(assigned[0]?.id ? String(assigned[0].id) : '');
      })
      .catch(e => { if (alive) setError(messageFrom(e)); })
      .finally(() => { if (alive) setLoading(false); });
    return () => { alive = false; };
  }, [token]);

  const load = useCallback(async () => {
    if (!sectionId) { setReport(null); return; }
    setLoading(true); setError('');
    try {
      const query = '?fromDate=' + encodeURIComponent(fromDate) + '&toDate=' + encodeURIComponent(toDate);
      setReport(await apiRequest('/api/v1/attendance/reports/sections/' + encodeURIComponent(sectionId) + query, { token }));
    } catch (e) { setError(messageFrom(e)); setReport(null); }
    finally { setLoading(false); }
  }, [token, sectionId, fromDate, toDate]);
  useEffect(() => { if (sectionId) load(); }, [sectionId, load]);

  return <section className="ops-workspace">
    <SectionTitle eyebrow="TEACHING INSIGHTS" title="Class reports" description="Monitor attendance across only the sections assigned to your teacher account." action={<button className="ops-button secondary" onClick={load} disabled={loading || !sectionId}>↻ Refresh</button>}/>
    {error && <Notice type="error">{error}</Notice>}
    <div className="ops-filter-panel">
      <label>Assigned section<select value={sectionId} onChange={e=>setSectionId(e.target.value)} disabled={!sections.length}>
        {sections.map(s=><option key={s.id} value={s.id}>{s.className} · {s.sectionName} · {s.academicYear}</option>)}
      </select></label>
      <label>From date<input type="date" value={fromDate} max={toDate} onChange={e=>setFromDate(e.target.value)}/></label>
      <label>To date<input type="date" value={toDate} min={fromDate} max={today()} onChange={e=>setToDate(e.target.value)}/></label>
      <button className="ops-button primary" onClick={load} disabled={loading || !sectionId}>Run report</button>
    </div>
    {loading ? <div className="ops-loading">Loading class report…</div> : report ? <>
      <div className="ops-stat-row ops-stat-row-wide">
        <div className="ops-stat"><b>{report.attendancePercentage ?? 0}%</b><span>Attendance rate</span></div>
        <div className="ops-stat"><b>{report.totalStudents ?? 0}</b><span>Students</span></div>
        <div className="ops-stat"><b>{report.totalSessions ?? 0}</b><span>Finalized sessions</span></div>
        <div className="ops-stat"><b>{report.presentCount ?? 0}</b><span>Present</span></div>
        <div className="ops-stat"><b>{report.absentCount ?? 0}</b><span>Absent</span></div>
      </div>
      <div className="ops-table-wrap"><table className="ops-table">
        <thead><tr><th>Student</th><th>Records</th><th>Present</th><th>Absent</th><th>Late</th><th>Excused</th><th>Attendance</th></tr></thead>
        <tbody>{(report.students||[]).map(s=><tr key={s.studentId}>
          <td><b>{s.studentName}</b><small>{s.admissionNumber}</small></td>
          <td>{s.totalRecords}</td><td>{s.presentCount}</td><td>{s.absentCount}</td><td>{s.lateCount}</td><td>{s.excusedCount}</td><td><b>{s.attendancePercentage}%</b></td>
        </tr>)}</tbody>
      </table></div>
    </> : <Empty title="No report available" description={sections.length ? 'Choose an assigned section and date range to generate its attendance report.' : 'No active section assignments are available for this teacher account.'}/>}
  </section>;
}

export function AuditWorkspace({ token }) {
  const [events,setEvents]=useState([]);const [loading,setLoading]=useState(true);const [error,setError]=useState('');const [query,setQuery]=useState('');
  const load=useCallback(async()=>{setLoading(true);setError('');try{setEvents(rowsFrom(await apiRequest('/api/v1/audit-logs',{token})));}catch(e){setError(messageFrom(e));}finally{setLoading(false);}},[token]);
  useEffect(()=>{load();},[load]);
  const filtered=events.filter((event)=>JSON.stringify(event).toLowerCase().includes(query.toLowerCase()));
  return <section className="ops-workspace">
    <SectionTitle eyebrow="GOVERNANCE & COMPLIANCE" title="Audit trail" description="A chronological record of important administrative and financial changes." action={<button className="ops-button secondary" onClick={load} disabled={loading}>↻ Refresh</button>}/>
    <div className="ops-toolbar"><div className="ops-search"><span>⌕</span><input value={query} onChange={(e)=>setQuery(e.target.value)} placeholder="Search actions, records or actor IDs…"/></div><span className="ops-count">{events.length} recorded events</span></div>
    {error&&<Notice type="error">{error}</Notice>}
    {loading?<div className="ops-loading">Loading audit events…</div>:filtered.length?<div className="ops-audit-list">{filtered.map((event,index)=><article className="ops-audit-item" key={event.id||index}>
      <div className="ops-audit-marker">◷</div><div className="ops-audit-content">
        <div className="ops-audit-heading"><b>{pretty(event.action)}</b><Badge value={event.entityType||'SYSTEM'}/></div>
        <p>{event.entityType||'Record'} #{event.entityId||'—'} · Actor user #{event.actorUserId||'—'}</p>
        <div className="ops-audit-meta"><span>Request {event.requestId||'—'}</span><span>{event.ipAddress||'IP unavailable'}</span><time>{event.occurredAt?new Date(event.occurredAt).toLocaleString('en-IN'):'Timestamp unavailable'}</time></div>
        {(event.oldValues||event.newValues)&&<details className="json-view"><summary>View recorded changes</summary><pre>{JSON.stringify({oldValues:event.oldValues,newValues:event.newValues},null,2)}</pre></details>}
      </div></article>)}</div>:<Empty title="No audit events recorded yet" description="Events appear here after tracked administrative and financial actions are performed."/>}
  </section>;
}


export function StudentResultsWorkspace({ token }) {
  const [exams,setExams]=useState([]);
  const [loading,setLoading]=useState(true);
  const [error,setError]=useState('');
  const [selectedExam,setSelectedExam]=useState('');
  const [report,setReport]=useState(null);
  const [busy,setBusy]=useState(false);

  const load=useCallback(async()=>{
    setLoading(true);setError('');
    try{
      const rows=rowsFrom(await apiRequest('/api/v1/student/examinations',{token}));
      setExams(rows);
      setSelectedExam((old)=>old || (rows[0]?.id ? String(rows[0].id) : ''));
    }catch(e){setError(messageFrom(e));setExams([]);}
    finally{setLoading(false);}
  },[token]);
  useEffect(()=>{load();},[load]);

  async function openReport(e){
    e.preventDefault();if(!selectedExam)return;
    setBusy(true);setError('');setReport(null);
    try{setReport(await apiRequest('/api/v1/student/examinations/'+encodeURIComponent(selectedExam)+'/report-card',{token}));}
    catch(e){setError(messageFrom(e));}
    finally{setBusy(false);}
  }

  return <section className="ops-workspace">
    <SectionTitle eyebrow="STUDENT PORTAL" title="My examination results" description="View published results for examinations in which you are enrolled." action={<button className="ops-button secondary" onClick={load} disabled={loading}>↻ Refresh</button>}/>
    {error&&<Notice type="error">{error}</Notice>}
    {loading?<div className="ops-loading">Loading your published examinations…</div>:exams.length?<form className="lookup-form" onSubmit={openReport}>
      <label>Published examination<select value={selectedExam} onChange={(e)=>{setSelectedExam(e.target.value);setReport(null);}} required>
        {exams.map((exam)=><option value={exam.id} key={exam.id}>{exam.examName} · {exam.academicYear} · {exam.className} {exam.sectionName}</option>)}
      </select></label>
      <button className="button primary" disabled={busy}>{busy?'Loading…':'View my report card →'}</button>
    </form>:<Empty title="No published results yet" description="Your examination report cards will appear here after the school publishes results."/>}
    {report&&<div className="ops-card">
      <div className="ops-card-head"><div><h3>{report.examName}</h3><p>{report.className} · {report.sectionName} · {report.academicYear}</p></div><span className="ops-badge good">Published</span></div>
      <div className="result-summary">
        <div><span>STUDENT</span><b>{report.studentName}</b></div>
        <div><span>PERCENTAGE</span><b>{report.percentage}%</b></div>
        <div><span>GRADE</span><b>{report.grade}</b></div>
        <div><span>RESULT</span><b>{report.passed?'Passed':'Not passed'}</b></div>
      </div>
      <div className="ops-table-wrap"><table className="ops-table"><thead><tr><th>Subject</th><th>Exam date</th><th>Marks</th><th>Maximum</th><th>Pass marks</th><th>Attendance</th><th>Result</th></tr></thead>
        <tbody>{(report.subjects||[]).map((subject)=><tr key={subject.examinationSubjectId}>
          <td><b>{subject.subjectName}</b><small>{subject.subjectCode}</small></td>
          <td>{subject.examinationDate}</td><td>{subject.marksObtained}</td><td>{subject.maxMarks}</td><td>{subject.passMarks}</td>
          <td>{subject.attendanceStatus}</td><td><span className={'ops-badge '+(subject.passed?'good':'warn')}>{subject.passed?'Passed':'Failed'}</span></td>
        </tr>)}</tbody>
      </table></div>
    </div>}
  </section>;
}
