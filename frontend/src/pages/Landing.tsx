import { useEffect } from "react";
import LandingNav from "@/components/landing/LandingNav";
import Hero from "@/components/landing/Hero";
import Showcase from "@/components/landing/Showcase";
import Pricing from "@/components/landing/Pricing";
import Faq from "@/components/landing/Faq";
import { ScrollProgress } from "@/components/landing/effects";
import {
  LogosStrip,
  Features,
  HowItWorks,
  AnswerDemo,
  Stats,
  FinalCta,
  Footer,
} from "@/components/landing/sections";

/**
 * The public marketing landing page. Mounted at "/", it leads visitors from the
 * core promise (cited answers over trade documents) to the product, the proof,
 * and a free sign-up. The authenticated product lives under /app.
 */
export default function Landing() {
  // Smooth in-page anchor scrolling for the nav links.
  useEffect(() => {
    const root = document.documentElement;
    const prev = root.style.scrollBehavior;
    root.style.scrollBehavior = "smooth";
    return () => {
      root.style.scrollBehavior = prev;
    };
  }, []);

  return (
    <div className="relative min-h-screen bg-bg text-fg">
      <ScrollProgress />
      <LandingNav />
      <main>
        <Hero />
        <LogosStrip />
        <Features />
        <HowItWorks />
        <Showcase />
        <AnswerDemo />
        <Stats />
        <Pricing />
        <Faq />
        <FinalCta />
      </main>
      <Footer />
    </div>
  );
}
