import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import {
  FileStack,
  Layers,
  Ship,
  ArrowUpRight,
  MessageSquareText,
  Upload,
  Sparkles,
} from "lucide-react";
import { api, ApiError } from "@/lib/api";
import type { Overview } from "@/lib/types";
import { useAuth } from "@/auth/AuthContext";
import { Card, Badge, Spinner } from "@/components/ui/primitives";
import { GridPattern } from "@/components/ui/Backgrounds";
import { cn, humanize } from "@/lib/utils";

export default function Dashboard() {
  const { user, isAdmin } = useAuth();
  const [data, setData] = useState<Overview | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .overview()
      .then(setData)
      .catch((e) =>
        setError(e instanceof ApiError ? e.message : "Failed to load overview"),
      );
  }, []);

  const typeEntries = useMemo(
    () => Object.entries(data?.documentsByType ?? {}).sort((a, b) => b[1] - a[1]),
    [data],
  );
  const incotermEntries = useMemo(
    () => Object.entries(data?.documentsByIncoterm ?? {}).sort((a, b) => b[1] - a[1]),
    [data],
  );

  return (
    <div className="space-y-8">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-sm text-slate-400">
            Welcome back, <span className="text-slate-200">{user?.username}</span>
          </p>
          <h1 className="mt-1 text-3xl font-semibold tracking-tight text-white">
            Corpus <span className="text-gradient">overview</span>
          </h1>
        </div>
        <Badge tone="cyan">
          <Sparkles className="h-3.5 w-3.5" /> Live from /api/v1/overview
        </Badge>
      </header>

      {error && (
        <Card className="border-rose-500/20 p-5 text-sm text-rose-300">{error}</Card>
      )}

      {!data && !error ? (
        <div className="grid place-items-center py-24">
          <Spinner className="h-7 w-7" />
        </div>
      ) : (
        data && (
          <>
            <div className="grid gap-4 sm:grid-cols-3">
              <StatCard
                icon={FileStack}
                label="Documents indexed"
                value={data.totalDocuments}
                tone="accent"
                delay={0}
              />
              <StatCard
                icon={Layers}
                label="Document types"
                value={typeEntries.length}
                tone="cyan"
                delay={0.08}
              />
              <StatCard
                icon={Ship}
                label="INCOTERMs tracked"
                value={incotermEntries.length}
                tone="emerald"
                delay={0.16}
              />
            </div>

            <div className="grid gap-5 lg:grid-cols-2">
              <Breakdown
                title="By document type"
                empty="No documents yet — ingest a few to see the split."
                entries={typeEntries.map(([k, v]) => [humanize(k), v])}
                barClass="from-accent to-accent-soft"
              />
              <Breakdown
                title="By INCOTERM"
                empty="No INCOTERMs extracted yet."
                entries={incotermEntries}
                barClass="from-cyanish to-sky-400"
              />
            </div>

            <div className="grid gap-4 sm:grid-cols-3">
              <QuickAction to="/query" icon={MessageSquareText} label="Ask the corpus" sub="Run a grounded RAG query" />
              <QuickAction
                to="/documents"
                icon={isAdmin ? Upload : FileStack}
                label={isAdmin ? "Ingest a document" : "Browse documents"}
                sub={isAdmin ? "Chunk, embed and index" : "View the indexed corpus"}
              />
              <QuickAction to="/reference" icon={Layers} label="Reference lookup" sub="INCOTERMS & HS codes" />
            </div>
          </>
        )
      )}
    </div>
  );
}

/* --------------------------- pieces --------------------------- */

const TONE_RING = {
  accent: "from-accent/20 text-accent-glow",
  cyan: "from-cyanish/20 text-cyanish",
  emerald: "from-emerald-500/20 text-emerald-300",
} as const;

function StatCard({
  icon: Icon,
  label,
  value,
  tone,
  delay,
}: {
  icon: typeof FileStack;
  label: string;
  value: number;
  tone: keyof typeof TONE_RING;
  delay: number;
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay, duration: 0.4 }}
    >
      <Card className="relative overflow-hidden p-5">
        <GridPattern />
        <div
          className={cn(
            "grid h-10 w-10 place-items-center rounded-xl bg-gradient-to-br to-transparent",
            TONE_RING[tone],
          )}
        >
          <Icon className="h-5 w-5" />
        </div>
        <p className="mt-4 text-3xl font-semibold tracking-tight text-white">
          <CountUp value={value} />
        </p>
        <p className="mt-1 text-sm text-slate-400">{label}</p>
      </Card>
    </motion.div>
  );
}

function Breakdown({
  title,
  entries,
  barClass,
  empty,
}: {
  title: string;
  entries: [string, number][];
  barClass: string;
  empty: string;
}) {
  const max = Math.max(1, ...entries.map(([, v]) => v));
  return (
    <Card className="p-6">
      <h2 className="text-sm font-semibold uppercase tracking-wider text-slate-400">
        {title}
      </h2>
      <div className="mt-5 space-y-3.5">
        {entries.length === 0 ? (
          <p className="text-sm text-slate-500">{empty}</p>
        ) : (
          entries.map(([label, value], i) => (
            <div key={label} className="space-y-1.5">
              <div className="flex items-center justify-between text-sm">
                <span className="text-slate-300">{label}</span>
                <span className="font-mono text-slate-400">{value}</span>
              </div>
              <div className="h-2 overflow-hidden rounded-full bg-white/[0.05]">
                <motion.div
                  initial={{ width: 0 }}
                  animate={{ width: `${(value / max) * 100}%` }}
                  transition={{ delay: 0.1 + i * 0.05, duration: 0.6, ease: "easeOut" }}
                  className={cn("h-full rounded-full bg-gradient-to-r", barClass)}
                />
              </div>
            </div>
          ))
        )}
      </div>
    </Card>
  );
}

function QuickAction({
  to,
  icon: Icon,
  label,
  sub,
}: {
  to: string;
  icon: typeof FileStack;
  label: string;
  sub: string;
}) {
  return (
    <Link to={to} className="group">
      <Card className="flex items-center gap-4 p-5 transition hover:border-white/[0.12] hover:bg-ink-850/70">
        <div className="grid h-10 w-10 place-items-center rounded-xl bg-white/[0.05] text-slate-300 transition group-hover:text-white">
          <Icon className="h-5 w-5" />
        </div>
        <div className="flex-1">
          <p className="text-sm font-medium text-slate-200">{label}</p>
          <p className="text-xs text-slate-500">{sub}</p>
        </div>
        <ArrowUpRight className="h-4 w-4 text-slate-600 transition group-hover:text-accent-glow" />
      </Card>
    </Link>
  );
}

function CountUp({ value }: { value: number }) {
  const [n, setN] = useState(0);
  useEffect(() => {
    let raf = 0;
    const start = performance.now();
    const dur = 700;
    const tick = (t: number) => {
      const p = Math.min(1, (t - start) / dur);
      // easeOutCubic
      setN(Math.round((1 - Math.pow(1 - p, 3)) * value));
      if (p < 1) raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [value]);
  return <>{n.toLocaleString()}</>;
}
