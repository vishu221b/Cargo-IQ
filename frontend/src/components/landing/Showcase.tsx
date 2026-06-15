import { useRef } from "react";
import { motion, useScroll, useTransform } from "framer-motion";
import {
  Ship,
  ReceiptText,
  Landmark,
  Globe2,
  Boxes,
  ScrollText,
} from "lucide-react";
import { PixelGrid } from "./effects";

const DOCS = [
  {
    icon: Ship,
    name: "Bills of Lading",
    blurb: "Carriers, ports, consignees, freight terms — extracted and queryable.",
    tag: "B/L",
  },
  {
    icon: ReceiptText,
    name: "Commercial Invoices",
    blurb: "Line items, values, currencies and totals, reconciled to shipments.",
    tag: "CI",
  },
  {
    icon: Landmark,
    name: "Letters of Credit",
    blurb: "Terms, expiry, beneficiaries and documentary conditions, in plain English.",
    tag: "L/C",
  },
  {
    icon: Globe2,
    name: "INCOTERMS 2020",
    blurb: "EXW → DDP. Who bears cost, risk and insurance, at every transfer point.",
    tag: "Rules",
  },
  {
    icon: Boxes,
    name: "HS Codes",
    blurb: "Tariff classification lookups grounded in the harmonised schedule.",
    tag: "Tariff",
  },
  {
    icon: ScrollText,
    name: "Customs & Trade Finance",
    blurb: "The long tail of declarations and finance docs, one retrieval layer.",
    tag: "More",
  },
];

export default function Showcase() {
  const ref = useRef<HTMLDivElement>(null);
  const { scrollYProgress } = useScroll({
    target: ref,
    offset: ["start start", "end end"],
  });
  // Slide the track from 2% to roughly -62% of its width across the scroll.
  const x = useTransform(scrollYProgress, [0, 1], ["2%", "-62%"]);

  return (
    <section id="showcase" ref={ref} className="relative h-[280vh]">
      <div className="sticky top-0 flex h-screen flex-col justify-center overflow-hidden">
        <div className="mx-auto mb-10 w-full max-w-6xl px-5 md:px-10">
          <p className="text-sm font-medium uppercase tracking-[0.2em] text-accent-glow">
            One retrieval layer
          </p>
          <h2 className="mt-3 max-w-2xl text-balance text-4xl font-semibold tracking-tight text-fg sm:text-5xl">
            Every trade document, <span className="text-gradient">one source of truth.</span>
          </h2>
        </div>

        <motion.div style={{ x }} className="flex gap-6 px-5 md:px-10">
          {DOCS.map((d) => (
            <article
              key={d.name}
              className="group relative flex h-[22rem] w-[20rem] shrink-0 flex-col justify-between overflow-hidden rounded-3xl border border-line/[0.08] bg-surface/50 p-7 backdrop-blur-xl transition hover:border-accent/30 sm:w-[24rem]"
            >
              <PixelGrid className="opacity-50" />
              <div className="relative">
                <div className="flex items-center justify-between">
                  <div className="grid h-12 w-12 place-items-center rounded-2xl bg-gradient-to-br from-accent/25 to-cyanish/10 text-accent-glow">
                    <d.icon className="h-6 w-6" />
                  </div>
                  <span className="rounded-full border border-line/[0.10] bg-surface/70 px-2.5 py-1 font-mono text-[11px] text-faint">
                    {d.tag}
                  </span>
                </div>
                <h3 className="mt-6 text-2xl font-semibold tracking-tight text-fg">
                  {d.name}
                </h3>
                <p className="mt-3 text-[15px] leading-relaxed text-muted">
                  {d.blurb}
                </p>
              </div>
              <div className="relative h-px w-full bg-gradient-to-r from-accent/40 via-cyanish/30 to-transparent" />
            </article>
          ))}
        </motion.div>

        <div className="mx-auto mt-8 w-full max-w-6xl px-5 text-sm text-faint md:px-10">
          Keep scrolling — the corpus scrolls with you →
        </div>
      </div>
    </section>
  );
}
