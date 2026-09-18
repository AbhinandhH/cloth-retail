import { useEffect, useRef, useState } from "react";
import { fetchAdminCustomers } from "../api/adminCustomers";
import { useDebouncedValue } from "../hooks/useDebouncedValue";
import type { AdminCustomerRow } from "../types";

/** Search-as-you-type customer picker for report filters - same shape/reasoning as ProductCombobox. */
export default function CustomerCombobox({
  value,
  label,
  onChange,
  error,
}: {
  value: number | string | null;
  label: string | null;
  onChange: (id: number | string | null, label: string | null) => void;
  error?: string;
}) {
  const [query, setQuery] = useState(label ?? "");
  const [open, setOpen] = useState(false);
  const [suggestions, setSuggestions] = useState<AdminCustomerRow[]>([]);
  const [loading, setLoading] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const debouncedQuery = useDebouncedValue(query, 300);

  useEffect(() => {
    setQuery(label ?? "");
  }, [value, label]);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  useEffect(() => {
    if (!open || debouncedQuery.trim() === "") {
      setSuggestions([]);
      return;
    }
    let cancelled = false;
    setLoading(true);
    fetchAdminCustomers({ q: debouncedQuery.trim(), size: 8 })
      .then((res) => {
        if (!cancelled) setSuggestions(res.content);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [debouncedQuery, open]);

  const handleSelect = (customer: AdminCustomerRow) => {
    onChange(customer.id, customer.fullName);
    setQuery(customer.fullName);
    setOpen(false);
  };

  const handleInputChange = (text: string) => {
    setQuery(text);
    setOpen(true);
    if (text !== label) {
      onChange(null, null);
    }
  };

  return (
    <div>
      <label className="block text-sm font-medium text-zinc-700">Customer</label>
      <div className="relative mt-1" ref={containerRef}>
        <input
          type="text"
          value={query}
          onChange={(e) => handleInputChange(e.target.value)}
          onFocus={() => setOpen(true)}
          placeholder="All customers"
          autoComplete="off"
          className="w-full rounded-md border border-zinc-300 bg-white px-2 py-1.5 text-sm focus:border-zinc-500 focus:outline-none"
        />
        {open && query.trim() !== "" && (
          <ul className="absolute z-10 mt-1 max-h-56 w-full overflow-y-auto rounded-md border border-zinc-200 bg-white py-1 shadow-lg">
            {loading && <li className="px-3 py-2 text-sm text-zinc-500">Searching…</li>}
            {!loading && suggestions.length === 0 && (
              <li className="px-3 py-2 text-sm text-zinc-500">No customers match &ldquo;{query.trim()}&rdquo;.</li>
            )}
            {!loading &&
              suggestions.map((c) => (
                <li key={c.id}>
                  <button
                    type="button"
                    onClick={() => handleSelect(c)}
                    className="block w-full px-3 py-2 text-left text-sm hover:bg-zinc-50"
                  >
                    {c.fullName}
                    <span className="ml-1 text-xs text-zinc-400">{c.email}</span>
                  </button>
                </li>
              ))}
          </ul>
        )}
      </div>
      {error && <p className="mt-1 text-xs text-rose-600">{error}</p>}
    </div>
  );
}
