import { Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import Home from './pages/Home'
import ProductDetail from './pages/ProductDetail'
import Login from './pages/Login'
import Register from './pages/Register'
import VerifyOtp from './pages/VerifyOtp'
import AdminLogin from './pages/AdminLogin'
import AdminHome from './pages/AdminHome'
import AdminDashboard from './pages/AdminDashboard'
import AdminConfiguration from './pages/AdminConfiguration'
import AdminNotificationSettings from './pages/AdminNotificationSettings'
import AdminTaxSettings from './pages/AdminTaxSettings'
import AdminProductList from './pages/AdminProductList'
import AdminProductForm from './pages/AdminProductForm'
import AdminInventoryDashboard from './pages/AdminInventoryDashboard'
import AdminInventoryStock from './pages/AdminInventoryStock'
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
import AdminOrderDashboard from './pages/AdminOrderDashboard'
import AdminOrderDetail from './pages/AdminOrderDetail'
import AdminCustomerList from './pages/AdminCustomerList'
import AdminCustomerDetail from './pages/AdminCustomerDetail'
import WishlistPage from './pages/WishlistPage'
import CartPage from './pages/CartPage'
import CheckoutPage from './pages/CheckoutPage'
import PaymentPage from './pages/PaymentPage'
import OrderHistoryPage from './pages/OrderHistoryPage'
import OrderDetailPage from './pages/OrderDetailPage'
import Profile from './pages/Profile'
import NotFound from './pages/NotFound'
import RequireAuth from './components/RequireAuth'
import ScrollToTop from './components/ScrollToTop'

export default function App() {
  return (
    <>
      <ScrollToTop />
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
        path="/admin/dashboard"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminDashboard />
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
        path="/admin/notifications"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminNotificationSettings />
          </RequireAuth>
        }
      />
      <Route
        path="/admin/tax"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminTaxSettings />
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
        path="/admin/inventory/stock"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminInventoryStock />
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
      <Route
        path="/admin/orders"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminOrderDashboard />
          </RequireAuth>
        }
      />
      <Route
        path="/admin/orders/:id"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminOrderDetail />
          </RequireAuth>
        }
      />
      <Route
        path="/admin/customers"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminCustomerList />
          </RequireAuth>
        }
      />
      <Route
        path="/admin/customers/:id"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminCustomerDetail />
          </RequireAuth>
        }
      />

      {/* Customer-facing storefront, sharing the Navbar/Footer layout. */}
      <Route element={<Layout />}>
        <Route path="/" element={<Home />} />
        <Route path="/product/:slug" element={<ProductDetail />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/verify" element={<VerifyOtp />} />
        <Route
          path="/wishlist"
          element={
            <RequireAuth>
              <WishlistPage />
            </RequireAuth>
          }
        />
        <Route
          path="/cart"
          element={
            <RequireAuth>
              <CartPage />
            </RequireAuth>
          }
        />
        <Route
          path="/checkout"
          element={
            <RequireAuth>
              <CheckoutPage />
            </RequireAuth>
          }
        />
        <Route
          path="/checkout/payment/:orderId"
          element={
            <RequireAuth>
              <PaymentPage />
            </RequireAuth>
          }
        />
        <Route
          path="/orders"
          element={
            <RequireAuth>
              <OrderHistoryPage />
            </RequireAuth>
          }
        />
        <Route
          path="/orders/:id"
          element={
            <RequireAuth>
              <OrderDetailPage />
            </RequireAuth>
          }
        />
        <Route
          path="/profile"
          element={
            <RequireAuth>
              <Profile />
            </RequireAuth>
          }
        />
        <Route path="*" element={<NotFound />} />
      </Route>
      </Routes>
    </>
  )
}
