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
