import { useEffect, useRef, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import {
  Upload,
  Trash2,
  FileText,
  Ship,
  X,
  Filter,
  PackageOpen,
  ClipboardPaste,
  FileUp,
} from "lucide-react";
import { api, ApiError } from "@/lib/api";
import { DOCUMENT_TYPES, type DocumentSummary, type DocumentType } from "@/lib/types";
import { useAuth } from "@/auth/AuthContext";
import { useToast } from "@/components/ui/Toast";
import {
  Badge,
  Button,
  Card,
  Field,
  Input,
  Select,
  Spinner,
  Textarea,
} from "@/components/ui/primitives";
import { cn, formatDate, humanize } from "@/lib/utils";

const PAGE_SIZE = 24;

const TYPE_TONE: Record<string, "accent" | "cyan" | "emerald" | "amber" | "default"> = {
  BILL_OF_LADING: "accent",
  COMMERCIAL_INVOICE: "emerald",
  LETTER_OF_CREDIT: "amber",
  REFERENCE: "cyan",
};

export default function Documents() {
  const { isAdmin } = useAuth();
  const toast = useToast();
  const [docs, setDocs] = useState<DocumentSummary[] | null>(null);
  const [filter, setFilter] = useState<DocumentType | "">("");
  const [showIngest, setShowIngest] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);

  async function load() {
    setDocs(null);
    setError(null);
    try {
      const page = await api.listDocuments(filter || undefined, PAGE_SIZE, 0);
      setDocs(page);
      setHasMore(page.length === PAGE_SIZE);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Failed to load documents");
      setDocs([]);
    }
  }

  async function loadMore() {
    if (!docs) return;
    setLoadingMore(true);
    try {
      const page = await api.listDocuments(filter || undefined, PAGE_SIZE, docs.length);
      setDocs((cur) => (cur ? [...cur, ...page] : page));
      setHasMore(page.length === PAGE_SIZE);
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Failed to load more");
    } finally {
      setLoadingMore(false);
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filter]);

  async function remove(doc: DocumentSummary) {
    if (!confirm(`Delete "${doc.title}"? This removes its vectors too.`)) return;
    try {
      await api.deleteDocument(doc.id);
      setDocs((d) => (d ? d.filter((x) => x.id !== doc.id) : d));
      toast.success("Document deleted.");
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Delete failed");
    }
  }

  return (
    <div className="space-y-7">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight text-fg">Documents</h1>
          <p className="mt-1 text-sm text-muted">
            The ingested corpus — chunked, embedded and searchable.
          </p>
        </div>
        {isAdmin && (
          <Button icon={<Upload className="h-4 w-4" />} onClick={() => setShowIngest(true)}>
            Add document
          </Button>
        )}
      </header>

      <div className="flex items-center gap-3">
        <Filter className="h-4 w-4 text-faint" />
        <div className="w-56">
          <Select value={filter} onChange={(e) => setFilter(e.target.value as DocumentType | "")}>
            <option value="">All types</option>
            {DOCUMENT_TYPES.map((t) => (
              <option key={t} value={t}>
                {humanize(t)}
              </option>
            ))}
          </Select>
        </div>
      </div>

      {error && <Card className="border-rose-500/20 p-5 text-sm text-rose-300">{error}</Card>}

      {docs === null ? (
        <div className="grid place-items-center py-24">
          <Spinner className="h-7 w-7" />
        </div>
      ) : docs.length === 0 ? (
        <EmptyState isAdmin={isAdmin} onIngest={() => setShowIngest(true)} />
      ) : (
        <>
          <div className="grid gap-3">
            {docs.map((doc, i) => (
              <motion.div
                key={doc.id}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: Math.min(i * 0.03, 0.3) }}
              >
                <Card className="flex items-start gap-4 p-4 transition hover:border-line/[0.12]">
                  <div className="grid h-11 w-11 shrink-0 place-items-center rounded-xl bg-line/[0.05] text-fg">
                    <FileText className="h-5 w-5" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <h3 className="truncate font-medium text-fg">{doc.title}</h3>
                      <Badge tone={TYPE_TONE[doc.type] ?? "default"}>{humanize(doc.type)}</Badge>
                      {doc.metadata.incoterm && (
                        <Badge tone="cyan">
                          <Ship className="h-3 w-3" /> {doc.metadata.incoterm}
                        </Badge>
                      )}
                    </div>
                    <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-xs text-faint">
                      <span>{doc.chunkCount} chunks</span>
                      {doc.metadata.vesselName && <span>Vessel: {doc.metadata.vesselName}</span>}
                      {doc.metadata.portOfDischarge && <span>→ {doc.metadata.portOfDischarge}</span>}
                      {doc.metadata.invoiceValue != null && (
                        <span>
                          {doc.metadata.currency ?? ""} {doc.metadata.invoiceValue.toLocaleString()}
                        </span>
                      )}
                      <span>{formatDate(doc.ingestedAt)}</span>
                    </div>
                  </div>
                  {isAdmin && (
                    <button
                      onClick={() => remove(doc)}
                      className="rounded-lg p-2 text-faint transition hover:bg-rose-500/10 hover:text-rose-300"
                      title="Delete"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  )}
                </Card>
              </motion.div>
            ))}
          </div>

          {hasMore && (
            <div className="flex justify-center pt-1">
              <Button variant="subtle" loading={loadingMore} onClick={loadMore}>
                Load more
              </Button>
            </div>
          )}
        </>
      )}

      <AnimatePresence>
        {showIngest && (
          <IngestModal
            onClose={() => setShowIngest(false)}
            onDone={(d) => {
              setDocs((cur) => (cur ? [d, ...cur] : [d]));
              setShowIngest(false);
              toast.success(`Ingested "${d.title}" (${d.chunkCount} chunks).`);
            }}
          />
        )}
      </AnimatePresence>
    </div>
  );
}

