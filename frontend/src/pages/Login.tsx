import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";
import { Boxes, LogIn, UserPlus, ArrowRight } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { useToast } from "@/components/ui/Toast";
import { AuroraBackground, Spotlight } from "@/components/ui/Backgrounds";
import { Button, Field, Input } from "@/components/ui/primitives";
import { ApiError } from "@/lib/api";

type Mode = "login" | "register";

export default function Login() {
  const { user, login, register } = useAuth();
  const navigate = useNavigate();
  const location = useLocation() as { state?: { from?: { pathname?: string } } };
  const toast = useToast();

  const [mode, setMode] = useState<Mode>("login");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);

  // Already signed in -> bounce to the app.
  if (user) {
    const to = location.state?.from?.pathname ?? "/";
    return <NavigateOnce to={to} navigate={navigate} />;
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    try {
      if (mode === "login") {
        await login(username.trim(), password);
        toast.success("Welcome back.");
      } else {
        await register(username.trim(), password);
        toast.success("Account created — you're in.");
      }
      navigate(location.state?.from?.pathname ?? "/", { replace: true });
    } catch (err) {
      const msg =
        err instanceof ApiError ? err.message : "Something went wrong. Try again.";
      toast.error(msg);
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuroraBackground className="grid min-h-screen place-items-center px-5">
      <Spotlight />
      <motion.div
        initial={{ opacity: 0, y: 18 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, ease: "easeOut" }}
        className="glass-strong w-full max-w-md rounded-3xl p-8 shadow-card"
      >
        <div className="mb-7 flex items-center gap-3">
          <div className="grid h-11 w-11 place-items-center rounded-2xl bg-gradient-to-br from-accent to-cyanish shadow-glow">
            <Boxes className="h-6 w-6 text-white" />
          </div>
          <div>
            <h1 className="text-lg font-semibold tracking-tight text-white">cargo-iq</h1>
            <p className="text-xs text-slate-400">
              Grounded intelligence for trade documents
            </p>
          </div>
        </div>

        <div className="mb-6 grid grid-cols-2 gap-1 rounded-xl bg-ink-850/60 p-1">
          {(["login", "register"] as Mode[]).map((m) => (
            <button
              key={m}
              onClick={() => setMode(m)}
              className="relative rounded-lg py-2 text-sm font-medium capitalize text-slate-300 transition"
            >
              {mode === m && (
                <motion.span
                  layoutId="auth-tab"
                  className="absolute inset-0 -z-10 rounded-lg bg-white/[0.08] ring-1 ring-white/[0.08]"
                  transition={{ type: "spring", stiffness: 380, damping: 30 }}
                />
              )}
              {m === "login" ? "Sign in" : "Register"}
            </button>
          ))}
        </div>

        <form onSubmit={submit} className="space-y-4">
          <Field label="Username">
            <Input
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="admin"
              autoComplete="username"
              required
              minLength={3}
            />
          </Field>
          <Field
            label="Password"
            hint={mode === "register" ? "At least 8 characters." : undefined}
          >
            <Input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              autoComplete={mode === "login" ? "current-password" : "new-password"}
              required
              minLength={8}
            />
          </Field>

          <Button type="submit" loading={busy} className="w-full" icon={mode === "login" ? <LogIn className="h-4 w-4" /> : <UserPlus className="h-4 w-4" />}>
            {mode === "login" ? "Sign in" : "Create account"}
            {!busy && <ArrowRight className="h-4 w-4 opacity-70" />}
          </Button>
        </form>

        <AnimatePresence>
          {mode === "login" && (
            <motion.p
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: "auto" }}
              exit={{ opacity: 0, height: 0 }}
              className="mt-5 text-center text-xs text-slate-500"
            >
              Dev bootstrap admin: <span className="font-mono text-slate-400">admin / admin12345</span>
            </motion.p>
          )}
        </AnimatePresence>
      </motion.div>
    </AuroraBackground>
  );
}

function NavigateOnce({
  to,
  navigate,
}: {
  to: string;
  navigate: ReturnType<typeof useNavigate>;
}) {
  queueMicrotask(() => navigate(to, { replace: true }));
  return null;
}
