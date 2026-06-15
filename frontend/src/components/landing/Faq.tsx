import { useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { Plus } from "lucide-react";
import { cn } from "@/lib/utils";
import { Reveal } from "./effects";

const FAQS = [
  {
    q: "Do I need an API key to try it?",
    a: "No. cargo-iq ships with a dependency-free mock embedding and chat model, so the full stack — REST API and MCP server — runs with zero keys. Add OpenAI, Anthropic, Gemini or a local Ollama model whenever you're ready, per request or via config.",
  },
  {
    q: "What documents can it understand?",
    a: "Bills of Lading, Commercial Invoices, Letters of Credit, INCOTERMS, HS codes and the wider long tail of customs and trade-finance paperwork. Everything lands in one pgvector-backed retrieval layer.",
  },
  {
    q: "How do citations work?",
    a: "Retrieval feeds the model only the most relevant chunks, and the system prompt constrains it to answer from that context. Every response carries citations back to the source documents — and gracefully declines when the corpus can't support an answer.",
  },
  {
    q: "Can I use it from my own tools and agents?",
    a: "Yes. The same capabilities are exposed as a clean REST API under /api/v1 and as an embedded MCP server at POST /mcp, so agentic clients can call cargo-iq as a tool with no extra glue.",
  },
  {
    q: "Is it secure for multiple users?",
    a: "Authentication is stateless JWT with USER and ADMIN roles. Corpus-mutating endpoints require admin; read access requires an authenticated user. Auth, docs, health and the MCP endpoint are the only public surfaces.",
  },
  {
    q: "Is billing live yet?",
    a: "Not yet — the pricing plans shown here are placeholders. The free tier already runs the complete product today; paid plans are on the roadmap.",
  },
];

export default function Faq() {
  const [open, setOpen] = useState<number | null>(0);

  return (
    <section id="faq" className="mx-auto max-w-3xl px-5 py-28 md:px-10">
      <Reveal>
        <p className="text-center text-sm font-medium uppercase tracking-[0.2em] text-accent-glow">
          FAQ
        </p>
        <h2 className="mx-auto mt-3 max-w-xl text-balance text-center text-4xl font-semibold tracking-tight text-fg sm:text-5xl">
          Questions, <span className="text-gradient">answered.</span>
        </h2>
      </Reveal>

      <div className="mt-12 space-y-3">
        {FAQS.map((item, i) => {
          const isOpen = open === i;
          return (
            <Reveal key={item.q} delay={i * 0.04}>
              <div
                className={cn(
                  "overflow-hidden rounded-2xl border bg-surface/40 backdrop-blur-xl transition-colors",
                  isOpen ? "border-accent/30" : "border-line/[0.08]",
                )}
              >
                <button
                  onClick={() => setOpen(isOpen ? null : i)}
                  className="flex w-full items-center justify-between gap-4 px-5 py-4 text-left"
                >
                  <span className="text-[15px] font-medium text-fg">{item.q}</span>
                  <motion.span
                    animate={{ rotate: isOpen ? 45 : 0 }}
                    transition={{ duration: 0.2 }}
                    className="grid h-7 w-7 shrink-0 place-items-center rounded-full bg-line/[0.06] text-muted"
                  >
                    <Plus className="h-4 w-4" />
                  </motion.span>
                </button>
                <AnimatePresence initial={false}>
                  {isOpen && (
                    <motion.div
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: "auto", opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      transition={{ duration: 0.28, ease: [0.22, 1, 0.36, 1] }}
                    >
                      <p className="px-5 pb-5 text-[15px] leading-relaxed text-muted">
                        {item.a}
                      </p>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
            </Reveal>
          );
        })}
      </div>
    </section>
  );
}
