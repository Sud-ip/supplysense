import { useAppDispatch, useAppSelector } from '../../app/hooks'
import { loggedOut } from '../auth/authSlice'

// M7b scope: this screen exists to prove the auth flow works end-to-end
// (protected route, token attached to requests, logout clears state) -
// it is NOT the real dashboard. M7c replaces this with the actual
// summary/trends/top-movers screen backed by the M6 dashboard endpoints.
export default function HomePage() {
  const { email, role, tenantId } = useAppSelector((state) => state.auth)
  const dispatch = useAppDispatch()

  return (
    <div className="min-h-screen bg-slate-50 p-6">
      <div className="max-w-md mx-auto bg-white rounded-lg border border-slate-200 p-6">
        <h1 className="text-lg font-semibold text-slate-900 mb-4">You're logged in</h1>

        <dl className="space-y-2 text-sm mb-6">
          <div className="flex justify-between">
            <dt className="text-slate-500">Email</dt>
            <dd className="text-slate-900 font-medium">{email}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-slate-500">Role</dt>
            <dd className="text-slate-900 font-medium">{role}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-slate-500">Tenant ID</dt>
            <dd className="text-slate-900 font-mono text-xs">{tenantId}</dd>
          </div>
        </dl>

        <button
          onClick={() => dispatch(loggedOut())}
          className="w-full bg-slate-100 text-slate-700 rounded py-2 text-sm font-medium hover:bg-slate-200"
        >
          Log out
        </button>
      </div>
    </div>
  )
}
