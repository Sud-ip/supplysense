import { NavLink, Outlet } from 'react-router-dom'
import { useAppDispatch, useAppSelector } from '../app/hooks'
import { loggedOut } from '../features/auth/authSlice'
import { useRole } from '../app/useRole'

const navItems = [
  { to: '/', label: 'Dashboard', icon: '⌂' },
  { to: '/products', label: 'Products', icon: '□' },
  { to: '/suppliers', label: 'Suppliers', icon: '◇' },
  { to: '/locations', label: 'Locations', icon: '⌖' },
  { to: '/inventory', label: 'Inventory', icon: '▣' },
  { to: '/inventory/thresholds', label: 'Reorder points', icon: '◎' },
  { to: '/sales/new', label: 'Record sale', icon: '↗' },
  { to: '/sales/import', label: 'Import CSV', icon: '⇧' }
]

export default function Layout() {
  const { email, role } = useAppSelector((state) => state.auth)
  const { canManageUsers } = useRole()
  const dispatch = useAppDispatch()

  const navClass = ({ isActive }: { isActive: boolean }) =>
    `flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm transition-all ${isActive ? 'bg-white/12 font-medium text-white shadow-sm' : 'text-white/60 hover:bg-white/5 hover:text-white'}`

  return (
    <div className="min-h-screen lg:flex">
      <aside className="relative flex shrink-0 flex-col overflow-hidden bg-ink px-4 py-5 lg:fixed lg:inset-y-0 lg:w-72">
        <div className="absolute -right-16 -top-20 h-48 w-48 rounded-full bg-brand/25 blur-3xl" />
        <div className="relative flex items-center gap-3 px-3 pb-8">
          <span className="grid h-9 w-9 place-items-center rounded-xl bg-brand text-lg font-bold text-white shadow-lg shadow-brand/30">S</span>
          <div><span className="font-display text-lg font-semibold tracking-tight text-white">SupplySense</span><p className="text-[10px] font-medium uppercase tracking-[.18em] text-white/40">Inventory intelligence</p></div>
        </div>
        <nav className="relative flex-1 space-y-1">
          <p className="px-3 pb-2 text-[10px] font-semibold uppercase tracking-[.16em] text-white/35">Workspace</p>
          {navItems.map((item) => <NavLink key={item.to} to={item.to} end={item.to === '/'} className={navClass}><span className="grid h-5 w-5 place-items-center text-base text-brand-soft">{item.icon}</span>{item.label}</NavLink>)}
          {canManageUsers && <NavLink to="/team" className={navClass}><span className="grid h-5 w-5 place-items-center text-brand-soft">♧</span>Team</NavLink>}
        </nav>
        <div className="relative mx-1 mt-6 rounded-2xl border border-white/10 bg-white/5 p-3">
          <NavLink to="/profile" className="flex items-center gap-3 transition-opacity hover:opacity-80">
            <span className="grid h-9 w-9 place-items-center rounded-full bg-brand-soft text-sm font-bold text-brand">{email?.[0]?.toUpperCase() ?? 'S'}</span>
            <div className="min-w-0"><p className="truncate text-sm font-medium text-white/90">{email}</p><p className="text-xs text-white/40">{role}</p></div>
          </NavLink>
          <button onClick={() => dispatch(loggedOut())} className="mt-3 text-xs font-medium text-white/50 transition-colors hover:text-white">Log out</button>
        </div>
      </aside>
      <main className="min-w-0 flex-1 lg:ml-72"><div className="mx-auto max-w-7xl p-5 sm:p-8 lg:p-10"><Outlet /></div></main>
    </div>
  )
}