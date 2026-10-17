import { useEffect, useRef, useState } from 'react'
import '@fontsource-variable/archivo/wdth.css'
import '@fontsource-variable/martian-mono/wdth.css'
import '@fontsource-variable/unbounded/index.css'
import RepoMap from './RepoMap.jsx'
import ParticleWave from './ParticleWave.jsx'
import './App.css'

// The three things the backend does, in order. `at` is the second the label
// appears; the last one holds until the response arrives.
const STAGES = [
  { label: 'Cloning the repository', at: 0 },
  { label: 'Reading files for TODOs and disabled tests', at: 3 },
  { label: 'Ranking what a newcomer could finish', at: 8 },
]

const EXAMPLES = ['fastapi/fastapi', 'psf/requests', 'pallets/click']

function cleanName(value) {
  return value.trim().replace(/\.git$/, '').replace(/\/+$/, '')
}

// Accepts "owner/repo"; returns the full GitHub URL, or null if it doesn't look like one.
function toRepoUrl(value) {
  const v = cleanName(value)
  return /^[\w.-]+\/[\w.-]+$/.test(v) ? `https://github.com/${v}` : null
}

function messageFor(status) {
  if (status === 400 || status === 404) return 'That repository doesn’t exist or isn’t public.'
  if (status === 429) return 'Too many scans in a short time. Wait a minute, then try again.'
  return 'The scan failed on the server. Try again in a moment.'
}

