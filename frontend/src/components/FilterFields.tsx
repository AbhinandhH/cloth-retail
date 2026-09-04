import FilterSelect from './FilterSelect'
import type { Category, Color, Size } from '../types'

export interface FilterValues {
  categoryId: string
  sizeId: string
  colorId: string
  minPrice: string
  maxPrice: string
}

interface FilterFieldsProps {
  categories: Category[]
  sizes: Size[]
  colors: Color[]
  values: FilterValues
  onChange: <K extends keyof FilterValues>(key: K, value: FilterValues[K]) => void
}

/**
 * The actual filter controls (category/size/color/price), with no opinion on
 * where they're rendered — the desktop sticky sidebar and the mobile
 * FilterSheet both wrap this in different chrome, so the fields themselves
 * exist in exactly one place.
 */
export default function FilterFields({ categories, sizes, colors, values, onChange }: FilterFieldsProps) {
  return (
    <div className="space-y-5">
      <FilterSelect
        label="Category"
        value={values.categoryId}
        onChange={(v) => onChange('categoryId', v)}
        options={categories.map((c) => ({ value: String(c.id), label: c.name }))}
      />
      <FilterSelect
        label="Size"
        value={values.sizeId}
        onChange={(v) => onChange('sizeId', v)}
        options={sizes.map((s) => ({ value: String(s.id), label: s.name }))}
      />
      <FilterSelect
        label="Color"
        value={values.colorId}
        onChange={(v) => onChange('colorId', v)}
        options={colors.map((c) => ({ value: String(c.id), label: c.name }))}
      />
      <div>
        <p className="mb-2 text-sm font-medium text-zinc-900">Price</p>
        <div className="flex items-center gap-2">
          <input
            type="number"
            min={0}
            inputMode="decimal"
            placeholder="Min"
            value={values.minPrice}
            onChange={(e) => onChange('minPrice', e.target.value)}
            className="w-full rounded-md border border-zinc-300 px-2 py-1.5 text-sm focus:border-zinc-500 focus:outline-none"
          />
          <span className="text-zinc-400">–</span>
          <input
            type="number"
            min={0}
            inputMode="decimal"
            placeholder="Max"
            value={values.maxPrice}
            onChange={(e) => onChange('maxPrice', e.target.value)}
            className="w-full rounded-md border border-zinc-300 px-2 py-1.5 text-sm focus:border-zinc-500 focus:outline-none"
          />
        </div>
      </div>
    </div>
  )
}
