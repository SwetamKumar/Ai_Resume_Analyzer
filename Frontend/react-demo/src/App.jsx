import { useState, useRef } from 'react'
import { analyzeResume, analyzeText, fixResume, downloadResumePdf } from './services/api'
import './App.css'

function ScoreRing({ score }) {
  const radius = 54
  const circ = 2 * Math.PI * radius
  const offset = circ - (score / 100) * circ
  const color = score >= 70 ? '#22c55e' : score >= 50 ? '#eab308' : '#ef4444'
  return (
    <div className="score-ring-wrap">
      <svg width="140" height="140" viewBox="0 0 140 140">
        <circle cx="70" cy="70" r={radius} fill="none" stroke="#1a1a24" strokeWidth="12" />
        <circle cx="70" cy="70" r={radius} fill="none" stroke={color} strokeWidth="12"
          strokeDasharray={circ} strokeDashoffset={offset} strokeLinecap="round"
          transform="rotate(-90 70 70)" style={{ transition: 'stroke-dashoffset 1s ease' }} />
      </svg>
      <div className="score-ring-inner">
        <span className="score-number" style={{ color }}>{score}</span>
        <span className="score-label">/ 100</span>
      </div>
    </div>
  )
}

function Chip({ text, type }) {
  return <span className={`chip chip-${type}`}>{text}</span>
}

