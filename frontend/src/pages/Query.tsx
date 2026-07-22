import { useEffect, useRef, useState } from "react";
import { motion } from "framer-motion";
import {
  Sparkles,
  ShieldCheck,
  ShieldAlert,
  Quote,
  FileText,
  Layers,
  Split,
  ArrowDownWideNarrow,
  Plus,
  User,
} from "lucide-react";
import { api, ApiError } from "@/lib/api";
import {
  DOCUMENT_TYPES,
  type DocumentType,
  type QueryResult,
  type RetrievalOptions,
} from "@/lib/types";
import { useToast } from "@/components/ui/Toast";
import { Badge, Button, Card, Select, Spinner, Textarea } from "@/components/ui/primitives";
import ModelPicker from "@/components/ModelPicker";
import { loadModelChoice, type ModelChoice } from "@/lib/models";
import { cn, humanize } from "@/lib/utils";

const SUGGESTIONS = [
  "Which shipments were discharged at Brisbane and what INCOTERMs applied?",
  "Summarise the Letter of Credit terms for the Brisbane Coffee Co. shipment.",
  "What is the vessel and BL number for the Karnataka Spice consignment?",
];

/** One exchange in the conversation: the question and its (eventual) answer. */
interface Turn {
  id: string;
  question: string;
  result: QueryResult | null;
  error?: string;
}

const newConversationId = () =>
  typeof crypto !== "undefined" && "randomUUID" in crypto
    ? crypto.randomUUID()
    : `conv-${Date.now()}-${Math.random().toString(36).slice(2)}`;

