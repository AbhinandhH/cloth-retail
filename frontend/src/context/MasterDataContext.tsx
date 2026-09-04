import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { fetchCategories, fetchColors, fetchSizes } from '../api/products'
import * as mastersApi from '../api/adminMasters'
import { getErrorMessage } from '../api/client'
import type { AdminBrand, AdminMaterial, AdminVendor, Category, Color, DamageReasonOption, Size } from '../types'

type MasterKey = 'categories' | 'colors' | 'sizes' | 'brands' | 'materials' | 'vendors' | 'damageReasons'

interface MasterState<T> {
  data: T[]
  loading: boolean
  error: string | null
}

interface MasterMap {
  categories: Category
  colors: Color
  sizes: Size
  brands: AdminBrand
  materials: AdminMaterial
  vendors: AdminVendor
  damageReasons: DamageReasonOption
}

function emptyState<T>(): MasterState<T> {
  return { data: [], loading: false, error: null }
}

// Categories/colors/sizes/damage-reasons hit PUBLIC endpoints (no auth) since
// the storefront — Home.tsx in particular — needs them while logged out.
// Brands/materials/vendors have no public equivalent, so they go through the
// admin endpoints (active-only) — fine since only admin-authenticated screens
// consume them today.
const FETCHERS: { [K in MasterKey]: () => Promise<MasterMap[K][]> } = {
  categories: fetchCategories,
  colors: fetchColors,
  sizes: fetchSizes,
  damageReasons: mastersApi.fetchPublicDamageReasons,
  brands: () => mastersApi.fetchAdminBrands({ active: true }),
  materials: () => mastersApi.fetchAdminMaterials({ active: true }),
  vendors: () => mastersApi.fetchAdminVendors({ active: true }),
}

interface MasterDataContextValue {
  state: { [K in MasterKey]: MasterState<MasterMap[K]> }
  ensure: (key: MasterKey) => void
  /** Clears a master's cached entry so the next read re-fetches — call after admin create/update/deactivate/delete. */
  refreshMaster: (key: MasterKey) => void
}

const MasterDataContext = createContext<MasterDataContextValue | undefined>(undefined)

/**
 * Cached, lazy-loaded master data (categories/colors/sizes/brands/materials/
 * vendors/damageReasons) shared across the app. Nothing fetches until a
 * consumer calls one of the useX() hooks below — this is deliberately a
 * no-op provider on mount, unlike SiteConfigProvider's eager load.
 *
 * Concurrent first-mount calls to the same master (e.g. several components
 * mounting together) are deduped via an in-flight-promise ref, not React
 * state, since state updates are async and would otherwise race.
 */
export function MasterDataProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<{ [K in MasterKey]: MasterState<MasterMap[K]> }>(() => ({
    categories: emptyState(),
    colors: emptyState(),
    sizes: emptyState(),
    brands: emptyState(),
    materials: emptyState(),
    vendors: emptyState(),
    damageReasons: emptyState(),
  }))

  // Synchronous bookkeeping refs — source of truth for "should ensure() start
  // a new fetch", independent of React's async state batching.
  const loadedRef = useRef<Partial<Record<MasterKey, boolean>>>({})
  const inFlightRef = useRef<Partial<Record<MasterKey, Promise<unknown>>>>({})

  const ensure = useCallback((key: MasterKey) => {
    if (loadedRef.current[key] || inFlightRef.current[key]) return

    setState((prev) => ({ ...prev, [key]: { ...prev[key], loading: true, error: null } }))

    const promise = FETCHERS[key]()
      .then((data) => {
        loadedRef.current[key] = true
        setState((prev) => ({ ...prev, [key]: { data, loading: false, error: null } }))
      })
      .catch((err) => {
        loadedRef.current[key] = false
        setState((prev) => ({ ...prev, [key]: { data: [], loading: false, error: getErrorMessage(err) } }))
      })
      .finally(() => {
        delete inFlightRef.current[key]
      })

    inFlightRef.current[key] = promise
  }, [])

  const refreshMaster = useCallback((key: MasterKey) => {
    loadedRef.current[key] = false
    delete inFlightRef.current[key]
    setState((prev) => ({ ...prev, [key]: emptyState() }))
  }, [])

  const value = useMemo<MasterDataContextValue>(() => ({ state, ensure, refreshMaster }), [state, ensure, refreshMaster])

  return <MasterDataContext.Provider value={value}>{children}</MasterDataContext.Provider>
}

function useMasterContext() {
  const ctx = useContext(MasterDataContext)
  if (!ctx) throw new Error('useMasterData hooks must be used within a MasterDataProvider')
  return ctx
}

function useMasterList<K extends MasterKey>(key: K): MasterState<MasterMap[K]> {
  const ctx = useMasterContext()
  // Effect, not a direct call during render — ensure() calls setState, which
  // must not happen synchronously while a different component is rendering.
  useEffect(() => {
    ctx.ensure(key)
  }, [ctx, key])
  return ctx.state[key]
}

export function useCategories() {
  return useMasterList('categories')
}
export function useColors() {
  return useMasterList('colors')
}
export function useSizes() {
  return useMasterList('sizes')
}
export function useBrands() {
  return useMasterList('brands')
}
export function useMaterials() {
  return useMasterList('materials')
}
export function useVendors() {
  return useMasterList('vendors')
}
export function useDamageReasons() {
  return useMasterList('damageReasons')
}

/** Call after admin create/update/deactivate/delete so the relevant cache re-fetches on next read. */
export function useRefreshMaster() {
  return useMasterContext().refreshMaster
}

export type { MasterKey }