export default function App() {
  // ── Input state ────────────────────────────────────────────────────────────
  const [mode, setMode]             = useState('file')
  const [resumeFile, setResumeFile] = useState(null)
  const [resumeText, setResumeText] = useState('')
  const [jobDesc, setJobDesc]       = useState('')
  const [dragging, setDragging]     = useState(false)

  // ── Analyze state ──────────────────────────────────────────────────────────
  const [loading, setLoading]               = useState(false)
  const [result, setResult]                 = useState(null)   // AnalysisResponse
  const [extractedText, setExtractedText]   = useState('')     // text from PDF/input — used by fix
  const [error, setError]                   = useState('')

  // ── Fix state ──────────────────────────────────────────────────────────────
  const [fixing, setFixing]               = useState(false)
  const [fixResult, setFixResult]         = useState(null)
  const [editedResume, setEditedResume]   = useState('')
  const [fixError, setFixError]           = useState('')

  // ── Download state ─────────────────────────────────────────────────────────
  const [downloading, setDownloading]     = useState(false)
  const [candidateName, setCandidateName] = useState('')

  const fileRef = useRef()

  // ── File handling ──────────────────────────────────────────────────────────
  const handleFile = (file) => {
    if (file && (file.type === 'application/pdf' || file.type === 'text/plain')) {
      setResumeFile(file); setError('')
    } else {
      setError('Only PDF or .txt files are supported.')
    }
  }

  // ── Analyze ────────────────────────────────────────────────────────────────
  const handleSubmit = async () => {
    setError(''); setFixResult(null); setEditedResume(''); setExtractedText('')
    if (!jobDesc.trim())                              { setError('Please enter a job description.'); return }
    if (mode === 'file' && !resumeFile)               { setError('Please upload your resume.'); return }
    if (mode === 'text' && !resumeText.trim())        { setError('Please paste your resume text.'); return }

    setLoading(true); setResult(null)
    try {
      // Both endpoints now return { analysis, extractedText }
      const wrapper = mode === 'file'
        ? await analyzeResume(resumeFile, jobDesc)
        : await analyzeText(resumeText, jobDesc)

      setResult(wrapper.analysis)
      setExtractedText(wrapper.extractedText)  // store for fix step
    } catch (err) {
      setError(err.response?.data || 'Something went wrong. Is the backend running?')
    } finally {
      setLoading(false)
    }
  }

  // ── Fix ────────────────────────────────────────────────────────────────────
  const handleFix = async () => {
    setFixError('')
    if (!extractedText.trim()) {
      setFixError('No resume text found. Please re-analyze your resume first.')
      return
    }
    setFixing(true)
    try {
      const data = await fixResume(extractedText, jobDesc, result.missingSkills, result.improvements)
      setFixResult(data)
      setEditedResume(data.fixedResumeText)
    } catch (err) {
      setFixError(err.response?.data || 'Fix failed. Please try again.')
    } finally {
      setFixing(false)
    }
  }

  // ── Download PDF ───────────────────────────────────────────────────────────
  const handleDownload = async () => {
    if (!editedResume.trim()) { setFixError('No resume content to download.'); return }
    setDownloading(true)
    try {
      await downloadResumePdf(editedResume, candidateName)
    } catch {
      setFixError('PDF download failed. Please try again.')
    } finally {
      setDownloading(false)
    }
  }

  const verdictClass = result ? ({
    'Strong Match': 'verdict-green',
    'Good Match':   'verdict-blue',
    'Needs Work':   'verdict-yellow',
    'Poor Match':   'verdict-red'
  }[result.verdict] || '') : ''

  // ── Render ─────────────────────────────────────────────────────────────────
  return (
    <div className="app">
      <header className="header">
        <div className="header-inner">
          <div className="logo">
            <span className="logo-icon">⚡</span>
            <span className="logo-text">ResumeAI</span>
          </div>
          <p className="header-sub">Analyze · Fix · Download — all in one place.</p>
        </div>
        <div className="header-glow" />
      </header>

      <main className="main">

        {/* ── Step 1: Input ── */}
        <div className="step-label">STEP 1 — UPLOAD YOUR RESUME & JOB DESCRIPTION</div>
        <section className="input-section">
          <div className="card">
            <div className="card-title"><span>01</span> Your Resume</div>
            <div className="toggle-row">
              <button className={`toggle-btn ${mode==='file'?'active':''}`} onClick={()=>setMode('file')}>Upload PDF / TXT</button>
              <button className={`toggle-btn ${mode==='text'?'active':''}`} onClick={()=>setMode('text')}>Paste Text</button>
            </div>
            {mode === 'file' ? (
              <div className={`dropzone ${dragging?'dragging':''} ${resumeFile?'has-file':''}`}
                onDragOver={e=>{e.preventDefault();setDragging(true)}}
                onDragLeave={()=>setDragging(false)}
                onDrop={e=>{e.preventDefault();setDragging(false);handleFile(e.dataTransfer.files[0])}}
                onClick={()=>fileRef.current.click()}>
                <input ref={fileRef} type="file" accept=".pdf,.txt" style={{display:'none'}}
                  onChange={e=>handleFile(e.target.files[0])} />
                {resumeFile ? (
                  <div className="file-selected">
                    <span className="file-icon">📄</span>
                    <span className="file-name">{resumeFile.name}</span>
                    <span className="file-size">{(resumeFile.size/1024).toFixed(1)} KB</span>
                  </div>
                ) : (
                  <div className="dropzone-hint">
                    <span className="drop-icon">⬆</span>
                    <p>Drag & drop your resume here</p>
                    <p className="hint-small">PDF or TXT · max 5MB</p>
                  </div>
                )}
              </div>
            ) : (
              <textarea className="textarea" rows={10}
                placeholder="Paste your resume content here..."
                value={resumeText} onChange={e=>setResumeText(e.target.value)} />
            )}
          </div>

          <div className="card">
            <div className="card-title"><span>02</span> Job Description</div>
            <textarea className="textarea" rows={10}
              placeholder="Paste the full job description here..."
              value={jobDesc} onChange={e=>setJobDesc(e.target.value)} />
          </div>
        </section>

        {error && <div className="error-box">⚠ {error}</div>}

        <button className={`analyze-btn ${loading?'loading':''}`} onClick={handleSubmit} disabled={loading}>
          {loading
            ? <span className="spinner-row"><span className="spinner"/> Analyzing with AI...</span>
            : '✦ Analyze My Resume'}
        </button>

        {/* ── Step 2: Results ── */}
        {result && (
          <section className="results">
            <div className="step-label">STEP 2 — YOUR ANALYSIS RESULTS</div>

            <div className="result-hero">
              <ScoreRing score={result.matchScore} />
              <div className="result-hero-text">
                <div className={`verdict-badge ${verdictClass}`}>{result.verdict}</div>
                <p className="summary-text">{result.summary}</p>
              </div>
            </div>

            <div className="skills-grid">
              <div className="skill-card skill-green">
                <h3>✓ Matched Skills</h3>
                <div className="chips">
                  {result.matchedSkills?.length > 0
                    ? result.matchedSkills.map((s,i)=><Chip key={i} text={s} type="green"/>)
                    : <p className="empty-note">None identified</p>}
                </div>
              </div>
              <div className="skill-card skill-red">
                <h3>✗ Missing Skills</h3>
                <div className="chips">
                  {result.missingSkills?.length > 0
                    ? result.missingSkills.map((s,i)=><Chip key={i} text={s} type="red"/>)
                    : <p className="empty-note">None — great match!</p>}
                </div>
              </div>
            </div>

            <div className="card improvements-card">
              <div className="card-title"><span>💡</span> How to Improve</div>
              <ol className="improvements-list">
                {result.improvements?.map((tip,i)=><li key={i}>{tip}</li>)}
              </ol>
            </div>

            {/* ── Step 3: Fix ── */}
            <div className="step-label">STEP 3 — LET AI FIX YOUR RESUME</div>
            <div className="fix-banner">
              <div className="fix-banner-text">
                <div className="fix-banner-title">✦ AI Resume Fixer</div>
                <p>AI will rewrite your resume to naturally add missing skills and apply all improvements — keeping your real experience intact.</p>
              </div>
              <button className={`fix-btn ${fixing?'loading':''}`} onClick={handleFix} disabled={fixing}>
                {fixing
                  ? <span className="spinner-row"><span className="spinner"/>Fixing...</span>
                  : '🔧 Fix My Resume'}
              </button>
            </div>

            {fixError && <div className="error-box">⚠ {fixError}</div>}

            {/* ── Step 4: Edit + Download ── */}
            {fixResult && (
              <div className="fix-result">
                <div className="step-label">STEP 4 — REVIEW, EDIT & DOWNLOAD</div>

                <div className="changes-summary">
                  <span className="changes-icon">📝</span>
                  <p>{fixResult.changesSummary}</p>
                </div>

                <div className="name-row">
                  <label className="name-label">Your Name (for PDF header)</label>
                  <input className="name-input" type="text"
                    placeholder="e.g. Rahul Kumar"
                    value={candidateName} onChange={e=>setCandidateName(e.target.value)} />
                </div>

                <div className="editor-wrap">
                  <div className="editor-header">
                    <span className="editor-title">✏ Your Optimized Resume</span>
                    <span className="editor-hint">Edit freely before downloading</span>
                  </div>
                  <textarea className="textarea resume-editor" rows={24}
                    value={editedResume} onChange={e=>setEditedResume(e.target.value)} />
                </div>

                <button className={`download-btn ${downloading?'loading':''}`}
                  onClick={handleDownload} disabled={downloading || !editedResume.trim()}>
                  {downloading
                    ? <span className="spinner-row"><span className="spinner"/>Generating PDF...</span>
                    : '⬇ Download Optimized Resume as PDF'}
                </button>
              </div>
            )}

            <button className="reset-btn" onClick={()=>{
              setResult(null); setFixResult(null); setEditedResume('')
              setResumeFile(null); setResumeText(''); setJobDesc('')
              setError(''); setFixError(''); setExtractedText('')
            }}>↺ Start Over</button>
          </section>
        )}
      </main>

      <footer className="footer">
        Built with Spring Boot + OpenRouter AI · AI Resume Analyzer
      </footer>
    </div>
  )
}
