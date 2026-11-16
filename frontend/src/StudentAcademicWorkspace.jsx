import { useEffect, useState } from 'react';
import { apiRequest } from './api.js';

const messageFrom = e => e?.payload?.message || e?.message || 'Student academic details could not be loaded.';

export function StudentAcademicWorkspace({ token, onNavigate }) {
  const [overview,setOverview] = useState(null);
  const [loading,setLoading] = useState(true);
  const [error,setError] = useState('');

  useEffect(()=>{
    let alive=true;
    setLoading(true);
    apiRequest('/api/v1/student/academics',{token})
      .then(data=>{if(alive){setOverview(data?.data||data);setError('');}})
      .catch(e=>{if(alive)setError(messageFrom(e));})
      .finally(()=>{if(alive)setLoading(false);});
    return ()=>{alive=false;};
  },[token]);

  const student=overview?.student||{};
  const subjects=overview?.subjects||[];
  const hasEnrollment=Boolean(student.sectionId || student.sectionName);

  return <section className="student-academics-workspace">
    <div className="student-academics-hero">
      <div><span className="eyebrow light">YOUR LEARNING PROFILE</span><h2>{[student.firstName,student.lastName].filter(Boolean).join(' ')||'My academics'}</h2><p>{student.admissionNumber ? 'Admission '+student.admissionNumber : 'Your school academic details'}</p></div>
      <div className="student-academics-year">{student.academicYear||'Academic year not assigned'}</div>
    </div>
    {error&&<div className="ops-notice error" role="alert">{error}</div>}
    {loading?<div className="ops-loading">Loading your academic information…</div>:<>
      <div className="student-academic-summary">
        <article><span>CURRENT CLASS</span><b>{student.className||'Not assigned'}</b></article>
        <article><span>SECTION</span><b>{student.sectionName||'Not assigned'}</b></article>
        <article><span>ACADEMIC YEAR</span><b>{student.academicYear||'—'}</b></article>
        <article><span>ENROLLED FROM</span><b>{student.enrollmentStartDate||'—'}</b></article>
      </div>
      <div className="student-academic-panel">
        <div className="student-academic-panel-head"><div><span className="eyebrow">SUBJECT DIRECTORY</span><h3>My subjects and teachers</h3><p>Subjects are taken from your current active class assignment.</p></div><span className="student-subject-count">{subjects.length} subject(s)</span></div>
        {!hasEnrollment ? <div className="ops-empty"><div className="ops-empty-icon">▦</div><h3>No active class enrollment</h3><p>Contact the school administrator to confirm your current class and section.</p></div> : subjects.length ? <div className="student-subject-grid">
          {subjects.map(subject=><article className="student-subject-card" key={subject.subjectId}>
            <div className="student-subject-icon">▤</div><div className="student-subject-content"><span>{subject.subjectCode}</span><h4>{subject.subjectName}</h4><p>{subject.teacherName||'Teacher not assigned'}</p>{subject.teacherEmail&&<a href={'mailto:'+subject.teacherEmail}>{subject.teacherEmail}</a>}</div>
            <small>Assigned {subject.assignedOn||'date unavailable'}</small>
          </article>)}
        </div> : <div className="ops-empty"><div className="ops-empty-icon">▤</div><h3>No active subjects or teachers listed</h3><p>Your section is assigned, but subject-teacher assignments are not available yet. Check with the school office.</p></div>}
      </div>
      <div className="student-learning-links">
        <button onClick={()=>onNavigate('myattendance')}><span>✓</span><div><b>Attendance history</b><small>Review registers and attendance rate</small></div><i>→</i></button>
        <button onClick={()=>onNavigate('myresults')}><span>▤</span><div><b>Examination results</b><small>View published report cards</small></div><i>→</i></button>
        <button onClick={()=>onNavigate('myfees')}><span>₹</span><div><b>Fee statement</b><small>Review invoices and payments</small></div><i>→</i></button>
      </div>
    </>}
  </section>;
}
