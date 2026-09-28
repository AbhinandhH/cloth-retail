/**
 * Small set of loading primitives so every page's skeleton state is built
 * from the same shimmer/radius language instead of one-off `animate-pulse`
 * divs with inconsistent rounding.
 */
const BASE = 'animate-pulse rounded-md bg-[var(--surface-elevated)]'

export function SkeletonBlock({ className = '' }: { className?: string }) {
  return <div className={`${BASE} ${className}`} />
}

export function SkeletonText({ className = '', width = 'w-full' }: { className?: string; width?: string }) {
  return <div className={`${BASE} h-3.5 ${width} ${className}`} />
}

export function SkeletonImage({ className = '' }: { className?: string }) {
  return <div className={`${BASE} aspect-[3/4] w-full rounded-lg ${className}`} />
}

export function SkeletonCircle({ className = 'h-10 w-10' }: { className?: string }) {
  return <div className={`${BASE} rounded-full ${className}`} />
}
