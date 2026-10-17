import { useEffect, useRef } from 'react'

// A field of glowing points rolling like a wave, seen in perspective.
// Canvas 2D, no library. Pauses with the tab (requestAnimationFrame) and
// draws a single still frame when the viewer prefers reduced motion.

const COLS = 140
const ROWS = 30

export default function ParticleWave() {
  const canvasRef = useRef(null)

  useEffect(() => {
    const canvas = canvasRef.current
    const ctx = canvas.getContext('2d')
    const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    let w = 0
    let h = 0
    let raf = 0

    function resize() {
      const dpr = Math.min(window.devicePixelRatio || 1, 2)
      w = canvas.clientWidth
      h = canvas.clientHeight
      canvas.width = w * dpr
      canvas.height = h * dpr
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
    }

    function draw(t) {
      ctx.clearRect(0, 0, w, h)
      const focal = h * 0.95
      const horizon = h * 0.02
      const camH = 0.55
      for (let r = 0; r < ROWS; r++) {
        const z = 0.8 + (r / (ROWS - 1)) * 3.6 // near → far
        const depth = 1 / z
        const alpha = Math.min(1, 0.95 * depth)
        for (let c = 0; c < COLS; c++) {
          const x = -1.9 + (c / (COLS - 1)) * 3.8
          const y =
            Math.sin(x * 2.1 + t * 0.55 + z * 1.25) * 0.16 +
            Math.sin(x * 0.8 - t * 0.35 + z * 0.4) * 0.11
          const sx = w / 2 + (x / z) * focal * 1.25
          if (sx < -4 || sx > w + 4) continue
          const sy = horizon + ((camH - y) / z) * focal
          // crests glow brighter and lean toward cyan
          const crest = (y + 0.27) / 0.54
          const size = 2.1 * depth + crest * 0.6
          const g = Math.round(140 + crest * 80)
          ctx.fillStyle = `rgba(${Math.round(90 - crest * 30)}, ${g}, 255, ${alpha * (0.35 + crest * 0.65)})`
          ctx.fillRect(sx - size / 2, sy - size / 2, size, size)
        }
      }
    }

    function frame(ms) {
      draw(ms / 1000)
      raf = requestAnimationFrame(frame)
    }

    resize()
    window.addEventListener('resize', resize)
    if (reduce) draw(0)
    else raf = requestAnimationFrame(frame)

    return () => {
      cancelAnimationFrame(raf)
      window.removeEventListener('resize', resize)
    }
  }, [])

  return <canvas ref={canvasRef} className="wave" aria-hidden="true" />
}