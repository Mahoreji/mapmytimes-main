import Link from "next/link";
import type { Metadata } from "next";
import { factCheckPolicy } from "@/content/legal";
import { SITE } from "@/lib/utils";
import { SearchCheck, ArrowLeft } from "lucide-react";
import { formatDate } from "@/lib/utils";

export const metadata: Metadata = {
  title: `${factCheckPolicy.title} | MapMyTimes Verification Standards`,
  description: factCheckPolicy.description,
  alternates: { canonical: `/${factCheckPolicy.slug}` },
  openGraph: {
    title: `${factCheckPolicy.title} — ${SITE.name}`,
    description: factCheckPolicy.description,
    url: `/${factCheckPolicy.slug}`,
    type: "article",
  },
  twitter: {
    title: `${factCheckPolicy.title} — ${SITE.name}`,
    description: factCheckPolicy.description,
  },
  other: {
    "article-section": "Policy / Fact Checking",
    "article-tag": "Fact Check Policy, Verification, Misinformation, News Accuracy, MapMyTimes",
  },
};

export default function FactCheckPolicyPage() {
  const page = factCheckPolicy;
  return (
    <div>
      <section className="bg-ink-950 text-white border-b-4 border-news">
        <div className="mx-auto max-w-4xl px-4 py-14 sm:py-18">
          <Link
            href="/about"
            className="inline-flex items-center gap-2 text-[11px] font-bold uppercase tracking-widest text-white/60 hover:text-white mb-4"
          >
            <ArrowLeft className="h-3.5 w-3.5" />
            About MapMyTimes
          </Link>
          <div className="ribbon text-xs mb-4">{page.eyebrow}</div>
          <h1 className="font-headline text-4xl sm:text-5xl md:text-6xl uppercase leading-none">
            {page.title}
          </h1>
          <div className="mt-6 flex flex-wrap items-center gap-4 text-sm text-white/70">
            <div className="inline-flex items-center gap-2">
              <SearchCheck className="h-4 w-4 text-news" />
              <span>Last updated: {formatDate(page.lastUpdated, { day: "2-digit", month: "long", year: "numeric" })}</span>
            </div>
            <span className="text-white/20">·</span>
            <span>Published by {SITE.name}</span>
          </div>
          <p className="mt-6 max-w-2xl text-white/80 text-base sm:text-lg leading-relaxed">
            How MapMyTimes verifies claims, ranks sources, publishes corrections, and distinguishes
            news, opinion, and fact-check content — plus how readers can flag misinformation.
          </p>
        </div>
      </section>

      <article className="mx-auto max-w-3xl px-4 py-14 sm:py-18">
        <div className="space-y-1">
          {page.sections.map((s) => (
            <section key={s.heading} className="scroll-mt-24 border-b border-ink-950/10 pb-10 last:border-none">
              <h2 className="font-headline uppercase text-2xl sm:text-3xl mb-4">{s.heading}</h2>
              {s.paragraphs?.map((p, i) => (
                <p key={i} className="my-5 leading-relaxed text-ink-800 text-[15px] sm:text-base">
                  {p}
                </p>
              ))}
              {s.list && (
                <ul className="my-5 pl-6 list-disc marker:text-news space-y-2.5">
                  {s.list.map((li, i) => (
                    <li key={i} className="leading-relaxed text-ink-800 text-[15px] sm:text-base">
                      {li}
                    </li>
                  ))}
                </ul>
              )}
              {s.midParagraphs?.map((p, i) => (
                <p key={`mp-${i}`} className="my-5 leading-relaxed text-ink-800 text-[15px] sm:text-base">
                  {p}
                </p>
              ))}
              {s.secondList && (
                <ul className="my-5 pl-6 list-disc marker:text-news space-y-2.5">
                  {s.secondList.map((li, i) => (
                    <li key={`sl-${i}`} className="leading-relaxed text-ink-800 text-[15px] sm:text-base">
                      {li}
                    </li>
                  ))}
                </ul>
              )}
              {s.postListParagraphs?.map((p, i) => (
                <p key={`plp-${i}`} className="my-5 leading-relaxed text-ink-800 text-[15px] sm:text-base">
                  {p}
                </p>
              ))}
            </section>
          ))}
        </div>

        <div className="mt-14 bg-ink-950 text-white border-2 border-ink-950 p-6 sm:p-8">
          <h3 className="font-headline uppercase text-xl mb-3">Spot something that needs checking?</h3>
          <p className="text-sm text-white/80 leading-relaxed mb-4">
            Flag a story, share evidence, or submit a claim for our fact-check desk. We respond to
            every complete submission.
          </p>
          <Link
            href="/contact"
            className="inline-flex items-center gap-2 bg-news hover:bg-news/90 border-2 border-news text-white px-5 py-2.5 font-bold uppercase tracking-wider text-xs sm:text-sm transition-colors"
          >
            Submit to Fact-Check desk
          </Link>
        </div>
      </article>
    </div>
  );
}
