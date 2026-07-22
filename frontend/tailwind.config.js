/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  darkMode: "class",
  theme: {
    extend: {
      fontFamily: {
        sans: ["Inter", "ui-sans-serif", "system-ui", "sans-serif"],
        mono: ["JetBrains Mono", "ui-monospace", "monospace"],
      },
      colors: {
        // Semantic tokens driven by CSS variables (see index.css) so the whole
        // UI re-themes between dark and light from one switch.
        bg: "rgb(var(--bg) / <alpha-value>)",
        surface: "rgb(var(--surface) / <alpha-value>)",
        surface2: "rgb(var(--surface-2) / <alpha-value>)",
        line: "rgb(var(--line) / <alpha-value>)",
        fg: "rgb(var(--fg) / <alpha-value>)",
        muted: "rgb(var(--muted) / <alpha-value>)",
        faint: "rgb(var(--faint) / <alpha-value>)",
        // Charcoal base with a "Freight Amber" accent system: amber primary,
        // steel-blue support. Warm, industrial, container-yard energy.
        ink: {
          950: "#070c12",
          900: "#0b0f17",
          850: "#0f141d",
          800: "#161c27",
          700: "#1e2635",
          600: "#2a3346",
        },
        accent: {
          DEFAULT: "#f59e0b", // amber-500
          soft: "#fbbf24", // amber-400
          glow: "#fcd34d", // amber-300
        },
        // Steel-blue support colour (kept under the historical `cyanish` name so
        // existing gradients/badges pick it up without a mass rename).
        cyanish: "#38bdf8", // sky-400
      },
      boxShadow: {
        glow: "0 0 40px -10px rgba(245,158,11,0.45)",
        card: "0 1px 0 0 rgba(255,255,255,0.04) inset, 0 20px 50px -20px rgba(0,0,0,0.6)",
      },
      keyframes: {
        aurora: {
          "0%, 100%": { transform: "translate(-10%, -10%) rotate(0deg)" },
          "50%": { transform: "translate(10%, 10%) rotate(180deg)" },
        },
        shimmer: {
          "100%": { transform: "translateX(100%)" },
        },
        "fade-up": {
          "0%": { opacity: "0", transform: "translateY(8px)" },
          "100%": { opacity: "1", transform: "translateY(0)" },
        },
        // Infinite horizontal ticker — content is duplicated, so -50% loops seamlessly.
        marquee: {
          "0%": { transform: "translateX(0)" },
          "100%": { transform: "translateX(-50%)" },
        },
        "marquee-y": {
          "0%": { transform: "translateY(0)" },
          "100%": { transform: "translateY(-50%)" },
        },
        float: {
          "0%, 100%": { transform: "translateY(0)" },
          "50%": { transform: "translateY(-14px)" },
        },
        "gradient-x": {
          "0%, 100%": { "background-position": "0% 50%" },
          "50%": { "background-position": "100% 50%" },
        },
        "spin-slow": {
          to: { transform: "rotate(360deg)" },
        },
        "pulse-glow": {
          "0%, 100%": { opacity: "0.4", transform: "scale(1)" },
          "50%": { opacity: "1", transform: "scale(1.06)" },
        },
        blink: {
          "0%, 100%": { opacity: "1" },
          "50%": { opacity: "0" },
        },
      },
      animation: {
        aurora: "aurora 18s ease-in-out infinite",
        shimmer: "shimmer 2s infinite",
        "fade-up": "fade-up 0.4s ease-out both",
        marquee: "marquee var(--marquee-duration, 40s) linear infinite",
        "marquee-y": "marquee-y var(--marquee-duration, 40s) linear infinite",
        float: "float 7s ease-in-out infinite",
        "gradient-x": "gradient-x 8s ease infinite",
        "spin-slow": "spin-slow 28s linear infinite",
        "pulse-glow": "pulse-glow 4s ease-in-out infinite",
        blink: "blink 1.1s step-end infinite",
      },
    },
  },
  plugins: [],
};
