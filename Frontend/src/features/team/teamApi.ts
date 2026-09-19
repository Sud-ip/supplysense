import apiClient from '../../api/client'
import type { UserAccount, CreateUserRequest } from '../../api/types'

export const teamApi = {
  list: () => apiClient.get<UserAccount[]>('/api/v1/users').then((r) => r.data),
  create: (body: CreateUserRequest) => apiClient.post<UserAccount>('/api/v1/users', body).then((r) => r.data),
  deactivate: (id: string) => apiClient.delete(`/api/v1/users/${id}`)
}
