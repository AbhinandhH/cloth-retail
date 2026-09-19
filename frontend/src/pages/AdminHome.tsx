import { useEffect, useMemo, useRef } from "react";
import type { ReactNode } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import type { AdminModule as ModulePermissionModule } from "../types";
import { useSiteConfig } from "../context/SiteConfigContext";
import AmbientBackground from "../components/AmbientBackground";
import BrandHero from "../components/BrandHero";
import BrandMark from "../components/BrandMark";

const MODULES_ANCHOR_ID = "admin-modules";
// Set once the admin home page has been shown this session, so a later return trip (e.g. opening
// a module then hitting back) can skip straight to the module grid instead of re-showing the
// hero - see the effect in AdminHome() below, and Home.tsx's identical HOME_VISITED_KEY pattern.
const ADMIN_HOME_VISITED_KEY = "adminHome:visited";

// Same theme-derived-token pattern as Footer.tsx: scoped custom properties so
// Tailwind's arbitrary-value classes (including hover: variants) can each
// reference a single var() rather than a full color-mix(...) expression.
const MODULE_STYLES = `
  .admin-modules-surface {
    --admin-border: color-mix(in srgb, var(--brand-secondary, #d8b878) 35%, var(--brand-background, #ffffff));
    --admin-border-hover: color-mix(in srgb, var(--brand-secondary, #d8b878) 60%, var(--brand-background, #ffffff));
    --admin-accent: color-mix(in srgb, var(--brand-secondary, #a9781f) 85%, var(--brand-text, #1c1712));
    --admin-ink: var(--brand-text, #1c1712);
    --admin-muted: color-mix(in srgb, var(--brand-text, #1c1712) 55%, var(--brand-background, #ffffff));
    /* Header chrome - see Navbar.tsx's own NAV_STYLES comment for why this
       can't stay a fixed white/zinc bar: a dark theme's near-white
       --brand-text would render BrandMark's logo invisible against it. */
    --admin-nav-text: color-mix(in srgb, var(--brand-text, #3f3f46) 78%, var(--brand-background, #ffffff));
    --admin-nav-border: color-mix(in srgb, var(--brand-text, #e4e4e7) 14%, var(--brand-background, #ffffff));
    --admin-nav-hover-bg: color-mix(in srgb, var(--brand-text, #fafafa) 6%, var(--brand-background, #ffffff));
  }
`;

function DashboardIcon() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <rect x="3.5" y="3.5" width="7" height="9" rx="1.25" strokeWidth={1.75} />
      <rect x="13.5" y="3.5" width="7" height="5" rx="1.25" strokeWidth={1.75} />
      <rect x="13.5" y="11.5" width="7" height="9" rx="1.25" strokeWidth={1.75} />
      <rect x="3.5" y="15.5" width="7" height="5" rx="1.25" strokeWidth={1.75} />
    </svg>
  );
}

function ProductsIcon() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M20.59 13.41L11 3.83A2 2 0 009.59 3.2L4 3a1 1 0 00-1 1l.2 5.59a2 2 0 00.59 1.4l9.6 9.6a2 2 0 002.82 0l4.4-4.4a2 2 0 000-2.78z" />
      <circle cx="8" cy="8" r="1.25" fill="currentColor" stroke="none" />
    </svg>
  );
}

function InventoryIcon() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M3 8l9-4.5L21 8m-18 0l9 4.5M3 8v8l9 4.5M21 8l-9 4.5m9-4.5v8l-9 4.5m0-8v8" />
    </svg>
  );
}

function StockIcon() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M9 4.5h6a1 1 0 011 1V6h1.5A1.5 1.5 0 0119 7.5v11A1.5 1.5 0 0117.5 20h-11A1.5 1.5 0 015 18.5v-11A1.5 1.5 0 016.5 6H8v-.5a1 1 0 011-1z" />
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M8.5 12h7M8.5 15.5h7" />
    </svg>
  );
}

function OrdersIcon() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M6 7l1.2-3h9.6L18 7M6 7h12M6 7l-1.2 12.2A1 1 0 005.8 20.5h12.4a1 1 0 001-1.3L18 7" />
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M9 10.5a3 3 0 006 0" />
    </svg>
  );
}

function ReturnsIcon() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M4 8l4-4m-4 4l4 4m-4-4h11a5 5 0 015 5v1" />
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M20 16l-4 4m4-4l-4-4m4 4H9a5 5 0 01-5-5v-1" />
    </svg>
  );
}

function CustomersIcon() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <circle cx="9" cy="8" r="3" strokeWidth={1.75} />
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M3.5 19.5a5.5 5.5 0 0111 0" />
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M15.5 8.5a2.75 2.75 0 110 5.5M17 14.5a4.5 4.5 0 014.5 4.5" />
    </svg>
  );
}

function MastersIcon() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M12 3l8 4-8 4-8-4 8-4zM4 11l8 4 8-4M4 15l8 4 8-4" />
    </svg>
  );
}

