import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import {
  ArrowRight,
  Sparkles,
  ShieldCheck,
  Quote,
  FileText,
  PlayCircle,
} from "lucide-react";
import { Button } from "@/components/ui/primitives";
import { AuroraBackground, Spotlight, GridPattern } from "@/components/ui/Backgrounds";
import { ParticleField, WordReveal, Reveal, Parallax } from "./effects";

export default function Hero() {
  return (
    <AuroraBackground className="relative isolate min-h-screen overflow-hidden">
      <ParticleField className="opacity-70" />
      <Spotlight />
      <GridPattern />

      <div className="mx-auto grid max-w-6xl grid-cols-1 items-center gap-12 px-5 pb-24 pt-36 md:px-10 lg:grid-cols-[1.05fr_0.95fr] lg:pt-40">
        {/* Copy */}
        <div className="text-center lg:text-left">
          <Reveal>
            <span className="inline-flex items-center gap-2 rounded-full border border-line/[0.10] bg-surface/60 px-3.5 py-1.5 text-xs font-medium text-muted backdrop-blur">
              <span className="relative flex h-2 w-2">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-75" />
                <span className="relative inline-flex h-2 w-2 rounded-full bg-emerald-400" />
              </span>
              Runs with zero API keys — grounded RAG + MCP, out of the box
            </span>
          </Reveal>

          <h1 className="mt-6 text-balance text-5xl font-semibold leading-[1.02] tracking-tight text-fg sm:text-6xl lg:text-[4.25rem]">
            <WordReveal text="Turn cargo paperwork into" />{" "}
            <WordReveal
              text="answers you can cite."
              highlight={["answers", "cite."]}
              delay={0.25}
            />
          </h1>

          <Reveal delay={0.5}>
            <p className="mx-auto mt-6 max-w-xl text-pretty text-lg leading-relaxed text-muted lg:mx-0">
              cargo-iq reads your Bills of Lading, Commercial Invoices, Letters of
              Credit, INCOTERMS and HS codes — then answers questions in plain
              language, with every claim traced back to the source document.
            </p>
          </Reveal>

          <Reveal delay={0.65}>
            <div className="mt-9 flex flex-col items-center gap-3 sm:flex-row lg:justify-start">
              <Link to="/login">
                <Button size="md" className="group h-12 px-6 text-[15px]" icon={<Sparkles className="h-4 w-4" />}>
                  Start free
                  <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-0.5" />
                </Button>
              </Link>
              <a href="#how">
                <Button size="md" variant="subtle" className="h-12 px-6 text-[15px]" icon={<PlayCircle className="h-4 w-4" />}>
                  See how it works
                </Button>
              </a>
            </div>
          </Reveal>

          <Reveal delay={0.8}>
            <div className="mt-8 flex flex-wrap items-center justify-center gap-x-6 gap-y-2 text-xs text-faint lg:justify-start">
              <span className="inline-flex items-center gap-1.5">
                <ShieldCheck className="h-3.5 w-3.5 text-emerald-400" /> JWT + RBAC built in
              </span>
              <span className="inline-flex items-center gap-1.5">
                <Sparkles className="h-3.5 w-3.5 text-accent-glow" /> OpenAI · Anthropic · Gemini · Ollama
              </span>
              <span className="inline-flex items-center gap-1.5">
                <FileText className="h-3.5 w-3.5 text-cyanish" /> REST API + MCP server
              </span>
            </div>
          </Reveal>
        </div>

        {/* Floating answer-card mockup */}
        <Parallax speed={40} className="relative hidden lg:block">
          <motion.div
            initial={{ opacity: 0, y: 30, rotateX: 8 }}
            animate={{ opacity: 1, y: 0, rotateX: 0 }}
            transition={{ duration: 0.9, delay: 0.4, ease: [0.22, 1, 0.36, 1] }}
            style={{ perspective: 1200 }}
            className="relative"
          >
            <AnswerCard />
            <motion.div
              animate={{ y: [0, -12, 0] }}
              transition={{ duration: 6, repeat: Infinity, ease: "easeInOut" }}
              className="glass-strong absolute -right-6 -top-6 hidden rounded-2xl px-4 py-3 shadow-card xl:block"
            >
              <p className="text-[11px] uppercase tracking-wider text-faint">Grounded</p>
              <p className="text-2xl font-semibold text-fg">3 citations</p>
            </motion.div>
            <motion.div
              animate={{ y: [0, 12, 0] }}
              transition={{ duration: 7, repeat: Infinity, ease: "easeInOut", delay: 1 }}
              className="glass-strong absolute -bottom-7 -left-7 hidden rounded-2xl px-4 py-3 shadow-card xl:block"
            >
              <p className="text-[11px] uppercase tracking-wider text-faint">Latency</p>
              <p className="text-2xl font-semibold text-gradient">820ms</p>
            </motion.div>
          </motion.div>
        </Parallax>
      </div>

      {/* scroll cue */}
      <motion.div
        animate={{ y: [0, 8, 0], opacity: [0.4, 1, 0.4] }}
        transition={{ duration: 2, repeat: Infinity }}
        className="absolute inset-x-0 bottom-6 mx-auto hidden w-fit text-xs text-faint md:block"
      >
        Scroll to explore
      </motion.div>
    </AuroraBackground>
  );
}

function AnswerCard() {
  return (
    <div className="glass-strong relative overflow-hidden rounded-3xl p-6 shadow-card">
      <div className="flex items-center gap-2 border-b border-line/[0.08] pb-4">
        <span className="h-3 w-3 rounded-full bg-rose-400/80" />
        <span className="h-3 w-3 rounded-full bg-amber-400/80" />
        <span className="h-3 w-3 rounded-full bg-emerald-400/80" />
        <span className="ml-2 font-mono text-xs text-faint">POST /api/v1/query</span>
      </div>

      <div className="mt-5 space-y-4 text-sm">
        <div className="flex justify-end">
          <p className="max-w-[85%] rounded-2xl rounded-br-sm bg-accent/15 px-4 py-2.5 text-fg">
            What INCOTERM applies to the Shanghai → Rotterdam shipment, and who
            pays the freight?
          </p>
        </div>

        <div className="rounded-2xl rounded-bl-sm border border-line/[0.08] bg-surface/60 px-4 py-3 text-fg">
          <p className="leading-relaxed">
            The Bill of Lading specifies{" "}
            <span className="font-semibold text-accent-glow">CIF Rotterdam</span>.
            Under CIF the <span className="font-semibold">seller</span> arranges
            and pays ocean freight and insurance to the destination port; risk
            transfers to the buyer once goods are loaded on board.
          </p>
          <div className="mt-3 flex flex-wrap gap-2">
            {["BoL-4471.pdf · p.1", "INCOTERMS-2020 · CIF", "Invoice-8830.pdf"].map(
              (c) => (
                <span
                  key={c}
                  className="inline-flex items-center gap-1 rounded-full border border-cyanish/30 bg-cyanish/10 px-2 py-0.5 font-mono text-[11px] text-cyanish"
                >
                  <Quote className="h-3 w-3" /> {c}
                </span>
              ),
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
