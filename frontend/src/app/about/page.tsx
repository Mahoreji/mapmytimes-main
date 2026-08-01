import Link from "next/link";
import { Button } from "@/components/ui/Button";
import { Badge, SectionTitle } from "@/components/posts/PostCard";
import { ShieldCheck, Gauge, Users, Scale, Target, Award } from "lucide-react";
import { SITE } from "@/lib/utils";

export const metadata = {
  title: `About — ${SITE.tagline}`,
  description:
    "Learn about MapMyTimes — Journalism of Integrity. Our mission, newsroom standards, and the team behind independent, verified reporting.",
};

export default function AboutPage() {
  return (
    <div>
      <section className="bg-ink-950 text-white border-b-4 border-news">
        <div className="mx-auto max-w-5xl px-4 py-16 sm:py-20">
          <div className="ribbon text-xs mb-4">About</div>
          <h1 className="font-headline text-4xl sm:text-6xl uppercase leading-none max-w-4xl">
            Journalism of Integrity — the stories that must be told.
          </h1>
          <p className="mt-6 max-w-2xl text-white/80 text-lg leading-relaxed">
            MapMyTimes is an independent digital newsroom under MAPMYTOUR LLP, built for readers
            who want verified facts, unflinching investigations, and storytelling that refuses to
            look away. We do not chase outrage — we chase truth.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link href="/contact">
              <Button variant="news" size="lg">Contact newsroom</Button>
            </Link>
            <Link href="/explore">
              <Button
                variant="outline"
                size="lg"
                className="bg-transparent border-white text-white hover:bg-white hover:text-ink-950"
              >
                Read today&apos;s stories
              </Button>
            </Link>
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-7xl px-4 py-16 grid grid-cols-1 md:grid-cols-3 gap-6">
        {[
          {
            icon: <Target className="h-6 w-6" />,
            title: "Our Mission",
            body:
              "To deliver journalism that serves citizens — holding power to account, amplifying voices on the margins, and giving readers the context they need to form their own opinions.",
          },
          {
            icon: <ShieldCheck className="h-6 w-6" />,
            title: "Accuracy First",
            body:
              "Every story is fact-checked, sourced, and verified before it is published. We correct openly, update transparently, and disclose conflicts clearly.",
          },
          {
            icon: <Users className="h-6 w-6" />,
            title: "For Readers, Not Algorithms",
            body:
              "We build experiences around what matters, not what keeps you scrolling. Journalism, for us, is a public trust — not a content product.",
          },
        ].map((card) => (
          <article key={card.title} className="border-2 border-ink-950 p-6 hover:shadow-hard-sm transition-shadow bg-white">
            <div className="h-12 w-12 bg-news text-white border-2 border-ink-950 inline-flex items-center justify-center">
              {card.icon}
            </div>
            <h2 className="font-headline uppercase text-xl mt-4">{card.title}</h2>
            <p className="text-sm text-ink-800 mt-2 leading-relaxed">{card.body}</p>
          </article>
        ))}
      </section>

      <section className="bg-white border-y-2 border-ink-950">
        <div className="mx-auto max-w-5xl px-4 py-16 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-6">
          {[
            { k: "Accuracy",   v: "100%",  sub: "Fact-checked reporting" },
            { k: "Independence", v: "Editorially Free", sub: "From influence" },
            { k: "Standards",  v: "Strict", sub: "Copy + legal review" },
            { k: "Corrections", v: "Public", sub: "Open & transparent" },
            { k: "Ownership",  v: "MAPMYTOUR LLP", sub: "MapMyTimes brand" },
          ].map((s) => (
            <div key={s.k} className="text-center">
              <div className="font-headline text-3xl sm:text-4xl uppercase text-news leading-none">
                {s.v}
              </div>
              <div className="mt-2 text-xs font-bold uppercase tracking-widest text-ink-950">
                {s.k}
              </div>
              <div className="mt-1 text-xs text-ink-600">{s.sub}</div>
            </div>
          ))}
        </div>
      </section>

      <section className="mx-auto max-w-5xl px-4 py-16">
        <SectionTitle eyebrow="Standards" title="Our editorial principles" />
        <ul className="mt-8 grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-6">
          {[
            {
              icon: <Scale className="h-5 w-5" />,
              title: "Fairness & impartiality",
              text: "We seek all relevant sides, not false balance. Where facts are settled, we say so.",
            },
            {
              icon: <Gauge className="h-5 w-5" />,
              title: "Independence",
              text: "No advertiser, partner, or owner ever dictates what we publish or suppress.",
            },
            {
              icon: <Award className="h-5 w-5" />,
              title: "Honesty in sourcing",
              text: "We name sources wherever possible. Anonymous sourcing is a last resort, never lazy.",
            },
            {
              icon: <Users className="h-5 w-5" />,
              title: "Respect for those we cover",
              text: "We report on harm without replicating it — especially for the vulnerable and marginalised.",
            },
          ].map((p) => (
            <li key={p.title} className="flex gap-4 items-start border-b border-ink-950/10 pb-6">
              <div className="h-10 w-10 flex-shrink-0 bg-ink-950 text-white border-2 border-ink-950 flex items-center justify-center">
                {p.icon}
              </div>
              <div>
                <div className="font-bold uppercase tracking-wider text-sm">{p.title}</div>
                <p className="text-sm text-ink-700 mt-1">{p.text}</p>
              </div>
            </li>
          ))}
        </ul>
      </section>

      <section className="bg-news border-t-4 border-ink-950">
        <div className="mx-auto max-w-5xl px-4 py-14 text-white text-center">
          <Badge variant="ink" className="mb-4">Journalism of Integrity</Badge>
          <h2 className="font-headline text-3xl sm:text-5xl uppercase leading-none">
            Support independent news — read, share, subscribe.
          </h2>
          <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
            <Link href="/signup">
              <Button variant="ink" className="bg-white text-ink-950 hover:bg-ink-950 hover:text-white border-white transition-colors" size="lg">Join our readers</Button>
            </Link>
            <Link href="/contact">
              <Button variant="outline" size="lg" className="bg-transparent border-white text-white hover:bg-white hover:text-ink-950">Pitch a story</Button>
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
