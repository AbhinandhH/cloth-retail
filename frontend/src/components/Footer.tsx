import { Link } from "react-router-dom";
import { useSiteConfig } from "../context/SiteConfigContext";

// Local, theme-derived tokens (same color-mix pattern as AmbientBackground/
// BrandHero) scoped to .footer-surface so Tailwind's arbitrary-value syntax
// can reference a single var() each (text-[var(--footer-accent)] etc.)
// rather than a full color-mix(...) expression with commas, which arbitrary
// values don't parse reliably.
const FOOTER_STYLES = `
  .footer-surface {
    --footer-border: color-mix(in srgb, var(--brand-secondary, #b8860b) 35%, var(--brand-background, #ffffff));
    --footer-accent: color-mix(in srgb, var(--brand-secondary, #b8860b) 85%, var(--brand-text, #1c1712));
    --footer-ink: var(--brand-text, #1c1712);
    --footer-muted: color-mix(in srgb, var(--brand-text, #1c1712) 55%, var(--brand-background, #ffffff));
    --footer-link: color-mix(in srgb, var(--brand-text, #1c1712) 75%, var(--brand-background, #ffffff));
    --footer-copyright: color-mix(in srgb, var(--brand-text, #1c1712) 45%, var(--brand-background, #ffffff));
  }
`;

function InstagramIcon() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <rect x="3" y="3" width="18" height="18" rx="5" strokeWidth={1.75} />
      <circle cx="12" cy="12" r="4" strokeWidth={1.75} />
      <circle cx="17.2" cy="6.8" r="1" fill="currentColor" stroke="none" />
    </svg>
  );
}

function WhatsAppIcon() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={1.75}
        d="M4 20l1.3-3.9A8 8 0 1112 20a8 8 0 01-4.7-1.5L4 20z"
      />
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={1.75}
        d="M9 9.5c0 3 2.5 5.5 5.5 5.5.4 0 .8-.3.8-.7v-1c0-.3-.2-.6-.5-.7l-1.6-.5c-.3-.1-.6 0-.7.2l-.3.5c-1-.5-1.9-1.4-2.4-2.4l.5-.3c.2-.2.3-.5.2-.7l-.5-1.6c-.1-.3-.4-.5-.7-.5h-1c-.4 0-.7.4-.7.8V9.5z"
      />
    </svg>
  );
}

function FacebookIcon() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={1.75}
        d="M14 9h2V6h-2c-1.7 0-3 1.3-3 3v2H9v3h2v6h3v-6h2.2l.8-3H14V9.3c0-.2.1-.3.3-.3H14z"
      />
    </svg>
  );
}

