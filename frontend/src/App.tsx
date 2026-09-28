import { Route, Routes } from 'react-router-dom'
import Layout from './customer/shell/Layout'
import Home from './customer/home/Home'
import ProductDetail from './customer/product/ProductDetail'
import Login from './customer/auth/Login'
import Register from './customer/auth/Register'
import VerifyOtp from './customer/auth/VerifyOtp'
import AdminLogin from './admin/auth/Login'
import AdminHome from './admin/Home'
import AdminDashboard from './admin/dashboard/Dashboard'
import AdminConfiguration from './admin/configuration/Configuration'
import AdminNotificationSettings from './admin/notifications/NotificationSettings'
import AdminStaff from './admin/staff/Staff'
import AdminActivityLog from './admin/activity-log/ActivityLog'
import AdminProfile from './admin/profile/Profile'
import AdminTaxSettings from './admin/tax/TaxSettings'
import AdminProductList from './admin/products/ProductList'
import AdminProductForm from './admin/products/ProductForm'
import AdminInventoryDashboard from './admin/inventory/Dashboard'
import AdminInventoryStock from './admin/inventory/Stock'
import AdminInventoryHistory from './admin/inventory/History'
import AdminMastersHub from './admin/masters/Hub'
import AdminCategories from './admin/masters/Categories'
import AdminSubCategories from './admin/masters/SubCategories'
import AdminColors from './admin/masters/Colors'
import AdminSizes from './admin/masters/Sizes'
import AdminSizeGroups from './admin/masters/SizeGroups'
import AdminSizeCharts from './admin/masters/SizeCharts'
import AdminBrands from './admin/masters/Brands'
import AdminMaterials from './admin/masters/Materials'
import AdminVendors from './admin/masters/Vendors'
import AdminDamageReasons from './admin/masters/DamageReasons'
import AdminOrderDashboard from './admin/orders/Dashboard'
import AdminReports from './admin/reports/Reports'
import AdminOrderDetail from './admin/orders/Detail'
import AdminReturnList from './admin/returns/List'
import AdminReturnDetail from './admin/returns/Detail'
import AdminCustomerList from './admin/customers/List'
import AdminCustomerDetail from './admin/customers/Detail'
import WishlistPage from './customer/wishlist/WishlistPage'
import CartPage from './customer/cart/CartPage'
import CheckoutPage from './pages/CheckoutPage'
import PaymentPage from './pages/PaymentPage'
import OrderHistoryPage from './customer/orders/OrderHistoryPage'
import OrderDetailPage from './customer/orders/OrderDetailPage'
import ReturnRequestPage from './customer/orders/ReturnRequestPage'
import Profile from './customer/account/Profile'
import NotFound from './pages/NotFound'
import AdminSubscriptionBilling from './admin/subscription/SubscriptionBilling'
import RequireAuth from '@/shared/components/RequireAuth'
import ScrollToTop from '@/shared/components/ScrollToTop'
import IdleSessionWatcher from '@/shared/components/IdleSessionWatcher'
import LockoutGate from '@/shared/components/LockoutGate'

export default function App() {
  return (
    <>
      <ScrollToTop />
      <IdleSessionWatcher />
      <LockoutGate>
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
        path="/admin/staff"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminStaff />
          </RequireAuth>
        }
      />
      <Route
        path="/admin/activity-log"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminActivityLog />
          </RequireAuth>
        }
      />
      <Route
        path="/admin/subscription"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminSubscriptionBilling />
          </RequireAuth>
        }
      />
      <Route
        path="/admin/profile"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminProfile />
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
        path="/admin/size-charts"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminSizeCharts />
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
        path="/admin/reports"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminReports />
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
        path="/admin/returns"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminReturnList />
          </RequireAuth>
        }
      />
      <Route
        path="/admin/returns/:id"
        element={
          <RequireAuth redirectTo="/admin/login" requireAdmin>
            <AdminReturnDetail />
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
        <Route
          path="/orders/:orderId/items/:itemId/return"
          element={
            <RequireAuth>
              <ReturnRequestPage />
            </RequireAuth>
          }
        />
        <Route path="*" element={<NotFound />} />
      </Route>
      </Routes>
      </LockoutGate>
    </>
  )
}
