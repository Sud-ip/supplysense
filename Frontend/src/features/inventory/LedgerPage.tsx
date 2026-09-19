import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { inventoryApi } from './inventoryApi'
import type { StockLedgerEntry } from '../../api/types'

const reasonColors: Record<StockLedgerEntry['reason'], string> = {
  PURCHASE: 'text-success',
  RETURN: 'text-success',
  SALE: 'text-danger',
  ADJUSTMENT: 'text-warning',
  TRANSFER: 'text-ink/60'
}

export default function LedgerPage() {
  const [searchParams] = useSearchParams()
  const productId = searchParams.get('productId') ?? undefined
  const locationId = searchParams.get('locationId') ?? undefined
  const [entries, setEntries] = useState<StockLedgerEntry[]>([])

  useEffect(() => {
    inventoryApi.getLedger(productId, locationId).then((page) => setEntries(page.content))
  }, [productId, locationId])

  return (
    <div>
      <h1 className="font-display text-2xl font-semibold text-ink mb-1">Stock ledger</h1>
      <p className="text-ink/50 text-sm mb-6">
        Every stock change ever recorded, in order. This is the immutable source of truth the current balance is
        calculated from - it can be filtered but never edited.
      </p>

      <div className="bg-white border border-line rounded overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-paper text-ink/50 text-left">
            <tr>
              <th className="px-4 py-2.5 font-medium">When</th>
              <th className="px-4 py-2.5 font-medium">Product</th>
              <th className="px-4 py-2.5 font-medium">Location</th>
              <th className="px-4 py-2.5 font-medium">Change</th>
              <th className="px-4 py-2.5 font-medium">Reason</th>
              <th className="px-4 py-2.5 font-medium">By</th>
            </tr>
          </thead>
          <tbody>
            {entries.map((e) => (
              <tr key={e.id} className="border-t border-line">
                <td className="px-4 py-2.5 text-xs text-ink/40 data">{new Date(e.createdAt).toLocaleString()}</td>
                <td className="px-4 py-2.5 data text-xs">{e.productSku}</td>
                <td className="px-4 py-2.5 text-ink/50">{e.locationName}</td>
                <td className={`px-4 py-2.5 font-medium data ${e.quantityDelta > 0 ? 'text-success' : 'text-danger'}`}>
                  {e.quantityDelta > 0 ? '+' : ''}{e.quantityDelta}
                </td>
                <td className={`px-4 py-2.5 ${reasonColors[e.reason]}`}>{e.reason}</td>
                <td className="px-4 py-2.5 text-ink/40 text-xs">{e.createdByEmail}</td>
              </tr>
            ))}
            {entries.length === 0 && (
              <tr><td colSpan={6} className="px-4 py-10 text-center text-ink/40">No ledger entries yet</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
