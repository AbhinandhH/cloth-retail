import { useState } from 'react'
import type { MouseEvent } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { getErrorMessage } from '../api/client'
import { useAuth } from '../context/AuthContext'
import { useWishlist } from '../context/WishlistContext'

export function HeartIcon({ filled, className = 'h-4 w-4' }: { filled: boolean; className?: string }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      className={className}
      fill={filled ? 'currentColor' : 'none'}
      stroke="currentColor"
      strokeWidth={filled ? 0 : 1.75}
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M12 20.5c-.3 0-.6-.1-.8-.3C7.4 17 3.5 13.4 3.5 9.4 3.5 6.6 5.7 4.5 8.4 4.5c1.5 0 2.9.7 3.6 1.9.7-1.2 2.1-1.9 3.6-1.9 2.7 0 4.9 2.1 4.9 4.9 0 4-3.9 7.6-7.7 10.8-.2.2-.5.3-.8.3z"
      />
    </svg>
  )
}

/**
 * The toggle itself, with no positioning opinion — both ProductCard and
 * ProductDetail overlay it on the bottom-right corner of the product image via
 * `className="absolute bottom-... right-..."`. Both need the exact same
 * guest-redirect/toggle/error behavior, so that logic lives here once rather
 * than being copied.
 */
export default function WishlistButton({
  productId,
  size = 'md',
  className = '',
}: {
  productId: number | string
  size?: 'sm' | 'md'
  className?: string
}) {
  const { isAuthenticated } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const { isWishlisted, toggle } = useWishlist()
  const [isToggling, setIsToggling] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const wishlisted = isWishlisted(productId)
  const dimensions = size === 'sm' ? 'h-9 w-9' : 'h-11 w-11'
  const iconClasses = size === 'sm' ? 'h-4 w-4' : 'h-5 w-5'
  // Tailwind's `.relative`/`.absolute` rules share specificity, so whichever is
  // later in the generated stylesheet wins regardless of class-string order —
  // a hardcoded `relative` here would silently defeat a caller's `absolute`
  // override. Only default to `relative` when the caller isn't already
  // specifying a position.
  const hasPositionOverride = /\b(absolute|fixed|sticky|static)\b/.test(className)

  const handleClick = async (e: MouseEvent) => {
    e.preventDefault()
    e.stopPropagation()

    // Guests get sent straight to login (with a return path), matching the
    // existing "Add to cart" pattern.
    if (!isAuthenticated) {
      navigate('/login', { state: { from: location } })
      return
    }
    if (isToggling) return

    setIsToggling(true)
    setError(null)
    try {
      await toggle(productId)
    } catch (err) {
      setError(getErrorMessage(err))
      window.setTimeout(() => setError(null), 2500)
    } finally {
      setIsToggling(false)
    }
  }

  return (
    <div className={`${hasPositionOverride ? '' : 'relative'} ${className}`}>
      <button
        type="button"
        onClick={handleClick}
        disabled={isToggling}
        aria-label={wishlisted ? 'Remove from wishlist' : 'Add to wishlist'}
        aria-pressed={wishlisted}
        // The icon itself is small but the button's own box is a full touch
        // target, per "proper mobile touch target even though the icon
        // itself is small".
        className={`flex ${dimensions} items-center justify-center rounded-full bg-white/85 shadow-soft backdrop-blur-sm transition-transform duration-150 hover:scale-110 active:scale-95 disabled:cursor-wait disabled:opacity-70 ${
          wishlisted ? 'text-rose-600' : 'text-zinc-600'
        }`}
      >
        <HeartIcon filled={wishlisted} className={iconClasses} />
      </button>
      {error && (
        <p className="absolute right-0 top-full mt-1 whitespace-nowrap rounded-md bg-zinc-900/90 px-2 py-1 text-[10px] font-medium text-white shadow-soft">
          {error}
        </p>
      )}
    </div>
  )
}
