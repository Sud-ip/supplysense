import apiClient from '../../api/client'
import type {
  InventoryBalance,
  StockLedgerEntry,
  StockAdjustmentRequest,
  ReorderThreshold,
  ReorderThresholdRequest
} from '../../api/types'

export const inventoryApi = {
  getBalances: (productId?: string) =>
    apiClient
      .get<InventoryBalance[]>('/api/v1/inventory/balances', { params: { productId } })
      .then((r) => r.data),

  recordAdjustment: (body: StockAdjustmentRequest) =>
    apiClient.post('/api/v1/inventory/adjustments', body),

  getLedger: (productId?: string, locationId?: string, page = 0, size = 20) =>
    apiClient
      .get<{ content: StockLedgerEntry[]; totalElements: number }>('/api/v1/inventory/ledger', {
        params: { productId, locationId, page, size }
      })
      .then((r) => r.data)
}

export const reorderThresholdsApi = {
  list: () => apiClient.get<ReorderThreshold[]>('/api/v1/reorder-thresholds').then((r) => r.data),
  upsert: (body: ReorderThresholdRequest) =>
    apiClient.put<ReorderThreshold>('/api/v1/reorder-thresholds', body).then((r) => r.data)
}
