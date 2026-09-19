import { useAppSelector } from './hooks'
import type { Role } from '../api/types'

// Central place for "can this role write?" logic, matching the backend's
// @PreAuthorize rules exactly (OWNER/MANAGER for catalog+inventory
// writes; all three for sales). Frontend role checks are a UX
// convenience only - hiding a button a STAFF user isn't allowed to use -
// never a security boundary. The real enforcement is server-side
// (@PreAuthorize) and stays true even if this hook were wrong or bypassed.
export function useRole() {
  const role = useAppSelector((state) => state.auth.role) as Role | null
  const canManageCatalog = role === 'OWNER' || role === 'MANAGER'
  const canManageUsers = role === 'OWNER'

  return { role, canManageCatalog, canManageUsers }
}
