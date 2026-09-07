import { toMediaUrl } from "../api/client";

export default function BrandMark({
  businessName,
  logoUrl,
}: {
  businessName: string | null | undefined;
  logoUrl: string | null | undefined;
}) {
  const resolvedLogo = toMediaUrl(logoUrl);
  if (resolvedLogo) {
    return (
      <img
        src={resolvedLogo}
        alt={businessName ?? "Logo"}
        className="h-9 w-auto object-contain"
      />
    );
  }
  if (businessName) {
    return (
      <span
        className="shimmer-text text-xl font-bold tracking-tight"
        style={{ ["--shimmer-base" as string]: "#18181b" }}
      >
        {businessName}
      </span>
    );
  }
  return (
    <span
      className="shimmer-text text-xl font-bold tracking-tight"
      style={{ ["--shimmer-base" as string]: "#18181b" }}
    >
      Loom Atelier Studio
    </span>
  );
}
