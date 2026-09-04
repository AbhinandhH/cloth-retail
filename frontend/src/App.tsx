import { Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import Home from './pages/Home'
import ProductDetail from './pages/ProductDetail'
import Login from './pages/Login'
import Register from './pages/Register'
import AdminLogin from './pages/AdminLogin'
import AdminHome from './pages/AdminHome'
import AdminConfiguration from './pages/AdminConfiguration'
import AdminProductList from './pages/AdminProductList'
import AdminProductForm from './pages/AdminProductForm'
import AdminInventoryDashboard from './pages/AdminInventoryDashboard'
import AdminInventoryHistory from './pages/AdminInventoryHistory'
import AdminMastersHub from './pages/AdminMastersHub'
import AdminCategories from './pages/AdminCategories'
import AdminSubCategories from './pages/AdminSubCategories'
import AdminColors from './pages/AdminColors'
import AdminSizes from './pages/AdminSizes'
import AdminSizeGroups from './pages/AdminSizeGroups'
import AdminBrands from './pages/AdminBrands'
import AdminMaterials from './pages/AdminMaterials'
import AdminVendors from './pages/AdminVendors'
import AdminDamageReasons from './pages/AdminDamageReasons'
import NotFound from './pages/NotFound'
import RequireAuth from './components/RequireAuth'

export default function App() {
  return (
    <Routes>
      {/* Admin area — conceptually separate from the customer app. */}
      <Route path="/admin/login" element={<AdminLogin />} />
      <Route
        path="/admin"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminHome />
          </RequireAuth>
        }
      />
      <Route
        path="/admin/configuration"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminConfiguration />
          </RequireAuth>
        }
      />
      <Route
        path="/admin/products"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminProductList />
          </RequireAuth>
        }
      />
      <Route
        path="/admin/products/new"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminProductForm />
          </RequireAuth>
        }
      />
      <Route
        path="/admin/products/:id"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminProductForm />
          </RequireAuth>
        }
      />
      <Route
        path="/admin/inventory"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminInventoryDashboard />
          </RequireAuth>
        }
      />
      <Route
        path="/admin/inventory/history"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminInventoryHistory />
          </RequireAuth>
        }
      />
      <Route
        path="/admin/masters"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminMastersHub />
          </RequireAuth>
        }
      />
      <Route
        path="/admin/categories"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminCategories />
          </RequireAuth>
        }
      />
      <Route
        path="/admin/sub-categories"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminSubCategories />
          </RequireAuth>
        }
      />
      <Route
        path="/admin/colors"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminColors />
          </RequireAuth>
        }
      />
      <Route
        path="/admin/sizes"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminSizes />
          </RequireAuth>
        }
      />
      <Route
        path="/admin/size-groups"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminSizeGroups />
          </RequireAuth>
        }
      />
      <Route
        path="/admin/brands"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminBrands />
          </RequireAuth>
        }
      />
      <Route
        path="/admin/materials"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminMaterials />
          </RequireAuth>
        }
      />
      <Route
        path="/admin/vendors"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminVendors />
          </RequireAuth>
        }
      />
      <Route
        path="/admin/damage-reasons"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminDamageReasons />
          </RequireAuth>
        }
      />

      {/* Customer-facing storefront, sharing the Navbar/Footer layout. */}
      <Route element={<Layout />}>
        <Route path="/" element={<Home />} />
        <Route path="/product/:slug" element={<ProductDetail />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  )
}
