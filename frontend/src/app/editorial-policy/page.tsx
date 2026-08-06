import Link from "next/link";
import type { Metadata } from "next";
import { editorialPolicy } from "@/content/legal";
import { SITE } from "@/lib/utils";
import { FileEdit, ArrowLeft } from "lucide-react";
import { formatDate } from "@/lib/utils";

export const metadata: Metadata = {
  title: `${editorialPolicy.title} | MapMyTimes Newsroom Standards`,
  description: editorialPolicy.description,
  alternates: { canonical: `/${editorialPolicy.slug}` },
  openGraph: {
    title: `${editorialPolicy.title} — ${SITE.name}`,
    description: editorialPolicy.description,
    url: `/${editorialPolicy.slug}`,
    type: "article",
  },
  twitter: {
    title: `${editorialPolicy.title} — ${SITE.name}`,
    description: editorialPolicy.description,
  },
  other: {
    "article-section": "Policy / Legal",
    "article-tag": "Editorial Policy, Newsroom Standards, MapMyTimes, MAPMYTOUR LLP",
  },
};

export default function EditorialPolicyPage() {
  const page = editorialPolicy;
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
              <FileEdit className="h-4 w-4 text-news" />
              <span>Last updated: {formatDate(page.lastUpdated, { day: "2-digit", month: "long", year: "numeric" })}</span>
            </div>
            <span className="text-white/20">·</span>
            <span>Published by {SITE.name}</span>
          </div>
          <p className="mt-6 max-w-2xl text-white/80 text-base sm:text-lg leading-relaxed">
            The editorial standards that govern every story, headline, and correction MapMyTimes
            publishes. Read this page to understand our values, sourcing, sponsored content
            disclosures, and how we uphold editorial independence.
          </p>
        </div>
      </section>

      <article className="mx-auto max-w-3xl px-4 py-14 sm:py-18">
        <div className="prose-headings:font-headline prose-headings:uppercase prose-headings:tracking-tight prose-h2:text-2xl prose-h2:sm:text-3xl prose-h2:mb-4 prose-h2:mt-12 prose-p:my-5 prose-p:leading-relaxed prose-p:text-ink-800 prose-li:my-2.5 prose-li:leading-relaxed prose-li:text-ink-800 prose-ul:my-5 prose-ul:pl-6 prose-ul:list-disc prose-ul:marker:text-news space-y-1">
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
          <h3 className="font-headline uppercase text-xl mb-3">Questions about our standards?</h3>
          <p className="text-sm text-white/80 leading-relaxed mb-4">
            Write to our editors — we answer every serious inquiry relating to newsroom standards,
            corrections, and independence.
          </p>
          <Link
            href="/contact"
            className="inline-flex items-center gap-2 bg-news hover:bg-news/90 border-2 border-news text-white px-5 py-2.5 font-bold uppercase tracking-wider text-xs sm:text-sm transition-colors"
          >
            Contact the newsroom
          </Link>
        </div>
      </article>
    </div>
  );
}
