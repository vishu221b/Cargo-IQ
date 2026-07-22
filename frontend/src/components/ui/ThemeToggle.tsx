import { motion } from "framer-motion";
import { Moon, Sun } from "lucide-react";
import { useTheme } from "@/theme/ThemeContext";
import { cn } from "@/lib/utils";

/** A compact, animated light/dark switch. */
export default function ThemeToggle({ className }: { className?: string }) {
  const { theme, toggle } = useTheme();
  const isDark = theme === "dark";

  return (
    <button
      onClick={toggle}
      aria-label={`Switch to ${isDark ? "light" : "dark"} theme`}
      title={`Switch to ${isDark ? "light" : "dark"} theme`}
      className={cn(
        "relative inline-flex h-8 w-14 items-center rounded-full border border-line/[0.10] bg-surface2/80 px-1 transition",
        className,
      )}
    >
      <motion.span
        layout
        transition={{ type: "spring", stiffness: 500, damping: 32 }}
        className={cn(
          "grid h-6 w-6 place-items-center rounded-full bg-gradient-to-br shadow-glow",
          isDark ? "from-accent to-orange-500" : "from-amber-400 to-orange-500",
        )}
        style={{ marginLeft: isDark ? 0 : "auto" }}
      >
        {isDark ? (
          <Moon className="h-3.5 w-3.5 text-white" />
        ) : (
          <Sun className="h-3.5 w-3.5 text-white" />
        )}
      </motion.span>
    </button>
  );
}