function ConfigurationIcon() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M10.3 3.6c.2-.9 1.2-.9 1.4 0l.2.9c.1.5.5.9 1 1l.9.2c.9.2.9 1.2 0 1.4l-.9.2c-.5.1-.9.5-1 1l-.2.9c-.2.9-1.2.9-1.4 0l-.2-.9c-.1-.5-.5-.9-1-1l-.9-.2c-.9-.2-.9-1.2 0-1.4l.9-.2c.5-.1.9-.5 1-1z" />
      <circle cx="11" cy="16" r="3.25" />
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M17 12.5l1.4-.4M17 19.5l1.4.4" />
    </svg>
  );
}

function ReportsIcon() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M5 20V6.5A1.5 1.5 0 016.5 5h8.6L19 8.9V20a0 0 0 010 0H5a0 0 0 010 0z" />
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M9 16v-3M12.5 16v-5M16 16v-2" />
    </svg>
  );
}

function NotificationsIcon() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={1.75}
        d="M15 17h5l-1.4-1.4A2 2 0 0118 14.2V11a6 6 0 10-12 0v3.2a2 2 0 01-.6 1.4L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
      />
    </svg>
  );
}

function StaffIcon() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <circle cx="12" cy="7.5" r="3" strokeWidth={1.75} />
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M5.5 19.5a6.5 6.5 0 0113 0" />
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M18 4.5a3 3 0 010 6M20.5 19.5a6.5 6.5 0 00-3.5-5.8" />
    </svg>
  );
}

interface AdminModuleTile {
  to: string;
  label: string;
  description: string;
  icon: ReactNode;
  /** The operational module this tile is gated by (view permission) - omitted for governance tiles, which are gated by `governance` instead. */
  module?: ModulePermissionModule;
  /** Store-governance tile (Tax, Site Config, Notifications, Staff) - gated on isStoreAdmin (ADMIN or SUPER_ADMIN), never permission-gated, never EMPLOYEE. */
  governance?: boolean;
}

const MODULES: AdminModuleTile[] = [
  { to: "/admin/dashboard", label: "Dashboard", description: "Sales & stock overview", icon: <DashboardIcon />, module: "DASHBOARD" },
  { to: "/admin/products", label: "Products", description: "Catalog & variants", icon: <ProductsIcon />, module: "PRODUCTS" },
  { to: "/admin/inventory", label: "Inventory", description: "Stock movements", icon: <InventoryIcon />, module: "INVENTORY" },
  { to: "/admin/inventory/stock", label: "Stock", description: "On-hand levels", icon: <StockIcon />, module: "INVENTORY" },
  { to: "/admin/orders", label: "Orders", description: "Fulfilment queue", icon: <OrdersIcon />, module: "ORDERS" },
  { to: "/admin/returns", label: "Returns", description: "Exchanges & damage claims", icon: <ReturnsIcon />, module: "RETURNS" },
  { to: "/admin/customers", label: "Customers", description: "Accounts & order history", icon: <CustomersIcon />, module: "CUSTOMERS" },
  { to: "/admin/reports", label: "Reports", description: "Sales, stock & GST reports", icon: <ReportsIcon />, module: "REPORTS" },
  { to: "/admin/masters", label: "Masters", description: "Shared reference data", icon: <MastersIcon />, module: "MASTERS" },
  { to: "/admin/staff", label: "Staff", description: "Admin & employee accounts", icon: <StaffIcon />, governance: true },
  { to: "/admin/configuration", label: "Site configuration", description: "Branding & theme", icon: <ConfigurationIcon />, governance: true },
  { to: "/admin/notifications", label: "Notifications", description: "Email/SMS OTP on-off switches", icon: <NotificationsIcon />, governance: true },
];

function ModuleCard({ mod }: { mod: AdminModuleTile }) {
  return (
    <Link
      to={mod.to}
      className="group flex flex-col items-center gap-3 rounded-2xl border border-[var(--admin-border)] bg-[var(--brand-background,#ffffff)]/60 px-4 py-6 text-center shadow-sm ring-1 ring-black/[0.02] backdrop-blur-sm transition-all hover:-translate-y-0.5 hover:border-[var(--admin-border-hover)] hover:bg-[var(--brand-background,#ffffff)]/85 hover:shadow-md"
    >
      <span
        className="flex h-14 w-14 items-center justify-center rounded-full text-[var(--admin-accent)] transition-transform group-hover:scale-105"
        style={{
          background:
            "linear-gradient(160deg, color-mix(in srgb, var(--brand-secondary, #d4af37) 30%, var(--brand-background, #ffffff)) 0%, color-mix(in srgb, var(--brand-secondary, #d4af37) 55%, var(--brand-background, #ffffff)) 100%)",
        }}
      >
        {mod.icon}
      </span>
      <span className="text-sm font-semibold text-[var(--admin-ink)]">{mod.label}</span>
      <span className="text-xs text-[var(--admin-muted)]">{mod.description}</span>
    </Link>
  );
}

