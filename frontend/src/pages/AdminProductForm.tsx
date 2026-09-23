import { useCallback, useEffect, useState } from "react";
import type { FormEvent, ReactNode } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import * as adminProductsApi from "../api/adminProducts";
import * as adminMastersApi from "../api/adminMasters";
import { getErrorMessage, getFieldErrors, toMediaUrl } from "../api/client";
import { useCategories, useColors } from "../context/MasterDataContext";
import ImageUploadField from "../components/ImageUploadField";
import SelectField from "../components/SelectField";
import TextField from "../components/TextField";
import VendorCombobox from "../components/VendorCombobox";
import type {
  AdminColorImages,
  AdminProductDetail,
  AdminProductVariant,
  AdminSizeChart,
  AdminVendor,
  Brand,
  Material,
  ProductAdminRequest,
  ProductStatus,
  Size,
  SubCategory,
} from "../types";

type TabKey = "details" | "variants" | "images";

interface ImageRow {
  id?: number | string;
  url: string;
  displayOrder: number;
  primary: boolean;
  mediaType: "IMAGE" | "VIDEO";
}

interface VariantRow {
  /** Stable local key for React list rendering — independent of the server id (new rows have none yet). */
  key: string;
  id?: number | string;
  sku: string;
  sizeId: string;
  colorId: string;
  sellingPrice: string;
  costPrice: string;
  discountPercent: string;
  stockQuantity: string;
  lowStockThreshold: string;
  active: boolean;
  // Read-only display-only fields, present only when loaded from the server.
  reservedQuantity?: number;
  damagedQuantity?: number;
  availableQuantity?: number;
}

let localKeySeq = 0;
function nextLocalKey() {
  localKeySeq += 1;
  return `local-${localKeySeq}`;
}

function toVariantRow(v: AdminProductVariant): VariantRow {
  return {
    key: String(v.id ?? nextLocalKey()),
    id: v.id,
    sku: v.sku ?? "",
    sizeId: v.sizeId != null ? String(v.sizeId) : "",
    colorId: v.colorId != null ? String(v.colorId) : "",
    sellingPrice: v.sellingPrice != null ? String(v.sellingPrice) : "",
    costPrice: v.costPrice != null ? String(v.costPrice) : "",
    discountPercent: v.discountPercent != null ? String(v.discountPercent) : "",
    stockQuantity: v.stockQuantity != null ? String(v.stockQuantity) : "",
    lowStockThreshold:
      v.lowStockThreshold != null ? String(v.lowStockThreshold) : "",
    active: v.active,
    reservedQuantity: v.reservedQuantity,
    damagedQuantity: v.damagedQuantity,
    availableQuantity: v.availableQuantity,
  };
}

/** Server's colorImages array -> the form's colorId-keyed local state (see the `colorImages` state below). */
function toColorImagesMap(colorImages: AdminColorImages[]): Record<string, ImageRow[]> {
  const map: Record<string, ImageRow[]> = {};
  for (const ci of colorImages) {
    map[String(ci.colorId)] = ci.images
      .slice()
      .sort((a, b) => a.displayOrder - b.displayOrder)
      .map((img) => ({
        id: img.id,
        url: img.url,
        displayOrder: img.displayOrder,
        primary: img.primary,
        mediaType: img.mediaType ?? "IMAGE",
      }));
  }
  return map;
}

function blankVariantRow(prefill: {
  sellingPrice: string;
  costPrice: string;
}): VariantRow {
  return {
    key: nextLocalKey(),
    sku: "",
    sizeId: "",
    colorId: "",
    sellingPrice: prefill.sellingPrice,
    costPrice: prefill.costPrice,
    discountPercent: "",
    stockQuantity: "",
    lowStockThreshold: "",
    active: true,
  };
}

/** Simple client-side slug derivation — the Details tab has no dedicated slug field (see form's deviation note). */
function slugify(name: string): string {
  return name
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/(^-+|-+$)/g, "");
}

function toNumberOrNull(value: string): number | null {
  return value.trim() === "" ? null : Number(value);
}

