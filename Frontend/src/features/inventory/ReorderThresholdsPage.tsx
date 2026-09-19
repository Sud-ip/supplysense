import { useEffect, useState } from 'react'
import { reorderThresholdsApi } from './inventoryApi'
import { productsApi, locationsApi } from '../catalog/catalogApi'
import type { ReorderThreshold, ReorderThresholdRequest, Product, Location } from '../../api/types'
import { useRole } from '../../app/useRole'

const emptyForm: ReorderThresholdRequest = { productId: '', locationId: '', minQuantity: 0, reorderQuantity: 0 }

export default function ReorderThresholdsPage() {
  const { canManageCatalog } = useRole()
  const [thresholds, setThresholds] = useState<ReorderThreshold[]>([])
  const [products, setProducts] = useState<Product[]>([])
  const [locations, setLocations] = useState<Location[]>([])
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState<ReorderThresholdRequest>(emptyForm)
  const [error, setError] = useState<string | null>(null)

  function loadAll() {
    reorderThresholdsApi.list().then(setThresholds)
    productsApi.list().then(setProducts)
    locationsApi.list().then(setLocations)
  }
  useEffect(loadAll, [])

  async function handleSubmit() {
    setError(null)
    try {
      await reorderThresholdsApi.upsert(form)
      setForm(emptyForm); setShowForm(false); loadAll()
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Failed to save threshold')
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="font-display text-2xl font-semibold text-ink">Reorder thresholds</h1>
        {canManageCatalog && (
          <button onClick={() => setShowForm(!showForm)} className="bg-brand hover:bg-brand-dark transition-colors text-white text-sm rounded px-3 py-1.5">
            {showForm ? 'Cancel' : 'Set threshold'}
          </button>
        )}
      </div>
      <p className="text-ink/50 text-sm mb-5">
        Setting a threshold for a product/location that already has one updates it.
      </p>

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
            <input type="number" placeholder="Minimum quantity" value={form.minQuantity || ''}
              onChange={(e) => setForm({ ...form, minQuantity: Number(e.target.value) })}
              className="rounded border border-line px-3 py-2 text-sm data focus:outline-none focus:ring-2 focus:ring-brand" />
            <input type="number" placeholder="Reorder quantity" value={form.reorderQuantity || ''}
              onChange={(e) => setForm({ ...form, reorderQuantity: Number(e.target.value) })}
              className="rounded border border-line px-3 py-2 text-sm data focus:outline-none focus:ring-2 focus:ring-brand" />
          </div>
          <button onClick={handleSubmit} className="bg-brand hover:bg-brand-dark transition-colors text-white text-sm rounded px-3 py-1.5">
            Save threshold
          </button>
        </div>
      )}

      <div className="bg-white border border-line rounded overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-paper text-ink/50 text-left">
            <tr>
              <th className="px-4 py-2.5 font-medium">SKU</th>
              <th className="px-4 py-2.5 font-medium">Location</th>
              <th className="px-4 py-2.5 font-medium">Min quantity</th>
              <th className="px-4 py-2.5 font-medium">Reorder quantity</th>
            </tr>
          </thead>
          <tbody>
            {thresholds.map((t) => (
              <tr key={t.id} className="border-t border-line">
                <td className="px-4 py-2.5 data text-xs">{t.productSku}</td>
                <td className="px-4 py-2.5 text-ink/50">{t.locationName}</td>
                <td className="px-4 py-2.5 data">{t.minQuantity}</td>
                <td className="px-4 py-2.5 data">{t.reorderQuantity}</td>
              </tr>
            ))}
            {thresholds.length === 0 && (
              <tr><td colSpan={4} className="px-4 py-10 text-center text-ink/40">No thresholds set yet - low-stock alerts won't fire until at least one is set</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
