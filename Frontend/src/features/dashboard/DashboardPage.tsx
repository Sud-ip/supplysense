import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import { dashboardApi } from './dashboardApi'
import type { DashboardSummary, StockTrendPoint, TopMover, LowStockAlert } from '../../api/types'

const reasonLabels: Record<LowStockAlert['reasons'][number], string> = {
  BELOW_MIN_THRESHOLD: 'Below minimum',
  PROJECTED_STOCKOUT_SOON: 'Stockout soon'
}

export default function DashboardPage() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [trends, setTrends] = useState<StockTrendPoint[]>([])
  const [topMovers, setTopMovers] = useState<TopMover[]>([])
  const [alerts, setAlerts] = useState<LowStockAlert[]>([])

  useEffect(() => {
    dashboardApi.getSummary().then(setSummary)
    dashboardApi.getTrends(30).then(setTrends)
    dashboardApi.getTopMovers(30, 10).then(setTopMovers)
    dashboardApi.getLowStockAlerts().then(setAlerts)
  }, [])

  const hasAlerts = (summary?.lowStockCount ?? 0) > 0
  const tooltipStyle = { fontFamily: 'IBM Plex Sans', fontSize: 12, borderRadius: 12, border: '1px solid #E4E9F2', boxShadow: '0 12px 25px -12px rgba(30,41,70,.22)' }

  return (
    <div className="space-y-7">
      <section className="flex flex-col justify-between gap-5 sm:flex-row sm:items-end">
        <div><p className="mb-2 text-xs font-semibold uppercase tracking-[.16em] text-brand">Command center</p><h1 className="page-title">Inventory, in focus.</h1><p className="page-subtitle">Your live view of stock health and sales momentum.</p></div>
        <Link to="/sales/new" className="btn-primary">+ Record a sale</Link>
      </section>

      <section className="grid gap-4 md:grid-cols-3">
        <div className={`relative overflow-hidden rounded-2xl border p-6 shadow-xl shadow-slate-200/40 ${hasAlerts ? 'border-warning/30 bg-warning-soft' : 'border-brand/20 bg-gradient-to-br from-brand to-[#7487ff]'}`}>
          <p className={`mb-3 text-sm font-medium ${hasAlerts ? 'text-warning' : 'text-white/70'}`}>Needs attention</p>
          <p className={`font-display text-5xl font-semibold data ${hasAlerts ? 'text-warning' : 'text-white'}`}>{summary?.lowStockCount ?? '—'}</p>
          <p className={`mt-2 text-xs ${hasAlerts ? 'text-ink/45' : 'text-white/65'}`}>{hasAlerts ? 'products low or trending toward a stockout' : 'Everything is comfortably stocked'}</p>
        </div>
        <div className="surface p-6"><div className="mb-4 flex items-center justify-between"><p className="text-sm font-medium text-ink/55">Active SKUs</p><span className="badge bg-brand-soft text-brand">Catalog</span></div><p className="font-display text-4xl font-semibold data">{summary?.totalActiveSkus ?? '—'}</p><p className="mt-2 text-xs text-ink/40">Products currently available to sell</p></div>
        <div className="surface p-6"><div className="mb-4 flex items-center justify-between"><p className="text-sm font-medium text-ink/55">Inventory value</p><span className="badge bg-success-soft text-success">Live</span></div><p className="font-display text-4xl font-semibold data">{summary ? `$${summary.totalInventoryValue}` : '—'}</p><p className="mt-2 text-xs text-ink/40">Based on current quantity on hand</p></div>
      </section>

      <section className="grid gap-5 xl:grid-cols-2">
        <div className="surface p-6"><div className="mb-5 flex items-start justify-between"><div><h2 className="font-display text-lg font-semibold">Stock movement</h2><p className="mt-1 text-xs text-ink/45">Units moving in and out over 30 days</p></div><span className="badge bg-slate-100 text-ink/55">30 days</span></div><ResponsiveContainer width="100%" height={240}><LineChart data={trends}><CartesianGrid vertical={false} strokeDasharray="3 3" stroke="#E4E9F2" /><XAxis dataKey="date" tick={{ fontSize: 10, fontFamily: 'IBM Plex Mono' }} stroke="#9AA5B5" tickLine={false} axisLine={false} /><YAxis tick={{ fontSize: 10, fontFamily: 'IBM Plex Mono' }} stroke="#9AA5B5" tickLine={false} axisLine={false} /><Tooltip contentStyle={tooltipStyle} /><Line type="monotone" dataKey="unitsIn" stroke="#1C8A5B" name="Units in" strokeWidth={2.5} dot={false} /><Line type="monotone" dataKey="unitsOut" stroke="#C4433D" name="Units out" strokeWidth={2.5} dot={false} /></LineChart></ResponsiveContainer></div>
        <div className="surface p-6"><div className="mb-5 flex items-start justify-between"><div><h2 className="font-display text-lg font-semibold">Top movers</h2><p className="mt-1 text-xs text-ink/45">Your highest-volume products</p></div><span className="badge bg-brand-soft text-brand">30 days</span></div><ResponsiveContainer width="100%" height={240}><BarChart data={topMovers} layout="vertical" margin={{ left: 36 }}><CartesianGrid horizontal={false} strokeDasharray="3 3" stroke="#E4E9F2" /><XAxis type="number" tick={{ fontSize: 10, fontFamily: 'IBM Plex Mono' }} stroke="#9AA5B5" tickLine={false} axisLine={false} /><YAxis type="category" dataKey="productSku" tick={{ fontSize: 10, fontFamily: 'IBM Plex Mono' }} stroke="#9AA5B5" tickLine={false} axisLine={false} width={70} /><Tooltip contentStyle={tooltipStyle} /><Bar dataKey="totalUnitsSold" fill="#536DFE" name="Units sold" radius={[0, 8, 8, 0]} /></BarChart></ResponsiveContainer></div>
      </section>

      <section className="table-wrap"><div className="flex items-center justify-between border-b border-line px-6 py-5"><div><h2 className="font-display text-lg font-semibold">Low-stock alerts</h2><p className="mt-1 text-xs text-ink/45">Prioritize replenishment before you run out.</p></div><span className={`badge ${hasAlerts ? 'bg-warning-soft text-warning' : 'bg-success-soft text-success'}`}>{alerts.length} open</span></div><table className="data-table"><thead><tr><th>SKU</th><th>Location</th><th>On hand</th><th>Min</th><th>Projected stockout</th><th>Reason</th></tr></thead><tbody>{alerts.map((a) => <tr key={`${a.productId}-${a.locationId}`}><td className="data text-xs font-semibold">{a.productSku}</td><td className="text-ink/60">{a.locationName}</td><td className="font-semibold text-danger data">{a.quantityOnHand}</td><td className="text-ink/60 data">{a.minQuantity}</td><td className="text-ink/60 data">{a.projectedStockoutDate ? new Date(a.projectedStockoutDate).toLocaleDateString() : '—'}</td><td>{a.reasons.map((reason) => <span key={reason} className="badge mr-1 bg-warning-soft text-warning">{reasonLabels[reason]}</span>)}</td></tr>)}{alerts.length === 0 && <tr><td colSpan={6} className="py-14 text-center text-ink/40">Nothing needs attention right now.</td></tr>}</tbody></table></section>
    </div>
  )
}