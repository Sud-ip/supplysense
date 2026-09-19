import { useEffect, useState } from 'react'
import apiClient from '../../api/client'

type Status = 'checking' | 'up' | 'down'

// Calls GET /actuator/health - the one endpoint from the whole backend
// that's public with no JWT needed (see SecurityConfig). This screen's
// only job is proving the frontend can actually reach the backend at
// all, including CORS - before we build anything that depends on that
// connection working.
export default function HealthCheck() {
  const [status, setStatus] = useState<Status>('checking')
  const [detail, setDetail] = useState<string>('')

  useEffect(() => {
    apiClient
      .get('/actuator/health')
      .then((res) => {
        setStatus(res.data.status === 'UP' ? 'up' : 'down')
        setDetail(JSON.stringify(res.data))
      })
      .catch((err) => {
        setStatus('down')
        setDetail(
          err.message === 'Network Error'
            ? 'Network Error - is the backend running, and is CORS configured?'
            : err.message
        )
      })
  }, [])

  const statusStyles: Record<Status, string> = {
    checking: 'bg-slate-100 text-slate-600',
    up: 'bg-emerald-100 text-emerald-700',
    down: 'bg-red-100 text-red-700'
  }

  return (
    <div className="bg-white rounded-lg border border-slate-200 p-5">
      <div className="flex items-center justify-between mb-3">
        <span className="text-sm font-medium text-slate-700">Backend connection</span>
        <span className={`text-xs font-medium px-2 py-1 rounded ${statusStyles[status]}`}>
          {status === 'checking' ? 'Checking...' : status === 'up' ? 'Connected' : 'Not connected'}
        </span>
      </div>
      {detail && <p className="text-xs text-slate-400 break-all">{detail}</p>}
    </div>
  )
}
