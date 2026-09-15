import { formatPrice } from '../lib/formatPrice'

interface TaxIncludedNoteProps {
  cgstPercent: number
  cgstAmount: number
  sgstPercent: number
  sgstAmount: number
}

/**
 * Small clarifying note for wherever a subtotal/total breakdown is shown (Cart, Checkout, Order
 * detail): prices are tax-inclusive, so CGST/SGST are already inside the Total above, not added
 * to it - shown as a subordinate note rather than its own line item so it doesn't read as
 * something still to be added on top. Renders nothing when tax isn't configured (0%).
 */
export default function TaxIncludedNote({ cgstPercent, cgstAmount, sgstPercent, sgstAmount }: TaxIncludedNoteProps) {
  if (cgstAmount <= 0 && sgstAmount <= 0) return null
  return (
    <p className="text-xs text-zinc-400">
      Includes CGST {formatPrice(cgstAmount)} ({cgstPercent}%) + SGST {formatPrice(sgstAmount)} ({sgstPercent}%)
    </p>
  )
}
