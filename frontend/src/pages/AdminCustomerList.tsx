import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { fetchAdminCustomers } from "../api/adminCustomers";
import type { AdminCustomerQuery } from "../api/adminCustomers";
import { getErrorMessage } from "../api/client";
import { formatPrice } from "../lib/formatPrice";
import { useDebouncedValue } from "../hooks/useDebouncedValue";
import type { AdminCustomerRow } from "../types";

const PAGE_SIZE = 20;

type StatusFilter = "" | "true" | "false";

/** Icon-only "back to admin home" affordance, matching the other admin screens' back links. */
function AdminHomeBackLink({ className = "" }: { className?: string }) {
  return (
    <Link
      to="/admin"
      aria-label="Back to admin home"
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

export default function AdminCustomerList() {
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

  return <AdminCustomerListContent />;
}

function AdminCustomerListContent() {
  const [page, setPage] = useState(0);
  const [searchInput, setSearchInput] = useState("");
  const debouncedSearch = useDebouncedValue(searchInput, 400);
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("");

  const [rows, setRows] = useState<AdminCustomerRow[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setPage(0);
  }, [debouncedSearch, statusFilter]);

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    const query: AdminCustomerQuery = {
      page,
      size: PAGE_SIZE,
      q: debouncedSearch,
      enabled: statusFilter === "" ? undefined : statusFilter === "true",
    };
    fetchAdminCustomers(query)
      .then((res) => {
        setRows(res.content);
        setTotalPages(res.totalPages);
        setTotalElements(res.totalElements);
      })
      .catch((err) => {
        setError(getErrorMessage(err));
        setRows([]);
        setTotalPages(0);
        setTotalElements(0);
      })
      .finally(() => setLoading(false));
  }, [page, debouncedSearch, statusFilter]);

  useEffect(() => {
    load();
  }, [load]);

  const pageNumbers = useMemo(() => {
    if (totalPages <= 1) return [];
    const windowSize = 5;
    let start = Math.max(0, page - Math.floor(windowSize / 2));
    const end = Math.min(totalPages, start + windowSize);
    start = Math.max(0, end - windowSize);
    return Array.from({ length: end - start }, (_, i) => start + i);
  }, [page, totalPages]);

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="flex items-start gap-2">
        <AdminHomeBackLink className="mt-0.5" />
        <div>
          <h1 className="text-2xl font-semibold text-zinc-900">Customers</h1>
          <p className="mt-1 text-sm text-zinc-500">Search customers, review order history, and manage account status.</p>
        </div>
      </div>

      <div className="mt-6 flex flex-wrap items-center gap-3">
        <input
          type="search"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          placeholder="Search by name, email, or phone…"
          className="w-full max-w-sm rounded-md border border-zinc-300 px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
        />
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as StatusFilter)}
          className="rounded-md border border-zinc-300 bg-white px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
        >
          <option value="">All statuses</option>
          <option value="true">Active</option>
          <option value="false">Disabled</option>
        </select>
      </div>

      {error && (
        <div className="mt-4 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</div>
      )}

      <div className="mt-4 overflow-x-auto rounded-lg border border-zinc-200">
        <table className="w-full min-w-[720px] text-left text-sm">
          <thead className="bg-zinc-50 text-xs uppercase tracking-wide text-zinc-500">
            <tr>
              <th className="px-3 py-2 font-medium">Customer</th>
              <th className="px-3 py-2 font-medium">Contact</th>
              <th className="px-3 py-2 text-right font-medium">Orders</th>
              <th className="px-3 py-2 text-right font-medium">Total spent</th>
              <th className="px-3 py-2 font-medium">Status</th>
              <th className="px-3 py-2 font-medium">Joined</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-100">
            {loading ? (
              <tr>
                <td colSpan={6} className="px-3 py-8 text-center text-sm text-zinc-400">
                  Loading…
                </td>
              </tr>
            ) : rows.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-3 py-8 text-center text-sm text-zinc-400">
                  No customers match these filters.
                </td>
              </tr>
            ) : (
              rows.map((row) => (
                <tr key={row.id} className="hover:bg-zinc-50">
                  <td className="whitespace-nowrap px-3 py-2">
                    <Link to={`/admin/customers/${row.id}`} className="font-medium text-zinc-900 hover:underline">
                      {row.fullName}
                    </Link>
                  </td>
                  <td className="whitespace-nowrap px-3 py-2">
                    <div className="text-zinc-800">{row.email}</div>
                    <div className="text-xs text-zinc-400">{row.mobileNumber ?? "—"}</div>
                  </td>
                  <td className="whitespace-nowrap px-3 py-2 text-right text-zinc-700">{row.orderCount}</td>
                  <td className="whitespace-nowrap px-3 py-2 text-right font-medium text-zinc-900">
                    {formatPrice(row.totalSpent)}
                  </td>
                  <td className="whitespace-nowrap px-3 py-2">
                    <span
                      className={`rounded-full border px-1.5 py-0.5 text-[10px] font-medium ${
                        row.enabled
                          ? "border-emerald-200 bg-emerald-50 text-emerald-700"
                          : "border-rose-200 bg-rose-50 text-rose-700"
                      }`}
                    >
                      {row.enabled ? "ACTIVE" : "DISABLED"}
                    </span>
                  </td>
                  <td className="whitespace-nowrap px-3 py-2 text-zinc-500">{formatDateTime(row.createdAt)}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="mt-3 flex items-center justify-between">
        <p className="text-xs text-zinc-500">
          {loading ? "Loading…" : `${totalElements} customer${totalElements === 1 ? "" : "s"}`}
        </p>
        {totalPages > 1 && (
          <div className="flex items-center gap-1">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm font-medium text-zinc-700 disabled:opacity-40"
            >
              Prev
            </button>
            {pageNumbers.map((p) => (
              <button
                key={p}
                onClick={() => setPage(p)}
                className={`rounded-md px-3 py-1.5 text-sm font-medium ${
                  p === page ? "bg-zinc-900 text-white" : "border border-zinc-300 text-zinc-700 hover:bg-zinc-50"
                }`}
              >
                {p + 1}
              </button>
            ))}
            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
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
    </div>
  );
}
