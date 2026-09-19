import { useEffect, useState } from 'react'
import { teamApi } from './teamApi'
import type { UserAccount, CreateUserRequest } from '../../api/types'
import { useRole } from '../../app/useRole'
import { useAppSelector } from '../../app/hooks'

const emptyForm: CreateUserRequest = { fullName: '', email: '', password: '', role: 'STAFF' }

export default function TeamPage() {
  const { canManageUsers } = useRole()
  const currentUserId = useAppSelector((s) => s.auth.userId)

  const [users, setUsers] = useState<UserAccount[]>([])
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState<CreateUserRequest>(emptyForm)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  function load() {
    teamApi.list().then(setUsers)
  }
  useEffect(load, [])

  async function handleSubmit() {
    setError(null)
    setSubmitting(true)
    try {
      await teamApi.create(form)
      setForm(emptyForm)
      setShowForm(false)
      load()
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Failed to create user')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDeactivate(id: string) {
    if (!confirm('Deactivate this team member? They will no longer be able to sign in.')) return
    await teamApi.deactivate(id)
    load()
  }

  if (!canManageUsers) {
    return (
      <div className="max-w-md">
        <h1 className="font-display text-2xl font-semibold text-ink mb-2">Team</h1>
        <p className="text-ink/50 text-sm">
          Only the account owner can view and manage team members.
        </p>
      </div>
    )
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-1">
        <h1 className="font-display text-2xl font-semibold text-ink">Team</h1>
        <button
          onClick={() => setShowForm(!showForm)}
          className="bg-brand hover:bg-brand-dark transition-colors text-white text-sm rounded px-3 py-1.5"
        >
          {showForm ? 'Cancel' : 'Invite team member'}
        </button>
      </div>
      <p className="text-ink/50 text-sm mb-5">
        Manage who can sign in to this account and what they can do. Managers can edit the catalog and inventory;
        staff can record sales and view stock but can't change catalog data or see cost prices.
      </p>

      {showForm && (
        <div className="bg-white border border-line rounded p-4 mb-5">
          {error && <div className="text-sm text-danger bg-danger-soft rounded px-3 py-2 mb-3">{error}</div>}
          <div className="grid grid-cols-2 gap-3 mb-3">
            <input
              placeholder="Full name"
              value={form.fullName}
              onChange={(e) => setForm({ ...form, fullName: e.target.value })}
              className="rounded border border-line px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand"
            />
            <input
              type="email"
              placeholder="Email"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              className="rounded border border-line px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand"
            />
            <input
              type="password"
              placeholder="Temporary password"
              minLength={8}
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              className="rounded border border-line px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand"
            />
            <select
              value={form.role}
              onChange={(e) => setForm({ ...form, role: e.target.value as CreateUserRequest['role'] })}
              className="rounded border border-line px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand"
            >
              <option value="STAFF">Staff</option>
              <option value="MANAGER">Manager</option>
            </select>
          </div>
          <p className="text-xs text-ink/40 mb-3">
            Share this password with them directly - there's no invite email yet, so they'll sign in with exactly
            what you set here. At least 8 characters.
          </p>
          <button
            onClick={handleSubmit}
            disabled={submitting}
            className="bg-brand hover:bg-brand-dark transition-colors text-white text-sm rounded px-3 py-1.5 disabled:opacity-50"
          >
            {submitting ? 'Creating...' : 'Create account'}
          </button>
        </div>
      )}

      <div className="bg-white border border-line rounded overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-paper text-ink/50 text-left">
            <tr>
              <th className="px-4 py-2.5 font-medium">Name</th>
              <th className="px-4 py-2.5 font-medium">Email</th>
              <th className="px-4 py-2.5 font-medium">Role</th>
              <th className="px-4 py-2.5 font-medium">Status</th>
              <th className="px-4 py-2.5" />
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id} className="border-t border-line">
                <td className="px-4 py-2.5">
                  {u.fullName}
                  {u.id === currentUserId && <span className="text-ink/40 text-xs ml-1.5">(you)</span>}
                </td>
                <td className="px-4 py-2.5 data text-xs">{u.email}</td>
                <td className="px-4 py-2.5">
                  <span
                    className={`text-xs rounded px-2 py-0.5 ${
                      u.role === 'OWNER' ? 'bg-brand-soft text-brand' : 'bg-paper text-ink/60'
                    }`}
                  >
                    {u.role}
                  </span>
                </td>
                <td className="px-4 py-2.5">
                  <span className={`text-xs rounded px-2 py-0.5 ${u.active ? 'bg-success-soft text-success' : 'bg-paper text-ink/40'}`}>
                    {u.active ? 'Active' : 'Inactive'}
                  </span>
                </td>
                <td className="px-4 py-2.5 text-right">
                  {u.role !== 'OWNER' && u.active && (
                    <button onClick={() => handleDeactivate(u.id)} className="text-danger/70 hover:text-danger text-xs">
                      Deactivate
                    </button>
                  )}
                </td>
              </tr>
            ))}
            {users.length === 0 && (
              <tr>
                <td colSpan={5} className="px-4 py-10 text-center text-ink/40">
                  No team members yet
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
