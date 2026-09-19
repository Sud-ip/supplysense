import { useEffect, useState } from 'react'
import { locationsApi } from './catalogApi'
import type { Location, LocationRequest } from '../../api/types'
import { useRole } from '../../app/useRole'

const emptyForm: LocationRequest = { name: '', address: null }

export default function LocationsPage() {
  const { canManageCatalog } = useRole()
  const [locations, setLocations] = useState<Location[]>([])
  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState<LocationRequest>(emptyForm)
  const [error, setError] = useState<string | null>(null)

  function load() { locationsApi.list().then(setLocations) }
  useEffect(load, [])

  function startCreate() { setForm(emptyForm); setEditingId(null); setShowForm(true); setError(null) }
  function startEdit(l: Location) {
    setForm({ name: l.name, address: l.address })
    setEditingId(l.id); setShowForm(true); setError(null)
  }

  async function handleSubmit() {
    setError(null)
    try {
      if (editingId) await locationsApi.update(editingId, form)
      else await locationsApi.create(form)
      setShowForm(false); load()
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Failed to save location')
    }
  }

  async function handleDeactivate(id: string) {
    if (!confirm('Deactivate this location?')) return
    await locationsApi.deactivate(id); load()
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="font-display text-2xl font-semibold text-ink">Locations</h1>
        {canManageCatalog && (
          <button onClick={startCreate} className="bg-brand hover:bg-brand-dark transition-colors text-white text-sm rounded px-3 py-1.5">
            Add location
          </button>
        )}
      </div>

      {showForm && (
        <div className="bg-white border border-line rounded p-4 mb-5">
          {error && <div className="text-sm text-danger bg-danger-soft rounded px-3 py-2 mb-3">{error}</div>}
          <div className="grid grid-cols-2 gap-3 mb-3">
            <input placeholder="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })}
              className="rounded border border-line px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand" />
            <input placeholder="Address" value={form.address ?? ''} onChange={(e) => setForm({ ...form, address: e.target.value || null })}
              className="rounded border border-line px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand" />
          </div>
          <div className="flex gap-2">
            <button onClick={handleSubmit} className="bg-brand hover:bg-brand-dark transition-colors text-white text-sm rounded px-3 py-1.5">
              {editingId ? 'Save changes' : 'Create location'}
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
              <th className="px-4 py-2.5 font-medium">Address</th>
              <th className="px-4 py-2.5 font-medium">Status</th>
              {canManageCatalog && <th className="px-4 py-2.5" />}
            </tr>
          </thead>
          <tbody>
            {locations.map((l) => (
              <tr key={l.id} className="border-t border-line">
                <td className="px-4 py-2.5">{l.name}</td>
                <td className="px-4 py-2.5 text-ink/50">{l.address ?? '—'}</td>
                <td className="px-4 py-2.5">
                  <span className={`text-xs rounded px-2 py-0.5 ${l.active ? 'bg-success-soft text-success' : 'bg-paper text-ink/40'}`}>
                    {l.active ? 'Active' : 'Inactive'}
                  </span>
                </td>
                {canManageCatalog && (
                  <td className="px-4 py-2.5 text-right space-x-3">
                    <button onClick={() => startEdit(l)} className="text-ink/50 hover:text-brand text-xs">Edit</button>
                    <button onClick={() => handleDeactivate(l.id)} className="text-danger/70 hover:text-danger text-xs">Deactivate</button>
                  </td>
                )}
              </tr>
            ))}
            {locations.length === 0 && (
              <tr><td colSpan={4} className="px-4 py-10 text-center text-ink/40">No locations yet</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
