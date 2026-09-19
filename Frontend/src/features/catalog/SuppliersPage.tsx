import { useEffect, useState } from 'react'
import { suppliersApi } from './catalogApi'
import type { Supplier, SupplierRequest } from '../../api/types'
import { useRole } from '../../app/useRole'

const emptyForm: SupplierRequest = { name: '', contactInfo: null }

export default function SuppliersPage() {
  const { canManageCatalog } = useRole()
  const [suppliers, setSuppliers] = useState<Supplier[]>([])
  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState<SupplierRequest>(emptyForm)
  const [error, setError] = useState<string | null>(null)

  function load() { suppliersApi.list().then(setSuppliers) }
  useEffect(load, [])

  function startCreate() { setForm(emptyForm); setEditingId(null); setShowForm(true); setError(null) }
  function startEdit(s: Supplier) {
    setForm({ name: s.name, contactInfo: s.contactInfo })
    setEditingId(s.id); setShowForm(true); setError(null)
  }

  async function handleSubmit() {
    setError(null)
    try {
      if (editingId) await suppliersApi.update(editingId, form)
      else await suppliersApi.create(form)
      setShowForm(false); load()
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Failed to save supplier')
    }
  }

  async function handleDeactivate(id: string) {
    if (!confirm('Deactivate this supplier?')) return
    await suppliersApi.deactivate(id); load()
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="font-display text-2xl font-semibold text-ink">Suppliers</h1>
        {canManageCatalog && (
          <button onClick={startCreate} className="bg-brand hover:bg-brand-dark transition-colors text-white text-sm rounded px-3 py-1.5">
            Add supplier
          </button>
        )}
      </div>

      {showForm && (
        <div className="bg-white border border-line rounded p-4 mb-5">
          {error && <div className="text-sm text-danger bg-danger-soft rounded px-3 py-2 mb-3">{error}</div>}
          <div className="grid grid-cols-2 gap-3 mb-3">
            <input placeholder="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })}
              className="rounded border border-line px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand" />
            <input placeholder="Contact info" value={form.contactInfo ?? ''} onChange={(e) => setForm({ ...form, contactInfo: e.target.value || null })}
              className="rounded border border-line px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand" />
          </div>
          <div className="flex gap-2">
            <button onClick={handleSubmit} className="bg-brand hover:bg-brand-dark transition-colors text-white text-sm rounded px-3 py-1.5">
              {editingId ? 'Save changes' : 'Create supplier'}
            </button>
            <button onClick={() => setShowForm(false)} className="bg-paper text-ink/70 text-sm rounded px-3 py-1.5 border border-line">Cancel</button>
          </div>
        </div>
      )}

      <div className="bg-white border border-line rounded overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-paper text-ink/50 text-left">
            <tr>
              <th className="px-4 py-2.5 font-medium">Name</th>
              <th className="px-4 py-2.5 font-medium">Contact</th>
              <th className="px-4 py-2.5 font-medium">Status</th>
              {canManageCatalog && <th className="px-4 py-2.5" />}
            </tr>
          </thead>
          <tbody>
            {suppliers.map((s) => (
              <tr key={s.id} className="border-t border-line">
                <td className="px-4 py-2.5">{s.name}</td>
                <td className="px-4 py-2.5 text-ink/50">{s.contactInfo ?? '—'}</td>
                <td className="px-4 py-2.5">
                  <span className={`text-xs rounded px-2 py-0.5 ${s.active ? 'bg-success-soft text-success' : 'bg-paper text-ink/40'}`}>
                    {s.active ? 'Active' : 'Inactive'}
                  </span>
                </td>
                {canManageCatalog && (
                  <td className="px-4 py-2.5 text-right space-x-3">
                    <button onClick={() => startEdit(s)} className="text-ink/50 hover:text-brand text-xs">Edit</button>
                    <button onClick={() => handleDeactivate(s.id)} className="text-danger/70 hover:text-danger text-xs">Deactivate</button>
                  </td>
                )}
              </tr>
            ))}
            {suppliers.length === 0 && (
              <tr><td colSpan={4} className="px-4 py-10 text-center text-ink/40">No suppliers yet</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
