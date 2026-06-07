import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Search, Ship, Package, ArrowRightLeft, Coins, Truck } from "lucide-react";
import { api, ApiError } from "@/lib/api";
import { INCOTERMS, type HsCode, type IncotermDetail } from "@/lib/types";
import { useToast } from "@/components/ui/Toast";
import { Badge, Button, Card, Input, Spinner } from "@/components/ui/primitives";
import { cn } from "@/lib/utils";

type Tab = "incoterms" | "hs";

export default function Reference() {
  const [tab, setTab] = useState<Tab>("incoterms");

  return (
    <div className="space-y-7">
      <header>
        <h1 className="text-3xl font-semibold tracking-tight text-fg">Reference</h1>
        <p className="mt-1 text-sm text-muted">
          Deterministic lookups — INCOTERMS 2020 rules and the HS tariff schedule. No LLM in the loop.
        </p>
      </header>

      <div className="inline-grid grid-cols-2 gap-1 rounded-xl bg-surface/60 p-1">
        {(
          [
            ["incoterms", "INCOTERMS 2020"],
            ["hs", "HS codes"],
          ] as [Tab, string][]
        ).map(([key, label]) => (
          <button
            key={key}
            onClick={() => setTab(key)}
            className="relative rounded-lg px-5 py-2 text-sm font-medium text-fg transition"
          >
            {tab === key && (
              <motion.span
                layoutId="ref-tab"
                className="absolute inset-0 -z-10 rounded-lg bg-line/[0.08] ring-1 ring-line/[0.08]"
                transition={{ type: "spring", stiffness: 380, damping: 30 }}
              />
            )}
            {label}
          </button>
        ))}
      </div>

      <AnimatePresence mode="wait">
        {tab === "incoterms" ? (
          <motion.div key="i" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
            <IncotermPanel />
          </motion.div>
        ) : (
          <motion.div key="h" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
            <HsPanel />
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

function IncotermPanel() {
  const toast = useToast();
  const [code, setCode] = useState<string>("CIF");
  const [detail, setDetail] = useState<IncotermDetail | null>(null);
  const [busy, setBusy] = useState(false);

  async function lookup(c: string) {
    setCode(c);
    setBusy(true);
    setDetail(null);
    try {
      setDetail(await api.incoterm(c));
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Lookup failed");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap gap-2">
        {INCOTERMS.map((c) => (
          <button
            key={c}
            onClick={() => lookup(c)}
            className={cn(
              "rounded-lg border px-3 py-1.5 font-mono text-sm transition",
              code === c
                ? "border-accent/40 bg-accent/15 text-accent-glow"
                : "border-line/[0.08] bg-line/[0.03] text-fg hover:border-line/[0.16] hover:text-fg",
            )}
          >
            {c}
          </button>
        ))}
      </div>

      {busy && (
        <Card className="flex items-center gap-3 p-6 text-muted">
          <Spinner /> Loading rule…
        </Card>
      )}

      {detail && (
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}>
          <Card className="p-6">
            <div className="flex items-center gap-3">
              <span className="rounded-lg bg-gradient-to-br from-accent to-cyanish px-3 py-1.5 font-mono text-sm font-semibold text-white shadow-glow">
                {detail.rule}
              </span>
              <h2 className="text-lg font-medium text-fg">{detail.summary}</h2>
            </div>
            <div className="mt-5 grid gap-4 sm:grid-cols-2">
              <Detail icon={Ship} label="Seller obligations" value={detail.sellerObligations} />
              <Detail icon={Package} label="Buyer obligations" value={detail.buyerObligations} />
              <Detail icon={ArrowRightLeft} label="Risk transfer" value={detail.riskTransfer} />
              <Detail icon={Coins} label="Cost transfer" value={detail.costTransfer} />
            </div>
            <div className="mt-4 rounded-xl border border-line/[0.06] bg-line/[0.02] p-4">
              <div className="mb-1 flex items-center gap-2 text-xs font-medium uppercase tracking-wider text-faint">
                <Truck className="h-3.5 w-3.5" /> Typical use
              </div>
              <p className="text-sm text-fg">{detail.typicalUseCase}</p>
            </div>
          </Card>
        </motion.div>
      )}
    </div>
  );
}

function Detail({
  icon: Icon,
  label,
  value,
}: {
  icon: typeof Ship;
  label: string;
  value: string;
}) {
  return (
    <div className="rounded-xl border border-line/[0.06] bg-line/[0.02] p-4">
      <div className="mb-1.5 flex items-center gap-2 text-xs font-medium uppercase tracking-wider text-faint">
        <Icon className="h-3.5 w-3.5" /> {label}
      </div>
      <p className="text-sm leading-relaxed text-fg">{value}</p>
    </div>
  );
}

function HsPanel() {
  const toast = useToast();
  const [term, setTerm] = useState("");
  const [results, setResults] = useState<HsCode[] | null>(null);
  const [busy, setBusy] = useState(false);

  async function search(e: React.FormEvent) {
    e.preventDefault();
    const q = term.trim();
    if (!q) return;
    setBusy(true);
    setResults(null);
    try {
      // Numeric input -> exact code lookup; text -> description search.
      if (/^\d{4,10}$/.test(q)) {
        setResults([await api.hsByCode(q)]);
      } else {
        setResults(await api.hsSearch(q, 15));
      }
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "No match found");
      setResults([]);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="space-y-5">
      <form onSubmit={search} className="flex gap-2">
        <div className="relative flex-1">
          <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-faint" />
          <Input
            value={term}
            onChange={(e) => setTerm(e.target.value)}
            placeholder="Search by description (e.g. coffee) or exact code (e.g. 090111)"
            className="pl-10"
          />
        </div>
        <Button type="submit" loading={busy} icon={<Search className="h-4 w-4" />}>
          Search
        </Button>
      </form>

      {results && results.length === 0 && (
        <Card className="p-5 text-sm text-muted">No HS codes matched that query.</Card>
      )}

      {results && results.length > 0 && (
        <div className="grid gap-3">
          {results.map((hs, i) => (
            <motion.div
              key={hs.code}
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.04 }}
            >
              <Card className="flex items-center gap-4 p-4">
                <span className="rounded-lg bg-line/[0.05] px-3 py-2 font-mono text-sm text-cyanish">
                  {hs.code}
                </span>
                <div className="min-w-0 flex-1">
                  <p className="text-sm text-fg">{hs.description}</p>
                  <p className="text-xs text-faint">Chapter {hs.chapter}</p>
                </div>
                <Badge tone="cyan">HS {hs.code.slice(0, 2)}</Badge>
              </Card>
            </motion.div>
          ))}
        </div>
      )}
    </div>
  );
}
