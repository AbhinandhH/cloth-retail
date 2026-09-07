import { Link } from "react-router-dom";
import { useSiteConfig } from "../context/SiteConfigContext";

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
      className="relative overflow-hidden border-t border-[#e7d9b8]"
      style={{ background: "linear-gradient(180deg, #ffffff 0%, #faf6ec 55%, #f6eeda 100%)" }}
    >
      {/* Thin gold thread across the very top edge, echoing the hero's own divider
          under its heading — the one recurring "brand line" motif tying every
          section of the page back to the same premium theme. */}
      <div
        className="absolute inset-x-0 top-0 h-px"
        style={{ background: "linear-gradient(90deg, transparent, #b8860b, transparent)" }}
      />
      {/* Same soft gold corner wash used in the hero, kept very faint here since
          the footer is a quiet closing note rather than the main event. */}
      <div
        className="pointer-events-none absolute -left-16 -top-20 h-64 w-64 rounded-full"
        style={{ background: "radial-gradient(circle, rgba(212,175,55,0.10), transparent 70%)" }}
        aria-hidden="true"
      />
      <div
        className="pointer-events-none absolute -right-20 bottom-0 h-72 w-72 rounded-full"
        style={{ background: "radial-gradient(circle, rgba(212,175,55,0.08), transparent 70%)" }}
        aria-hidden="true"
      />

      <div className="relative mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
        <div className="grid grid-cols-2 gap-x-8 gap-y-12 sm:grid-cols-4">
          <div className="col-span-2 sm:col-span-1">
            <span className="font-display text-2xl font-semibold tracking-tight text-[#1c1712]">
              {businessName ?? <>Loom Atelier Studio</>}
            </span>
            <div
              className="mt-3 h-px w-12"
              style={{ background: "linear-gradient(90deg, #b8860b, transparent)" }}
            />
            <p className="mt-3 max-w-[26ch] text-sm leading-relaxed text-[#6b5f4f]">
              {tagline}
            </p>
          </div>

          <div>
            <h3 className="text-xs font-semibold uppercase tracking-[0.15em] text-[#a9781f]">
              Shop
            </h3>
            <ul className="mt-4 space-y-2.5 text-sm text-[#4a4238]">
              <li>
                <Link to="/" className="transition-colors hover:text-[#1c1712]">
                  All products
                </Link>
              </li>
              <li>
                <Link
                  to="/?category=new"
                  className="transition-colors hover:text-[#1c1712]"
                >
                  New arrivals
                </Link>
              </li>
            </ul>
          </div>

          {(contactEmail || contactPhone) && (
            <div>
              <h3 className="text-xs font-semibold uppercase tracking-[0.15em] text-[#a9781f]">
                Support
              </h3>
              <ul className="mt-4 space-y-2.5 text-sm text-[#4a4238]">
                {contactEmail && (
                  <li>
                    <a
                      href={`mailto:${contactEmail}`}
                      className="transition-colors hover:text-[#1c1712]"
                    >
                      {contactEmail}
                    </a>
                  </li>
                )}
                {contactPhone && (
                  <li>
                    <a
                      href={`tel:${contactPhone}`}
                      className="transition-colors hover:text-[#1c1712]"
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
              <h3 className="text-xs font-semibold uppercase tracking-[0.15em] text-[#a9781f]">
                Follow
              </h3>
              <ul className="mt-4 space-y-2.5 text-sm text-[#4a4238]">
                {instagramUrl && (
                  <li>
                    <a
                      href={instagramUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="inline-flex items-center gap-2 transition-colors hover:text-[#1c1712]"
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
                      className="inline-flex items-center gap-2 transition-colors hover:text-[#1c1712]"
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
                      className="inline-flex items-center gap-2 transition-colors hover:text-[#1c1712]"
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
          className="mt-12 pt-6 text-center text-xs text-[#8a7d68] sm:text-left"
          style={{ borderTop: "1px solid rgba(184,134,11,0.2)" }}
        >
          {footerText}
        </div>
      </div>
    </footer>
  );
}
