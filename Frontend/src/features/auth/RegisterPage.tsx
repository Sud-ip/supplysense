import { type FormEvent, useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import apiClient from '../../api/client'
import { useAppDispatch } from '../../app/hooks'
import { credentialsReceived, type AuthCredentials } from './authSlice'

interface RegisterResponse {
  accessToken: string
  refreshToken: string
  userId: string
  tenantId: string
  email: string
  role: AuthCredentials['role']
}

export default function RegisterPage() {
  const [businessName, setBusinessName] = useState('')
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const dispatch = useAppDispatch()
  const navigate = useNavigate()

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)

    try {
      const res = await apiClient.post<RegisterResponse>('/api/v1/auth/register', {
        businessName,
        fullName,
        email,
        password
      })
      dispatch(credentialsReceived(res.data))
      navigate('/')
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Registration failed. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-6">
      <div className="max-w-sm w-full">
        <div className="flex items-center gap-2.5 mb-8">
          <span className="w-2.5 h-2.5 rounded-full bg-brand" aria-hidden="true" />
          <span className="font-display font-semibold text-ink text-lg tracking-tight">SupplySense</span>
        </div>

        <h1 className="font-display text-2xl font-semibold text-ink mb-1">Register your business</h1>
        <p className="text-ink/50 mb-6">You'll be set up as the account owner.</p>

        <form onSubmit={handleSubmit} className="bg-white border border-line rounded p-6 space-y-4">
          {error && (
            <div className="text-sm text-danger bg-danger-soft rounded px-3 py-2">{error}</div>
          )}

          <div>
            <label className="block text-sm font-medium text-ink mb-1">Business name</label>
            <input
              required
              value={businessName}
              onChange={(e) => setBusinessName(e.target.value)}
              className="w-full rounded border border-line px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-ink mb-1">Your full name</label>
            <input
              required
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              className="w-full rounded border border-line px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-ink mb-1">Email</label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full rounded border border-line px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-ink mb-1">Password</label>
            <input
              type="password"
              required
              minLength={8}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full rounded border border-line px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand"
            />
            <p className="text-xs text-ink/40 mt-1">At least 8 characters</p>
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="w-full bg-brand hover:bg-brand-dark transition-colors text-white rounded py-2 text-sm font-medium disabled:opacity-50"
          >
            {submitting ? 'Creating account...' : 'Create account'}
          </button>

          <p className="text-sm text-ink/50 text-center">
            Already have an account?{' '}
            <Link to="/login" className="text-brand font-medium hover:text-brand-dark">
              Sign in
            </Link>
          </p>
        </form>
      </div>
    </div>
  )
}
