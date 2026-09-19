import { Routes, Route } from 'react-router-dom'
import LoginPage from './features/auth/LoginPage'
import RegisterPage from './features/auth/RegisterPage'
import ProtectedRoute from './routes/ProtectedRoute'
import Layout from './components/Layout'

import DashboardPage from './features/dashboard/DashboardPage'
import ProductsPage from './features/catalog/ProductsPage'
import SuppliersPage from './features/catalog/SuppliersPage'
import LocationsPage from './features/catalog/LocationsPage'
import InventoryBalancesPage from './features/inventory/InventoryBalancesPage'
import LedgerPage from './features/inventory/LedgerPage'
import ReorderThresholdsPage from './features/inventory/ReorderThresholdsPage'
import RecordSalePage from './features/sales/RecordSalePage'
import ImportSalesPage from './features/sales/ImportSalesPage'
import TeamPage from './features/team/TeamPage'
import ProfilePage from './features/profile/ProfilePage'

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/products" element={<ProductsPage />} />
          <Route path="/suppliers" element={<SuppliersPage />} />
          <Route path="/locations" element={<LocationsPage />} />
          <Route path="/inventory" element={<InventoryBalancesPage />} />
          <Route path="/inventory/ledger" element={<LedgerPage />} />
          <Route path="/inventory/thresholds" element={<ReorderThresholdsPage />} />
          <Route path="/sales/new" element={<RecordSalePage />} />
          <Route path="/sales/import" element={<ImportSalesPage />} />
          <Route path="/team" element={<TeamPage />} />
          <Route path="/profile" element={<ProfilePage />} />
        </Route>
      </Route>
    </Routes>
  )
}

export default App