export default function Query() {
  const toast = useToast();
  const [q, setQ] = useState("");
  const [type, setType] = useState<DocumentType | "">("");
  const [model, setModel] = useState<ModelChoice>(loadModelChoice);
  const [retrieval, setRetrieval] = useState<RetrievalOptions>({
    hybrid: true,
    multiQuery: true,
    rerank: true,
  });
  const [turns, setTurns] = useState<Turn[]>([]);
  const [conversationId, setConversationId] = useState(newConversationId);
  const [busy, setBusy] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [turns, busy]);

  function toggle(key: keyof RetrievalOptions) {
    setRetrieval((r) => ({ ...r, [key]: !r[key] }));
  }

  function newChat() {
    setTurns([]);
    setConversationId(newConversationId());
    setQ("");
  }

  async function run(question?: string) {
    const text = (question ?? q).trim();
    if (!text || busy) return;
    const id = newConversationId();
    setTurns((t) => [...t, { id, question: text, result: null }]);
    setQ("");
    setBusy(true);
    try {
      const res = await api.query({
        query: text,
        filterByType: type || null,
        topK: 6,
        provider: model.provider,
        model: model.model,
        hybrid: retrieval.hybrid,
        multiQuery: retrieval.multiQuery,
        rerank: retrieval.rerank,
        conversationId,
      });
      setTurns((t) => t.map((x) => (x.id === id ? { ...x, result: res } : x)));
    } catch (e) {
      const msg = e instanceof ApiError ? e.message : "Query failed";
      setTurns((t) => t.map((x) => (x.id === id ? { ...x, error: msg } : x)));
      toast.error(msg);
    } finally {
      setBusy(false);
    }
  }

  const started = turns.length > 0;

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight text-fg">
            Ask the <span className="text-gradient">corpus</span>
          </h1>
          <p className="mt-1 text-sm text-muted">
            Retrieval-augmented answers, grounded in your documents and cited back to the source.
          </p>
        </div>
        {started && (
          <Button variant="subtle" icon={<Plus className="h-4 w-4" />} onClick={newChat}>
            New chat
          </Button>
        )}
      </header>

      {/* conversation thread */}
      {started && (
        <div className="space-y-6">
          {turns.map((turn) => (
            <div key={turn.id} className="space-y-4">
              {/* question */}
              <div className="flex justify-end">
                <div className="flex max-w-[85%] items-start gap-2.5 rounded-2xl rounded-tr-sm bg-accent/12 px-4 py-2.5 text-sm text-fg">
                  <span>{turn.question}</span>
                  <User className="mt-0.5 h-4 w-4 shrink-0 text-accent-glow" />
                </div>
              </div>
              {/* answer */}
              {turn.error ? (
                <Card className="border-rose-500/20 p-4 text-sm text-rose-300">{turn.error}</Card>
              ) : turn.result ? (
                <AnswerBlock result={turn.result} />
              ) : (
                <Card className="flex items-center gap-3 p-5 text-muted">
                  <Spinner /> Retrieving context and generating a grounded answer…
                </Card>
              )}
            </div>
          ))}
          <div ref={bottomRef} />
        </div>
      )}

      {/* composer */}
      <Card className="p-5">
        <Textarea
          value={q}
          onChange={(e) => setQ(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter" && (e.metaKey || e.ctrlKey)) run();
          }}
          rows={started ? 2 : 3}
          placeholder={started ? "Ask a follow-up…" : "Ask anything about the ingested shipping documents…"}
          className="border-0 bg-transparent px-0 text-base focus:ring-0"
        />

        {/* retrieval strategy toggles */}
        <div className="mt-2 flex flex-wrap items-center gap-2">
          <span className="text-xs font-medium uppercase tracking-wider text-faint">Retrieval</span>
          <ToggleChip active={retrieval.hybrid} onClick={() => toggle("hybrid")} icon={<Layers className="h-3.5 w-3.5" />}>
            Hybrid
          </ToggleChip>
          <ToggleChip active={retrieval.multiQuery} onClick={() => toggle("multiQuery")} icon={<Split className="h-3.5 w-3.5" />}>
            Multi-query
          </ToggleChip>
          <ToggleChip active={retrieval.rerank} onClick={() => toggle("rerank")} icon={<ArrowDownWideNarrow className="h-3.5 w-3.5" />}>
            Rerank
          </ToggleChip>
        </div>

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
            {started ? "Send" : "Ask"}
            <kbd className="ml-1 hidden rounded bg-ink-950/10 px-1.5 py-0.5 text-[10px] text-ink-950/70 sm:inline-flex">
              ⌘↵
            </kbd>
          </Button>
        </div>
      </Card>

      {!started && !busy && (
        <div className="space-y-2">
          <p className="text-xs font-medium uppercase tracking-wider text-faint">Try</p>
          <div className="flex flex-wrap gap-2">
            {SUGGESTIONS.map((s) => (
              <button
                key={s}
                onClick={() => run(s)}
                className="rounded-full border border-line/[0.08] bg-line/[0.03] px-3.5 py-1.5 text-left text-sm text-fg transition hover:border-accent/40"
              >
                {s}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function AnswerBlock({ result }: { result: QueryResult }) {
  return (
    <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} className="space-y-4">
      <Card className="p-6">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            {result.grounded ? (
              <Badge tone="emerald">
                <ShieldCheck className="h-3.5 w-3.5" /> Grounded · {result.citations.length} sources
              </Badge>
            ) : (
              <Badge tone="rose">
                <ShieldAlert className="h-3.5 w-3.5" /> Ungrounded
              </Badge>
            )}
            {result.retrievalStrategy && (
              <Badge tone="accent" className="font-mono">
                {result.retrievalStrategy}
              </Badge>
            )}
          </div>
        </div>
        <p className="whitespace-pre-wrap leading-relaxed text-fg">{result.answer}</p>
      </Card>

      {result.citations.length > 0 && (
        <div className="space-y-3">
          <p className="text-xs font-medium uppercase tracking-wider text-faint">Citations</p>
          <div className="grid gap-3">
            {result.citations.map((c, i) => (
              <Card key={c.chunkId} className="p-4">
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
            ))}
          </div>
        </div>
      )}
    </motion.div>
  );
}

function ToggleChip({
  active,
  onClick,
  icon,
  children,
}: {
  active: boolean;
  onClick: () => void;
  icon: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-medium transition",
        active
          ? "border-accent/40 bg-accent/15 text-accent-glow"
          : "border-line/[0.1] bg-line/[0.03] text-faint hover:text-muted",
      )}
    >
      {icon}
      {children}
    </button>
  );
}
