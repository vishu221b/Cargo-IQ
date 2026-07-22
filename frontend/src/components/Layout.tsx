import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import {
  LayoutDashboard,
  FileStack,
  MessageSquareText,
  BookMarked,
  Settings as SettingsIcon,
  LogOut,
  Boxes,
  ShieldCheck,
} from "lucide-react";
import { motion } from "framer-motion";
import { useAuth } from "@/auth/AuthContext";
import { Badge } from "@/components/ui/primitives";
import ThemeToggle from "@/components/ui/ThemeToggle";
import { cn } from "@/lib/utils";

const NAV = [
  { to: "/app", label: "Dashboard", icon: LayoutDashboard, end: true },
  { to: "/app/documents", label: "Documents", icon: FileStack, end: false },
  { to: "/app/query", label: "Ask the corpus", icon: MessageSquareText, end: false },
  { to: "/app/reference", label: "Reference", icon: BookMarked, end: false },
  { to: "/app/settings", label: "Settings", icon: SettingsIcon, end: false },
];

export default function Layout() {
  const { user, isAdmin, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <div className="flex min-h-screen">
      {/* Sidebar */}
      <aside className="sticky top-0 hidden h-screen w-64 shrink-0 flex-col border-r border-line/[0.06] bg-surface/40 px-4 py-6 backdrop-blur-xl md:flex">
        <Link to="/" className="flex items-center gap-2.5 px-2" title="Back to home">
          <div className="grid h-9 w-9 place-items-center rounded-xl bg-gradient-to-br from-accent to-cyanish shadow-glow">
            <Boxes className="h-5 w-5 text-white" />
          </div>
          <div>
            <p className="text-sm font-semibold tracking-tight text-fg">cargo-iq</p>
            <p className="text-[11px] text-faint">trade intelligence</p>
          </div>
        </Link>

        <nav className="mt-8 flex flex-1 flex-col gap-1">
          {NAV.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                cn(
                  "group relative flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition",
                  isActive
                    ? "text-fg"
                    : "text-muted hover:bg-line/[0.04] hover:text-fg",
                )
              }
            >
              {({ isActive }) => (
                <>
                  {isActive && (
                    <motion.span
                      layoutId="nav-active"
                      className="absolute inset-0 -z-10 rounded-xl bg-line/[0.06] ring-1 ring-line/[0.08]"
                      transition={{ type: "spring", stiffness: 400, damping: 32 }}
                    />
                  )}
                  <item.icon className="h-[18px] w-[18px]" />
                  {item.label}
                </>
              )}
            </NavLink>
          ))}
        </nav>

        <div className="mt-auto space-y-3">
          <div className="flex items-center justify-between px-1">
            <span className="text-xs font-medium uppercase tracking-wider text-faint">
              Theme
            </span>
            <ThemeToggle />
          </div>
          <div className="hairline h-px" />
          <div className="flex items-center justify-between px-1">
            <div className="min-w-0">
              <p className="truncate text-sm font-medium text-fg">
                {user?.username}
              </p>
              <div className="mt-1">
                {isAdmin ? (
                  <Badge tone="accent">
                    <ShieldCheck className="h-3 w-3" /> Admin
                  </Badge>
                ) : (
                  <Badge>User</Badge>
                )}
              </div>
            </div>
            <button
              onClick={() => {
                logout();
                navigate("/login");
              }}
              className="rounded-lg p-2 text-faint transition hover:bg-line/[0.05] hover:text-rose-300"
              title="Sign out"
            >
              <LogOut className="h-[18px] w-[18px]" />
            </button>
          </div>
        </div>
      </aside>

      {/* Main */}
      <main className="relative flex-1">
        <div className="mx-auto w-full max-w-6xl px-5 py-8 md:px-10 md:py-12">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
