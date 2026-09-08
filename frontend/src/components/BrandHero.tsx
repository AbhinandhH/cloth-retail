import type { ReactNode } from "react";

// The actual Loom Atelier logo mark, cropped from the brand reference photo to
// just the monogram + gold ribbon (public/brand/loom-atelier-mark.png, 930×500
// — the printed "LOOM ATELIER" wordmark and chevron beneath it in the original
// photo are deliberately excluded from this crop) so it doesn't duplicate the
// heading rendered right below it. Sits in normal flow above that heading (not
// as a background watermark behind it), at full opacity, mirroring the
// reference photo's own layout: mark, then wordmark beneath.
//
// The PNG's background isn't just cropped, it's genuinely cut out: alpha was
// derived per-pixel from the source photo as 255 - min(R,G,B) (the near-white
// studio backdrop has a high minimum channel -> near-zero alpha; the dark ink
// and saturated gold ribbon have a low minimum channel -> high alpha), which
// falls off smoothly rather than at a hard-edged threshold. That's what makes
// it sit directly on the page with no visible crop rectangle regardless of
// whatever's rendered behind it - no color-matched backdrop or blend-mode
// trick to keep in sync, unlike an opaque photo would need.
function LoomAtelierMark({ className }: { className?: string }) {
  return (
    <div
      className={`loom-mark-reveal relative ${className}`}
      aria-hidden="true"
    >
      <div
        className="absolute inset-0"
        style={{
          backgroundImage: "url(/brand/loom-atelier-mark.png)",
          backgroundSize: "contain",
          backgroundPosition: "center",
          backgroundRepeat: "no-repeat",
        }}
      />
    </div>
  );
}

// One small finishing touch for the mark: a gentle fade+rise reveal on load
// (staggered in just after the heading settles, not simultaneous with it).
// It's a plain CSS animation, so it's already covered by index.css's blanket
// prefers-reduced-motion rule.
const MARK_STYLES = `
  @keyframes loom-mark-reveal {
    from { opacity: 0; transform: scale(0.94) translateY(6px); }
    to { opacity: 1; transform: scale(1) translateY(0); }
  }
  .loom-mark-reveal {
    animation: loom-mark-reveal 900ms cubic-bezier(0.16, 1, 0.3, 1) both;
    animation-delay: 100ms;
  }
`;

// Scroll-driven "melt" transition for the hero surface, using native CSS
// scroll-timeline animation (animation-timeline: view()) so it runs entirely
// on the compositor — no scroll listener, no JS per frame. The bottom-heavy
// "before" shape is a full rect; the "after" shape carves an irregular,
// drippy edge into the TOP of the box (mirroring the hero exiting the
// viewport top-first as the page scrolls down), combined with a rising blur
// and fade so it reads as dissolving rather than a hard cut. Gated behind
// both a feature check and prefers-reduced-motion — unsupported/reduced-
// motion browsers just get the hero scrolling away normally, no melt, which
// is a fully acceptable, non-broken fallback.
const HERO_MELT_STYLES = `
  @keyframes hero-melt-dissolve {
    0% {
      clip-path: polygon(0% 0%, 100% 0%, 100% 100%, 0% 100%);
      filter: blur(0px);
      opacity: 1;
    }
    55% {
      filter: blur(3px);
      opacity: 0.85;
    }
    100% {
      clip-path: polygon(
        0% 34%, 9% 14%, 18% 42%, 27% 18%, 36% 46%, 45% 16%, 54% 44%,
        63% 12%, 72% 40%, 81% 20%, 90% 38%, 100% 16%,
        100% 100%, 0% 100%
      );
      filter: blur(14px);
      opacity: 0;
    }
  }
  @supports (animation-timeline: view()) {
    @media (prefers-reduced-motion: no-preference) {
      .hero-melt-surface {
        animation: hero-melt-dissolve linear both;
        animation-timeline: view();
        animation-range: exit;
      }
    }
  }
`;

export interface BrandHeroCta {
  label: string;
  onClick: () => void;
}

// Derived, theme-aware tokens (see AmbientBackground's own doc comment for
// why color-mix(... , transparent) / color-mix(... , var(--brand-text)) is
// the pattern used throughout instead of fixed hex): ACCENT leans the raw
// theme secondary toward the theme's own ink color so it stays legible
// against either a light-background preset (most of them) or a dark one
// (e.g. "Black & Rose Gold"); MUTED does the same for body-ish text by
// softening ink toward the background instead. INK/ON_INK are the theme's
// raw text/background colors, used for the solid CTA pill - since text and
// background are already a contrasting pair by construction (that's their
// whole purpose), using one as a button's fill and the other as its label
// stays legible regardless of whether the active theme is light or dark.
const ACCENT =
  "color-mix(in srgb, var(--brand-secondary, #b8860b) 85%, var(--brand-text, #1c1712))";
const INK = "var(--brand-text, #1c1712)";
const MUTED =
  "color-mix(in srgb, var(--brand-text, #1c1712) 55%, var(--brand-background, #ffffff))";
const ON_INK = "var(--brand-background, #f5f1e8)";
const glow = (pct: number) =>
  `color-mix(in srgb, var(--brand-secondary, #d4af37) ${pct}%, transparent)`;

