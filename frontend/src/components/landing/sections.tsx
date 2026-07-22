import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { motion, useInView, useScroll, useTransform } from "framer-motion";
import {
  Quote,
  Shield,
  Boxes,
  Network,
  Database,
  Cpu,
  Layers,
  Upload,
  Search,
  MessageSquareText,
  ArrowRight,
  Sparkles,
} from "lucide-react";
import { Button } from "@/components/ui/primitives";
import { GridPattern } from "@/components/ui/Backgrounds";
import { cn } from "@/lib/utils";
import {
  Reveal,
  Counter,
  Marquee,
  SpotlightCard,
  ParticleField,
  PixelGrid,
  Parallax,
} from "./effects";

/* =================================================================== *
 * Logos marquee — social proof strip.
 * =================================================================== */
const PARTNERS = [
  "MAERSK",
  "DHL",
  "Kuehne+Nagel",
  "DB Schenker",
  "DSV",
  "CMA CGM",
  "Expeditors",
  "Hapag-Lloyd",
  "FedEx Trade",
  "Flexport",
];

export function LogosStrip() {
  return (
    <section className="border-y border-line/[0.06] bg-surface/20 py-10">
      <p className="mb-7 text-center text-xs font-medium uppercase tracking-[0.25em] text-faint">
        Built for the teams that move global trade
      </p>
      <Marquee speed={38} className="[mask-image:linear-gradient(90deg,transparent,black_12%,black_88%,transparent)]">
        {PARTNERS.map((p) => (
          <span
            key={p}
            className="mx-8 select-none whitespace-nowrap text-xl font-semibold tracking-tight text-faint/70 transition hover:text-fg"
          >
            {p}
          </span>
        ))}
      </Marquee>
    </section>
  );
}

/* =================================================================== *
 * Features — bento grid of spotlight cards.
 * =================================================================== */
const FEATURES = [
  {
    icon: Quote,
    title: "Answers with citations, never guesses",
    body: "Every response is grounded in retrieved chunks and cites the exact source document. If the corpus can't support a claim, cargo-iq says so instead of hallucinating.",
    span: "md:col-span-2",
    accent: "from-accent/20",
  },
  {
    icon: Network,
    title: "REST API + MCP server",
    body: "The same capabilities exposed twice — a clean /api/v1 and an embedded MCP server at POST /mcp for agentic tools.",
    span: "",
    accent: "from-cyanish/20",
  },
  {
    icon: Cpu,
    title: "Bring any model — or none",
    body: "Ships with a dependency-free mock so it runs with zero keys. Switch to OpenAI, Anthropic, Gemini or local Ollama per request.",
    span: "",
    accent: "from-orange-500/20",
  },
  {
    icon: Database,
    title: "pgvector-backed retrieval",
    body: "Postgres + pgvector similarity search over chunked, embedded documents — re-ranking and hybrid search slot in cleanly.",
    span: "",
    accent: "from-emerald-500/20",
  },
  {
    icon: Shield,
    title: "Auth & RBAC, day one",
    body: "Stateless JWT with USER / ADMIN roles. Corpus-mutating endpoints require admin; everything is locked down by default.",
    span: "",
    accent: "from-amber-500/20",
  },
  {
    icon: Layers,
    title: "Hexagonal architecture",
    body: "Strict ports & adapters. Swap your vector store or model vendor by rewriting one adapter — the core never changes.",
    span: "md:col-span-2",
    accent: "from-accent/20",
  },
];

export function Features() {
  return (
    <section id="features" className="relative mx-auto max-w-6xl px-5 py-28 md:px-10">
      <ParticleField className="opacity-30" density={0.00007} />
      <Reveal>
        <p className="text-sm font-medium uppercase tracking-[0.2em] text-accent-glow">
          Why cargo-iq
        </p>
        <h2 className="mt-3 max-w-2xl text-balance text-4xl font-semibold tracking-tight text-fg sm:text-5xl">
          Production-grade RAG, <span className="text-gradient">without the guesswork.</span>
        </h2>
        <p className="mt-4 max-w-xl text-lg text-muted">
          The capabilities your trade-document workflows actually need — grounded,
          governed, and ready to integrate.
        </p>
      </Reveal>

      <div className="mt-12 grid grid-cols-1 gap-5 md:grid-cols-3">
        {FEATURES.map((f, i) => (
          <Reveal key={f.title} delay={i * 0.06} className={f.span}>
            <SpotlightCard className="h-full p-7">
              <div
                className={cn(
                  "grid h-12 w-12 place-items-center rounded-2xl bg-gradient-to-br to-transparent text-fg",
                  f.accent,
                )}
              >
                <f.icon className="h-6 w-6" />
              </div>
              <h3 className="mt-5 text-xl font-semibold tracking-tight text-fg">
                {f.title}
              </h3>
              <p className="mt-2.5 text-[15px] leading-relaxed text-muted">{f.body}</p>
            </SpotlightCard>
          </Reveal>
        ))}
      </div>
    </section>
  );
}

