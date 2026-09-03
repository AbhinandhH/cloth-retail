import { Link } from 'react-router-dom'

export default function Footer() {
  return (
    <footer className="border-t border-zinc-200 bg-zinc-50">
      <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
        <div className="grid grid-cols-2 gap-8 sm:grid-cols-4">
          <div className="col-span-2 sm:col-span-1">
            <span className="text-lg font-bold tracking-tight text-zinc-900">
              THREAD<span className="text-rose-600">CO</span>
            </span>
            <p className="mt-2 text-sm text-zinc-500">Clothing that fits your life.</p>
          </div>

          <div>
            <h3 className="text-sm font-semibold text-zinc-900">Shop</h3>
            <ul className="mt-3 space-y-2 text-sm text-zinc-500">
              <li><Link to="/" className="hover:text-zinc-900">All products</Link></li>
              <li><Link to="/?category=new" className="hover:text-zinc-900">New arrivals</Link></li>
            </ul>
          </div>

          <div>
            <h3 className="text-sm font-semibold text-zinc-900">Support</h3>
            <ul className="mt-3 space-y-2 text-sm text-zinc-500">
              <li><a href="mailto:hello@threadco.example" className="hover:text-zinc-900">hello@threadco.example</a></li>
              <li><a href="tel:+10000000000" className="hover:text-zinc-900">+1 (000) 000-0000</a></li>
            </ul>
          </div>

          <div>
            <h3 className="text-sm font-semibold text-zinc-900">Follow</h3>
            <ul className="mt-3 space-y-2 text-sm text-zinc-500">
              <li><a href="https://instagram.com" target="_blank" rel="noreferrer" className="hover:text-zinc-900">Instagram</a></li>
              <li><a href="https://tiktok.com" target="_blank" rel="noreferrer" className="hover:text-zinc-900">TikTok</a></li>
            </ul>
          </div>
        </div>

        <div className="mt-8 border-t border-zinc-200 pt-6 text-xs text-zinc-400">
          &copy; {new Date().getFullYear()} ThreadCo. All rights reserved.
        </div>
      </div>
    </footer>
  )
}
