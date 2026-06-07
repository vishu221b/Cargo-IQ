import {
  forwardRef,
  type ButtonHTMLAttributes,
  type HTMLAttributes,
  type InputHTMLAttributes,
  type ReactNode,
  type SelectHTMLAttributes,
  type TextareaHTMLAttributes,
} from "react";
import { Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";

/* ----------------------------- Button ----------------------------- */

type ButtonVariant = "primary" | "ghost" | "subtle" | "danger";
type ButtonSize = "sm" | "md";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  loading?: boolean;
  icon?: ReactNode;
}

const VARIANTS: Record<ButtonVariant, string> = {
  primary:
    "bg-accent text-white shadow-glow hover:bg-accent-soft active:translate-y-px",
  ghost:
    "bg-transparent text-muted hover:bg-line/[0.06] hover:text-fg",
  subtle:
    "bg-line/[0.05] text-fg border border-line/[0.08] hover:bg-line/[0.08]",
  danger:
    "bg-rose-500/90 text-white hover:bg-rose-500 active:translate-y-px",
};

const SIZES: Record<ButtonSize, string> = {
  sm: "h-9 px-3.5 text-sm gap-1.5",
  md: "h-11 px-5 text-sm gap-2",
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    { className, variant = "primary", size = "md", loading, icon, children, disabled, ...props },
    ref,
  ) => (
    <button
      ref={ref}
      disabled={disabled || loading}
      className={cn(
        "inline-flex select-none items-center justify-center rounded-xl font-medium transition-all duration-150",
        "focus-visible:focus-ring disabled:pointer-events-none disabled:opacity-50",
        VARIANTS[variant],
        SIZES[size],
        className,
      )}
      {...props}
    >
      {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : icon}
      {children}
    </button>
  ),
);
Button.displayName = "Button";

/* ----------------------------- Card ------------------------------- */

export function Card({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn("glass rounded-2xl shadow-card", className)}
      {...props}
    />
  );
}

/* ----------------------------- Inputs ----------------------------- */

const FIELD =
  "w-full rounded-xl bg-surface/70 border border-line/[0.08] px-3.5 py-2.5 text-sm text-fg " +
  "placeholder:text-faint transition focus:border-accent/50 focus:outline-none focus:ring-2 focus:ring-accent/30";

export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(
  ({ className, ...props }, ref) => (
    <input ref={ref} className={cn(FIELD, className)} {...props} />
  ),
);
Input.displayName = "Input";

export const Textarea = forwardRef<
  HTMLTextAreaElement,
  TextareaHTMLAttributes<HTMLTextAreaElement>
>(({ className, ...props }, ref) => (
  <textarea ref={ref} className={cn(FIELD, "resize-y leading-relaxed", className)} {...props} />
));
Textarea.displayName = "Textarea";

export const Select = forwardRef<
  HTMLSelectElement,
  SelectHTMLAttributes<HTMLSelectElement>
>(({ className, children, ...props }, ref) => (
  <select ref={ref} className={cn(FIELD, "appearance-none pr-8", className)} {...props}>
    {children}
  </select>
));
Select.displayName = "Select";

export function Field({ label, children, hint }: { label: string; children: ReactNode; hint?: string }) {
  return (
    <label className="block space-y-1.5">
      <span className="text-xs font-medium uppercase tracking-wider text-muted">{label}</span>
      {children}
      {hint && <span className="block text-xs text-faint">{hint}</span>}
    </label>
  );
}

/* ----------------------------- Badge ------------------------------ */

const BADGE_TONES = {
  default: "bg-line/[0.06] text-fg border-line/[0.08]",
  accent: "bg-accent/15 text-accent-glow border-accent/30",
  cyan: "bg-cyanish/10 text-cyanish border-cyanish/30",
  emerald: "bg-emerald-500/10 text-emerald-300 border-emerald-500/30",
  rose: "bg-rose-500/10 text-rose-300 border-rose-500/30",
  amber: "bg-amber-500/10 text-amber-300 border-amber-500/30",
} as const;

export function Badge({
  children,
  tone = "default",
  className,
}: {
  children: ReactNode;
  tone?: keyof typeof BADGE_TONES;
  className?: string;
}) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-xs font-medium",
        BADGE_TONES[tone],
        className,
      )}
    >
      {children}
    </span>
  );
}

/* ----------------------------- Spinner ---------------------------- */

export function Spinner({ className }: { className?: string }) {
  return <Loader2 className={cn("h-5 w-5 animate-spin text-accent", className)} />;
}
