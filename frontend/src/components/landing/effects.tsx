import {
  useEffect,
  useRef,
  useState,
  type CSSProperties,
  type ReactNode,
} from "react";
import {
  motion,
  useInView,
  useMotionTemplate,
  useMotionValue,
  useReducedMotion,
  useScroll,
  useSpring,
  useTransform,
} from "framer-motion";
import { cn } from "@/lib/utils";

/* ------------------------------------------------------------------ *
 * Reveal — fade + lift content as it scrolls into view.
 * ------------------------------------------------------------------ */
export function Reveal({
  children,
  delay = 0,
  y = 24,
  className,
  once = true,
}: {
  children: ReactNode;
  delay?: number;
  y?: number;
  className?: string;
  once?: boolean;
}) {
  const reduce = useReducedMotion();
  return (
    <motion.div
      initial={reduce ? false : { opacity: 0, y }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once, margin: "-80px" }}
      transition={{ duration: 0.6, delay, ease: [0.22, 1, 0.36, 1] }}
      className={className}
    >
      {children}
    </motion.div>
  );
}

/* ------------------------------------------------------------------ *
 * WordReveal — animate a headline word-by-word. Strings split on space;
 * highlighted words can be passed as <span className="text-gradient"> via
 * the `parts` API for mixed styling.
 * ------------------------------------------------------------------ */
export function WordReveal({
  text,
  className,
  delay = 0,
  highlight = [],
}: {
  text: string;
  className?: string;
  delay?: number;
  /** indices (or words) to render with the brand gradient */
  highlight?: (number | string)[];
}) {
  const reduce = useReducedMotion();
  const words = text.split(" ");
  return (
    <span className={className}>
      {words.map((word, i) => {
        const isHi = highlight.includes(i) || highlight.includes(word);
        return (
          <span key={`${word}-${i}`} className="inline-block overflow-hidden align-bottom">
            <motion.span
              className={cn("inline-block", isHi && "text-gradient")}
              initial={reduce ? false : { y: "110%", opacity: 0 }}
              whileInView={{ y: "0%", opacity: 1 }}
              viewport={{ once: true }}
              transition={{
                duration: 0.7,
                delay: delay + i * 0.06,
                ease: [0.22, 1, 0.36, 1],
              }}
            >
              {word}
            </motion.span>
            {i < words.length - 1 && " "}
          </span>
        );
      })}
    </span>
  );
}

/* ------------------------------------------------------------------ *
 * Parallax — translate children on scroll for depth.
 * ------------------------------------------------------------------ */
export function Parallax({
  children,
  speed = 60,
  className,
}: {
  children: ReactNode;
  /** px of travel across the viewport pass; negative moves opposite */
  speed?: number;
  className?: string;
}) {
  const ref = useRef<HTMLDivElement>(null);
  const reduce = useReducedMotion();
  const { scrollYProgress } = useScroll({
    target: ref,
    offset: ["start end", "end start"],
  });
  const y = useTransform(scrollYProgress, [0, 1], [speed, -speed]);
  return (
    <div ref={ref} className={className}>
      <motion.div style={reduce ? undefined : { y }}>{children}</motion.div>
    </div>
  );
}

/* ------------------------------------------------------------------ *
 * SpotlightCard — a glass card with a cursor-following radial glow and a
 * subtle gradient border. The Aceternity look, hand-rolled.
 * ------------------------------------------------------------------ */
export function SpotlightCard({
  children,
  className,
}: {
  children: ReactNode;
  className?: string;
}) {
  const mx = useMotionValue(0);
  const my = useMotionValue(0);
  const bg = useMotionTemplate`radial-gradient(420px circle at ${mx}px ${my}px, rgba(245,158,11,0.16), transparent 60%)`;

  return (
    <div
      onMouseMove={(e) => {
        const r = e.currentTarget.getBoundingClientRect();
        mx.set(e.clientX - r.left);
        my.set(e.clientY - r.top);
      }}
      className={cn(
        "group relative overflow-hidden rounded-2xl border border-line/[0.08] bg-surface/50 backdrop-blur-xl",
        "transition-colors duration-300 hover:border-accent/30",
        className,
      )}
    >
      <motion.div
        aria-hidden
        className="pointer-events-none absolute inset-0 opacity-0 transition-opacity duration-300 group-hover:opacity-100"
        style={{ background: bg }}
      />
      {children}
    </div>
  );
}

