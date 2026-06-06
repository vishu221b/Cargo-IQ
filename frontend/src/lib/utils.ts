import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

/** Conditional + conflict-free Tailwind class composition. */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/** Title-case an UPPER_SNAKE enum value, e.g. BILL_OF_LADING -> "Bill Of Lading". */
export function humanize(value: string): string {
  return value
    .toLowerCase()
    .split(/[_\s]+/)
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(" ");
}

export function formatDate(iso: string): string {
  try {
    return new Date(iso).toLocaleString(undefined, {
      dateStyle: "medium",
      timeStyle: "short",
    });
  } catch {
    return iso;
  }
}