export default function App() {
  const [input, setInput] = useState('')
  const [phase, setPhase] = useState('idle') // idle | loading | done | error
  const [results, setResults] = useState([])
  const [error, setError] = useState('')
  const [repo, setRepo] = useState('')
  const [elapsed, setElapsed] = useState(0)
  const [hoverRank, setHoverRank] = useState(null)
  const [selectedRank, setSelectedRank] = useState(0)
  const mapRef = useRef(null)

  // Count seconds while a scan runs; drives the stage list and the timer.
  useEffect(() => {
    if (phase !== 'loading') return
    const start = Date.now()
    setElapsed(0)
    const id = setInterval(() => setElapsed(Math.floor((Date.now() - start) / 1000)), 250)
    return () => clearInterval(id)
  }, [phase])

  const loading = phase === 'loading'

  // When results land, bring the board (with its pins) into view
  useEffect(() => {
    if (phase !== 'done' || !results.length) return
    const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    // wait a frame for the detail card to mount, then show the board and its card together
    const id = setTimeout(() => {
      mapRef.current?.querySelector('.map-wrap')?.scrollIntoView({ behavior: reduce ? 'auto' : 'smooth', block: 'end' })
    }, 120)
    return () => clearTimeout(id)
  }, [phase, results.length])

  async function scout(name) {
    const repoUrl = toRepoUrl(name)
    if (!repoUrl) {
      setError('Enter a repository as owner/repo, for example psf/requests.')
      setPhase('error')
      return
    }
    setRepo(cleanName(name))
    setResults([])
    setHoverRank(null)
    setSelectedRank(0)
    setError('')
    setPhase('loading')

    try {
      const res = await fetch('/api/analyze', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ repoUrl }),
      })
      if (!res.ok) throw new Error(messageFor(res.status))
      const data = await res.json()
      setResults(data.opportunities ?? [])
      setPhase('done')
    } catch (err) {
      // fetch throws TypeError when the server can't be reached at all
      setError(err instanceof TypeError ? 'Couldn’t reach the RepoScout server. Is it running?' : err.message)
      setPhase('error')
    }
  }

  function onSubmit(e) {
    e.preventDefault()
    scout(input)
  }

  function tryExample(name) {
    setInput(name)
    scout(name)
  }

  // Pasting a full GitHub URL works too: keep only the owner/repo part.
  function onChange(e) {
    setInput(e.target.value.replace(/^\s*(https?:\/\/)?(www\.)?github\.com\//i, ''))
  }

  // What the map shows
  let mapMode = 'idle'
  if (loading) mapMode = 'scanning'
  else if (phase === 'done') mapMode = results.length ? 'done' : 'empty'
  else if (phase === 'error') mapMode = 'error'

  const active = phase === 'done' && results.length ? hoverRank ?? selectedRank : null
  const current = STAGES.reduce((acc, s, i) => (elapsed >= s.at ? i : acc), 0)

  return (
    <div className="app">
      <header className="hero">
        <div className="glow glow-violet" aria-hidden="true" />
        <div className="glow glow-blue" aria-hidden="true" />
        <ParticleWave />

        <nav className="topbar">
          <span className="wordmark">
            <ReticleIcon />
            RepoScout
          </span>
          <span className="chip"><span className="chip-dot" />Ranked by Gemini</span>
        </nav>

        <div className="hero-inner">
          <h1>Find the line worth fixing.</h1>

          <div className="intro-row">
            <p className="lede">
              Paste a public GitHub repo. RepoScout reads every file for TODOs and disabled
              tests, throws out the ones blocked on someone else’s decision, and hands you the
              few a newcomer could actually finish, down to the line.
            </p>

            <div className="search-col">
              <form className="search" onSubmit={onSubmit}>
                <label htmlFor="repo" className="visually-hidden">GitHub repository</label>
                <div className="field">
                  <span className="prefix" aria-hidden="true">github.com/</span>
                  <input
                    id="repo"
                    value={input}
                    onChange={onChange}
                    placeholder="owner/repo"
                    autoComplete="off"
                    autoCapitalize="off"
                    spellCheck="false"
                    disabled={loading}
                  />
                </div>
                <button type="submit" className="primary" disabled={loading || !input.trim()}>
                  {loading ? 'Scouting…' : 'Scout it'}
                </button>
              </form>

              <p className="examples">
                <span>Try</span>
                {EXAMPLES.map((name) => (
                  <button key={name} type="button" className="example" onClick={() => tryExample(name)} disabled={loading}>
                    {name}
                  </button>
                ))}
              </p>

              {phase === 'error' && (
                <div className="hero-error" role="alert">
                  <AlertIcon />
                  <p>{error}</p>
                </div>
              )}
            </div>
          </div>
        </div>

        <section className="map-section" ref={mapRef} aria-label="Repository map">
          <RepoMap
            seed={repo || 'reposcout'}
            mode={mapMode}
            results={results}
            active={active}
            onHover={setHoverRank}
            onSelect={setSelectedRank}
          />

          {!(phase === 'done' && results.length > 0) && (
          <div className="map-caption glass" aria-live="polite">
            {loading ? (
              <>
              <span className="timer stages-timer">{elapsed}s</span>
              <ol className="stages">
                {STAGES.map((s, i) => {
                  const state = i < current ? 'done' : i === current ? 'active' : 'pending'
                  return (
                    <li key={s.label} className={`stage stage-${state}`}>
                      <span className="stage-mark" aria-hidden="true">{state === 'done' && <CheckIcon />}</span>
                      <span>{s.label}</span>
                      {state === 'active' && <span className="visually-hidden"> (in progress)</span>}
                    </li>
                  )
                })}
              </ol>
              </>
            ) : phase === 'done' ? (
              <p>{results.length ? `Scan complete. ${results.length} worth starting, pinned above and listed below.` : 'Scan complete. Nothing ready to pick up.'}</p>
            ) : phase === 'error' ? (
              <p>No scan this time. Fix the repo name and try again.</p>
            ) : (
              <p>Every repo gets its own map. The glowing marks are what the scan picks up.</p>
            )}
          </div>
          )}
        </section>
      </header>

      <main className="below">
        {phase === 'done' && results.length > 0 && (
          <Results results={results} repo={repo} active={active} onHover={setHoverRank} />
        )}
        {phase === 'done' && results.length === 0 && <Empty repo={repo} onTry={tryExample} />}
      </main>
    </div>
  )
}

function Results({ results, repo, active, onHover }) {
  return (
    <section className="results-section glass" aria-label="Results">
      <p className="summary">
        {results.length === 1 ? '1 opportunity' : `${results.length} opportunities`} in{' '}
        <code>{repo}</code>, best first
      </p>
      <ol className="results">
        {results.map((r, i) => (
          <li
            key={`${r.file}:${r.line}`}
            className={`result${i === active ? ' result-active' : ''}`}
            style={{ '--i': i }}
            onMouseEnter={() => onHover(i)}
            onMouseLeave={() => onHover(null)}
          >
            <div className="marker">
              <span className="rank">#{i + 1}</span>
              <span className="line-no">
                <span className="line-l">L</span>
                {r.line}
              </span>
            </div>
            <div className="body">
              <code className="path">{r.file}</code>
              <h2>{r.title}</h2>
              <p>{r.whatToDo}</p>
              <a className="open" href={r.url} target="_blank" rel="noreferrer">
                Open line {r.line} on GitHub
                <ArrowIcon />
              </a>
            </div>
          </li>
        ))}
      </ol>
    </section>
  )
}

function Empty({ repo, onTry }) {
  const others = EXAMPLES.filter((e) => e !== repo).slice(0, 2)
  return (
    <section className="empty">
      <h2>Nothing ready to pick up in <code>{repo}</code>.</h2>
      <p>
        RepoScout found no TODOs or disabled tests a newcomer could finish today. Either the
        code has none, or the ones it has are waiting on a future release or a design decision.
        Repos with active test suites tend to have more.
      </p>
      <p className="examples examples-light">
        <span>Try</span>
        {others.map((name) => (
          <button key={name} type="button" className="example" onClick={() => onTry(name)}>
            {name}
          </button>
        ))}
      </p>
    </section>
  )
}

/* Icons: one 1.75px stroke, 20px grid */
function ReticleIcon() {
  return (
    <svg viewBox="0 0 20 20" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" aria-hidden="true">
      <circle cx="10" cy="10" r="6.5" />
      <path d="M10 1.5v4M10 14.5v4M1.5 10h4M14.5 10h4" />
      <circle cx="10" cy="10" r="1.5" fill="currentColor" stroke="none" />
    </svg>
  )
}
function CheckIcon() {
  return (
    <svg viewBox="0 0 20 20" width="13" height="13" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M5 10.5l3.2 3.2L15 6.5" />
    </svg>
  )
}
function ArrowIcon() {
  return (
    <svg className="arrow" viewBox="0 0 20 20" width="15" height="15" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M7 13L13 7M8 7h5v5" />
    </svg>
  )
}
function AlertIcon() {
  return (
    <svg viewBox="0 0 20 20" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" aria-hidden="true">
      <circle cx="10" cy="10" r="7.5" />
      <path d="M10 6v4.5" />
      <circle cx="10" cy="13.75" r="0.6" fill="currentColor" />
    </svg>
  )
}