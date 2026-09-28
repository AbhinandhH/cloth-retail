import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { registerLockoutHandler } from '../api/client'

interface LockoutContextValue {
  isLockedOut: boolean
}

const LockoutContext = createContext<LockoutContextValue | undefined>(undefined)

/**
 * Tracks whether the backend has told us the site is locked out (subscription unpaid past its
 * due date - see SubscriptionAccessFilter). A sibling to SiteConfigProvider, not nested inside
 * AuthProvider: the lockout can hit an anonymous storefront visitor just as much as a logged-in
 * account, so it must not depend on auth state.
 *
 * There's no automatic un-lock here - a 402 is only ever cleared by the visitor getting a
 * successful response again (a page reload after the software owner marks the bill paid), which
 * matches how a real "the store is closed" outage would resolve for a visitor anyway.
 */
export function LockoutProvider({ children }: { children: ReactNode }) {
  const [isLockedOut, setIsLockedOut] = useState(false)

  useEffect(() => {
    registerLockoutHandler(() => setIsLockedOut(true))
    return () => registerLockoutHandler(null)
  }, [])

  const value = useMemo<LockoutContextValue>(() => ({ isLockedOut }), [isLockedOut])

  return <LockoutContext.Provider value={value}>{children}</LockoutContext.Provider>
}

export function useLockout() {
  const ctx = useContext(LockoutContext)
  if (!ctx) throw new Error('useLockout must be used within a LockoutProvider')
  return ctx
}
