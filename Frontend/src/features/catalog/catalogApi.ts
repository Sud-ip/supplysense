import apiClient from '../../api/client'
import type { Product, ProductRequest, Supplier, SupplierRequest, Location, LocationRequest } from '../../api/types'

export const productsApi = {
  list: () => apiClient.get<Product[]>('/api/v1/products').then((r) => r.data),
  create: (body: ProductRequest) => apiClient.post<Product>('/api/v1/products', body).then((r) => r.data),
  update: (id: string, body: ProductRequest) =>
    apiClient.put<Product>(`/api/v1/products/${id}`, body).then((r) => r.data),
  deactivate: (id: string) => apiClient.delete(`/api/v1/products/${id}`)
}

export const suppliersApi = {
  list: () => apiClient.get<Supplier[]>('/api/v1/suppliers').then((r) => r.data),
  create: (body: SupplierRequest) => apiClient.post<Supplier>('/api/v1/suppliers', body).then((r) => r.data),
  update: (id: string, body: SupplierRequest) =>
    apiClient.put<Supplier>(`/api/v1/suppliers/${id}`, body).then((r) => r.data),
  deactivate: (id: string) => apiClient.delete(`/api/v1/suppliers/${id}`)
}

export const locationsApi = {
  list: () => apiClient.get<Location[]>('/api/v1/locations').then((r) => r.data),
  create: (body: LocationRequest) => apiClient.post<Location>('/api/v1/locations', body).then((r) => r.data),
  update: (id: string, body: LocationRequest) =>
    apiClient.put<Location>(`/api/v1/locations/${id}`, body).then((r) => r.data),
  deactivate: (id: string) => apiClient.delete(`/api/v1/locations/${id}`)
}
