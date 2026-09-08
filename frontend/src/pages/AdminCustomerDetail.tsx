import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { fetchAdminCustomer, fetchAdminCustomerOrders, updateAdminCustomerStatus } from "../api/adminCustomers";
import { orderStatusBadgeClasses, orderStatusLabel } from "./AdminOrderDashboard";
import { getErrorMessage } from "../api/client";
import { formatPrice } from "../lib/formatPrice";
import ConfirmDialog from "../components/ConfirmDialog";
import type { AdminCustomerDetail as AdminCustomerDetailType, AdminOrderSummary } from "../types";

const ORDERS_PAGE_SIZE = 10;

function AdminHomeBackLink({ className = "" }: { className?: string }) {
  return (
    <Link
      to="/admin/customers"
      aria-label="Back to customers"
      className={`inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-zinc-700 transition-colors hover:bg-zinc-100 active:bg-zinc-200 ${className}`}
    >
      <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
      </svg>
    </Link>
  );
}

function formatDateTime(value: string) {
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleString("en-IN", { dateStyle: "medium", timeStyle: "short" });
}

function formatDate(value: string | null) {
  if (!value) return "—";
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleDateString("en-IN", { dateStyle: "medium" });
}

export default function AdminCustomerDetail() {
  const { isAdmin } = useAuth();

  if (!isAdmin) {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-zinc-950 px-4">
        <div className="w-full max-w-md rounded-xl border border-zinc-800 bg-zinc-900 p-8 text-center shadow-xl">
          <h1 className="text-xl font-semibold text-white">Admin access required</h1>
          <p className="mt-2 text-sm text-zinc-400">You need the ADMIN role to view customers.</p>
          <Link
            to="/admin"
            className="mt-6 inline-block rounded-lg border border-zinc-700 px-4 py-2 text-sm font-medium text-zinc-200 hover:bg-zinc-800"
          >
            Back to admin home
          </Link>
        </div>
      </div>
    );
  }

  return <AdminCustomerDetailContent />;
}

