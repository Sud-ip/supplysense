import { useEffect, useState } from 'react'
import { productsApi, suppliersApi } from './catalogApi'
import type { Product, ProductRequest, Supplier } from '../../api/types'
import { useRole } from '../../app/useRole'

const emptyForm: ProductRequest = { sku: '', name: '', supplierId: null, unitCost: null, unitPrice: null }

export default function ProductsPage() {
  const { canManageCatalog } = useRole()
  const [products, setProducts] = useState<Product[]>([])
  const [suppliers, setSuppliers] = useState<Supplier[]>([])
  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState<ProductRequest>(emptyForm)
  const [error, setError] = useState<string | null>(null)

  function loadAll() {
    productsApi.list().then(setProducts)
    suppliersApi.list().then(setSuppliers)
  }
  useEffect(loadAll, [])

  function startCreate() {
    setForm(emptyForm)
    setEditingId(null)
    setShowForm(true)
    setError(null)
  }

  function startEdit(p: Product) {
    setForm({
      sku: p.sku,
      name: p.name,
      supplierId: p.supplierId,
      unitCost: p.unitCost ? Number(p.unitCost) : null,
      unitPrice: p.unitPrice ? Number(p.unitPrice) : null
    })
    setEditingId(p.id)
    setShowForm(true)
    setError(null)
  }

  async function handleSubmit() {
    setError(null)
    try {
      if (editingId) await productsApi.update(editingId, form)
      else await productsApi.create(form)
      setShowForm(false)
      loadAll()
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Failed to save product')
    }
  }

  async function handleDeactivate(id: string) {
    if (!confirm('Deactivate this product?')) return
    await productsApi.deactivate(id)
    loadAll()
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="font-display text-2xl font-semibold text-ink">Products</h1>
        {canManageCatalog && (
          <button onClick={startCreate} className="bg-brand hover:bg-brand-dark transition-colors text-white text-sm rounded px-3 py-1.5">
            Add product
          </button>
        )}
      </div>

      {showForm && (
        <div className="bg-white border border-line rounded p-4 mb-5">
          {error && <div className="text-sm text-danger bg-danger-soft rounded px-3 py-2 mb-3">{error}</div>}
          <div className="grid grid-cols-2 gap-3 mb-3">
            <input
              placeholder="SKU"
              value={form.sku}
              onChange={(e) => setForm({ ...form, sku: e.target.value })}
              className="rounded border border-line px-3 py-2 text-sm data focus:outline-none focus:ring-2 focus:ring-brand"
            />
            <input
              placeholder="Name"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              className="rounded border border-line px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand"
            />
            <select
              value={form.supplierId ?? ''}
              onChange={(e) => setForm({ ...form, supplierId: e.target.value || null })}
              className="rounded border border-line px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand"
            >
              <option value="">No supplier</option>
              {suppliers.map((s) => (
                <option key={s.id} value={s.id}>{s.name}</option>
              ))}
            </select>
            <div />
            <input
              type="number"
              step="0.01"
              placeholder="Unit cost"
              value={form.unitCost ?? ''}
              onChange={(e) => setForm({ ...form, unitCost: e.target.value ? Number(e.target.value) : null })}
              className="rounded border border-line px-3 py-2 text-sm data focus:outline-none focus:ring-2 focus:ring-brand"
            />
            <input
              type="number"
              step="0.01"
              placeholder="Unit price"
              value={form.unitPrice ?? ''}
              onChange={(e) => setForm({ ...form, unitPrice: e.target.value ? Number(e.target.value) : null })}
              className="rounded border border-line px-3 py-2 text-sm data focus:outline-none focus:ring-2 focus:ring-brand"
            />
          </div>
          <div className="flex gap-2">
            <button onClick={handleSubmit} className="bg-brand hover:bg-brand-dark transition-colors text-white text-sm rounded px-3 py-1.5">
              {editingId ? 'Save changes' : 'Create product'}
            </button>
            <button onClick={() => setShowForm(false)} className="bg-paper text-ink/70 text-sm rounded px-3 py-1.5 border border-line">
              Cancel
            </button>
          </div>
        </div>
      )}

      <div className="bg-white border border-line rounded overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-paper text-ink/50 text-left">
            <tr>
              <th className="px-4 py-2.5 font-medium">SKU</th>
              <th className="px-4 py-2.5 font-medium">Name</th>
              <th className="px-4 py-2.5 font-medium">Supplier</th>
              <th className="px-4 py-2.5 font-medium">Cost</th>
              <th className="px-4 py-2.5 font-medium">Price</th>
              <th className="px-4 py-2.5 font-medium">Status</th>
              {canManageCatalog && <th className="px-4 py-2.5" />}
            </tr>
          </thead>
          <tbody>
            {products.map((p) => (
              <tr key={p.id} className="border-t border-line">
                <td className="px-4 py-2.5 data text-xs">{p.sku}</td>
                <td className="px-4 py-2.5">{p.name}</td>
                <td className="px-4 py-2.5 text-ink/50">{p.supplierName ?? '—'}</td>
                <td className="px-4 py-2.5 data">{p.unitCost ?? '—'}</td>
                <td className="px-4 py-2.5 data">{p.unitPrice ?? '—'}</td>
                <td className="px-4 py-2.5">
                  <span className={`text-xs rounded px-2 py-0.5 ${p.active ? 'bg-success-soft text-success' : 'bg-paper text-ink/40'}`}>
                    {p.active ? 'Active' : 'Inactive'}
                  </span>
                </td>
                {canManageCatalog && (
                  <td className="px-4 py-2.5 text-right space-x-3">
                    <button onClick={() => startEdit(p)} className="text-ink/50 hover:text-brand text-xs">Edit</button>
                    <button onClick={() => handleDeactivate(p.id)} className="text-danger/70 hover:text-danger text-xs">Deactivate</button>
                  </td>
                )}
              </tr>
            ))}
            {products.length === 0 && (
              <tr><td colSpan={7} className="px-4 py-10 text-center text-ink/40">No products yet</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
