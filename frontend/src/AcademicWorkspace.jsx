import { useCallback, useEffect, useMemo, useState } from 'react';
import { apiRequest, rowsFrom } from './api.js';

const CONFIG = {
  students: {
    title: 'Students', path: '/api/v1/academic/students', serverSearch: true,
    columns: ['admissionNumber', 'firstName', 'lastName', 'email', 'portalAccessEnabled', 'status'],
    createFields: [['admissionNumber','Admission number','text',true],['firstName','First name','text',true],['lastName','Last name','text',true],['dateOfBirth','Date of birth','date',false],['admissionDate','Admission date','date',true],['email','Student portal email','email',true],['phone','Phone number','tel',false],['initialPassword','Initial portal password (min. 12 characters)','password',true]],
    editFields: [['firstName','First name','text',true],['lastName','Last name','text',true],['dateOfBirth','Date of birth','date',false],['status','Status','enum',true,null,[{value:'ACTIVE',label:'Active'},{value:'INACTIVE',label:'Inactive'},{value:'GRADUATED',label:'Graduated'}]]],
    editable: true, deactivatePath:(r)=>'/api/v1/academic/students/'+r.id,
  },
  classes: { title:'Classes', path:'/api/v1/academic/classes', serverSearch:true, columns:['name','description'], createFields:[['name','Class name','text',true],['description','Description','text',false]], editFields:[['name','Class name','text',true],['description','Description','text',false]], editable:true },
  sections: { title:'Sections', path:'/api/v1/academic/sections', serverSearch:true, columns:['sectionName','className','academicYear'], createFields:[['schoolClassId','Class','select',true,'/api/v1/academic/classes'],['sectionName','Section name','text',true],['academicYear','Academic year','text',true,null,null,null,null,'2026-2027']], editFields:[['schoolClassId','Class','select',true,'/api/v1/academic/classes'],['sectionName','Section name','text',true],['academicYear','Academic year','text',true]], editable:true },
  subjects: { title:'Subjects', path:'/api/v1/academic/subjects', serverSearch:true, columns:['subjectCode','name','active'], createFields:[['subjectCode','Subject code','text',true],['name','Subject name','text',true]], editFields:[['subjectCode','Subject code','text',true],['name','Subject name','text',true]], editable:true, statusPath:(r)=>'/api/v1/academic/subjects/'+r.id+'/status' },
  enrollments: { title:'Enrollments', path:'/api/v1/academic/enrollments', columns:['admissionNumber','studentName','className','sectionName','academicYear','startDate','endDate','active'], createFields:[['studentId','Student','select',true,'/api/v1/academic/students'],['sectionId','Section','select',true,'/api/v1/academic/sections'],['startDate','Start date','date',true]], endPath:(r)=>'/api/v1/academic/enrollments/'+r.id+'/end' },
  assignments: { title:'Teacher assignments', path:'/api/v1/academic/teacher-assignments', columns:['employeeCode','teacherName','className','sectionName','subjectCode','subjectName','assignedOn','active'], createFields:[['teacherId','Teacher','select',true,'/api/v1/identity/teachers'],['sectionId','Section','select',true,'/api/v1/academic/sections'],['subjectId','Subject','select',true,'/api/v1/academic/subjects?active=true']], statusPath:(r)=>'/api/v1/academic/teacher-assignments/'+r.id+'/status' },
  guardians: { title:'Student–guardian links', path:'/api/v1/academic/student-guardians', columns:['studentName','admissionNumber','guardianId','relationship','primary'], createFields:[['studentId','Student','select',true,'/api/v1/academic/students'],['guardianId','Guardian','select',true,'/api/v1/identity/guardians'],['relationship','Relationship','text',true],['primary','Primary guardian','bool',true,null,[{value:'false',label:'No'},{value:'true',label:'Yes'}]]], statusPath:(r)=>'/api/v1/academic/student-guardians/'+r.studentId+'/'+r.guardianId+'/primary', unlinkPath:(r)=>'/api/v1/academic/student-guardians/'+r.studentId+'/'+r.guardianId },
};

const rowsOf = rowsFrom;
const errorText = (e) => e?.payload?.message || e?.message || 'The request could not be completed.';
const labelOf = (v) => String(v == null ? '—' : v).replaceAll('_',' ').replace(/\b\w/g,(c)=>c.toUpperCase());
const cellValue = (row,key) => {
  if (key === 'studentName' && row.firstName) return [row.firstName,row.lastName].filter(Boolean).join(' ');
  if (key === 'teacherName') return row.teacherName || row.name || row.email || '—';
  if (key === 'guardianName') return row.guardianName || row.name || row.email || '—';
  const v = key.split('.').reduce((a,p)=>a?.[p],row);
  if (v == null || v === '') return '—';
  if (typeof v === 'boolean') return v ? 'Yes' : 'No';
  if (typeof v === 'object') return Array.isArray(v) ? v.length+' items' : JSON.stringify(v);
  return String(v);
};

