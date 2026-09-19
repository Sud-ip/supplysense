import { useEffect, useState } from 'react'
import {
  LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer
} from 'recharts'
import { dashboardApi } from './dashboardApi'
import type { DashboardSummary, StockTrendPoint, TopMover, LowStockAlert } from '../../api/types'

const reasonLabels: Record<LowStockAlert['reasons'][number], string> = {
  BELOW_MIN_THRESHOLD: 'Below min threshold',
  PROJECTED_STOCKOUT_SOON: 'Stockout projected soon'
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

  return (
    <div>
      <h1 className="font-display text-2xl font-semibold text-ink mb-6">Dashboard</h1>

      {/* The low-stock count carries the most weight here on purpose -
          it's the number that tells you whether to act today. The other
          two stay quiet beside it. */}
      <div className="grid grid-cols-3 gap-5 mb-8">
        <div
          className={`col-span-1 rounded p-5 border ${
            hasAlerts ? 'bg-warning-soft border-warning/30' : 'bg-white border-line'
          }`}
        >
          <div className="text-sm text-ink/50 mb-1">Needs attention</div>
          <div className={`font-display text-4xl font-semibold data ${hasAlerts ? 'text-warning' : 'text-ink'}`}>
            {summary?.lowStockCount ?? '—'}
          </div>
          <div className="text-xs text-ink/40 mt-1">
            {hasAlerts ? 'products low or trending toward a stockout' : 'products need reordering'}
          </div>
        </div>

        <div className="bg-white border border-line rounded p-5">
          <div className="text-sm text-ink/50 mb-1">Active SKUs</div>
          <div className="font-display text-2xl font-semibold text-ink data">{summary?.totalActiveSkus ?? '—'}</div>
        </div>

        <div className="bg-white border border-line rounded p-5">
          <div className="text-sm text-ink/50 mb-1">Inventory value</div>
          <div className="font-display text-2xl font-semibold text-ink data">
            {summary ? `$${summary.totalInventoryValue}` : '—'}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-5 mb-8">
        <div className="bg-white border border-line rounded p-5">
          <h2 className="text-sm font-medium text-ink mb-4">Stock movement, last 30 days</h2>
          <ResponsiveContainer width="100%" height={220}>
            <LineChart data={trends}>
              <CartesianGrid strokeDasharray="3 3" stroke="#E2E5E9" />
              <XAxis dataKey="date" tick={{ fontSize: 11, fontFamily: 'IBM Plex Mono' }} stroke="#93A0AC" />
              <YAxis tick={{ fontSize: 11, fontFamily: 'IBM Plex Mono' }} stroke="#93A0AC" />
              <Tooltip contentStyle={{ fontFamily: 'IBM Plex Sans', fontSize: 13, borderRadius: 4 }} />
              <Line type="monotone" dataKey="unitsIn" stroke="#1C8A5B" name="Units in" strokeWidth={2} dot={false} />
              <Line type="monotone" dataKey="unitsOut" stroke="#C4433D" name="Units out" strokeWidth={2} dot={false} />
            </LineChart>
          </ResponsiveContainer>
        </div>

        <div className="bg-white border border-line rounded p-5">
          <h2 className="text-sm font-medium text-ink mb-4">Top-moving products, last 30 days</h2>
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={topMovers} layout="vertical" margin={{ left: 40 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#E2E5E9" />
              <XAxis type="number" tick={{ fontSize: 11, fontFamily: 'IBM Plex Mono' }} stroke="#93A0AC" />
              <YAxis
                type="category"
                dataKey="productSku"
                tick={{ fontSize: 11, fontFamily: 'IBM Plex Mono' }}
                stroke="#93A0AC"
                width={80}
              />
              <Tooltip contentStyle={{ fontFamily: 'IBM Plex Sans', fontSize: 13, borderRadius: 4 }} />
              <Bar dataKey="totalUnitsSold" fill="#0F6E5E" name="Units sold" radius={[0, 2, 2, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="bg-white border border-line rounded overflow-hidden">
        <div className="px-5 py-3 border-b border-line">
          <h2 className="text-sm font-medium text-ink">Low-stock alerts</h2>
        </div>
        <table className="w-full text-sm">
          <thead className="bg-paper text-ink/50 text-left">
            <tr>
              <th className="px-5 py-2 font-medium">SKU</th>
              <th className="px-5 py-2 font-medium">Location</th>
              <th className="px-5 py-2 font-medium">On hand</th>
              <th className="px-5 py-2 font-medium">Min</th>
              <th className="px-5 py-2 font-medium">Projected stockout</th>
              <th className="px-5 py-2 font-medium">Reason</th>
            </tr>
          </thead>
          <tbody>
            {alerts.map((a) => (
              <tr key={`${a.productId}-${a.locationId}`} className="border-t border-line">
                <td className="px-5 py-2.5 data text-xs">{a.productSku}</td>
                <td className="px-5 py-2.5 text-ink/60">{a.locationName}</td>
                <td className="px-5 py-2.5 font-medium text-danger data">{a.quantityOnHand}</td>
                <td className="px-5 py-2.5 text-ink/60 data">{a.minQuantity}</td>
                <td className="px-5 py-2.5 text-ink/60 data">
                  {a.projectedStockoutDate ? new Date(a.projectedStockoutDate).toLocaleDateString() : '—'}
                </td>
                <td className="px-5 py-2.5">
                  {a.reasons.map((r) => (
                    <span
                      key={r}
                      className="inline-block bg-warning-soft text-warning text-xs rounded px-2 py-0.5 mr-1"
                    >
                      {reasonLabels[r]}
                    </span>
                  ))}
                </td>
              </tr>
            ))}
            {alerts.length === 0 && (
              <tr>
                <td colSpan={6} className="px-5 py-10 text-center text-ink/40">
                  Nothing needs attention right now.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