export default function Footer() {
  const { config } = useSiteConfig();

  const businessName = config?.businessName ?? null;
  const tagline = config?.tagline ?? "Clothing that fits your life.";
  const footerText =
    config?.footerText ??
    `© ${new Date().getFullYear()} LoomAteLier All rights reserved.`;
  const contactEmail = config?.contactEmail ?? null;
  const contactPhone = config?.contactPhone ?? null;
  const instagramUrl = config?.instagramUrl ?? null;
  const whatsappNumber = config?.whatsappNumber ?? null;
  const facebookUrl = config?.facebookUrl ?? null;

  const hasSocial = Boolean(instagramUrl || whatsappNumber || facebookUrl);

  return (
    <footer
      className="footer-surface relative overflow-hidden border-t border-[var(--footer-border)]"
      style={{
        background:
          "linear-gradient(180deg, var(--brand-background, #ffffff) 0%, color-mix(in srgb, var(--brand-secondary, #b8860b) 8%, var(--brand-background, #ffffff)) 55%, color-mix(in srgb, var(--brand-secondary, #b8860b) 14%, var(--brand-background, #ffffff)) 100%)",
      }}
    >
      <style>{FOOTER_STYLES}</style>
      {/* Thin accent thread across the very top edge, echoing the hero's own divider
          under its heading — the one recurring "brand line" motif tying every
          section of the page back to the same theme. */}
      <div
        className="absolute inset-x-0 top-0 h-px"
        style={{ background: "linear-gradient(90deg, transparent, var(--footer-accent), transparent)" }}
      />
      {/* Same soft accent corner wash used in the hero, kept very faint here since
          the footer is a quiet closing note rather than the main event. */}
      <div
        className="pointer-events-none absolute -left-16 -top-20 h-64 w-64 rounded-full"
        style={{ background: "radial-gradient(circle, color-mix(in srgb, var(--brand-secondary, #d4af37) 10%, transparent), transparent 70%)" }}
        aria-hidden="true"
      />
      <div
        className="pointer-events-none absolute -right-20 bottom-0 h-72 w-72 rounded-full"
        style={{ background: "radial-gradient(circle, color-mix(in srgb, var(--brand-secondary, #d4af37) 8%, transparent), transparent 70%)" }}
        aria-hidden="true"
      />

      <div className="relative mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
        <div className="grid grid-cols-2 gap-x-8 gap-y-12 sm:grid-cols-4">
          <div className="col-span-2 sm:col-span-1">
            <span className="font-display text-2xl font-semibold tracking-tight text-[var(--footer-ink)]">
              {businessName ?? <>Loom Atelier Studio</>}
            </span>
            <div
              className="mt-3 h-px w-12"
              style={{ background: "linear-gradient(90deg, var(--footer-accent), transparent)" }}
            />
            <p className="mt-3 max-w-[26ch] text-sm leading-relaxed text-[var(--footer-muted)]">
              {tagline}
            </p>
          </div>

          <div>
            <h3 className="text-xs font-semibold uppercase tracking-[0.15em] text-[var(--footer-accent)]">
              Shop
            </h3>
            <ul className="mt-4 space-y-2.5 text-sm text-[var(--footer-link)]">
              <li>
                <Link to="/" className="transition-colors hover:text-[var(--footer-ink)]">
                  All products
                </Link>
              </li>
              <li>
                <Link
                  to="/?category=new"
                  className="transition-colors hover:text-[var(--footer-ink)]"
                >
                  New arrivals
                </Link>
              </li>
            </ul>
          </div>

          {(contactEmail || contactPhone) && (
            <div>
              <h3 className="text-xs font-semibold uppercase tracking-[0.15em] text-[var(--footer-accent)]">
                Support
              </h3>
              <ul className="mt-4 space-y-2.5 text-sm text-[var(--footer-link)]">
                {contactEmail && (
                  <li>
                    <a
                      href={`mailto:${contactEmail}`}
                      className="transition-colors hover:text-[var(--footer-ink)]"
                    >
                      {contactEmail}
                    </a>
                  </li>
                )}
                {contactPhone && (
                  <li>
                    <a
                      href={`tel:${contactPhone}`}
                      className="transition-colors hover:text-[var(--footer-ink)]"
                    >
                      {contactPhone}
                    </a>
                  </li>
                )}
              </ul>
            </div>
          )}

          {hasSocial && (
            <div>
              <h3 className="text-xs font-semibold uppercase tracking-[0.15em] text-[var(--footer-accent)]">
                Follow
              </h3>
              <ul className="mt-4 space-y-2.5 text-sm text-[var(--footer-link)]">
                {instagramUrl && (
                  <li>
                    <a
                      href={instagramUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="inline-flex items-center gap-2 transition-colors hover:text-[var(--footer-ink)]"
                    >
                      <InstagramIcon />
                      Instagram
                    </a>
                  </li>
                )}
                {whatsappNumber && (
                  <li>
                    <a
                      href={`https://wa.me/${whatsappNumber.replace(/[^\d]/g, "")}`}
                      target="_blank"
                      rel="noreferrer"
                      className="inline-flex items-center gap-2 transition-colors hover:text-[var(--footer-ink)]"
                    >
                      <WhatsAppIcon />
                      WhatsApp
                    </a>
                  </li>
                )}
                {facebookUrl && (
                  <li>
                    <a
                      href={facebookUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="inline-flex items-center gap-2 transition-colors hover:text-[var(--footer-ink)]"
                    >
                      <FacebookIcon />
                      Facebook
                    </a>
                  </li>
                )}
              </ul>
            </div>
          )}
        </div>

        <div
          className="mt-12 pt-6 text-center text-xs text-[var(--footer-copyright)] sm:text-left"
          style={{ borderTop: "1px solid color-mix(in srgb, var(--brand-secondary, #b8860b) 20%, transparent)" }}
        >
          {footerText}
        </div>
      </div>
    </footer>
  );
}