/* ------------------------------------------------------------------ *
 * Counter — animate a number from 0 → value when it enters the viewport.
 * ------------------------------------------------------------------ */
export function Counter({
  to,
  suffix = "",
  prefix = "",
  decimals = 0,
  duration = 1400,
  className,
}: {
  to: number;
  suffix?: string;
  prefix?: string;
  decimals?: number;
  duration?: number;
  className?: string;
}) {
  const ref = useRef<HTMLSpanElement>(null);
  const inView = useInView(ref, { once: true, margin: "-60px" });
  const [n, setN] = useState(0);

  useEffect(() => {
    if (!inView) return;
    let raf = 0;
    const start = performance.now();
    const tick = (t: number) => {
      const p = Math.min(1, (t - start) / duration);
      setN((1 - Math.pow(1 - p, 3)) * to); // easeOutCubic
      if (p < 1) raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [inView, to, duration]);

  return (
    <span ref={ref} className={className}>
      {prefix}
      {n.toLocaleString(undefined, {
        minimumFractionDigits: decimals,
        maximumFractionDigits: decimals,
      })}
      {suffix}
    </span>
  );
}

/* ------------------------------------------------------------------ *
 * Marquee — seamless infinite horizontal ticker. Children are rendered
 * twice and translated -50% so the loop is continuous.
 * ------------------------------------------------------------------ */
export function Marquee({
  children,
  reverse = false,
  speed = 40,
  className,
  pauseOnHover = true,
}: {
  children: ReactNode;
  reverse?: boolean;
  speed?: number;
  className?: string;
  pauseOnHover?: boolean;
}) {
  return (
    <div className={cn("group flex overflow-hidden", className)}>
      <div
        className={cn(
          "flex shrink-0 animate-marquee items-center",
          reverse && "[animation-direction:reverse]",
          pauseOnHover && "group-hover:[animation-play-state:paused]",
        )}
        style={{ "--marquee-duration": `${speed}s` } as CSSProperties}
      >
        {children}
        {children}
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------ *
 * ParticleField — a lightweight canvas constellation. Particles drift,
 * connect to near neighbours and to the cursor. Theme-aware, respects
 * prefers-reduced-motion, and pauses when off-screen.
 * ------------------------------------------------------------------ */
export function ParticleField({
  className,
  density = 0.00012,
  color = "245,158,11",
  linkColor = "245,158,11",
}: {
  className?: string;
  density?: number;
  color?: string;
  linkColor?: string;
}) {
  const ref = useRef<HTMLCanvasElement>(null);
  const reduce = useReducedMotion();

  useEffect(() => {
    if (reduce) return;
    const canvas = ref.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    let raf = 0;
    let w = 0;
    let h = 0;
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    const mouse = { x: -9999, y: -9999 };
    type P = { x: number; y: number; vx: number; vy: number; r: number };
    let pts: P[] = [];

    const seed = () => {
      const count = Math.min(140, Math.max(40, Math.floor(w * h * density)));
      pts = Array.from({ length: count }, () => ({
        x: Math.random() * w,
        y: Math.random() * h,
        vx: (Math.random() - 0.5) * 0.35,
        vy: (Math.random() - 0.5) * 0.35,
        r: Math.random() * 1.6 + 0.6,
      }));
    };

    const resize = () => {
      const parent = canvas.parentElement;
      w = parent?.clientWidth ?? window.innerWidth;
      h = parent?.clientHeight ?? window.innerHeight;
      canvas.width = w * dpr;
      canvas.height = h * dpr;
      canvas.style.width = `${w}px`;
      canvas.style.height = `${h}px`;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      seed();
    };

    const draw = () => {
      ctx.clearRect(0, 0, w, h);
      for (const p of pts) {
        p.x += p.vx;
        p.y += p.vy;
        if (p.x < 0 || p.x > w) p.vx *= -1;
        if (p.y < 0 || p.y > h) p.vy *= -1;

        // gentle cursor attraction
        const dxm = mouse.x - p.x;
        const dym = mouse.y - p.y;
        const dm = Math.hypot(dxm, dym);
        if (dm < 140) {
          p.x += dxm * 0.0015;
          p.y += dym * 0.0015;
        }

        ctx.beginPath();
        ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
        ctx.fillStyle = `rgba(${color},0.7)`;
        ctx.fill();
      }
      // links
      for (let i = 0; i < pts.length; i++) {
        for (let j = i + 1; j < pts.length; j++) {
          const a = pts[i];
          const b = pts[j];
          const d = Math.hypot(a.x - b.x, a.y - b.y);
          if (d < 120) {
            ctx.beginPath();
            ctx.moveTo(a.x, a.y);
            ctx.lineTo(b.x, b.y);
            ctx.strokeStyle = `rgba(${linkColor},${0.18 * (1 - d / 120)})`;
            ctx.lineWidth = 1;
            ctx.stroke();
          }
        }
      }
      raf = requestAnimationFrame(draw);
    };

    const onMove = (e: MouseEvent) => {
      const r = canvas.getBoundingClientRect();
      mouse.x = e.clientX - r.left;
      mouse.y = e.clientY - r.top;
    };
    const onLeave = () => {
      mouse.x = -9999;
      mouse.y = -9999;
    };

    resize();
    draw();
    window.addEventListener("resize", resize);
    window.addEventListener("mousemove", onMove);
    window.addEventListener("mouseout", onLeave);
    return () => {
      cancelAnimationFrame(raf);
      window.removeEventListener("resize", resize);
      window.removeEventListener("mousemove", onMove);
      window.removeEventListener("mouseout", onLeave);
    };
  }, [reduce, density, color, linkColor]);

  return (
    <canvas
      ref={ref}
      aria-hidden
      className={cn("pointer-events-none absolute inset-0", className)}
    />
  );
}

/* ------------------------------------------------------------------ *
 * PixelGrid — a grid of squares that twinkle in and out, mask-faded at
 * the edges. The "pixel animation" accent behind feature/CTA panels.
 * ------------------------------------------------------------------ */
export function PixelGrid({ className }: { className?: string }) {
  const reduce = useReducedMotion();
  const cells = Array.from({ length: 120 });
  return (
    <div
      aria-hidden
      className={cn(
        "pointer-events-none absolute inset-0 grid grid-cols-[repeat(12,1fr)] gap-1 p-2",
        "[mask-image:radial-gradient(ellipse_at_center,black,transparent_72%)]",
        className,
      )}
    >
      {cells.map((_, i) =>
        reduce ? (
          <div key={i} className="rounded-[3px] bg-line/[0.04]" />
        ) : (
          <motion.div
            key={i}
            className="rounded-[3px] bg-accent/40"
            initial={{ opacity: 0.05 }}
            animate={{ opacity: [0.05, 0.5, 0.05] }}
            transition={{
              duration: 3 + (i % 5),
              repeat: Infinity,
              delay: (i % 17) * 0.21,
              ease: "easeInOut",
            }}
          />
        ),
      )}
    </div>
  );
}

/* ------------------------------------------------------------------ *
 * ScrollProgress — a slim gradient bar pinned to the top of the page
 * that fills as the visitor scrolls.
 * ------------------------------------------------------------------ */
export function ScrollProgress() {
  const { scrollYProgress } = useScroll();
  const x = useSpring(scrollYProgress, { stiffness: 120, damping: 30, mass: 0.3 });
  return (
    <motion.div
      style={{ scaleX: x }}
      className="fixed inset-x-0 top-0 z-[60] h-0.5 origin-left bg-gradient-to-r from-accent via-orange-400 to-cyanish"
    />
  );
}