const STATUS_OPTIONS: { value: ProductStatus; label: string }[] = [
  { value: "DRAFT", label: "Draft" },
  { value: "ACTIVE", label: "Active" },
  { value: "INACTIVE", label: "Inactive" },
  { value: "ARCHIVED", label: "Archived" },
];

/**
 * Icon-only "back to product list" affordance (no text, per the admin UX spec).
 * Deliberately a fixed Link to /admin/products rather than history-based
 * navigate(-1) (compare components/BackButton.tsx) - this screen's one and
 * only parent is the product list, and a successful save already returns
 * there too, so both back controls and the post-save redirect stay consistent.
 */
function ProductListBackLink({ className = "" }: { className?: string }) {
  return (
    <Link
      to="/admin/products"
      aria-label="Back to all products"
      className={`inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-zinc-700 transition-colors hover:bg-zinc-100 active:bg-zinc-200 ${className}`}
    >
      <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
      </svg>
    </Link>
  );
}

function TabButton({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`rounded-md px-4 py-2 text-sm font-medium transition ${
        active ? "bg-zinc-900 text-white" : "text-zinc-600 hover:bg-zinc-200"
      }`}
    >
      {children}
    </button>
  );
}

export default function AdminProductForm() {
  const { isAdmin } = useAuth();
  const navigate = useNavigate();
  const { id: idParam } = useParams();
  const isNew = !idParam || idParam === "new";
  const productId = isNew ? null : idParam;

  const [activeTab, setActiveTab] = useState<TabKey>("details");

  const [loading, setLoading] = useState(!isNew);
  const [loadError, setLoadError] = useState<string | null>(null);

  // Master data for selects. Categories/colors come from the shared cache;
  // brands/materials stay on their existing admin-only endpoints (no public
  // equivalent exists, and this form is admin-only anyway).
  const { data: categories } = useCategories();
  const { data: colors } = useColors();
  const [subCategories, setSubCategories] = useState<SubCategory[]>([]);
  const [brands, setBrands] = useState<Brand[]>([]);
  const [materials, setMaterials] = useState<Material[]>([]);
  const [vendors, setVendors] = useState<AdminVendor[]>([]);
  const [sizeCharts, setSizeCharts] = useState<AdminSizeChart[]>([]);
  // Category-scoped sizes (GET /categories/{id}/available-sizes) — refetched
  // whenever the selected category changes, see the effect below.
  const [sizes, setSizes] = useState<Size[]>([]);
  const [sizesLoading, setSizesLoading] = useState(false);

  // Details tab state.
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [subCategoryId, setSubCategoryId] = useState("");
  const [brandId, setBrandId] = useState("");
  const [materialId, setMaterialId] = useState("");
  const [vendorId, setVendorId] = useState("");
  const [sizeChartId, setSizeChartId] = useState("");
  const [status, setStatus] = useState<ProductStatus>("DRAFT");
  const [baseSku, setBaseSku] = useState("");
  const [baseSellingPrice, setBaseSellingPrice] = useState("");
  const [baseCostPrice, setBaseCostPrice] = useState("");

  // Variants tab state.
  const [variants, setVariants] = useState<VariantRow[]>([]);
  // Images tab state — one shared image/video set per color (colorId -> images),
  // not per variant/size: every size of a color displays the exact same photos.
  const [colorImages, setColorImages] = useState<Record<string, ImageRow[]>>({});
  // Colors currently represented among the variants above, in first-seen order —
  // drives both which color sections the Images tab renders and which colors get
  // submitted in the payload (removing a color's last variant drops its images
  // section here, and its media group is deleted server-side on save).
  const usedColorIds = Array.from(new Set(variants.map((v) => v.colorId).filter((id) => id !== "")));

  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  // Load brand/material master data once (admin-only lists, stay on their
  // existing adminProducts.ts endpoints — see the deviation note above).
  useEffect(() => {
    adminProductsApi
      .fetchBrands()
      .then(setBrands)
      .catch(() => setBrands([]));
    adminProductsApi
      .fetchMaterials()
      .then(setMaterials)
      .catch(() => setMaterials([]));
    adminProductsApi
      .fetchVendors()
      .then(setVendors)
      .catch(() => setVendors([]));
    adminProductsApi
      .fetchSizeCharts()
      .then(setSizeCharts)
      .catch(() => setSizeCharts([]));
  }, []);

  // Sub-categories depend on the chosen category.
  useEffect(() => {
    if (!categoryId) {
      setSubCategories([]);
      return;
    }
    let cancelled = false;
    adminProductsApi
      .fetchSubCategories(categoryId)
      .then((list) => {
        if (!cancelled) setSubCategories(list);
      })
      .catch(() => {
        if (!cancelled) setSubCategories([]);
      });
    return () => {
      cancelled = true;
    };
  }, [categoryId]);

  // Available sizes are scoped to the selected category — refetched whenever
  // it changes. The backend always returns *something* here (either the
  // category's scoped sizes or a full fallback list), so this only renders
  // empty before any category is chosen.
  useEffect(() => {
    if (!categoryId) {
      setSizes([]);
      return;
    }
    let cancelled = false;
    setSizesLoading(true);
    adminMastersApi
      .fetchAvailableSizesForCategory(categoryId)
      .then((list) => {
        if (!cancelled) setSizes(list);
      })
      .catch(() => {
        if (!cancelled) setSizes([]);
      })
      .finally(() => {
        if (!cancelled) setSizesLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [categoryId]);

  const applyProduct = useCallback((product: AdminProductDetail) => {
    setName(product.name);
    setDescription(product.description ?? "");
    setCategoryId(String(product.categoryId));
    setSubCategoryId(
      product.subCategoryId != null ? String(product.subCategoryId) : "",
    );
    setBrandId(product.brandId != null ? String(product.brandId) : "");
    setMaterialId(String(product.materialId));
    setVendorId(product.vendorId != null ? String(product.vendorId) : "");
    setSizeChartId(product.sizeChartId != null ? String(product.sizeChartId) : "");
    setStatus(product.status);
    setBaseSku(product.baseSku ?? "");
    setBaseSellingPrice(
      product.baseSellingPrice != null ? String(product.baseSellingPrice) : "",
    );
    setBaseCostPrice(
      product.baseCostPrice != null ? String(product.baseCostPrice) : "",
    );
    setVariants((product.variants ?? []).map(toVariantRow));
    setColorImages(toColorImagesMap(product.colorImages ?? []));
  }, []);

  useEffect(() => {
    if (isNew) {
      setLoading(false);
      return;
    }
    let cancelled = false;
    setLoading(true);
    setLoadError(null);
    adminProductsApi
      .fetchAdminProduct(productId!)
      .then((product) => {
        if (!cancelled) applyProduct(product);
      })
      .catch((err) => {
        if (!cancelled) setLoadError(getErrorMessage(err));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [productId, isNew, applyProduct]);

  // --- Variant row helpers ---

  const updateVariant = (key: string, patch: Partial<VariantRow>) => {
    setVariants((prev) =>
      prev.map((v) => (v.key === key ? { ...v, ...patch } : v)),
    );
  };

  const addVariant = () => {
    setVariants((prev) => [
      ...prev,
      blankVariantRow({
        sellingPrice: baseSellingPrice,
        costPrice: baseCostPrice,
      }),
    ]);
  };

  const removeVariant = (key: string) => {
    setVariants((prev) => prev.filter((v) => v.key !== key));
  };

  // --- Per-color image/video helpers (shared across every size of that color) ---

  const handleMediaUploaded = (
    colorId: string,
    url: string | null,
    contentType?: string,
  ) => {
    if (!url) return;
    const mediaType: "IMAGE" | "VIDEO" = contentType?.startsWith("video/")
      ? "VIDEO"
      : "IMAGE";
    setColorImages((prev) => {
      const existing = prev[colorId] ?? [];
      const newImage: ImageRow = {
        url,
        displayOrder: existing.length,
        primary: existing.length === 0,
        mediaType,
      };
      return { ...prev, [colorId]: [...existing, newImage] };
    });
  };

  const setPrimaryImage = (colorId: string, index: number) => {
    setColorImages((prev) => ({
      ...prev,
      [colorId]: (prev[colorId] ?? []).map((img, i) => ({ ...img, primary: i === index })),
    }));
  };

  const moveImage = (colorId: string, index: number, direction: -1 | 1) => {
    setColorImages((prev) => {
      const imgs = (prev[colorId] ?? []).slice();
      const target = index + direction;
      if (target < 0 || target >= imgs.length) return prev;
      [imgs[index], imgs[target]] = [imgs[target], imgs[index]];
      return { ...prev, [colorId]: imgs.map((img, i) => ({ ...img, displayOrder: i })) };
    });
  };

  const removeImage = (colorId: string, index: number) => {
    setColorImages((prev) => {
      const imgs = (prev[colorId] ?? []).filter((_, i) => i !== index);
      if (imgs.length > 0 && !imgs.some((img) => img.primary)) {
        imgs[0] = { ...imgs[0], primary: true };
      }
      return { ...prev, [colorId]: imgs.map((img, i) => ({ ...img, displayOrder: i })) };
    });
  };

  // --- Save ---

  const buildPayload = (): ProductAdminRequest => ({
    categoryId: Number(categoryId),
    subCategoryId: subCategoryId ? Number(subCategoryId) : null,
    brandId: brandId ? Number(brandId) : null,
    materialId: Number(materialId),
    vendorId: Number(vendorId),
    sizeChartId: sizeChartId ? Number(sizeChartId) : null,
    name,
    slug: slugify(name),
    description,
    status,
    baseSku: baseSku.trim() === "" ? null : baseSku,
    baseSellingPrice: toNumberOrNull(baseSellingPrice),
    baseCostPrice: toNumberOrNull(baseCostPrice),
    variants: variants.map((v) => ({
      id: v.id ?? undefined,
      sku: v.sku,
      sizeId: Number(v.sizeId),
      colorId: Number(v.colorId),
      sellingPrice: Number(v.sellingPrice || 0),
      costPrice: toNumberOrNull(v.costPrice),
      discountPercent:
        v.discountPercent.trim() === "" ? 0 : Number(v.discountPercent),
      stockQuantity:
        v.stockQuantity.trim() === "" ? 0 : Number(v.stockQuantity),
      lowStockThreshold: toNumberOrNull(v.lowStockThreshold),
      active: v.active,
    })),
    colorImages: usedColorIds.map((colorId) => ({
      colorId: Number(colorId),
      images: (colorImages[colorId] ?? []).map((img, idx) => ({
        url: img.url,
        displayOrder: idx,
        primary: img.primary,
        mediaType: img.mediaType,
      })),
    })),
  });

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSaveError(null);
    setFieldErrors({});
    // vendorId isn't a native <select>/<input required> (VendorCombobox is custom), so an empty
    // selection wouldn't otherwise block submission - and Number('') is 0, not null, which would
    // pass the backend's @NotNull check and surface as a confusing "Vendor not found: 0" 404
    // instead of a clean "required" field error. Caught here before the request even goes out.
    if (!vendorId) {
      setFieldErrors({ vendorId: "must not be null" });
      return;
    }
    setSaving(true);
    try {
      const payload = buildPayload();
      if (isNew) {
        await adminProductsApi.createAdminProduct(payload);
      } else {
        await adminProductsApi.updateAdminProduct(productId!, payload);
      }
      // Per the admin UX spec: a successful save returns straight to the
      // Product List rather than staying on the form.
      navigate("/admin/products");
    } catch (err) {
      setSaveError(getErrorMessage(err));
      setFieldErrors(getFieldErrors(err));
      setSaving(false);
    }
  };

  if (!isAdmin) {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-zinc-950 px-4">
        <div className="w-full max-w-md rounded-xl border border-zinc-800 bg-zinc-900 p-8 text-center shadow-xl">
          <h1 className="text-xl font-semibold text-white">
            Admin access required
          </h1>
          <p className="mt-2 text-sm text-zinc-400">
            You need the ADMIN role to manage products.
          </p>
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

  if (loading) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center text-sm text-zinc-500">
        Loading product…
      </div>
    );
  }

  const fieldErrorList = Object.entries(fieldErrors);

  return (
    <div className="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="flex items-start gap-2">
        <ProductListBackLink className="mt-0.5" />
        <div>
          <h1 className="text-2xl font-semibold text-zinc-900">
            {isNew ? "Add product" : `Edit ${name || "product"}`}
          </h1>
          <p className="mt-1 text-sm text-zinc-500">
            Create and manage product details, variants, pricing, images, and inventory.
          </p>
        </div>
      </div>

      {loadError && (
        <div className="mt-6 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
          {loadError}
        </div>
      )}

      <form onSubmit={handleSubmit}>
        {/* Tabs */}
        <div className="mt-8 inline-flex gap-1 rounded-lg bg-zinc-100 p-1">
          <TabButton
            active={activeTab === "details"}
            onClick={() => setActiveTab("details")}
          >
            Details
          </TabButton>
          <TabButton
            active={activeTab === "variants"}
            onClick={() => setActiveTab("variants")}
          >
            Variants {variants.length > 0 && `(${variants.length})`}
          </TabButton>
          <TabButton
            active={activeTab === "images"}
            onClick={() => setActiveTab("images")}
          >
            Images
          </TabButton>
        </div>

        {/* Details tab */}
        {activeTab === "details" && (
          <section className="mt-6 space-y-6">
            <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
              <TextField
                label="Name"
                value={name}
                onChange={(v) => setName(v ?? "")}
              />
              <SelectField
                label="Status"
                value={status}
                onChange={(v) => setStatus(v as ProductStatus)}
                options={STATUS_OPTIONS}
                error={fieldErrors.status}
              />
            </div>

            <TextField
              label="Description"
              value={description}
              onChange={(v) => setDescription(v ?? "")}
              textarea
            />

            <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
              <SelectField
                label="Category"
                value={categoryId}
                onChange={(v) => {
                  setCategoryId(v);
                  setSubCategoryId("");
                }}
                placeholder="Select category"
                required
                options={categories.map((c) => ({
                  value: String(c.id),
                  label: c.name,
                }))}
                error={fieldErrors.categoryId}
              />
              <SelectField
                label="Sub-category"
                value={subCategoryId}
                onChange={setSubCategoryId}
                placeholder={
                  categoryId ? "— None —" : "Select a category first"
                }
                disabled={!categoryId}
                options={subCategories.map((s) => ({
                  value: String(s.id),
                  label: s.name,
                }))}
                error={fieldErrors.subCategoryId}
              />
              <SelectField
                label="Brand"
                value={brandId}
                onChange={setBrandId}
                placeholder="— None —"
                options={brands.map((b) => ({
                  value: String(b.id),
                  label: b.name,
                }))}
                error={fieldErrors.brandId}
              />
              <SelectField
                label="Material"
                value={materialId}
                onChange={setMaterialId}
                placeholder="Select material"
                required
                options={materials.map((m) => ({
                  value: String(m.id),
                  label: m.name,
                }))}
                error={fieldErrors.materialId}
              />
              <VendorCombobox
                vendors={vendors}
                value={vendorId}
                onChange={(id) => {
                  setVendorId(id);
                  if (id) setFieldErrors((prev) => ({ ...prev, vendorId: "" }));
                }}
                onVendorCreated={(v) => setVendors((prev) => [...prev, v])}
                error={fieldErrors.vendorId}
              />
              <SelectField
                label="Size chart"
                value={sizeChartId}
                onChange={setSizeChartId}
                placeholder="— None —"
                options={sizeCharts.map((c) => ({
                  value: String(c.id),
                  label: c.name,
                }))}
                error={fieldErrors.sizeChartId}
              />
            </div>

            <div>
              <h3 className="text-sm font-semibold text-zinc-900">
                Base values
              </h3>
              <p className="mt-1 text-xs text-zinc-500">
                Used to prefill new variants added below — not required, and not
                shown to customers directly.
              </p>
              <div className="mt-3 grid grid-cols-1 gap-6 sm:grid-cols-3">
                <TextField
                  label="Base SKU"
                  value={baseSku || null}
                  onChange={(v) => setBaseSku(v ?? "")}
                />
                <TextField
                  label="Base selling price"
                  type="number"
                  value={baseSellingPrice || null}
                  onChange={(v) => setBaseSellingPrice(v ?? "")}
                />
                <TextField
                  label="Base cost price"
                  type="number"
                  value={baseCostPrice || null}
                  onChange={(v) => setBaseCostPrice(v ?? "")}
                />
              </div>
            </div>
          </section>
        )}

        {/* Variants tab */}
        {activeTab === "variants" && (
          <section className="mt-6">
            {!categoryId && (
              <p className="mb-3 text-xs text-amber-600">
                Select a category in the Details tab to see available sizes.
              </p>
            )}
            {categoryId && sizesLoading && (
              <p className="mb-3 text-xs text-zinc-500">
                Loading available sizes…
              </p>
            )}
            <div className="overflow-x-auto rounded-lg border border-zinc-200">
              <table className="min-w-full divide-y divide-zinc-200 text-sm">
                <thead className="bg-zinc-50">
                  <tr className="text-left text-xs font-semibold uppercase tracking-wide text-zinc-500">
                    <th className="px-3 py-2">Color</th>
                    <th className="px-3 py-2">Size</th>
                    <th className="px-3 py-2">SKU</th>
                    <th className="px-3 py-2">Selling price</th>
                    <th className="px-3 py-2">Cost price</th>
                    <th className="px-3 py-2">Discount %</th>
                    <th className="px-3 py-2">Stock</th>
                    <th className="px-3 py-2">Low stock at</th>
                    <th className="px-3 py-2">Active</th>
                    <th className="px-3 py-2" />
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-100 bg-white">
                  {variants.length === 0 ? (
                    <tr>
                      <td
                        colSpan={10}
                        className="px-3 py-8 text-center text-zinc-500"
                      >
                        No variants yet. Add one below.
                      </td>
                    </tr>
                  ) : (
                    variants.map((v, idx) => {
                      const skuError = fieldErrors[`variants[${idx}].sku`];
                      const sizeError = fieldErrors[`variants[${idx}].sizeId`];
                      const colorError =
                        fieldErrors[`variants[${idx}].colorId`];
                      const priceError =
                        fieldErrors[`variants[${idx}].sellingPrice`];
                      return (
                        <tr key={v.key} className="align-top">
                          <td className="px-3 py-2">
                            <select
                              value={v.colorId}
                              onChange={(e) =>
                                updateVariant(v.key, {
                                  colorId: e.target.value,
                                })
                              }
                              className="w-32 rounded-md border border-zinc-300 px-2 py-1.5 text-sm"
                            >
                              <option value="">— Select —</option>
                              {colors.map((c) => (
                                <option key={c.id} value={String(c.id)}>
                                  {c.name}
                                </option>
                              ))}
                            </select>
                            {colorError && (
                              <p className="mt-1 text-[11px] text-rose-600">
                                {colorError}
                              </p>
                            )}
                          </td>
                          <td className="px-3 py-2">
                            <select
                              value={v.sizeId}
                              onChange={(e) =>
                                updateVariant(v.key, { sizeId: e.target.value })
                              }
                              className="w-24 rounded-md border border-zinc-300 px-2 py-1.5 text-sm"
                            >
                              <option value="">— Select —</option>
                              {sizes.map((s) => (
                                <option key={s.id} value={String(s.id)}>
                                  {s.name}
                                </option>
                              ))}
                            </select>
                            {sizeError && (
                              <p className="mt-1 text-[11px] text-rose-600">
                                {sizeError}
                              </p>
                            )}
                          </td>
                          <td className="px-3 py-2">
                            <input
                              type="text"
                              value={v.sku}
                              onChange={(e) =>
                                updateVariant(v.key, { sku: e.target.value })
                              }
                              className="w-32 rounded-md border border-zinc-300 px-2 py-1.5 text-sm"
                            />
                            {skuError && (
                              <p className="mt-1 text-[11px] text-rose-600">
                                {skuError}
                              </p>
                            )}
                          </td>
                          <td className="px-3 py-2">
                            <input
                              type="number"
                              value={v.sellingPrice}
                              onChange={(e) =>
                                updateVariant(v.key, {
                                  sellingPrice: e.target.value,
                                })
                              }
                              className="w-24 rounded-md border border-zinc-300 px-2 py-1.5 text-sm"
                            />
                            {priceError && (
                              <p className="mt-1 text-[11px] text-rose-600">
                                {priceError}
                              </p>
                            )}
                          </td>
                          <td className="px-3 py-2">
                            <input
                              type="number"
                              value={v.costPrice}
                              onChange={(e) =>
                                updateVariant(v.key, {
                                  costPrice: e.target.value,
                                })
                              }
                              className="w-24 rounded-md border border-zinc-300 px-2 py-1.5 text-sm"
                            />
                          </td>
                          <td className="px-3 py-2">
                            <input
                              type="number"
                              value={v.discountPercent}
                              onChange={(e) =>
                                updateVariant(v.key, {
                                  discountPercent: e.target.value,
                                })
                              }
                              className="w-20 rounded-md border border-zinc-300 px-2 py-1.5 text-sm"
                            />
                          </td>
                          <td className="px-3 py-2">
                            {v.id ? (
                              <div
                                title={`Reserved: ${v.reservedQuantity ?? 0} · Damaged: ${v.damagedQuantity ?? 0} · Available: ${v.availableQuantity ?? 0}`}
                              >
                                <span className="text-zinc-700">
                                  {v.stockQuantity || "0"}
                                </span>
                                <p className="mt-1 w-28 text-[10px] leading-tight text-zinc-400">
                                  Adjust stock from the Inventory page.
                                </p>
                              </div>
                            ) : (
                              <input
                                type="number"
                                min={0}
                                value={v.stockQuantity}
                                onChange={(e) =>
                                  updateVariant(v.key, {
                                    stockQuantity: e.target.value,
                                  })
                                }
                                className="w-20 rounded-md border border-zinc-300 px-2 py-1.5 text-sm"
                              />
                            )}
                          </td>
                          <td className="px-3 py-2">
                            <input
                              type="number"
                              min={0}
                              placeholder="5 (default)"
                              value={v.lowStockThreshold}
                              onChange={(e) =>
                                updateVariant(v.key, {
                                  lowStockThreshold: e.target.value,
                                })
                              }
                              className="w-24 rounded-md border border-zinc-300 px-2 py-1.5 text-sm placeholder:text-zinc-400"
                            />
                          </td>
                          <td className="px-3 py-2 text-center">
                            <input
                              type="checkbox"
                              checked={v.active}
                              onChange={(e) =>
                                updateVariant(v.key, {
                                  active: e.target.checked,
                                })
                              }
                              className="h-4 w-4"
                            />
                          </td>
                          <td className="px-3 py-2">
                            <button
                              type="button"
                              onClick={() => removeVariant(v.key)}
                              title="Remove variant"
                              className="text-zinc-400 hover:text-rose-600"
                            >
                              ×
                            </button>
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>
            <button
              type="button"
              onClick={addVariant}
              className="mt-3 rounded-md border border-zinc-300 px-3 py-1.5 text-sm font-medium text-zinc-700 hover:bg-zinc-50"
            >
              + Add variant
            </button>
          </section>
        )}

        {/* Images tab — one shared set per color, not per size: every size of a
            color displays the exact same photos, so this groups by colorId
            rather than iterating variants directly. */}
        {activeTab === "images" && (
          <section className="mt-6 space-y-8">
            {usedColorIds.length === 0 ? (
              <p className="text-sm text-zinc-500">
                Add at least one variant in the Variants tab before adding
                images.
              </p>
            ) : (
              usedColorIds.map((colorId) => {
                const images = colorImages[colorId] ?? [];
                const color = colors.find((c) => String(c.id) === colorId);
                const sizeNames = variants
                  .filter((v) => v.colorId === colorId)
                  .map((v) => sizes.find((s) => String(s.id) === v.sizeId)?.name)
                  .filter((n): n is string => Boolean(n));
                return (
                  <div
                    key={colorId}
                    className="rounded-lg border border-zinc-200 p-4"
                  >
                    <h3 className="text-sm font-semibold text-zinc-900">
                      {color?.name ?? "Unknown color"}
                    </h3>
                    <p className="mt-0.5 text-xs text-zinc-500">
                      Shared by every size of this color
                      {sizeNames.length > 0 ? ` — ${sizeNames.join(", ")}` : ""}.
                    </p>

                    {images.length > 0 && (
                      <div className="mt-3 flex flex-wrap gap-3">
                        {images.map((img, index) => {
                          const url = toMediaUrl(img.url);
                          return (
                            <div
                              key={img.id ?? `${img.url}-${index}`}
                              className="w-20 shrink-0"
                            >
                              <div className="relative">
                                {url && img.mediaType === "VIDEO" ? (
                                  <video
                                    src={url}
                                    muted
                                    playsInline
                                    className="h-20 w-20 rounded-md border border-zinc-200 object-cover"
                                  />
                                ) : url ? (
                                  <img
                                    src={url}
                                    alt=""
                                    className="h-20 w-20 rounded-md border border-zinc-200 object-cover"
                                  />
                                ) : null}
                                {img.mediaType === "VIDEO" && (
                                  <span className="absolute bottom-1 right-1 rounded bg-zinc-900/80 px-1 text-[9px] font-bold uppercase tracking-wide text-white">
                                    Video
                                  </span>
                                )}
                                {img.primary && (
                                  <span className="absolute left-1 top-1 rounded-full bg-amber-400 px-1 text-[10px] font-bold text-white">
                                    ★
                                  </span>
                                )}
                              </div>
                              <div className="mt-1 flex items-center justify-center gap-1 text-xs">
                                <button
                                  type="button"
                                  title="Set primary"
                                  onClick={() => setPrimaryImage(colorId, index)}
                                  disabled={img.primary}
                                  className="text-amber-500 hover:text-amber-600 disabled:opacity-30"
                                >
                                  ★
                                </button>
                                <button
                                  type="button"
                                  title="Move earlier"
                                  onClick={() => moveImage(colorId, index, -1)}
                                  disabled={index === 0}
                                  className="text-zinc-500 hover:text-zinc-800 disabled:opacity-30"
                                >
                                  ↑
                                </button>
                                <button
                                  type="button"
                                  title="Move later"
                                  onClick={() => moveImage(colorId, index, 1)}
                                  disabled={index === images.length - 1}
                                  className="text-zinc-500 hover:text-zinc-800 disabled:opacity-30"
                                >
                                  ↓
                                </button>
                                <button
                                  type="button"
                                  title="Remove"
                                  onClick={() => removeImage(colorId, index)}
                                  className="text-rose-500 hover:text-rose-700"
                                >
                                  ×
                                </button>
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    )}

                    <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
                      <ImageUploadField
                        label="Add image"
                        value={null}
                        onUploaded={(url, contentType) =>
                          handleMediaUploaded(colorId, url, contentType)
                        }
                      />
                      <ImageUploadField
                        label="Add video"
                        value={null}
                        accept="video/*"
                        isVideo
                        helpText="MP4, WebM or MOV, up to 50MB."
                        onUploaded={(url, contentType) =>
                          handleMediaUploaded(colorId, url, contentType)
                        }
                      />
                    </div>
                  </div>
                );
              })
            )}
          </section>
        )}

        {saveError && (
          <div className="mt-8 rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
            <p>{saveError}</p>
            {fieldErrorList.length > 0 && (
              <ul className="mt-2 list-inside list-disc">
                {fieldErrorList.map(([field, message]) => (
                  <li key={field}>
                    <span className="font-medium">{field}:</span> {message}
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}

        <div className="mt-6 flex justify-center sm:justify-end">
          <button
            type="submit"
            disabled={saving}
            className="w-full rounded-xl bg-zinc-900 px-7 py-3 text-sm font-semibold text-white transition-all duration-150 hover:bg-zinc-800 active:scale-[0.97] disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto"
          >
            {saving ? "Saving…" : isNew ? "Add product" : "Save"}
          </button>
        </div>

        <div className="mt-10 flex justify-center border-t border-zinc-100 pt-6">
          <ProductListBackLink />
        </div>
      </form>
    </div>
  );
}
