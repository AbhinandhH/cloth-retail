interface ErrorStateProps {
  title?: string
  message: string
  onRetry?: () => void
}

/**
 * Shared load-failure/network-error treatment — replaces bare red text boxes
 * with a consistent, calmer presentation plus an optional retry action.
 */
export default function ErrorState({ title = 'Something went wrong', message, onRetry }: ErrorStateProps) {
  return (
    <div className="flex flex-col items-center px-4 py-16 text-center">
      <div className="flex h-14 w-14 items-center justify-center rounded-full bg-rose-50 text-rose-500">
        <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={1.75}
            d="M12 9v3.75m0 3.75h.008v.008H12v-.008ZM21 12a9 9 0 11-18 0 9 9 0 0118 0z"
          />
        </svg>
      </div>
      <p className="mt-5 text-base font-medium text-zinc-900">{title}</p>
      <p className="mt-1.5 max-w-xs text-sm text-zinc-500">{message}</p>
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="mt-6 rounded-full border border-zinc-300 px-6 py-2.5 text-sm font-medium text-zinc-700 transition-colors hover:bg-zinc-50"
        >
          Try again
        </button>
      )}
    </div>
  )
}
