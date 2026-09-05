import { useNavigate } from 'react-router-dom'

/**
 * Bare chevron-left back affordance — no "Back"/"Go back" text per the design
 * spec. Uses browser history (navigate(-1)) so it composes correctly with
 * however the user actually arrived at the page. Not used on the homepage.
 */
export default function BackButton({ className = '' }: { className?: string }) {
  const navigate = useNavigate()

  return (
    <button
      type="button"
      onClick={() => navigate(-1)}
      aria-label="Go to previous page"
      className={`inline-flex h-10 w-10 items-center justify-center rounded-full text-zinc-700 transition-colors hover:bg-zinc-100 active:bg-zinc-200 ${className}`}
    >
      <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
      </svg>
    </button>
  )
}
