import { useEffect, useState } from 'react'
import { salesApi } from './salesApi'
import { productsApi, locationsApi } from '../catalog/catalogApi'
import type { Product, Location, SaleRequest, Sale } from '../../api/types'

const emptyForm: SaleRequest = { productId: '', locationId: '', quantity: 1, unitPrice: null, soldAt: null }

export default function RecordSalePage() {
  const [products, setProducts] = useState<Product[]>([])
  const [locations, setLocations] = useState<Location[]>([])
  const [form, setForm] = useState<SaleRequest>(emptyForm)
  const [error, setError] = useState<string | null>(null)
  const [lastSale, setLastSale] = useState<Sale | null>(null)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    productsApi.list().then(setProducts)
    locationsApi.list().then(setLocations)
  }, [])

  async function handleSubmit() {
    setError(null); setLastSale(null)
    if (!form.productId || !form.locationId || form.quantity <= 0) {
      setError('Product, location, and a positive quantity are required'); return
    }
    setSubmitting(true)
    try {
      const sale = await salesApi.recordSale(form)
      setLastSale(sale)
      setForm({ ...emptyForm, productId: form.productId, locationId: form.locationId })
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Failed to record sale')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="max-w-lg">
      <h1 className="font-display text-2xl font-semibold text-ink mb-1">Record a sale</h1>
      <p className="text-ink/50 text-sm mb-6">
        Recording a sale automatically reduces the product's stock at this location.
      </p>

      <div className="bg-white border border-line rounded p-5">
        {error && <div className="text-sm text-danger bg-danger-soft rounded px-3 py-2 mb-3">{error}</div>}
        {lastSale && (
          <div className="text-sm text-success bg-success-soft rounded px-3 py-2 mb-3">
            Recorded: {lastSale.quantity} x {lastSale.productSku} at {lastSale.locationName}
          </div>
        )}

        <div className="space-y-3 mb-4">
          <select value={form.productId} onChange={(e) => setForm({ ...form, productId: e.target.value })}
            className="w-full rounded border border-line px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand">
            <option value="">Select product</option>
            {products.map((p) => <option key={p.id} value={p.id}>{p.sku} - {p.name}</option>)}
          </select>

          <select value={form.locationId} onChange={(e) => setForm({ ...form, locationId: e.target.value })}
            className="w-full rounded border border-line px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand">
            <option value="">Select location</option>
            {locations.map((l) => <option key={l.id} value={l.id}>{l.name}</option>)}
          </select>

          <input type="number" min={1} placeholder="Quantity sold" value={form.quantity || ''}
            onChange={(e) => setForm({ ...form, quantity: Number(e.target.value) })}
            className="w-full rounded border border-line px-3 py-2 text-sm data focus:outline-none focus:ring-2 focus:ring-brand" />

          <input type="number" step="0.01" placeholder="Unit price (optional - defaults to product's listed price)"
            value={form.unitPrice ?? ''} onChange={(e) => setForm({ ...form, unitPrice: e.target.value ? Number(e.target.value) : null })}
            className="w-full rounded border border-line px-3 py-2 text-sm data focus:outline-none focus:ring-2 focus:ring-brand" />
        </div>

        <button onClick={handleSubmit} disabled={submitting}
          className="bg-brand hover:bg-brand-dark transition-colors text-white text-sm rounded px-3 py-1.5 disabled:opacity-50">
          {submitting ? 'Recording...' : 'Record sale'}
        </button>
      </div>
    </div>
  )
}
