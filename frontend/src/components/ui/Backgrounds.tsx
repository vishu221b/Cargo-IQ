import { type ReactNode } from "react";
import { cn } from "@/lib/utils";

/**
 * Aurora background — soft, slowly drifting colour fields behind content.
 * Inspired by the Aceternity / 21st.dev aesthetic, hand-rolled in Tailwind so
 * there's no extra dependency.
 */
export function AuroraBackground({
  children,
  className,
}: {
  children?: ReactNode;
  className?: string;
}) {
  return (
    <div className={cn("relative overflow-hidden", className)}>
      <div className="pointer-events-none absolute inset-0 -z-10">
        <div className="absolute -left-1/4 -top-1/4 h-[60vh] w-[60vh] animate-aurora rounded-full bg-accent/25 blur-[120px]" />
        <div className="absolute -right-1/4 top-1/3 h-[55vh] w-[55vh] animate-aurora rounded-full bg-cyanish/20 blur-[120px] [animation-delay:-6s]" />
        <div className="absolute bottom-0 left-1/3 h-[45vh] w-[45vh] animate-aurora rounded-full bg-fuchsia-500/15 blur-[120px] [animation-delay:-12s]" />
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top,transparent_30%,rgba(7,9,18,0.9))]" />
      </div>
      {children}
    </div>
  );
}

/** A faint dot grid — adds depth to dashboards without stealing attention. */
export function GridPattern({ className }: { className?: string }) {
  return (
    <div
      className={cn(
        "pointer-events-none absolute inset-0 -z-10 opacity-[0.4]",
        "[background-image:radial-gradient(rgba(255,255,255,0.08)_1px,transparent_1px)]",
        "[background-size:22px_22px]",
        "[mask-image:radial-gradient(ellipse_at_center,black,transparent_75%)]",
        className,
      )}
    />
  );
}

/** Spotlight glow — a conic shimmer used on hero/auth panels. */
export function Spotlight({ className }: { className?: string }) {
  return (
    <div
      className={cn(
        "pointer-events-none absolute -top-40 left-1/2 -z-10 h-[40rem] w-[40rem] -translate-x-1/2",
        "rounded-full bg-[conic-gradient(from_180deg_at_50%_50%,rgba(124,92,255,0.18),rgba(34,211,238,0.12),transparent_60%)]",
        "blur-3xl",
        className,
      )}
    />
  );
}
