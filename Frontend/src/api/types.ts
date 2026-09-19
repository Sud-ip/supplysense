// Mirrors the backend's response/request DTOs.

export type Role = 'OWNER' | 'MANAGER' | 'STAFF'

export interface Product {
  id: string
  sku: string
  name: string
  supplierId: string | null
  supplierName: string | null
  unitCost: string | null
  unitPrice: string | null
  active: boolean
  createdAt: string
}

export interface ProductRequest {
  sku: string
  name: string
  supplierId: string | null
  unitCost: number | null
  unitPrice: number | null
}

export interface Supplier {
  id: string
  name: string
  contactInfo: string | null
  active: boolean
  createdAt: string
}

export interface SupplierRequest {
  name: string
  contactInfo: string | null
}

export interface Location {
  id: string
  name: string
  address: string | null
  active: boolean
  createdAt: string
}

export interface LocationRequest {
  name: string
  address: string | null
}

export interface InventoryBalance {
  productId: string
  productSku: string
  productName: string
  locationId: string
  locationName: string
  quantityOnHand: number
  updatedAt: string
}

export interface StockLedgerEntry {
  id: string
  productId: string
  productSku: string
  locationId: string
  locationName: string
  quantityDelta: number
  reason: 'PURCHASE' | 'SALE' | 'ADJUSTMENT' | 'RETURN' | 'TRANSFER'
  referenceType: string | null
  referenceId: string | null
  createdByUserId: string
  createdByEmail: string
  createdAt: string
}

export interface StockAdjustmentRequest {
  productId: string
  locationId: string
  quantityDelta: number
  reason: 'ADJUSTMENT' | 'RETURN' | 'TRANSFER'
  note: string | null
}

export interface ReorderThreshold {
  id: string
  productId: string
  productSku: string
  locationId: string
  locationName: string
  minQuantity: number
  reorderQuantity: number
  updatedAt: string
}

export interface ReorderThresholdRequest {
  productId: string
  locationId: string
  minQuantity: number
  reorderQuantity: number
}

export interface Sale {
  id: string
  productId: string
  productSku: string
  locationId: string
  locationName: string
  quantity: number
  unitPrice: string
  soldAt: string
  source: 'MANUAL' | 'CSV_IMPORT'
  importBatchId: string | null
  createdAt: string
}

export interface SaleRequest {
  productId: string
  locationId: string
  quantity: number
  unitPrice: number | null
  soldAt: string | null
}

export interface RowError {
  rowNumber: number
  message: string
}

export interface CsvImportBatch {
  id: string
  filename: string
  status: 'PROCESSING' | 'COMPLETED' | 'COMPLETED_WITH_ERRORS' | 'FAILED'
  rowCount: number
  successCount: number
  errorCount: number
  errors: RowError[]
  createdAt: string
}

export interface DashboardSummary {
  totalActiveSkus: number
  lowStockCount: number
  totalInventoryValue: string
  generatedAt: string
}

export interface StockTrendPoint {
  date: string
  unitsIn: number
  unitsOut: number
  netChange: number
}

export interface TopMover {
  productId: string
  productSku: string
  productName: string
  totalUnitsSold: number
}

export interface LowStockAlert {
  productId: string
  productSku: string
  productName: string
  locationId: string
  locationName: string
  quantityOnHand: number
  minQuantity: number
  reorderQuantity: number
  predictedDailyDemand: string
  projectedStockoutDate: string | null
  reasons: ('BELOW_MIN_THRESHOLD' | 'PROJECTED_STOCKOUT_SOON')[]
}

// Added for the team management screen - mirrors the backend's
// UserResponse / CreateUserRequest (see UserController).
export interface UserAccount {
  id: string
  fullName: string
  email: string
  role: Role
  active: boolean
  createdAt: string
}

export interface CreateUserRequest {
  fullName: string
  email: string
  password: string
  role: 'MANAGER' | 'STAFF' // OWNER is deliberately not selectable - see backend UserService
}

export interface ApiErrorResponse {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
}
