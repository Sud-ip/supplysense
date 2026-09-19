import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { inventoryApi } from './inventoryApi'
import { productsApi, locationsApi } from '../catalog/catalogApi'
import type { InventoryBalance, Product, Location, StockAdjustmentRequest } from '../../api/types'
import { useRole } from '../../app/useRole'

const emptyForm: StockAdjustmentRequest = { productId: '', locationId: '', quantityDelta: 0, reason: 'ADJUSTMENT', note: null }

export default function InventoryBalancesPage() {
  const { canManageCatalog } = useRole()
  const [balances, setBalances] = useState<InventoryBalance[]>([])
  const [products, setProducts] = useState<Product[]>([])
  const [locations, setLocations] = useState<Location[]>([])
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState<StockAdjustmentRequest>(emptyForm)
  const [error, setError] = useState<string | null>(null)

  function loadAll() {
    inventoryApi.getBalances().then(setBalances)
    productsApi.list().then(setProducts)
    locationsApi.list().then(setLocations)
  }
  useEffect(loadAll, [])

  async function handleSubmit() {
    setError(null)
    if (!form.productId || !form.locationId || form.quantityDelta === 0) {
      setError('Product, location, and a non-zero quantity are required'); return
    }
    try {
      await inventoryApi.recordAdjustment(form)
      setForm(emptyForm); setShowForm(false); loadAll()
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Failed to record adjustment')
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="font-display text-2xl font-semibold text-ink">Inventory balances</h1>
        {canManageCatalog && (
          <button onClick={() => setShowForm(!showForm)} className="bg-brand hover:bg-brand-dark transition-colors text-white text-sm rounded px-3 py-1.5">
            {showForm ? 'Cancel' : 'Adjust stock'}
          </button>
        )}
      </div>

      {showForm && (
        <div className="bg-white border border-line rounded p-4 mb-5">
          {error && <div className="text-sm text-danger bg-danger-soft rounded px-3 py-2 mb-3">{error}</div>}
          <div className="grid grid-cols-2 gap-3 mb-3">
            <select value={form.productId} onChange={(e) => setForm({ ...form, productId: e.target.value })}
              className="rounded border border-line px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand">
              <option value="">Select product</option>
              {products.map((p) => <option key={p.id} value={p.id}>{p.sku} - {p.name}</option>)}
            </select>
            <select value={form.locationId} onChange={(e) => setForm({ ...form, locationId: e.target.value })}
              className="rounded border border-line px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand">
              <option value="">Select location</option>
              {locations.map((l) => <option key={l.id} value={l.id}>{l.name}</option>)}
            </select>
            <input type="number" placeholder="Quantity change (+/-)" value={form.quantityDelta || ''}
              onChange={(e) => setForm({ ...form, quantityDelta: Number(e.target.value) })}
              className="rounded border border-line px-3 py-2 text-sm data focus:outline-none focus:ring-2 focus:ring-brand" />
            <select value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value as StockAdjustmentRequest['reason'] })}
              className="rounded border border-line px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand">
              <option value="ADJUSTMENT">Adjustment</option>
              <option value="RETURN">Return</option>
              <option value="TRANSFER">Transfer</option>
            </select>
          </div>
          <p className="text-xs text-ink/40 mb-3">
            Positive numbers add stock, negative numbers remove it. Sales are entered on the "Record sale" screen
            and reduce stock automatically.
          </p>
          <button onClick={handleSubmit} className="bg-brand hover:bg-brand-dark transition-colors text-white text-sm rounded px-3 py-1.5">
            Record adjustment
          </button>
        </div>
      )}

      <div className="bg-white border border-line rounded overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-paper text-ink/50 text-left">
            <tr>
              <th className="px-4 py-2.5 font-medium">SKU</th>
              <th className="px-4 py-2.5 font-medium">Product</th>
              <th className="px-4 py-2.5 font-medium">Location</th>
              <th className="px-4 py-2.5 font-medium">On hand</th>
              <th className="px-4 py-2.5 font-medium">Updated</th>
              <th className="px-4 py-2.5" />
            </tr>
          </thead>
          <tbody>
            {balances.map((b) => (
              <tr key={`${b.productId}-${b.locationId}`} className="border-t border-line">
                <td className="px-4 py-2.5 data text-xs">{b.productSku}</td>
                <td className="px-4 py-2.5">{b.productName}</td>
                <td className="px-4 py-2.5 text-ink/50">{b.locationName}</td>
                <td className={`px-4 py-2.5 font-medium data ${b.quantityOnHand <= 0 ? 'text-danger' : 'text-ink'}`}>
                  {b.quantityOnHand}
                </td>
                <td className="px-4 py-2.5 text-ink/40 text-xs data">{new Date(b.updatedAt).toLocaleString()}</td>
                <td className="px-4 py-2.5 text-right">
                  <Link to={`/inventory/ledger?productId=${b.productId}&locationId=${b.locationId}`} className="text-ink/50 hover:text-brand text-xs">
                    View history
                  </Link>
                </td>
              </tr>
            ))}
            {balances.length === 0 && (
              <tr><td colSpan={6} className="px-4 py-10 text-center text-ink/40">No inventory recorded yet</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
