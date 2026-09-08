import { useEffect, useMemo, useRef, useState } from "react";
import type { FormEvent } from "react";
import { createPortal } from "react-dom";
import { createVendor } from "../api/adminMasters";
import { getErrorMessage } from "../api/client";
import type { AdminVendor } from "../types";

// Same phone pattern as the backend's VendorAdminRequest - kept in sync by hand since there's
// no shared validation layer between the two, so a typo here just means a false-negative UX
// check followed by the server's own message, never a false positive that blocks a valid vendor.
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_RE = /^\+?[0-9][0-9\s-]{6,19}$/;

/**
 * Mandatory vendor picker for the admin Add Product form: a search-as-you-type suggestion box
 * (not a plain <select>, since vendor lists can get long) plus a "+" that opens an inline
 * add-vendor form without leaving the product screen. The duplicate-name check there is purely
 * client-side against the vendor list this component already has loaded - no extra request - so
 * it's instant; format (email/phone) is also checked client-side before submitting, with the
 * server's own uniqueness/format checks (VendorService) as the authoritative backstop should the
 * two ever disagree (e.g. a vendor added concurrently by someone else).
 */
export default function VendorCombobox({
  vendors,
  value,
  onChange,
  onVendorCreated,
  error,
}: {
  vendors: AdminVendor[];
  value: string;
  onChange: (vendorId: string) => void;
  onVendorCreated: (vendor: AdminVendor) => void;
  error?: string;
}) {
  const selected = vendors.find((v) => String(v.id) === value) ?? null;
  const [query, setQuery] = useState(selected?.name ?? "");
  const [open, setOpen] = useState(false);
  const [addOpen, setAddOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  // Keep the visible text in sync when the selection changes from outside this component (edit-
  // mode prefill, or right after creating+auto-selecting a new vendor).
  useEffect(() => {
    setQuery(selected?.name ?? "");
  }, [selected?.id, selected?.name]);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const suggestions = useMemo(() => {
    const q = query.trim().toLowerCase();
    const list = q ? vendors.filter((v) => v.name.toLowerCase().includes(q)) : vendors;
    return list.slice(0, 8);
  }, [vendors, query]);

  const handleSelect = (vendor: AdminVendor) => {
    onChange(String(vendor.id));
    setQuery(vendor.name);
    setOpen(false);
  };

  const handleInputChange = (text: string) => {
    setQuery(text);
    setOpen(true);
    // Typing away from the selected vendor's exact name clears the selection - a mandatory field
    // can't silently keep a stale id once the visible text no longer matches what's selected.
    if (!(selected && text === selected.name)) {
      onChange("");
    }
  };

  return (
    <div>
      <label className="block text-sm font-medium text-zinc-900">
        Vendor <span className="text-rose-500">*</span>
      </label>
      <div className="mt-1 flex gap-2">
        <div className="relative flex-1" ref={containerRef}>
          <input
            type="text"
            value={query}
            onChange={(e) => handleInputChange(e.target.value)}
            onFocus={() => setOpen(true)}
            placeholder="Search vendors…"
            autoComplete="off"
            className="w-full rounded-md border border-zinc-300 bg-white px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
          />
          {open && suggestions.length > 0 && (
            <ul className="absolute z-10 mt-1 max-h-56 w-full overflow-y-auto rounded-md border border-zinc-200 bg-white py-1 shadow-lg">
              {suggestions.map((v) => (
                <li key={v.id}>
                  <button
                    type="button"
                    onClick={() => handleSelect(v)}
                    className="block w-full px-3 py-2 text-left text-sm hover:bg-zinc-50"
                  >
                    {v.name}
                  </button>
                </li>
              ))}
            </ul>
          )}
          {open && query.trim() !== "" && suggestions.length === 0 && (
            <div className="absolute z-10 mt-1 w-full rounded-md border border-zinc-200 bg-white px-3 py-2 text-sm text-zinc-500 shadow-lg">
              No vendors match &ldquo;{query.trim()}&rdquo;.
            </div>
          )}
        </div>
        <button
          type="button"
          onClick={() => setAddOpen(true)}
          aria-label="Add a new vendor"
          title="Add a new vendor"
          className="flex h-[38px] w-[38px] shrink-0 items-center justify-center rounded-md border border-zinc-300 text-lg leading-none text-zinc-600 hover:bg-zinc-50"
        >
          +
        </button>
      </div>
      {error && <p className="mt-1 text-xs text-rose-600">{error}</p>}

      {addOpen && (
        <AddVendorModal
          vendors={vendors}
          onClose={() => setAddOpen(false)}
          onCreated={(vendor) => {
            onVendorCreated(vendor);
            onChange(String(vendor.id));
            setQuery(vendor.name);
            setAddOpen(false);
          }}
        />
      )}
    </div>
  );
}

function AddVendorModal({
  vendors,
  onClose,
  onCreated,
}: {
  vendors: AdminVendor[];
  onClose: () => void;
  onCreated: (vendor: AdminVendor) => void;
}) {
  const [name, setName] = useState("");
  const [contactName, setContactName] = useState("");
  const [contactEmail, setContactEmail] = useState("");
  const [contactPhone, setContactPhone] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const nameDuplicate = useMemo(() => {
    const trimmed = name.trim().toLowerCase();
    return trimmed !== "" && vendors.some((v) => v.name.trim().toLowerCase() === trimmed);
  }, [name, vendors]);

  const emailValid = contactEmail.trim() === "" || EMAIL_RE.test(contactEmail.trim());
  const phoneValid = contactPhone.trim() === "" || PHONE_RE.test(contactPhone.trim());

  const canSubmit =
    name.trim() !== "" &&
    contactEmail.trim() !== "" &&
    contactPhone.trim() !== "" &&
    emailValid &&
    phoneValid &&
    !nameDuplicate &&
    !submitting;

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!canSubmit) return;
    setSubmitting(true);
    setSubmitError(null);
    try {
      const vendor = (await createVendor({
        name: name.trim(),
        contactName: contactName.trim() || null,
        contactEmail: contactEmail.trim(),
        contactPhone: contactPhone.trim(),
        active: true,
      })) as AdminVendor;
      onCreated(vendor);
    } catch (err) {
      setSubmitError(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  // Portaled to document.body: this component is used inside AdminProductForm's own <form>, and
  // a <form> nested inside another <form> is invalid HTML - the browser doesn't actually create a
  // second form element, so this modal's own submit button ends up bound to the OUTER product
  // form instead (submitting it - a full native page navigation, not this handler - the moment
  // "Add vendor" is clicked). Rendering outside the DOM tree entirely sidesteps the nesting.
  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4"
      onClick={onClose}
    >
      <div
        className="w-full max-w-sm rounded-lg bg-white p-5 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <h3 className="text-sm font-semibold text-zinc-900">Add vendor</h3>
        <form onSubmit={handleSubmit} className="mt-3 space-y-3">
          {submitError && (
            <div className="rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-xs text-rose-700">
              {submitError}
            </div>
          )}
          <div>
            <label className="block text-xs font-medium text-zinc-700">Vendor name *</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              autoFocus
              className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
            />
            {nameDuplicate && (
              <p className="mt-1 text-xs text-rose-600">A vendor with this name already exists.</p>
            )}
          </div>
          <div>
            <label className="block text-xs font-medium text-zinc-700">Contact name</label>
            <input
              type="text"
              value={contactName}
              onChange={(e) => setContactName(e.target.value)}
              className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-zinc-700">Email *</label>
            <input
              type="email"
              value={contactEmail}
              onChange={(e) => setContactEmail(e.target.value)}
              className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
            />
            {!emailValid && <p className="mt-1 text-xs text-rose-600">Enter a valid email address.</p>}
          </div>
          <div>
            <label className="block text-xs font-medium text-zinc-700">Phone *</label>
            <input
              type="tel"
              value={contactPhone}
              onChange={(e) => setContactPhone(e.target.value)}
              placeholder="+91 98765 43210"
              className="mt-1 w-full rounded-md border border-zinc-300 px-3 py-2 text-sm focus:border-zinc-500 focus:outline-none focus:ring-1 focus:ring-zinc-500"
            />
            {!phoneValid && <p className="mt-1 text-xs text-rose-600">Enter a valid phone number.</p>}
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded-md border border-zinc-300 px-3 py-1.5 text-sm font-medium text-zinc-700 hover:bg-zinc-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={!canSubmit}
              className="rounded-md bg-zinc-900 px-3 py-1.5 text-sm font-medium text-white hover:bg-zinc-800 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {submitting ? "Adding…" : "Add vendor"}
            </button>
          </div>
        </form>
      </div>
    </div>,
    document.body,
  );
}