/* =================================================================== *
 * How it works — three steps with a scroll-linked progress rail.
 * =================================================================== */
const STEPS = [
  {
    icon: Upload,
    title: "Ingest",
    body: "Drop in a Bill of Lading, invoice or LC. cargo-iq parses it into chunks, extracts metadata and persists the aggregate.",
    code: "POST /api/v1/documents",
  },
  {
    icon: Database,
    title: "Index",
    body: "Chunks are embedded and written to pgvector. Switch embedding providers any time — the wall between core and infra holds.",
    code: "embed → vector_store",
  },
  {
    icon: MessageSquareText,
    title: "Ask",
    body: "Query in natural language. Similarity search feeds a grounded prompt; you get an answer plus the citations behind it.",
    code: "POST /api/v1/query",
  },
];

export function HowItWorks() {
  const ref = useRef<HTMLDivElement>(null);
  const { scrollYProgress } = useScroll({
    target: ref,
    offset: ["start 70%", "end 60%"],
  });
  const height = useTransform(scrollYProgress, [0, 1], ["0%", "100%"]);

  return (
    <section id="how" className="relative border-y border-line/[0.06] bg-surface/20 py-28">
      <div className="mx-auto max-w-5xl px-5 md:px-10">
        <Reveal>
          <p className="text-center text-sm font-medium uppercase tracking-[0.2em] text-accent-glow">
            How it works
          </p>
          <h2 className="mx-auto mt-3 max-w-2xl text-balance text-center text-4xl font-semibold tracking-tight text-fg sm:text-5xl">
            From document to cited answer in <span className="text-gradient">three steps.</span>
          </h2>
        </Reveal>

        <div ref={ref} className="relative mt-16 pl-10 md:pl-0">
          {/* rail */}
          <div className="absolute left-[19px] top-2 h-full w-px bg-line/[0.10] md:left-1/2 md:-translate-x-1/2">
            <motion.div
              style={{ height }}
              className="w-px bg-gradient-to-b from-accent via-orange-400 to-cyanish"
            />
          </div>

          <div className="space-y-12 md:space-y-20">
            {STEPS.map((s, i) => (
              <Step key={s.title} step={s} index={i} />
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}

function Step({
  step,
  index,
}: {
  step: (typeof STEPS)[number];
  index: number;
}) {
  const left = index % 2 === 0;
  return (
    <div
      className={cn(
        "relative grid items-center gap-6 md:grid-cols-2",
      )}
    >
      {/* node */}
      <span className="absolute left-[-21px] top-1.5 grid h-10 w-10 place-items-center rounded-full border border-accent/40 bg-bg text-sm font-semibold text-accent-glow md:left-1/2 md:-translate-x-1/2">
        {index + 1}
      </span>

      <Reveal
        y={28}
        className={cn(
          "rounded-2xl border border-line/[0.08] bg-surface/50 p-6 backdrop-blur-xl",
          left ? "md:col-start-1 md:mr-10 md:text-right" : "md:col-start-2 md:ml-10",
        )}
      >
        <div
          className={cn(
            "mb-3 flex items-center gap-3",
            left && "md:flex-row-reverse",
          )}
        >
          <div className="grid h-11 w-11 place-items-center rounded-xl bg-gradient-to-br from-accent/25 to-transparent text-accent-glow">
            <step.icon className="h-5 w-5" />
          </div>
          <h3 className="text-xl font-semibold tracking-tight text-fg">{step.title}</h3>
        </div>
        <p className="text-[15px] leading-relaxed text-muted">{step.body}</p>
        <code className="mt-3 inline-block rounded-lg bg-bg/60 px-2.5 py-1 font-mono text-xs text-cyanish">
          {step.code}
        </code>
      </Reveal>
    </div>
  );
}

/* =================================================================== *
 * AnswerDemo — a faux terminal that types out a grounded answer.
 * =================================================================== */
const TYPED =
  "Under DAP Hamburg the seller bears all costs and risk to the named place; import duties remain the buyer's responsibility. [INCOTERMS-2020 · DAP] [BoL-2207.pdf · p.2]";

export function AnswerDemo() {
  const ref = useRef<HTMLDivElement>(null);
  const inView = useInView(ref, { once: true, margin: "-120px" });
  const [shown, setShown] = useState("");

  useEffect(() => {
    if (!inView) return;
    let i = 0;
    const id = setInterval(() => {
      i += 1;
      setShown(TYPED.slice(0, i));
      if (i >= TYPED.length) clearInterval(id);
    }, 18);
    return () => clearInterval(id);
  }, [inView]);

  return (
    <section className="mx-auto max-w-5xl px-5 py-28 md:px-10">
      <div className="grid items-center gap-12 lg:grid-cols-2">
        <Reveal>
          <p className="text-sm font-medium uppercase tracking-[0.2em] text-accent-glow">
            Grounded by design
          </p>
          <h2 className="mt-3 text-balance text-4xl font-semibold tracking-tight text-fg sm:text-5xl">
            Trust the answer because you can <span className="text-gradient">check the source.</span>
          </h2>
          <p className="mt-4 text-lg leading-relaxed text-muted">
            cargo-iq retrieves the most relevant chunks, then constrains the model
            to answer only from them. Citations aren't decoration — they're the
            contract. No source, no claim.
          </p>
          <ul className="mt-6 space-y-3">
            {[
              "Inline citations on every response",
              "Refuses gracefully when context is thin",
              "Same engine powers REST and MCP",
            ].map((t) => (
              <li key={t} className="flex items-center gap-3 text-[15px] text-fg">
                <span className="grid h-5 w-5 place-items-center rounded-full bg-emerald-500/15 text-emerald-300">
                  ✓
                </span>
                {t}
              </li>
            ))}
          </ul>
        </Reveal>

        <Parallax speed={30}>
          <div ref={ref} className="glass-strong overflow-hidden rounded-2xl shadow-card">
            <div className="flex items-center gap-2 border-b border-line/[0.08] px-4 py-3">
              <span className="h-3 w-3 rounded-full bg-rose-400/80" />
              <span className="h-3 w-3 rounded-full bg-amber-400/80" />
              <span className="h-3 w-3 rounded-full bg-emerald-400/80" />
              <span className="ml-2 font-mono text-xs text-faint">cargo-iq · query</span>
            </div>
            <div className="space-y-3 p-5 font-mono text-[13px] leading-relaxed">
              <p className="text-faint">$ ask "Who pays import duty on the Hamburg delivery?"</p>
              <p className="text-fg">
                {shown}
                <span className="ml-0.5 inline-block h-4 w-2 translate-y-0.5 animate-blink bg-accent-glow" />
              </p>
            </div>
          </div>
        </Parallax>
      </div>
    </section>
  );
}

/* =================================================================== *
 * Stats band.
 * =================================================================== */
const STATS = [
  { to: 5, suffix: "", label: "Model providers supported" },
  { to: 0, suffix: " keys", label: "Required to start", prefix: "" },
  { to: 100, suffix: "%", label: "Answers carry citations" },
  { to: 2, suffix: "", label: "Interfaces — REST + MCP" },
];

export function Stats() {
  return (
    <section className="border-y border-line/[0.06] bg-surface/20 py-16">
      <div className="mx-auto grid max-w-5xl grid-cols-2 gap-8 px-5 md:grid-cols-4 md:px-10">
        {STATS.map((s, i) => (
          <Reveal key={s.label} delay={i * 0.08} className="text-center">
            <p className="text-4xl font-semibold tracking-tight text-fg sm:text-5xl">
              <Counter to={s.to} suffix={s.suffix} prefix={s.prefix} />
            </p>
            <p className="mt-2 text-sm text-muted">{s.label}</p>
          </Reveal>
        ))}
      </div>
    </section>
  );
}

/* =================================================================== *
 * Final CTA.
 * =================================================================== */
export function FinalCta() {
  return (
    <section className="relative mx-auto max-w-6xl px-5 py-28 md:px-10">
      <div className="relative isolate overflow-hidden rounded-[2rem] border border-line/[0.10] bg-gradient-to-br from-accent/15 via-surface/40 to-cyanish/10 px-8 py-16 text-center shadow-card md:py-24">
        <PixelGrid className="opacity-60" />
        <ParticleField className="opacity-40" />
        <GridPattern />
        <Reveal>
          <span className="inline-flex items-center gap-2 rounded-full border border-line/[0.10] bg-surface/60 px-3.5 py-1.5 text-xs font-medium text-muted backdrop-blur">
            <Sparkles className="h-3.5 w-3.5 text-accent-glow" /> No card required to start
          </span>
          <h2 className="mx-auto mt-6 max-w-2xl text-balance text-4xl font-semibold tracking-tight text-fg sm:text-6xl">
            Stop reading paperwork. <br />
            <span className="text-gradient">Start asking it questions.</span>
          </h2>
          <p className="mx-auto mt-5 max-w-xl text-lg text-muted">
            Spin up cargo-iq in minutes, point it at your documents, and get cited
            answers your whole team can trust.
          </p>
          <div className="mt-9 flex flex-col items-center justify-center gap-3 sm:flex-row">
            <Link to="/login">
              <Button size="md" className="group h-12 px-7 text-[15px]">
                Get started free
                <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-0.5" />
              </Button>
            </Link>
            <a href="#pricing">
              <Button size="md" variant="subtle" className="h-12 px-7 text-[15px]">
                View pricing
              </Button>
            </a>
          </div>
        </Reveal>
      </div>
    </section>
  );
}

/* =================================================================== *
 * Footer.
 * =================================================================== */
export function Footer() {
  return (
    <footer className="border-t border-line/[0.06] bg-surface/20">
      <div className="mx-auto max-w-6xl px-5 py-14 md:px-10">
        <div className="grid gap-10 md:grid-cols-[1.4fr_1fr_1fr_1fr]">
          <div>
            <Link to="/" className="flex items-center gap-2.5">
              <div className="grid h-9 w-9 place-items-center rounded-xl bg-gradient-to-br from-accent to-cyanish shadow-glow">
                <Boxes className="h-5 w-5 text-white" />
              </div>
              <span className="text-[15px] font-semibold tracking-tight text-fg">
                cargo<span className="text-accent-glow">-iq</span>
              </span>
            </Link>
            <p className="mt-4 max-w-xs text-sm leading-relaxed text-muted">
              Grounded intelligence for cargo, freight and trade-finance documents.
              RAG + MCP, pgvector-backed, model-agnostic.
            </p>
          </div>

          <FooterCol
            title="Product"
            links={[
              ["Features", "#features"],
              ["How it works", "#how"],
              ["Coverage", "#showcase"],
              ["Pricing", "#pricing"],
            ]}
          />
          <FooterCol
            title="Get started"
            links={[
              ["Sign in", "/login"],
              ["Open app", "/app"],
              ["FAQ", "#faq"],
            ]}
          />
          <FooterCol
            title="Resources"
            links={[
              ["REST API /api/v1", "#features"],
              ["MCP server /mcp", "#features"],
              ["Architecture", "#how"],
            ]}
          />
        </div>

        <div className="mt-12 flex flex-col items-center justify-between gap-3 border-t border-line/[0.06] pt-6 text-xs text-faint sm:flex-row">
          <p>© {new Date().getFullYear()} cargo-iq. Apache-2.0 licensed.</p>
          <p>Built for global trade. Powered by grounded RAG.</p>
        </div>
      </div>
    </footer>
  );
}

function FooterCol({ title, links }: { title: string; links: [string, string][] }) {
  return (
    <div>
      <p className="text-xs font-semibold uppercase tracking-wider text-faint">{title}</p>
      <ul className="mt-4 space-y-2.5">
        {links.map(([label, href]) => {
          const internal = href.startsWith("/");
          return (
            <li key={label}>
              {internal ? (
                <Link to={href} className="text-sm text-muted transition hover:text-fg">
                  {label}
                </Link>
              ) : (
                <a href={href} className="text-sm text-muted transition hover:text-fg">
                  {label}
                </a>
              )}
            </li>
          );
        })}
      </ul>
    </div>
  );
}
