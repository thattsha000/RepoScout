import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'

// A 3D "terrain" of a repository, drawn from its name: the code lies flat on a glass board
// that turns slowly, and matches rise out of it as towers when the scan beam passes.
// After a scan, the three result towers carry pins you can hover or click to read
// that contribution. The terrain itself is decorative; only the cards hold real data.

const W = 640
const H = 340
const COLS = 16
const COL_W = 32
const GAP = 8
const LINE_H = 8
const SWEEP_S = 3 // one beam pass, in seconds
const STAGE_W = 900 // the scene's design size, scaled to fit its container
const STAGE_H = 460
const PIN_HEIGHTS = [112, 88, 70] // rank 1, 2, 3

// Small seeded random number generator (mulberry32) so the map is stable per repo
function seededRandom(text) {
  let h = 1779033703
  for (let i = 0; i < text.length; i++) {
    h = Math.imul(h ^ text.charCodeAt(i), 3432918353)
    h = (h << 13) | (h >>> 19)
  }
  return function next() {
    h = (h + 0x6d2b79f5) | 0
    let t = Math.imul(h ^ (h >>> 15), 1 | h)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

function buildMap(seed) {
  const rand = seededRandom(seed || 'reposcout')
  const lines = []
  for (let c = 0; c < COLS; c++) {
    const x0 = c * (COL_W + GAP)
    const count = 32 + Math.floor(rand() * 9)
    let level = 0
    for (let l = 0; l < count; l++) {
      if (rand() < 0.1) continue // blank line
      const step = rand()
      if (step < 0.25 && level < 3) level++
      else if (step > 0.75 && level > 0) level--
      const indent = level * 4
      const w = Math.max(4, Math.floor(rand() * (COL_W - indent)))
      lines.push({ x: x0 + indent, y: 8 + l * LINE_H, w })
    }
  }
  // Pick a scattering of "matches", each with its own resting height
  const pool = lines.filter((l) => l.w >= 8)
  const matches = []
  for (let i = 0; i < 13 && pool.length; i++) {
    const m = pool.splice(Math.floor(rand() * pool.length), 1)[0]
    matches.push({ ...m, rest: 22 + Math.floor(rand() * 30) })
  }
  matches.sort((a, b) => a.x - b.x)
  // Three of them become the pinned results: one from each third of the board, so pins never crowd
  const third = (lo, hi) => {
    const ids = matches.map((m, i) => i).filter((i) => matches[i].x >= lo && matches[i].x < hi)
    return ids[Math.floor(rand() * ids.length)] ?? 0
  }
  const pinIndexes = [third(W * 0.34, W * 0.67), third(0, W * 0.34), third(W * 0.67, W + 1)]
  // Specks of light drifting at different heights above the board
  const dust = Array.from({ length: 26 }, () => ({
    x: rand() * W,
    y: rand() * H,
    z: 20 + rand() * 150,
    size: 1.5 + rand() * 2.5,
    delay: rand() * 6,
  }))
  return { lines, matches, pinIndexes, dust }
}

export default function RepoMap({ seed, mode, results = [], active = null, onHover, onSelect }) {
  const { lines, matches, pinIndexes, dust } = useMemo(() => buildMap(seed), [seed])
  const outer = useRef(null)
  const wrap = useRef(null)
  const card = useRef(null)
  const tagRefs = useRef({})
  const [scale, setScale] = useState(1)
  const [cardPlace, setCardPlace] = useState(null) // where the card sits; set when a pin opens
  const [pinPoint, setPinPoint] = useState(null) // where the open pin is right now (it moves as the board turns)

  // Scale the fixed-size 3D stage to whatever width the layout gives it
  useEffect(() => {
    const ro = new ResizeObserver(([entry]) => setScale(Math.min(entry.contentRect.width / STAGE_W, 1.3)))
    ro.observe(wrap.current)
    return () => ro.disconnect()
  }, [])

  const scanning = mode === 'scanning'
  const pins = mode === 'done' ? Math.min(results.length, 3) : 0
  const rankOf = new Map(pinIndexes.slice(0, pins).map((mi, rank) => [mi, rank]))

  function measurePin(rank) {
    const tag = tagRefs.current[rank]
    if (!tag || !outer.current) return null
    const box = outer.current.getBoundingClientRect()
    const t = tag.getBoundingClientRect()
    return { x: t.left + t.width / 2 - box.left, y: t.bottom - box.top, width: box.width }
  }

  // When a pin opens, park the card under it (it stays put while the board turns)
  useLayoutEffect(() => {
    if (active === null || pins === 0) {
      setCardPlace(null)
      return
    }
    function place() {
      const p = measurePin(active)
      if (!p) return
      const cardW = card.current ? card.current.offsetWidth : 640
      const left = Math.max(0, Math.min(p.width - cardW, p.x - cardW / 2))
      // the beam lands on the card's top edge, as close under the pin as the card allows
      const anchorX = Math.max(left + 24, Math.min(left + cardW - 24, p.x))
      setCardPlace({ left, anchorX })
    }
    place()
    const id = setTimeout(place, 700) // again once the towers have finished rising
    window.addEventListener('resize', place)
    return () => {
      clearTimeout(id)
      window.removeEventListener('resize', place)
    }
  }, [active, pins, scale])

  // Follow the open pin every frame so the beam stays attached while the board spins
  useEffect(() => {
    if (active === null || pins === 0) {
      setPinPoint(null)
      return
    }
    let raf
    let last = null
    function tick() {
      const p = measurePin(active)
      if (p && (!last || Math.abs(p.x - last.x) > 0.5 || Math.abs(p.y - last.y) > 0.5)) {
        last = p
        setPinPoint({ x: p.x, y: p.y })
      }
      raf = requestAnimationFrame(tick)
    }
    tick()
    return () => cancelAnimationFrame(raf)
  }, [active, pins])

  const item = active !== null ? results[active] : null
  const stageLeft = `calc(50% - ${(STAGE_W * scale) / 2}px)`
  const beamBottom = STAGE_H * scale + 18

  return (
    <div className="map-wrap" ref={outer}>
      <div className="scene" ref={wrap} style={{ height: STAGE_H * scale }}>
        <div className="stage3d" style={{ width: STAGE_W, height: STAGE_H, left: stageLeft, transform: `scale(${scale})` }}>
          <div
            className={`board board-${mode}${active !== null ? ' has-active' : ''}`}
            style={{ width: W, height: H, '--sweep-to': `${W + 24}px` }}
          >
            <div className="board-glow" aria-hidden="true" />

            <svg className="floor" viewBox={`-8 -8 ${W + 16} ${H + 16}`} width={W + 16} height={H + 16} aria-hidden="true">
              <defs>
                <linearGradient id="sheen" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0" stopColor="#ffffff" stopOpacity="0.1" />
                  <stop offset="0.45" stopColor="#ffffff" stopOpacity="0" />
                  <stop offset="1" stopColor="#8b5cf6" stopOpacity="0.08" />
                </linearGradient>
              </defs>
              <rect x="-8" y="-8" width={W + 16} height={H + 16} rx="10" className="floor-plate" />
              {lines.map((l, i) => (
                <rect key={i} x={l.x} y={l.y} width={l.w} height="3" rx="1.5" className="floor-line" />
              ))}
              <rect x="-8" y="-8" width={W + 16} height={H + 16} rx="10" fill="url(#sheen)" />
            </svg>
            <i className="slab slab-front" />
            <i className="slab slab-right" />

            {/* a new key restarts the rise and the beam each time a scan begins; the board itself keeps spinning */}
            <div key={scanning ? `${seed}-scan` : `${seed}-${mode}`} className="fx">
              {scanning && <div className="wall" style={{ animationDuration: `${SWEEP_S}s` }} />}

              {matches.map((m, i) => {
                const rank = rankOf.get(i)
                const pinned = rank !== undefined
                const height = pinned ? PIN_HEIGHTS[rank] : m.rest
                const delay = scanning ? ((m.x + m.w / 2) / W) * SWEEP_S : pinned ? rank * 0.09 : 0
                return (
                  <div
                    key={i}
                    className={`tower${pinned ? ' tower-pinned' : ''}${pinned && rank === active ? ' tower-active' : ''}`}
                    style={{ left: m.x - 1, top: m.y - 3, width: m.w + 2, '--h': `${height}px`, animationDelay: `${delay}s` }}
                  >
                    <i className="face top" />
                    <i className="face front" />
                    <i className="face back" />
                    <i className="face left" />
                    <i className="face right" />
                    {pinned && (
                      <button
                        type="button"
                        ref={(el) => (tagRefs.current[rank] = el)}
                        className={`tag${rank === active ? ' tag-active' : ''}`}
                        aria-label={`Opportunity ${rank + 1}: ${results[rank]?.title ?? ''}`}
                        aria-expanded={rank === active}
                        onMouseEnter={() => onHover?.(rank)}
                        onMouseLeave={() => onHover?.(null)}
                        onFocus={() => onHover?.(rank)}
                        onBlur={() => onHover?.(null)}
                        onClick={() => onSelect?.(rank)}
                      >
                        {rank + 1}
                      </button>
                    )}
                  </div>
                )
              })}
            </div>

            {dust.map((d, i) => (
              <i
                key={i}
                className="dust"
                aria-hidden="true"
                style={{ left: d.x, top: d.y, '--z': `${d.z}px`, '--s': `${d.size}px`, animationDelay: `${d.delay}s` }}
              />
            ))}
          </div>
        </div>
      </div>

      {pins > 0 && pinPoint && cardPlace && (
        <svg className="pin-beam" aria-hidden="true">
          <line x1={pinPoint.x} y1={pinPoint.y + 4} x2={cardPlace.anchorX} y2={beamBottom} />
          <circle cx={cardPlace.anchorX} cy={beamBottom} r="3.5" />
        </svg>
      )}

      {item && (
        <aside
          key={active}
          ref={card}
          className="pin-card"
          style={{ marginLeft: cardPlace ? cardPlace.left : 0 }}
          aria-live="polite"
        >
          <div className="pin-card-marker">
            <span className="pin-card-rank">#{active + 1}</span>
            <span className="pin-card-line">L{item.line}</span>
          </div>
          <div className="pin-card-body">
            <code className="pin-card-path">{item.file}</code>
            <h3>{item.title}</h3>
            <p>{item.whatToDo}</p>
            <a className="open" href={item.url} target="_blank" rel="noreferrer">
              Open line {item.line} on GitHub
              <svg className="arrow" viewBox="0 0 20 20" width="15" height="15" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d="M7 13L13 7M8 7h5v5" />
              </svg>
            </a>
          </div>
        </aside>
      )}
    </div>
  )
}