import { useAppSelector } from '../../app/hooks'

// Read-only for now: shows what's already in the JWT/Redux state.
// Password change and profile editing aren't built yet - flagging that
// honestly in the UI rather than pretending this page does more than
// it does. A real "change password" flow needs a new backend endpoint
// (verify current password, hash and store the new one) that doesn't
// exist yet.
export default function ProfilePage() {
  const { email, role, tenantId, userId } = useAppSelector((state) => state.auth)

  return (
    <div className="max-w-md">
      <h1 className="font-display text-2xl font-semibold text-ink mb-6">Profile</h1>

      <div className="bg-white border border-line rounded p-5 mb-5">
        <dl className="space-y-4 text-sm">
          <div>
            <dt className="text-ink/50 mb-0.5">Email</dt>
            <dd className="text-ink font-medium">{email}</dd>
          </div>
          <div>
            <dt className="text-ink/50 mb-0.5">Role</dt>
            <dd>
              <span className="inline-block bg-brand-soft text-brand text-xs rounded px-2 py-0.5 font-medium">
                {role}
              </span>
            </dd>
          </div>
          <div>
            <dt className="text-ink/50 mb-0.5">User ID</dt>
            <dd className="text-ink/70 data text-xs">{userId}</dd>
          </div>
          <div>
            <dt className="text-ink/50 mb-0.5">Tenant ID</dt>
            <dd className="text-ink/70 data text-xs">{tenantId}</dd>
          </div>
        </dl>
      </div>

      <div className="bg-paper border border-line rounded p-4">
        <p className="text-xs text-ink/50">
          Password changes and profile editing aren't available yet. If you need a password reset for now, ask
          your account owner to deactivate and re-create your account with a new temporary password from the
          Team page.
        </p>
      </div>
    </div>
  )
}
