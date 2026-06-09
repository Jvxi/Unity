import { animate, stagger } from 'animejs'

type MotionTarget = Element | Element[] | NodeListOf<Element> | null | undefined

interface StaggerInOptions {
  y?: number
  x?: number
  scale?: number
  duration?: number
  delay?: number
  staggerDelay?: number
  from?: 'first' | 'center' | 'last' | 'random' | number
  ease?: string
}

function motionDisabled() {
  return typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

function normalizeTargets(targets: MotionTarget): Element[] {
  if (!targets) return []
  if (targets instanceof Element) return [targets]
  return Array.from(targets)
}

export function staggerIn(targets: MotionTarget, options: StaggerInOptions = {}) {
  const elements = normalizeTargets(targets)
  if (!elements.length) return
  if (motionDisabled()) {
    elements.forEach((el) => {
      const htmlEl = el as HTMLElement
      htmlEl.style.opacity = '1'
      htmlEl.style.transform = ''
    })
    return
  }

  animate(elements, {
    opacity: [{ from: 0 }, { to: 1 }],
    translateX: [{ from: options.x ?? 0 }, { to: 0 }],
    translateY: [{ from: options.y ?? 12 }, { to: 0 }],
    scale: [{ from: options.scale ?? 0.985 }, { to: 1 }],
    duration: options.duration ?? 360,
    delay: elements.length > 1
      ? stagger(options.staggerDelay ?? 45, { start: options.delay ?? 0, from: options.from ?? 'first', ease: 'out(2)' })
      : options.delay ?? 0,
    ease: options.ease ?? 'out(3)',
  })
}

export function panelIn(target: Element | null | undefined, options: StaggerInOptions = {}) {
  staggerIn(target, {
    x: options.x ?? 0,
    y: options.y ?? 10,
    scale: options.scale ?? 0.992,
    duration: options.duration ?? 320,
    delay: options.delay ?? 0,
    ease: options.ease ?? 'out(3)',
  })
}

export function softPulse(target: MotionTarget, scale = 1.035) {
  const elements = normalizeTargets(target)
  if (!elements.length || motionDisabled()) return

  animate(elements, {
    scale: [{ from: 1 }, { to: scale }, { to: 1 }],
    duration: 420,
    ease: 'out(3)',
  })
}

export function slideFlash(target: MotionTarget, x = 8) {
  const elements = normalizeTargets(target)
  if (!elements.length || motionDisabled()) return

  animate(elements, {
    translateX: [{ from: x }, { to: 0 }],
    opacity: [{ from: 0.72 }, { to: 1 }],
    duration: 260,
    ease: 'out(3)',
  })
}
