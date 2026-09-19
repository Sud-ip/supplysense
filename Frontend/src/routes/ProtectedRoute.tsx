import { Navigate, Outlet } from 'react-router-dom'
import { useAppSelector } from '../app/hooks'

// Wraps any set of routes that require a logged-in user. Checked against
// Redux state (which itself was hydrated from localStorage on load - see
// authSlice), not by pinging the backend - fast, synchronous, and correct
// as long as the token exists; an actually-expired-but-present token still
// gets past this check and is caught by the API client's 401 interceptor
// on the first real request instead.
export default function ProtectedRoute() {
  const accessToken = useAppSelector((state) => state.auth.accessToken)

  if (!accessToken) {
    return <Navigate to="/login" replace />
  }

  return <Outlet />
}