function AdminCustomerDetailContent() {
  const { id } = useParams<{ id: string }>();

  const [customer, setCustomer] = useState<AdminCustomerDetailType | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [orders, setOrders] = useState<AdminOrderSummary[]>([]);
  const [ordersPage, setOrdersPage] = useState(0);
  const [ordersTotalPages, setOrdersTotalPages] = useState(0);
  const [ordersLoading, setOrdersLoading] = useState(true);

  const [statusDialogOpen, setStatusDialogOpen] = useState(false);
  const [statusSaving, setStatusSaving] = useState(false);
  const [statusError, setStatusError] = useState<string | null>(null);

  const loadCustomer = useCallback(() => {
    if (!id) return;
    setLoading(true);
    setLoadError(null);
    fetchAdminCustomer(id)
      .then(setCustomer)
      .catch((err) => setLoadError(getErrorMessage(err)))
      .finally(() => setLoading(false));
  }, [id]);

  useEffect(() => {
    loadCustomer();
  }, [loadCustomer]);

  useEffect(() => {
    if (!id) return;
    setOrdersLoading(true);
    fetchAdminCustomerOrders(id, ordersPage, ORDERS_PAGE_SIZE)
      .then((res) => {
        setOrders(res.content);
        setOrdersTotalPages(res.totalPages);
      })
      .catch(() => {
        setOrders([]);
        setOrdersTotalPages(0);
      })
      .finally(() => setOrdersLoading(false));
  }, [id, ordersPage]);

  const handleToggleStatus = async () => {
    if (!id || !customer) return;
    setStatusSaving(true);
    setStatusError(null);
    try {
      const updated = await updateAdminCustomerStatus(id, !customer.enabled);
      setCustomer(updated);
      setStatusDialogOpen(false);
    } catch (err) {
      setStatusError(getErrorMessage(err));
    } finally {
      setStatusSaving(false);
    }
  };

  if (loading) {
    return <div className="flex min-h-[40vh] items-center justify-center text-sm text-zinc-500">Loading customer…</div>;
  }

  if (loadError || !customer) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6 lg:px-8">
        <AdminHomeBackLink />
        <div className="mt-4 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
          {loadError ?? "Customer not found."}
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-2">
          <AdminHomeBackLink className="mt-0.5" />
          <div>
            <h1 className="text-2xl font-semibold text-zinc-900">{customer.fullName}</h1>
            <p className="mt-1 text-sm text-zinc-500">Customer since {formatDate(customer.createdAt)}</p>
          </div>
        </div>
        <button
          type="button"
          onClick={() => setStatusDialogOpen(true)}
          className={`shrink-0 rounded-full border px-4 py-2 text-sm font-medium transition-colors ${
            customer.enabled
              ? "border-rose-200 text-rose-700 hover:bg-rose-50"
              : "border-emerald-200 text-emerald-700 hover:bg-emerald-50"
          }`}
        >
          {customer.enabled ? "Disable account" : "Enable account"}
        </button>
      </div>

      {/* Contact info + account status */}
      <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <InfoCard label="Email" value={customer.email} />
        <InfoCard label="Phone" value={customer.mobileNumber ?? "—"} />
        <InfoCard
          label="Account status"
          value={customer.enabled ? "Active" : "Disabled"}
          tone={customer.enabled ? "emerald" : "rose"}
        />
        <InfoCard label="Date of birth" value={formatDate(customer.dateOfBirth)} />
      </div>

      {/* Lifetime stats */}
      <div className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-2">
        <InfoCard label="Total orders" value={String(customer.orderCount)} />
        <InfoCard label="Total amount spent" value={formatPrice(customer.totalSpent)} />
      </div>

      {/* Order history */}
      <div className="mt-8">
        <h2 className="text-sm font-semibold text-zinc-900">Order history</h2>
        <div className="mt-2 overflow-x-auto rounded-lg border border-zinc-200">
          <table className="w-full min-w-[560px] text-left text-sm">
            <thead className="bg-zinc-50 text-xs uppercase tracking-wide text-zinc-500">
              <tr>
                <th className="px-3 py-2 font-medium">Order</th>
                <th className="px-3 py-2 font-medium">Placed</th>
                <th className="px-3 py-2 text-right font-medium">Items</th>
                <th className="px-3 py-2 text-right font-medium">Total</th>
                <th className="px-3 py-2 font-medium">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-100">
              {ordersLoading ? (
                <tr>
                  <td colSpan={5} className="px-3 py-8 text-center text-sm text-zinc-400">
                    Loading…
                  </td>
                </tr>
              ) : orders.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-3 py-8 text-center text-sm text-zinc-400">
                    No orders yet.
                  </td>
                </tr>
              ) : (
                orders.map((order) => (
                  <tr key={order.id} className="hover:bg-zinc-50">
                    <td className="whitespace-nowrap px-3 py-2">
                      <Link to={`/admin/orders/${order.id}`} className="font-medium text-zinc-900 hover:underline">
                        {order.orderNumber}
                      </Link>
                    </td>
                    <td className="whitespace-nowrap px-3 py-2 text-zinc-500">{formatDateTime(order.createdAt)}</td>
                    <td className="whitespace-nowrap px-3 py-2 text-right text-zinc-700">{order.itemCount}</td>
                    <td className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-900">
                      {formatPrice(order.totalAmount)}
                    </td>
                    <td className="whitespace-nowrap px-3 py-2">
                      <span
                        className={`rounded-full border px-1.5 py-0.5 text-[10px] font-medium ${orderStatusBadgeClasses(order.status)}`}
                      >
                        {orderStatusLabel(order.status)}
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {ordersTotalPages > 1 && (
          <div className="mt-3 flex items-center justify-end gap-1">
            <button
              onClick={() => setOrdersPage((p) => Math.max(0, p - 1))}
              disabled={ordersPage === 0}
              className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm font-medium text-zinc-700 disabled:opacity-40"
            >
              Prev
            </button>
            <span className="px-2 text-sm text-zinc-500">
              Page {ordersPage + 1} of {ordersTotalPages}
            </span>
            <button
              onClick={() => setOrdersPage((p) => Math.min(ordersTotalPages - 1, p + 1))}
              disabled={ordersPage >= ordersTotalPages - 1}
              className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm font-medium text-zinc-700 disabled:opacity-40"
            >
              Next
            </button>
          </div>
        )}
      </div>

      <div className="mt-10 flex justify-center border-t border-zinc-100 pt-6">
        <AdminHomeBackLink />
      </div>

      <ConfirmDialog
        open={statusDialogOpen}
        title={customer.enabled ? "Disable this account?" : "Enable this account?"}
        message={
          customer.enabled
            ? `${customer.fullName} will immediately be signed out and unable to log in until re-enabled.`
            : `${customer.fullName} will be able to log in again.`
        }
        confirmLabel={customer.enabled ? "Disable account" : "Enable account"}
        danger={customer.enabled}
        confirming={statusSaving}
        error={statusError}
        onConfirm={handleToggleStatus}
        onCancel={() => {
          setStatusDialogOpen(false);
          setStatusError(null);
        }}
      />
    </div>
  );
}

function InfoCard({
  label,
  value,
  tone = "default",
}: {
  label: string;
  value: string;
  tone?: "default" | "emerald" | "rose";
}) {
  const valueClasses = tone === "emerald" ? "text-emerald-600" : tone === "rose" ? "text-rose-600" : "text-zinc-900";
  return (
    <div className="rounded-lg border border-zinc-200 bg-white px-3 py-2.5">
      <p className="text-[11px] font-medium uppercase tracking-wide text-zinc-500">{label}</p>
      <p className={`mt-1 truncate text-lg font-semibold ${valueClasses}`}>{value}</p>
    </div>
  );
}
