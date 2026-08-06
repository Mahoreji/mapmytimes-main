import Link from "next/link";
import type { Metadata } from "next";
import { copyrightNotice } from "@/content/legal";
import { SITE } from "@/lib/utils";
import { Copyright, ArrowLeft } from "lucide-react";
import { formatDate } from "@/lib/utils";

export const metadata: Metadata = {
  title: `${copyrightNotice.title} | MapMyTimes — Content Ownership & Takedown`,
  description: copyrightNotice.description,
  alternates: { canonical: `/${copyrightNotice.slug}` },
  openGraph: {
    title: `${copyrightNotice.title} — ${SITE.name}`,
    description: copyrightNotice.description,
    url: `/${copyrightNotice.slug}`,
    type: "article",
  },
  twitter: {
    title: `${copyrightNotice.title} — ${SITE.name}`,
    description: copyrightNotice.description,
  },
  other: {
    "article-section": "Legal / Copyright",
    "article-tag": "Copyright Notice, DMCA, Takedown, Reprint Permission, Content Licensing, MapMyTimes",
  },
};

export default function CopyrightNoticePage() {
  const page = copyrightNotice;
  const year = new Date().getFullYear();
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
              <Copyright className="h-4 w-4 text-news" />
              <span>© {year} {SITE.name} · Last updated: {formatDate(page.lastUpdated, { day: "2-digit", month: "long", year: "numeric" })}</span>
            </div>
            <span className="text-white/20">·</span>
            <span>Published by {SITE.name}</span>
          </div>
          <p className="mt-6 max-w-2xl text-white/80 text-base sm:text-lg leading-relaxed">
            Content ownership, what you may and may not share or republish, how to request a
            reprint licence, and the DMCA-style procedure for notifying us of copyright
            infringements on our platforms.
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
          <h3 className="font-headline uppercase text-xl mb-3">Re-print, syndication or licensing?</h3>
          <p className="text-sm text-white/80 leading-relaxed mb-4">
            Want to republish a MapMyTimes article, image, or fact-check in your publication,
            website, book, newsletter, or documentary? Request a licence in writing — our desk
            responds within 15 working days for complete requests.
          </p>
          <Link
            href={`mailto:${SITE.email}?subject=Reprint%20%2F%20Licensing%20Request`}
            className="inline-flex items-center gap-2 bg-news hover:bg-news/90 border-2 border-news text-white px-5 py-2.5 font-bold uppercase tracking-wider text-xs sm:text-sm transition-colors"
          >
            Email Copyright & Licensing desk
          </Link>
        </div>
      </article>
    </div>
  );
}
