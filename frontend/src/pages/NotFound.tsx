import { Link } from 'react-router-dom'

export default function NotFound() {
  return (
    <div className="flex min-h-[70dvh] flex-col items-center justify-center px-4 text-center">
      <div className="animate-fade-in-up">
        <p className="font-display text-8xl text-zinc-200 sm:text-9xl">404</p>
        <h1 className="mt-4 font-display text-3xl text-zinc-900 sm:text-4xl">Lost in the racks</h1>
        <p className="mx-auto mt-3 max-w-sm text-sm text-zinc-500">
          We couldn&apos;t find the page you&apos;re looking for. It may have been moved or no longer exists.
        </p>
        <Link
          to="/"
          className="mt-8 inline-flex items-center justify-center rounded-xl bg-zinc-900 px-6 py-3 text-sm font-semibold text-white shadow-soft transition-opacity hover:opacity-90"
        >
          Back to shop
        </Link>
      </div>
    </div>
  )
}
