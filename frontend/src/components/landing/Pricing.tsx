import { useState } from "react";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { Check, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/primitives";
import { cn } from "@/lib/utils";
import { Reveal, PixelGrid } from "./effects";

type Cycle = "monthly" | "yearly";

const TIERS = [
  {
    name: "Starter",
    tagline: "For evaluating cargo-iq on your own corpus.",
    monthly: 0,
    yearly: 0,
    cta: "Start free",
    highlight: false,
    features: [
      "Mock model — zero API keys",
      "Up to 500 indexed documents",
      "REST API + MCP server",
      "Community support",
    ],
  },
  {
    name: "Pro",
    tagline: "For teams putting grounded answers into production.",
    monthly: 49,
    yearly: 39,
    cta: "Subscribe to Pro",
    highlight: true,
    features: [
      "Bring your own model — OpenAI / Anthropic / Gemini / Ollama",
      "Up to 100k indexed documents",
      "Re-ranking & hybrid search",
      "JWT auth + role-based access",
      "Priority email support",
    ],
  },
  {
    name: "Enterprise",
    tagline: "For regulated, high-volume trade operations.",
    monthly: null,
    yearly: null,
    cta: "Talk to us",
    highlight: false,
    features: [
      "Unlimited documents & throughput",
      "Self-hosted or VPC deployment",
      "SSO, audit logs & SLAs",
      "Dedicated solutions engineer",
    ],
  },
] as const;

export default function Pricing() {
  const [cycle, setCycle] = useState<Cycle>("yearly");

  return (
    <section id="pricing" className="relative mx-auto max-w-6xl px-5 py-28 md:px-10">
      <Reveal>
        <p className="text-center text-sm font-medium uppercase tracking-[0.2em] text-accent-glow">
          Pricing
        </p>
        <h2 className="mx-auto mt-3 max-w-2xl text-balance text-center text-4xl font-semibold tracking-tight text-fg sm:text-5xl">
          Start free. <span className="text-gradient">Scale when it earns its keep.</span>
        </h2>
        <p className="mx-auto mt-4 max-w-lg text-center text-lg text-muted">
          Placeholder plans — billing isn't live yet. The free tier already runs
          the full stack with no keys.
        </p>
      </Reveal>

      {/* billing toggle */}
      <Reveal delay={0.1}>
        <div className="mx-auto mt-8 flex w-fit items-center gap-1 rounded-full border border-line/[0.10] bg-surface/60 p-1">
          {(["monthly", "yearly"] as Cycle[]).map((c) => (
            <button
              key={c}
              onClick={() => setCycle(c)}
              className="relative rounded-full px-4 py-1.5 text-sm font-medium capitalize text-fg"
            >
              {cycle === c && (
                <motion.span
                  layoutId="cycle-pill"
                  className="absolute inset-0 -z-10 rounded-full bg-accent/20 ring-1 ring-accent/30"
                  transition={{ type: "spring", stiffness: 380, damping: 30 }}
                />
              )}
              {c}
              {c === "yearly" && (
                <span className="ml-1.5 rounded-full bg-emerald-500/15 px-1.5 py-0.5 text-[10px] font-semibold text-emerald-300">
                  −20%
                </span>
              )}
            </button>
          ))}
        </div>
      </Reveal>

      <div className="mt-12 grid grid-cols-1 items-stretch gap-6 lg:grid-cols-3">
        {TIERS.map((t, i) => {
          const price = cycle === "monthly" ? t.monthly : t.yearly;
          return (
            <Reveal key={t.name} delay={i * 0.08} className="h-full">
              <div
                className={cn(
                  "relative flex h-full flex-col overflow-hidden rounded-3xl border p-7 backdrop-blur-xl transition",
                  t.highlight
                    ? "border-accent/40 bg-surface/60 shadow-glow lg:-translate-y-3"
                    : "border-line/[0.08] bg-surface/40 hover:border-line/[0.14]",
                )}
              >
                {t.highlight && <PixelGrid className="opacity-40" />}
                {t.highlight && (
                  <span className="absolute right-5 top-5 inline-flex items-center gap-1 rounded-full bg-accent/20 px-2.5 py-1 text-[11px] font-semibold text-accent-glow ring-1 ring-accent/30">
                    <Sparkles className="h-3 w-3" /> Most popular
                  </span>
                )}

                <h3 className="text-lg font-semibold tracking-tight text-fg">{t.name}</h3>
                <p className="mt-1.5 min-h-[2.5rem] text-sm text-muted">{t.tagline}</p>

                <div className="mt-5 flex items-end gap-1">
                  {price === null ? (
                    <span className="text-3xl font-semibold tracking-tight text-fg">Custom</span>
                  ) : (
                    <>
                      <span className="text-5xl font-semibold tracking-tight text-fg">
                        ${price}
                      </span>
                      <span className="mb-1.5 text-sm text-faint">/mo</span>
                    </>
                  )}
                </div>
                {cycle === "yearly" && price !== null && price > 0 && (
                  <p className="mt-1 text-xs text-emerald-300">Billed annually</p>
                )}

                <Link to="/login" className="mt-6">
                  <Button
                    className="w-full"
                    variant={t.highlight ? "primary" : "subtle"}
                  >
                    {t.cta}
                  </Button>
                </Link>

                <ul className="mt-7 space-y-3">
                  {t.features.map((f) => (
                    <li key={f} className="flex items-start gap-2.5 text-sm text-fg">
                      <Check
                        className={cn(
                          "mt-0.5 h-4 w-4 shrink-0",
                          t.highlight ? "text-accent-glow" : "text-cyanish",
                        )}
                      />
                      <span className="text-muted">{f}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </Reveal>
          );
        })}
      </div>
    </section>
  );
}
