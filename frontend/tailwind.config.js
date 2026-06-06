/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      fontFamily: {
        sans: ["Inter", "ui-sans-serif", "system-ui", "sans-serif"],
        mono: ["JetBrains Mono", "ui-monospace", "monospace"],
      },
      colors: {
        // Refined slate base with a violet→cyan accent system.
        ink: {
          950: "#070912",
          900: "#0b0e1a",
          850: "#0f1320",
          800: "#141a2b",
          700: "#1c2436",
          600: "#2a3447",
        },
        accent: {
          DEFAULT: "#7c5cff",
          soft: "#9b86ff",
          glow: "#a78bfa",
        },
        cyanish: "#22d3ee",
      },
      boxShadow: {
        glow: "0 0 40px -10px rgba(124,92,255,0.45)",
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
      },
      animation: {
        aurora: "aurora 18s ease-in-out infinite",
        shimmer: "shimmer 2s infinite",
        "fade-up": "fade-up 0.4s ease-out both",
      },
    },
  },
  plugins: [],
};
