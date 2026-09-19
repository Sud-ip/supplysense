import apiClient from '../../api/client'
import type { DashboardSummary, StockTrendPoint, TopMover, LowStockAlert } from '../../api/types'

export const dashboardApi = {
  getSummary: () => apiClient.get<DashboardSummary>('/api/v1/dashboard/summary').then((r) => r.data),

  getTrends: (days = 30) =>
    apiClient.get<StockTrendPoint[]>('/api/v1/dashboard/trends', { params: { days } }).then((r) => r.data),

  getTopMovers: (days = 30, limit = 10) =>
    apiClient
      .get<TopMover[]>('/api/v1/dashboard/top-movers', { params: { days, limit } })
      .then((r) => r.data),

  getLowStockAlerts: () =>
    apiClient.get<LowStockAlert[]>('/api/v1/alerts/low-stock').then((r) => r.data)
}