function Modal({title,description,onClose,children}) {
  return <div className="ops-modal-backdrop" onMouseDown={(e)=>{if(e.target===e.currentTarget)onClose();}}>
    <section className="ops-modal" role="dialog" aria-modal="true">
      <div className="ops-modal-head"><div><h3>{title}</h3><p>{description}</p></div><button className="ops-close" onClick={onClose} aria-label="Close">×</button></div>
      {children}
    </section>
  </div>;
}

export function AcademicWorkspace({item,token}) {
  const config = CONFIG[item.id];
  const [payload,setPayload] = useState(null);
  const [rows,setRows] = useState([]);
  const [loading,setLoading] = useState(true);
  const [saving,setSaving] = useState(false);
  const [error,setError] = useState('');
  const [notice,setNotice] = useState(null);
  const [query,setQuery] = useState('');
  const [page,setPage] = useState(0);
  const [mode,setMode] = useState('');
  const [selected,setSelected] = useState(null);
  const [draft,setDraft] = useState({});
  const [options,setOptions] = useState({});
  const [optionLoading,setOptionLoading] = useState(false);

  const load = useCallback(async (nextPage=page) => {
    setLoading(true); setError('');
    try {
      const params = new URLSearchParams({page:String(nextPage),size:'20'});
      if (config.serverSearch && query.trim()) params.set('search',query.trim());
      const data = await apiRequest(config.path+'?'+params.toString(),{token});
      setPayload(data); setRows(rowsOf(data)); setPage(nextPage);
    } catch(e) { setError(errorText(e)); setPayload(e.payload||null); setRows([]); }
    finally { setLoading(false); }
  },[config,page,query,token]);

  useEffect(()=>{load(0);},[item.id,token]);
  useEffect(()=>{
    if(!config.serverSearch)return;
    const handle=setTimeout(()=>load(0),300);
    return ()=>clearTimeout(handle);
  },[query]);

  async function openForm(nextMode,row=null) {
    const fields = nextMode==='edit' ? (config.editFields||config.createFields) : config.createFields;
    setSelected(row); setMode(nextMode); setNotice(null); setOptions({});
    const values={};
    fields.forEach((f)=>{
      const initial=f[8];
      const fallback=(f[2]==='date' && f[3]) ? new Date().toISOString().slice(0,10) : (f[2]==='bool' ? 'false' : '');
      values[f[0]]=nextMode==='edit' ? (row?.[f[0]] == null ? '' : String(row[f[0]])) : (initial ?? fallback);
    });
    setDraft(values);
    const selectable=fields.filter((f)=>f[4]);
    if(selectable.length){
      setOptionLoading(true);
      const pairs=await Promise.all(selectable.map(async (f)=>{
        try {
          const optionPath=f[4]+(f[4].includes('?')?'&':'?')+'size=100';
          const data=await apiRequest(optionPath,{token});
          const found=rowsOf(data);
          const labelFn=f[7];
          return [f[0],found.map((x)=>({value:x[f[6]||'id'],label:typeof labelFn==='function'?labelFn(x):labelFn==='name'?x.name:(x.employeeCode||x.name||(x.className&&x.sectionName?(x.className+' · '+x.sectionName):null)||x.sectionName||x.subjectName||x.admissionNumber||(x.alternatePhone?('Guardian #'+x.id+' · '+x.alternatePhone):null)||x.email||('Record #'+x.id))}))];
        } catch { return [f[0],[]]; }
      }));
      setOptions(Object.fromEntries(pairs)); setOptionLoading(false);
    }
  }

  async function submit(e) {
    e.preventDefault(); setSaving(true); setNotice(null);
    try {
      const fields=mode==='edit'?(config.editFields||config.createFields):config.createFields;
      const body={};
      fields.forEach((f)=>{
        const name=f[0],type=f[2],raw=draft[name];
        body[name]=type==='number'||type==='select' ? Number(raw) : type==='bool' ? raw==='true' : (raw===''?null:raw);
      });
      const path=mode==='edit'?config.path+'/'+selected.id:config.path;
      await apiRequest(path,{method:mode==='edit'?'PUT':'POST',token,body});
      setMode(''); setNotice({type:'success',text:mode==='edit'?'Record updated successfully.':'Record created successfully.'});
      await load(0);
    } catch(e) { setNotice({type:'error',text:errorText(e)}); }
    finally { setSaving(false); }
  }

  async function action(kind,row) {
    if(saving)return;
    const path=kind==='end'?config.endPath(row):kind==='deactivate'?config.deactivatePath(row):kind==='unlink'?config.unlinkPath(row):config.statusPath(row);
    const question=kind==='end'?'End this enrollment?':kind==='deactivate'?'Deactivate this student?':kind==='unlink'?'Remove this student–guardian link?':'Change this record status?';
    if(!window.confirm(question))return;
    setSaving(true); setNotice(null);
    try {
      const body=kind==='status'?(item.id==='guardians'?{primary:!row.primary}:{active:!row.active}):undefined;
      await apiRequest(path,{method:['unlink','deactivate'].includes(kind)?'DELETE':'PATCH',token,body});
      setNotice({type:'success',text:kind==='end'?'Enrollment ended.':kind==='deactivate'?'Student deactivated.':kind==='unlink'?'Guardian link removed.':'Status updated.'});
      await load(page);
    } catch(e) { setNotice({type:'error',text:errorText(e)}); }
    finally { setSaving(false); }
  }

  const filtered=useMemo(()=>{
    if(config.serverSearch||!query.trim())return rows;
    const q=query.toLowerCase();
    return rows.filter((r)=>JSON.stringify(r).toLowerCase().includes(q));
  },[rows,query,config.serverSearch]);
  const total=payload?.totalElements??rows.length;
  const pages=payload?.totalPages??1;
  const fields=mode==='edit'?(config.editFields||config.createFields):config.createFields;

  if(!config)return <section className="ops-workspace"><div className="ops-notice error">This academic workspace is not configured.</div></section>;
  return <section className="ops-workspace">
    {notice&&<div className={'ops-notice '+notice.type} role="status"><span>{notice.type==='success'?'✓':'!'}</span><div>{notice.text}</div><button onClick={()=>setNotice(null)}>×</button></div>}
    <div className="ops-title"><div><span className="eyebrow">ACADEMIC ADMINISTRATION</span><h2>{config.title}</h2><p>Create, review and maintain {config.title.toLowerCase()} records.</p></div><button className="ops-button primary" onClick={()=>openForm('create')}>＋ Add {config.title.endsWith('s')?config.title.slice(0,-1):'record'}</button></div>
    {error&&<div className="ops-notice error"><span>!</span><div>{error}</div></div>}
    <div className="ops-toolbar"><label className="ops-search"><span>⌕</span><input value={query} onChange={(e)=>setQuery(e.target.value)} placeholder={'Search '+config.title.toLowerCase()+'…'}/></label><span className="ops-count">{total} records</span><button className="ops-button secondary" onClick={()=>load(page)} disabled={loading}>↻ Refresh</button></div>
    {loading?<div className="ops-loading">Loading {config.title.toLowerCase()}…</div>:filtered.length?<div className="ops-table-wrap"><table className="ops-table"><thead><tr>{['ID',...config.columns,'Actions'].map((c)=><th key={c}>{labelOf(c)}</th>)}</tr></thead><tbody>{filtered.map((row)=><tr key={row.id||String(row.studentId)+'-'+String(row.guardianId)}><td>{row.id??'—'}</td>{config.columns.map((c)=><td key={c}>{cellValue(row,c)}</td>)}<td><div className="ops-inline-actions">
      {config.editable&&<button className="ops-text-button" onClick={()=>openForm('edit',row)}>Edit</button>}
      {config.deactivatePath&&<button className="ops-text-button danger" onClick={()=>action('deactivate',row)}>Deactivate</button>}
      {config.endPath&&row.active&&<button className="ops-text-button danger" onClick={()=>action('end',row)}>End</button>}
      {config.statusPath&&<button className="ops-text-button" onClick={()=>action('status',row)}>{item.id==='guardians'?(row.primary?'Remove primary':'Make primary'):(row.active?'Deactivate':'Activate')}</button>}
      {config.unlinkPath&&<button className="ops-text-button danger" onClick={()=>action('unlink',row)}>Unlink</button>}
    </div></td></tr>)}</tbody></table></div>:<div className="ops-empty"><div className="ops-empty-icon">▤</div><h3>No {config.title.toLowerCase()} found</h3><p>{error?'The data could not be loaded for this account.':'Create your first record to get started.'}</p><button className="ops-button primary" onClick={()=>openForm('create')}>Add first record</button></div>}
    <div className="ops-form-footer"><span className="ops-muted">Page {page+1} of {Math.max(pages,1)}</span><div className="ops-actions"><button className="ops-button secondary" disabled={page<=0||loading} onClick={()=>load(page-1)}>Previous</button><button className="ops-button secondary" disabled={page+1>=pages||loading} onClick={()=>load(page+1)}>Next</button></div></div>
    {mode&&<Modal title={(mode==='edit'?'Edit ':'Add ')+config.title} description="Enter the record details. The server validates all fields before saving." onClose={()=>setMode('')}>
      <form className="ops-form" onSubmit={submit}>{fields.map((f)=><label key={f[0]}>{f[1]}{['select','enum','bool'].includes(f[2])?<select required={f[3]} value={draft[f[0]]||''} disabled={optionLoading} onChange={(e)=>setDraft((old)=>({...old,[f[0]]:e.target.value}))}><option value="">{optionLoading?'Loading options…':'Select…'}</option>{(f[5]||options[f[0]]||[]).map((o)=><option key={o.value??o.id} value={o.value??o.id}>{o.label??o.name}</option>)}</select>:<input type={f[2]} required={f[3]} value={draft[f[0]]??''} onChange={(e)=>setDraft((old)=>({...old,[f[0]]:e.target.value}))}/>}</label>)}
      <div className="ops-form-footer"><button type="button" className="ops-button secondary" onClick={()=>setMode('')}>Cancel</button><button className="ops-button primary" disabled={saving||optionLoading}>{saving?'Saving…':mode==='edit'?'Save changes':'Create record'}</button></div></form>
    </Modal>}
  </section>;
}
