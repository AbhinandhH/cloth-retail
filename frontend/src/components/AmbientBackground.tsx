// Full-bleed, page-wide ambient theme — a single cohesive marble/linen-like
// surface (layered accent-tinted radial washes + a faint film-grain overlay
// for tactile, "printed" quality) that sits behind the ENTIRE page rather
// than being boxed inside any one section. `position: fixed` so it holds
// still while the page scrolls over it (a parallax depth cue) — safe as long
// as no ancestor has a transform/filter/backdrop-blur that would turn it
// into a containing block for this `fixed` element (see Layout.tsx /
// AdminHome.tsx). Shared by the customer Home page and the admin dashboard
// so both read as the same surface — any section painting its own opaque
// background over this is what creates a visible seam (see BrandHero's own
// doc comment).
//
// Every color here is mixed from the active theme's --brand-secondary/
// --brand-background/--brand-text (see SiteConfigContext, which sets those
// from whichever preset the admin picked in Site configuration) rather than
// a fixed gold/cream palette, so switching the theme actually re-paints this
// - not just a button or two. color-mix(... X%, transparent) is the standard
// way to fake an rgba() of a CSS variable's color (browsers can't
// interpolate a var() straight into rgba()'s comma syntax); mixing toward
// the theme's own background/text keeps the wash coherent whether that
// theme is a light page (most presets) or a dark one (e.g. "Black & Rose
// Gold", where background is near-black and text is near-white).
const AMBIENT_STYLES = `
  .home-ambient-bg {
    position: fixed;
    inset: 0;
    z-index: -1;
    pointer-events: none;
    overflow: hidden;
    background:
      radial-gradient(1100px 620px at 12% -8%, color-mix(in srgb, var(--brand-secondary, #d4af37) 16%, transparent), transparent 60%),
      radial-gradient(900px 720px at 108% 12%, color-mix(in srgb, var(--brand-secondary, #d4af37) 13%, transparent), transparent 65%),
      radial-gradient(820px 900px at 50% 115%, color-mix(in srgb, var(--brand-text, #1c1712) 6%, transparent), transparent 70%),
      linear-gradient(
        165deg,
        var(--brand-background, #faf5e8) 0%,
        color-mix(in srgb, var(--brand-secondary, #d4af37) 24%, var(--brand-background, #faf5e8)) 35%,
        color-mix(in srgb, var(--brand-secondary, #d4af37) 45%, var(--brand-background, #faf5e8)) 65%,
        color-mix(in srgb, var(--brand-secondary, #d4af37) 22%, var(--brand-background, #faf5e8)) 100%
      );
  }
  .home-ambient-bg::after {
    content: '';
    position: absolute;
    inset: 0;
    opacity: 0.035;
    mix-blend-mode: multiply;
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='140' height='140'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='2' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  }
  .home-ambient-glow {
    position: absolute;
    top: -10%;
    left: 50%;
    width: 140vmax;
    height: 140vmax;
    transform: translate(-50%, 0);
    background: radial-gradient(circle, color-mix(in srgb, var(--brand-secondary, #d4af37) 10%, transparent), transparent 55%);
  }
  @keyframes home-ambient-flow {
    0% { transform: translate(-52%, -2%) rotate(0deg); }
    100% { transform: translate(-48%, 4%) rotate(4deg); }
  }
  @supports (animation-timeline: scroll()) {
    @media (prefers-reduced-motion: no-preference) {
      .home-ambient-glow {
        animation: home-ambient-flow linear both;
        animation-timeline: scroll(root);
      }
    }
  }
`;

export default function AmbientBackground() {
  return (
    <>
      <style>{AMBIENT_STYLES}</style>
      <div className="home-ambient-bg" aria-hidden="true">
        <div className="home-ambient-glow" />
      </div>
    </>
  );
}