/**
 * Premium, typography-forward brand intro — shared between the customer
 * homepage and the admin dashboard so both read as the same product rather
 * than two different apps stitched together.
 *
 * Deliberately has no background color of its own — it sits directly on the
 * shared, page-wide `AmbientBackground` (fixed behind the whole page) rather
 * than painting its own separate gradient rectangle. An earlier version gave
 * the hero its own (paler) local gradient, which produced a visible seam
 * wherever it faded back to white right where the richer page-wide ambient
 * tone was already showing through below the hero — no amount of edge
 * softening can hide a mismatch between two different colors. Letting the
 * hero be transparent means it's always exactly the same surface as the
 * rest of the page, so there's nothing left to seam against. Every caller
 * must therefore also render `<AmbientBackground />` somewhere on the page.
 */
export default function BrandHero({
  eyebrow,
  heading,
  tagline,
  cta,
}: {
  eyebrow: string;
  heading: ReactNode;
  tagline?: string | null;
  cta?: BrandHeroCta;
}) {
  return (
    <section
      // Full-bleed breakout: the page body is `mx-auto max-w-7xl`, so on any
      // viewport wider than 1280px the old `-mx-4/-mx-6/-mx-8` (which only
      // cancels that container's own padding) still left a visible strip of
      // plain page background on both sides — the hero read as "boxed" rather
      // than a real full-width section. left-1/2 + a negative 50vw margin is
      // the standard viewport-relative breakout: it centers a 100vw-wide box
      // on the actual browser viewport regardless of the parent's own width
      // or offset, so this reaches both edges on any screen size.
      className="hero-melt-surface relative left-1/2 right-1/2 -mx-[50vw] mb-8 w-screen overflow-hidden px-4 py-14 sm:px-6 sm:py-20 lg:px-8 lg:py-28"
    >
      <style>{HERO_MELT_STYLES}</style>
      <style>{MARK_STYLES}</style>

      <div className="pointer-events-none absolute inset-0" aria-hidden="true">
        {/* The diagonal light bands and gold corner blobs below are uniform
            patterns that would otherwise get truncated in a hard line right at
            this section's top/bottom edge (courtesy of its own overflow-hidden).
            Masking this whole group so it fades out approaching both edges
            means it dissolves into the shared page background instead of
            cutting off abruptly. */}
        <div
          className="absolute inset-0"
          style={{
            WebkitMaskImage:
              "linear-gradient(180deg, transparent 0%, black 20%, black 80%, transparent 100%)",
            maskImage:
              "linear-gradient(180deg, transparent 0%, black 20%, black 80%, transparent 100%)",
          }}
        >
          {/* Soft diagonal window-light bands, echoing the reference logo image's own
              soft studio lighting — blended (not flat-painted) so it reacts naturally
              to whatever's underneath instead of just striping the surface white. */}
          <div
            className="absolute inset-0"
            style={{
              background:
                "repeating-linear-gradient(112deg, rgba(255,253,247,0.55) 0px, rgba(255,253,247,0.55) 70px, transparent 70px, transparent 190px, rgba(255,253,247,0.32) 190px, rgba(255,253,247,0.32) 230px, transparent 230px, transparent 420px)",
              mixBlendMode: "soft-light",
            }}
          />
          <div
            className="absolute -left-12 -top-16 h-64 w-64 rounded-full sm:h-80 sm:w-80"
            style={{
              background: `radial-gradient(circle, ${glow(26)}, transparent 70%)`,
            }}
          />
          <div
            className="absolute -right-16 -bottom-10 h-72 w-72 rounded-full sm:h-96 sm:w-96"
            style={{
              background: `radial-gradient(circle, ${glow(21)}, transparent 70%)`,
            }}
          />
          <div
            className="absolute left-1/2 top-1/2 h-[420px] w-[420px] -translate-x-1/2 -translate-y-1/2 rounded-full sm:h-[520px] sm:w-[520px]"
            style={{
              background: `radial-gradient(circle, ${glow(16)}, transparent 72%)`,
            }}
          />
        </div>
      </div>

      <div className="relative mx-auto max-w-2xl text-center">
        <LoomAtelierMark className="pointer-events-none mx-auto h-[100px] w-[186px] sm:h-[140px] sm:w-[260px] lg:h-[170px] lg:w-[316px]" />
        <p
          className="animate-fade-in-up mt-2 text-[11px] font-semibold uppercase tracking-[0.25em] sm:text-xs"
          style={{ color: ACCENT }}
        >
          {eyebrow}
        </p>
        <h1
          className="animate-fade-in-up mt-2 font-display text-hero"
          style={{ animationDelay: "80ms" }}
        >
          <span
            className="shimmer-text"
            style={{ ["--shimmer-base" as string]: INK }}
          >
            {heading}
          </span>
        </h1>
        <div
          className="animate-scale-in mx-auto mt-4 h-px w-14 sm:w-16"
          style={{
            background: `linear-gradient(90deg, transparent, ${ACCENT}, transparent)`,
            animationDelay: "160ms",
          }}
        />
        {tagline && (
          <p
            className="animate-fade-in-up mt-4 text-sm sm:text-base"
            style={{ color: MUTED, animationDelay: "120ms" }}
          >
            {tagline}
          </p>
        )}
        {cta && (
          <button
            type="button"
            onClick={cta.onClick}
            className="animate-fade-in-up mt-12 inline-flex items-center gap-1.5 rounded-full px-6 py-2.5 text-sm font-semibold ring-1 transition-transform hover:scale-[1.02] sm:mt-16"
            style={{
              backgroundColor: INK,
              color: ON_INK,
              ["--tw-ring-color" as string]: `color-mix(in srgb, ${ACCENT} 50%, transparent)`,
              animationDelay: "200ms",
            }}
          >
            {cta.label}
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-3.5 w-3.5"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M19 9l-7 7-7-7"
              />
            </svg>
          </button>
        )}
      </div>
    </section>
  );
}
