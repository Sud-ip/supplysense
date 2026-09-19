import { createSlice, type PayloadAction } from '@reduxjs/toolkit'

export type Role = 'OWNER' | 'MANAGER' | 'STAFF'

export interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  userId: string | null
  tenantId: string | null
  email: string | null
  role: Role | null
}

export interface AuthCredentials {
  accessToken: string
  refreshToken: string
  userId: string
  tenantId: string
  email: string
  role: Role
}

const STORAGE_KEY = 'supplysense.auth'

// Reading persisted auth on load (not just in-memory Redux state) is what
// makes a page refresh not log the user out. This is a pragmatic choice
// over adding redux-persist as a dependency: for one slice with a handful
// of primitive fields, a manual localStorage read/write is simpler and
// has zero extra dependencies - redux-persist starts earning its keep
// once there are several slices that all need this, which isn't the
// case yet.
function loadPersistedAuth(): AuthState {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) {
    return emptyState()
  }
  try {
    return JSON.parse(raw) as AuthState
  } catch {
    return emptyState()
  }
}

function emptyState(): AuthState {
  return {
    accessToken: null,
    refreshToken: null,
    userId: null,
    tenantId: null,
    email: null,
    role: null
  }
}

const authSlice = createSlice({
  name: 'auth',
  initialState: loadPersistedAuth(),
  reducers: {
    credentialsReceived(state, action: PayloadAction<AuthCredentials>) {
      Object.assign(state, action.payload)
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
    },
    loggedOut() {
      localStorage.removeItem(STORAGE_KEY)
      return emptyState()
    }
  }
})

export const { credentialsReceived, loggedOut } = authSlice.actions
export default authSlice.reducer