export default function AdminHome() {
  const { user, logout, isStoreAdmin, hasModuleView } = useAuth();

  // isStoreAdmin covers both ADMIN and SUPER_ADMIN (the software owner inherits every ADMIN
  // privilege - see SecurityConfig's RoleHierarchy bean) - either sees every tile: governance ones
  // unconditionally, operational ones because it's seeded with full access at creation (see
  // ModulePermissionService.grantAllModules). An EMPLOYEE sees only the operational modules it's
  // been individually granted view access to, and no governance tiles at all.
  const visibleModules = useMemo(() => {
    return MODULES.filter((mod) => {
      if (mod.governance) return isStoreAdmin;
      if (isStoreAdmin) return true;
      return mod.module ? hasModuleView(mod.module) : false;
    });
  }, [isStoreAdmin, hasModuleView]);
  const { config } = useSiteConfig();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate("/admin/login", { replace: true });
  };

  const scrollToModules = () => {
    document
      .getElementById(MODULES_ANCHOR_ID)
      ?.scrollIntoView({ behavior: "smooth", block: "start" });
  };

  // First-ever visit this session -> show the hero as normal. Any later remount (opening a
  // module and coming back via its own back link) -> skip straight to the module grid. A plain
  // useEffect (not useLayoutEffect) is required: ScrollToTop (mounted near the root, see App.tsx)
  // resets scroll to 0 on every route change via its own useEffect, and sibling passive effects
  // fire in tree order, so this one only wins by firing after that reset, not before it.
  //
  // hasCheckedVisited guards against StrictMode's dev-only double-invoke of effects (mount ->
  // cleanup -> mount again, same component instance) - see Home.tsx's identical guard for the
  // full explanation of why skipping it makes even a genuine first-ever visit scroll incorrectly.
  const hasCheckedVisited = useRef(false);
  useEffect(() => {
    if (hasCheckedVisited.current) return;
    hasCheckedVisited.current = true;
    const alreadyVisited = sessionStorage.getItem(ADMIN_HOME_VISITED_KEY) === "true";
    sessionStorage.setItem(ADMIN_HOME_VISITED_KEY, "true");
    if (alreadyVisited) {
      document.getElementById(MODULES_ANCHOR_ID)?.scrollIntoView({ block: "start" });
    }
  }, []);

  return (
    // No bg-white: Home's own Layout wrapper lost this class too (see its own
    // comment) since an opaque background here sat between AmbientBackground
    // and the screen and washed most of its color out - both pages now render
    // the identical AmbientBackground component with nothing diluting it in
    // between, so they're guaranteed to match rather than needing a
    // compensating class to fake the same muted look.
    <div className="admin-modules-surface min-h-dvh">
      <style>{MODULE_STYLES}</style>
      {config?.theme?.richAmbient !== false && <AmbientBackground />}

      <header className="sticky top-0 z-40 border-b border-[var(--admin-nav-border)] bg-[var(--brand-background,#ffffff)]/95 backdrop-blur-sm">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
          <div className="flex items-center gap-3">
            <BrandMark businessName={config?.businessName} logoUrl={config?.logoUrl} />
            <span className="rounded-full border border-[var(--admin-border-hover)] px-2.5 py-0.5 text-[11px] font-semibold uppercase tracking-wide text-[var(--admin-accent)]">
              Admin
            </span>
          </div>
          <div className="flex items-center gap-4">
            <Link
              to="/admin/profile"
              className="hidden text-sm text-[var(--admin-nav-text)] hover:underline sm:inline"
            >
              {user?.fullName ?? user?.email}
            </Link>
            <button
              onClick={handleLogout}
              className="rounded-full border border-[var(--admin-nav-border)] px-4 py-1.5 text-sm font-medium text-[var(--admin-nav-text)] transition-colors hover:bg-[var(--admin-nav-hover-bg)]"
            >
              Log out
            </button>
          </div>
        </div>
      </header>

      {/* Same pt trim as Home.tsx's identical wrapper, for the same reason: the sticky
          header already carries its own height, so a full py-6 here doubled up as empty
          air above the hero's logo mark. */}
      <div className="relative mx-auto max-w-7xl px-4 pb-6 pt-0 sm:px-6 lg:px-8">
        <BrandHero
          eyebrow="Admin console"
          heading={config?.businessName || <>Loom Atelier Studio</>}
          tagline={`Signed in as ${user?.fullName ?? user?.email ?? "admin"}`}
          cta={{ label: "Jump to modules", onClick: scrollToModules }}
        />

        <section id={MODULES_ANCHOR_ID} className="scroll-mt-20 pb-16">
          <h2 className="text-center text-xs font-semibold uppercase tracking-[0.25em] text-[var(--admin-accent)]">
            Manage your store
          </h2>
          <div className="mx-auto mt-6 grid max-w-4xl grid-cols-2 gap-4 sm:grid-cols-3">
            {visibleModules.map((mod) => (
              <ModuleCard key={mod.to} mod={mod} />
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}
