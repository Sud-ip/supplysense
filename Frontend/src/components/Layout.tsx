import { NavLink, Outlet } from 'react-router-dom'
import { useAppDispatch, useAppSelector } from '../app/hooks'
import { loggedOut } from '../features/auth/authSlice'
import { useRole } from '../app/useRole'

const navItems = [
  { to: '/', label: 'Dashboard' },
  { to: '/products', label: 'Products' },
  { to: '/suppliers', label: 'Suppliers' },
  { to: '/locations', label: 'Locations' },
  { to: '/inventory', label: 'Inventory' },
  { to: '/inventory/thresholds', label: 'Reorder thresholds' },
  { to: '/sales/new', label: 'Record sale' },
  { to: '/sales/import', label: 'Import sales CSV' }
]

export default function Layout() {
  const { email, role } = useAppSelector((state) => state.auth)
  const { canManageUsers } = useRole()
  const dispatch = useAppDispatch()

  return (
    <div className="min-h-screen flex">
      <aside className="w-60 bg-ink flex flex-col shrink-0">
        <div className="p-5 flex items-center gap-2.5">
          <span className="w-2.5 h-2.5 rounded-full bg-brand" aria-hidden="true" />
          <span className="font-display font-semibold text-white tracking-tight">SupplySense</span>
        </div>

        <nav className="flex-1 px-3 space-y-0.5">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                `block px-3 py-2 rounded text-sm transition-colors ${
                  isActive ? 'bg-brand text-white font-medium' : 'text-white/60 hover:text-white hover:bg-white/5'
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}

          {canManageUsers && (
            <NavLink
              to="/team"
              className={({ isActive }) =>
                `block px-3 py-2 rounded text-sm transition-colors ${
                  isActive ? 'bg-brand text-white font-medium' : 'text-white/60 hover:text-white hover:bg-white/5'
                }`
              }
            >
              Team
            </NavLink>
          )}
        </nav>

        <div className="p-4 mx-3 mb-3 border-t border-white/10">
          <NavLink to="/profile" className="block pt-4 hover:opacity-80 transition-opacity">
            <p className="text-sm text-white/90 truncate">{email}</p>
            <p className="text-xs text-white/40">{role}</p>
          </NavLink>
          <button
            onClick={() => dispatch(loggedOut())}
            className="text-xs text-white/50 hover:text-white transition-colors mt-3"
          >
            Log out
          </button>
        </div>
      </aside>

      <main className="flex-1 p-8 overflow-auto">
        <Outlet />
      </main>
    </div>
  )
}
