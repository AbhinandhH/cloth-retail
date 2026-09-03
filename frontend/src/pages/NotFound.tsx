import { Link } from 'react-router-dom'

export default function NotFound() {
  return (
    <div className="flex min-h-[70dvh] flex-col items-center justify-center px-4 text-center">
      <p className="text-sm font-semibold text-rose-600">404</p>
      <h1 className="mt-2 text-3xl font-bold text-zinc-900">Page not found</h1>
      <p className="mt-2 text-sm text-zinc-500">Sorry, we couldn&apos;t find the page you&apos;re looking for.</p>
      <Link
        to="/"
        className="mt-6 rounded-lg bg-zinc-900 px-5 py-2.5 text-sm font-semibold text-white hover:bg-zinc-700"
      >
        Back to shop
      </Link>
    </div>
  )
}
