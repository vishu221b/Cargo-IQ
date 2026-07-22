import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  Sparkles,
  ShieldCheck,
  ShieldAlert,
  Quote,
  CornerDownLeft,
  FileText,
} from "lucide-react";
import { api, ApiError } from "@/lib/api";
import { DOCUMENT_TYPES, type DocumentType, type QueryResult } from "@/lib/types";
import { useToast } from "@/components/ui/Toast";
import { Badge, Button, Card, Select, Spinner, Textarea } from "@/components/ui/primitives";
import ModelPicker from "@/components/ModelPicker";
import { loadModelChoice, type ModelChoice } from "@/lib/models";
import { humanize } from "@/lib/utils";

const SUGGESTIONS = [
  "Which shipments were discharged at Brisbane and what INCOTERMs applied?",
  "Summarise the Letter of Credit terms for the Brisbane Coffee Co. shipment.",
  "What is the vessel and BL number for the Karnataka Spice consignment?",
];

export default function Query() {
  const toast = useToast();
  const [q, setQ] = useState("");
  const [type, setType] = useState<DocumentType | "">("");
  const [model, setModel] = useState<ModelChoice>(loadModelChoice);
  const [result, setResult] = useState<QueryResult | null>(null);
  const [busy, setBusy] = useState(false);

  async function run(question?: string) {
    const text = (question ?? q).trim();
    if (!text) return;
    if (question) setQ(question);
    setBusy(true);
    setResult(null);
    try {
      const res = await api.query({
        query: text,
        filterByType: type || null,
        topK: 6,
        provider: model.provider,
        model: model.model,
      });
      setResult(res);
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Query failed");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="space-y-7">
      <header>
        <h1 className="text-3xl font-semibold tracking-tight text-fg">
          Ask the <span className="text-gradient">corpus</span>
        </h1>
        <p className="mt-1 text-sm text-muted">
          Retrieval-augmented answers, grounded in your documents and cited back to the source.
        </p>
      </header>

      <Card className="p-5">
        <Textarea
          value={q}
          onChange={(e) => setQ(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter" && (e.metaKey || e.ctrlKey)) run();
          }}
          rows={3}
          placeholder="Ask anything about the ingested shipping documents…"
          className="border-0 bg-transparent px-0 text-base focus:ring-0"
        />
        <div className="mt-3 border-t border-line/[0.06] pt-4">
          <ModelPicker value={model} onChange={setModel} />
        </div>
        <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
          <div className="w-52">
            <Select value={type} onChange={(e) => setType(e.target.value as DocumentType | "")}>
              <option value="">Search all types</option>
              {DOCUMENT_TYPES.map((t) => (
                <option key={t} value={t}>
                  {humanize(t)}
                </option>
              ))}
            </Select>
          </div>
          <Button onClick={() => run()} loading={busy} icon={<Sparkles className="h-4 w-4" />}>
            Ask
            <kbd className="ml-1 hidden rounded bg-line/10 px-1.5 py-0.5 text-[10px] text-white/70 sm:inline-flex">
              ⌘↵
            </kbd>
          </Button>
        </div>
      </Card>

      {!result && !busy && (
        <div className="space-y-2">
          <p className="text-xs font-medium uppercase tracking-wider text-faint">Try</p>
          <div className="flex flex-wrap gap-2">
            {SUGGESTIONS.map((s) => (
              <button
                key={s}
                onClick={() => run(s)}
                className="rounded-full border border-line/[0.08] bg-line/[0.03] px-3.5 py-1.5 text-left text-sm text-fg transition hover:border-accent/40 hover:text-fg"
              >
                {s}
              </button>
            ))}
          </div>
        </div>
      )}

      {busy && (
        <Card className="flex items-center gap-3 p-6 text-muted">
          <Spinner /> Retrieving context and generating a grounded answer…
        </Card>
      )}

      <AnimatePresence mode="wait">
        {result && (
          <motion.div
            key="result"
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0 }}
            className="space-y-5"
          >
            <Card className="p-6">
              <div className="mb-4 flex items-center justify-between">
                <div className="flex items-center gap-2 text-sm text-muted">
                  <CornerDownLeft className="h-4 w-4" /> Answer
                </div>
                {result.grounded ? (
                  <Badge tone="emerald">
                    <ShieldCheck className="h-3.5 w-3.5" /> Grounded · {result.citations.length} sources
                  </Badge>
                ) : (
                  <Badge tone="rose">
                    <ShieldAlert className="h-3.5 w-3.5" /> Ungrounded
                  </Badge>
                )}
              </div>
              <p className="whitespace-pre-wrap leading-relaxed text-fg">
                {result.answer}
              </p>
            </Card>

            {result.citations.length > 0 && (
              <div className="space-y-3">
                <p className="text-xs font-medium uppercase tracking-wider text-faint">
                  Citations
                </p>
                <div className="grid gap-3">
                  {result.citations.map((c, i) => (
                    <motion.div
                      key={c.chunkId}
                      initial={{ opacity: 0, x: -8 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: i * 0.05 }}
                    >
                      <Card className="p-4">
                        <div className="mb-2 flex items-center justify-between gap-3">
                          <div className="flex min-w-0 items-center gap-2 text-sm text-fg">
                            <span className="grid h-6 w-6 shrink-0 place-items-center rounded-md bg-accent/15 text-xs font-semibold text-accent-glow">
                              {i + 1}
                            </span>
                            <FileText className="h-3.5 w-3.5 shrink-0 text-faint" />
                            <span className="truncate">{c.documentTitle}</span>
                          </div>
                          <Badge>chunk {c.chunkSequence} · {(c.score * 100).toFixed(0)}%</Badge>
                        </div>
                        <p className="flex gap-2 text-sm leading-relaxed text-muted">
                          <Quote className="mt-0.5 h-4 w-4 shrink-0 text-faint" />
                          <span className="line-clamp-4">{c.snippet}</span>
                        </p>
                      </Card>
                    </motion.div>
                  ))}
                </div>
              </div>
            )}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
