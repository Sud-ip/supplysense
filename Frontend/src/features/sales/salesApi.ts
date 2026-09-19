import apiClient from '../../api/client'
import type { Sale, SaleRequest, CsvImportBatch } from '../../api/types'

export const salesApi = {
  recordSale: (body: SaleRequest) => apiClient.post<Sale>('/api/v1/sales', body).then((r) => r.data),

  importCsv: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return apiClient
      .post<CsvImportBatch>('/api/v1/sales/import', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      .then((r) => r.data)
  },

  getImportStatus: (batchId: string) =>
    apiClient.get<CsvImportBatch>(`/api/v1/sales/import/${batchId}`).then((r) => r.data)
}
