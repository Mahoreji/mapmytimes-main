"use client";

import Link from "next/link";
import { Mic, Mail, Phone, MapPin, Facebook, Twitter, Instagram, Youtube, Linkedin } from "lucide-react";
import { SITE } from "@/lib/utils";
import { BrandLogo } from "@/components/site/SiteHeader";
import { useLanguage } from "@/lib/i18n/LanguageContext";

export function SiteFooter() {
  const { t } = useLanguage();
  const socialList = [
    { label: "Facebook", href: SITE.socials.facebook, Icon: Facebook },
    { label: "X / Twitter", href: SITE.socials.twitter, Icon: Twitter },
    { label: "Instagram", href: SITE.socials.instagram, Icon: Instagram },
    { label: "YouTube", href: SITE.socials.youtube, Icon: Youtube },
    { label: "LinkedIn", href: SITE.socials.linkedin, Icon: Linkedin },
  ] as const;

  return (
    <footer className="mt-16 bg-ink-950 text-white border-t-4 border-news">
      <div className="mx-auto max-w-7xl px-4 py-12 grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-10">
        <div className="space-y-5 sm:col-span-2 md:col-span-3 lg:col-span-1">
          <Link href="/" aria-label="MapMyTimes — Home" className="inline-block">
            <BrandLogo className="h-24 sm:h-28 md:h-32 w-auto drop-shadow-[0_4px_0_rgba(227,30,36,0.4)]" />
          </Link>
          <p className="text-sm text-white/75 leading-relaxed">
            MapMyTimes is an independent news platform committed to verified, unflinching
            journalism — reports, investigations, and storytelling that serves the public good.
          </p>
          <div className="pt-2">
            <div className="flex items-center gap-3 mb-3">
              <span className="h-1.5 w-1.5 rounded-full bg-news" />
              <h5 className="font-headline uppercase tracking-[0.18em] text-[11px] sm:text-xs text-white/85">
                {t.common.footer.followUs}
              </h5>
            </div>
            <div className="flex items-center flex-wrap gap-2">
              {socialList.map(({ label, href, Icon }) => (
                <a
                  key={label}
                  href={href}
                  target="_blank"
                  rel="noopener noreferrer"
                  aria-label={`${label} — ${t.common.footer.followUs}`}
                  title={label}
                  className="group h-10 w-10 flex items-center justify-center border-2 border-white/25 text-white/80 bg-ink-900 hover:bg-news hover:border-news hover:text-white hover:-translate-y-0.5 hover:shadow-[0_3px_0_0_rgba(227,30,36,0.7)] transition-all"
                >
                  <Icon className="w-4.5 h-4.5" strokeWidth={2} />
                </a>
              ))}
            </div>
          </div>
        </div>

        <div>
          <h4 className="font-headline uppercase text-lg mb-4">Sections</h4>
          <ul className="grid grid-cols-2 gap-y-2 text-sm text-white/80">
            {[
              ["Shorts", "/shorts"],
              ["India", "/category/india"],
              ["World", "/category/world"],
              ["Business", "/category/business"],
              ["Technology", "/category/technology"],
              ["Sports", "/category/sports"],
              ["Politics", "/category/politics"],
              ["Culture", "/category/culture"],
              ["Opinion", "/category/opinion"],
              ["All Sections", "/sections"],
              ["Search", "/search"],
              ["Trending Tags", "/tag/india"],
            ].map(([label, href]) => (
              <li key={href}>
                <Link href={href} className="hover:text-news">
                  {label}
                </Link>
              </li>
            ))}
          </ul>
        </div>

        <div>
          <h4 className="font-headline uppercase text-lg mb-4">Company</h4>
          <ul className="flex flex-col gap-2 text-sm text-white/80">
            <li>
              <Link href="/about" className="hover:text-news">About MapMyTimes</Link>
            </li>
            <li>
              <Link href="/contact" className="hover:text-news">Contact Newsroom</Link>
            </li>
            <li>
              <Link href="/explore" className="hover:text-news">Explore</Link>
            </li>
            <li>
              <Link href="/careers" className="hover:text-news">Careers & Journalism Jobs</Link>
            </li>
            <li>
              <Link href="/signup" className="hover:text-news">Sign up — Create free account</Link>
            </li>
            <li>
              <Link href="/login" className="hover:text-news">Sign in</Link>
            </li>
            <li>
              <Link href="/forgot-password" className="hover:text-news">Forgot Password</Link>
            </li>
          </ul>
        </div>

        <div>
          <h4 className="font-headline uppercase text-lg mb-4">Newsroom</h4>
          <ul className="flex flex-col gap-3 text-sm text-white/80">
            <li className="flex items-start gap-3">
              <Mail className="h-4 w-4 text-news mt-0.5" />
              <a href={`mailto:${SITE.email}`} className="hover:text-news break-all">
                {SITE.email}
              </a>
            </li>
            <li className="flex items-start gap-3">
              <Phone className="h-4 w-4 text-news mt-0.5" />
              <a href={`tel:${SITE.phone}`} className="hover:text-news">
                {SITE.phone}
              </a>
            </li>
            <li className="flex items-start gap-3">
              <MapPin className="h-4 w-4 text-news mt-0.5" />
              <span>MAPMYTOUR LLP, India</span>
            </li>
          </ul>
        </div>

        <div>
          <h4 className="font-headline uppercase text-lg mb-4">Legal & Policies</h4>
          <ul className="flex flex-col gap-2 text-sm text-white/80">
            <li>
              <Link href="/editorial-policy" className="hover:text-news">Editorial Policy</Link>
            </li>
            <li>
              <Link href="/fact-check-policy" className="hover:text-news">Fact-Check Policy</Link>
            </li>
            <li>
              <Link href="/privacy-policy" className="hover:text-news">Privacy Policy</Link>
            </li>
            <li>
              <Link href="/terms-and-conditions" className="hover:text-news">Terms & Conditions</Link>
            </li>
            <li>
              <Link href="/copyright-notice" className="hover:text-news">Copyright Notice</Link>
            </li>
          </ul>
        </div>
      </div>

      <div className="border-t-2 border-white/10">
        <div className="mx-auto max-w-7xl px-4 py-5 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-white/60">
          <div className="flex items-center gap-2">
            <Mic className="h-3.5 w-3.5 text-news" />
            <span>© {new Date().getFullYear()} MapMyTimes — MAPMYTOUR LLP. All rights reserved.</span>
          </div>
          <div className="flex flex-wrap items-center justify-center gap-x-5 gap-y-2">
            <Link href="/about" className="hover:text-white">About</Link>
            <Link href="/contact" className="hover:text-white">Contact</Link>
            <Link href="/privacy-policy" className="hover:text-white">Privacy</Link>
            <Link href="/terms-and-conditions" className="hover:text-white">Terms</Link>
            <Link href="/copyright-notice" className="hover:text-white">Copyright</Link>
            <span className="text-white/30">|</span>
            <span className="tracking-widest font-bold text-white/80">
              {SITE.tagline}
            </span>
          </div>
        </div>
      </div>
    </footer>
  );
}