function EmptyState({ isAdmin, onIngest }: { isAdmin: boolean; onIngest: () => void }) {
  return (
    <Card className="grid place-items-center gap-3 px-6 py-16 text-center">
      <div className="grid h-14 w-14 place-items-center rounded-2xl bg-line/[0.05] text-muted">
        <PackageOpen className="h-7 w-7" />
      </div>
      <p className="text-fg">No documents in the corpus yet.</p>
      {isAdmin ? (
        <Button variant="subtle" icon={<Upload className="h-4 w-4" />} onClick={onIngest}>
          Add your first document
        </Button>
      ) : (
        <p className="text-sm text-faint">Ask an admin to ingest documents.</p>
      )}
    </Card>
  );
}

type IngestMode = "paste" | "upload";

function IngestModal({
  onClose,
  onDone,
}: {
  onClose: () => void;
  onDone: (d: DocumentSummary) => void;
}) {
  const toast = useToast();
  const [mode, setMode] = useState<IngestMode>("paste");
  const [title, setTitle] = useState("");
  const [type, setType] = useState<DocumentType>("BILL_OF_LADING");
  const [text, setText] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [busy, setBusy] = useState(false);
  const fileInput = useRef<HTMLInputElement>(null);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    try {
      let doc: DocumentSummary;
      if (mode === "upload") {
        if (!file) {
          toast.error("Choose a file to upload.");
          setBusy(false);
          return;
        }
        doc = await api.uploadDocument({ file, type, title: title.trim() || undefined });
      } else {
        doc = await api.ingestDocument({ title: title.trim(), type, text });
      }
      onDone(doc);
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : "Ingest failed");
    } finally {
      setBusy(false);
    }
  }

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 z-50 grid place-items-center bg-bg/70 p-4 backdrop-blur-sm"
      onClick={onClose}
    >
      <motion.div
        initial={{ opacity: 0, scale: 0.96, y: 12 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.96, y: 12 }}
        transition={{ type: "spring", stiffness: 320, damping: 28 }}
        onClick={(e) => e.stopPropagation()}
        className="glass-strong w-full max-w-lg rounded-2xl p-6 shadow-card"
      >
        <div className="mb-5 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-fg">Add a document</h2>
          <button onClick={onClose} className="rounded-lg p-1.5 text-faint hover:bg-line/[0.05] hover:text-fg">
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* mode switch */}
        <div className="mb-5 grid grid-cols-2 gap-1 rounded-xl bg-line/[0.05] p-1">
          <ModeTab active={mode === "paste"} onClick={() => setMode("paste")} icon={<ClipboardPaste className="h-4 w-4" />}>
            Paste text
          </ModeTab>
          <ModeTab active={mode === "upload"} onClick={() => setMode("upload")} icon={<FileUp className="h-4 w-4" />}>
            Upload file
          </ModeTab>
        </div>

        <form onSubmit={submit} className="space-y-4">
          <Field label="Title" hint={mode === "upload" ? "Optional — defaults to the file name." : undefined}>
            <Input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required={mode === "paste"}
              placeholder="BL — Pacific Roasters → Brisbane"
            />
          </Field>
          <Field label="Type">
            <Select value={type} onChange={(e) => setType(e.target.value as DocumentType)}>
              {DOCUMENT_TYPES.map((t) => (
                <option key={t} value={t}>
                  {humanize(t)}
                </option>
              ))}
            </Select>
          </Field>

          {mode === "paste" ? (
            <Field label="Document text" hint="Pasted raw text is chunked, embedded and indexed on submit.">
              <Textarea
                value={text}
                onChange={(e) => setText(e.target.value)}
                required
                rows={8}
                placeholder="Paste the document contents…"
              />
            </Field>
          ) : (
            <Field label="File" hint="PDF, DOCX, HTML or TXT — text is extracted server-side, then chunked and indexed.">
              <button
                type="button"
                onClick={() => fileInput.current?.click()}
                className="flex w-full items-center gap-3 rounded-xl border border-dashed border-line/[0.15] bg-surface/50 px-4 py-6 text-left transition hover:border-accent/50"
              >
                <div className="grid h-10 w-10 shrink-0 place-items-center rounded-lg bg-accent/15 text-accent-glow">
                  <FileUp className="h-5 w-5" />
                </div>
                <div className="min-w-0">
                  <p className="truncate text-sm text-fg">
                    {file ? file.name : "Choose a file…"}
                  </p>
                  <p className="text-xs text-faint">
                    {file ? `${(file.size / 1024).toFixed(0)} KB` : "PDF · DOCX · HTML · TXT"}
                  </p>
                </div>
              </button>
              <input
                ref={fileInput}
                type="file"
                accept=".pdf,.docx,.doc,.html,.htm,.txt,.md,.rtf,application/pdf,text/plain"
                className="hidden"
                onChange={(e) => {
                  const f = e.target.files?.[0] ?? null;
                  setFile(f);
                  if (f && !title.trim()) setTitle(f.name.replace(/\.[^.]+$/, ""));
                }}
              />
            </Field>
          )}

          <div className="flex justify-end gap-2 pt-1">
            <Button type="button" variant="ghost" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" loading={busy} icon={<Upload className="h-4 w-4" />}>
              {mode === "upload" ? "Upload & ingest" : "Ingest"}
            </Button>
          </div>
        </form>
      </motion.div>
    </motion.div>
  );
}

function ModeTab({
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
      className={cn(
        "flex items-center justify-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition",
        active ? "bg-accent/15 text-accent-glow" : "text-muted hover:text-fg",
      )}
    >
      {icon}
      {children}
    </button>
  );
}
