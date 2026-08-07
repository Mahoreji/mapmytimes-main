"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import {
  Search,
  Menu,
  X,
  Mic,
  User,
  LogOut,
  LayoutDashboard,
  Settings,
  Briefcase,
  ChevronDown,
  PlayCircle,
  Newspaper,
} from "lucide-react";
import { cn, SITE } from "@/lib/utils";
import { useAuth } from "@/lib/auth/AuthProvider";
import { useLanguage } from "@/lib/i18n/LanguageContext";
import { IconButton, Button } from "@/components/ui/Button";
import { NotificationBell } from "@/components/notifications/NotificationBell";
import { LanguageToggle } from "@/components/i18n/LanguageToggle";

const NAV = [
  { href: "/", key: "home" },
  { href: "/sections", key: "news", hasDropdown: true },
  { href: "/videos", key: "videos" },
  { href: "/shorts", key: "shorts" },
  { href: "/about", key: "about" },
  { href: "/our-team", key: "ourTeam" },
  { href: "/contact", key: "contact" },
  { href: "/careers", key: "careers" },
] as const;

const NEWS_CATEGORIES = [
  { href: "/category/india", key: "india" },
  { href: "/category/world", key: "world" },
  { href: "/category/business", key: "business" },
  { href: "/category/technology", key: "tech" },
  { href: "/category/sports", key: "sports" },
  { href: "/category/politics", key: "politics" },
  { href: "/category/culture", key: "culture" },
  { href: "/category/opinion", key: "opinion" },
] as const;

export function BrandLogo({ className, variant = "default" }: { className?: string; variant?: "default" | "inverted" }) {
  const fallbackBg = variant === "inverted" ? "bg-white" : "bg-ink-950";
  const fallbackFg = variant === "inverted" ? "text-ink-950" : "text-white";
  return (
    <img
      src="/assets/logos/mapmytimes-logo.png"
      alt="Map My Times — Journalism of Integrity"
      className={className}
      onError={(e) => {
        const el = e.currentTarget;
        if (el.dataset.fb === "1") return;
        el.dataset.fb = "1";
        el.removeAttribute("src");
        el.style.display = "none";
        const wrap = el.parentElement;
        if (!wrap) return;
        const fb = document.createElement("div");
        fb.className = "relative";
        fb.innerHTML = `
          <div class="${fallbackBg} ${fallbackFg} font-headline px-2 sm:px-3 py-1.5 border-2 border-ink-950 shadow-hard-sm">
            <span class="${fallbackFg}">MAP </span>
            <span class="text-news">MY</span>
            <span class="${fallbackFg}"> TIMES</span>
          </div>
          <div class="mt-1 mx-1 bg-news text-white text-[9px] sm:text-[10px] font-headline px-1 text-center tracking-[0.25em] -rotate-1">
            — JOURNALISM OF INTEGRITY —
          </div>
        `;
        wrap.appendChild(fb);
      }}
    />
  );
}

export function SiteHeader() {
  const pathname = usePathname();
  const router = useRouter();
  const auth = useAuth();
  const { t } = useLanguage();
  const [scrolled, setScrolled] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [newsOpen, setNewsOpen] = useState(false);
  const newsRef = useRef<HTMLDivElement>(null);
  const [q, setQ] = useState("");
  const dict = t.nav as Record<string, string>;

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  useEffect(() => {
    setMobileOpen(false);
    setMenuOpen(false);
    setNewsOpen(false);
  }, [pathname]);

  useEffect(() => {
    function onDocClick(e: MouseEvent) {
      if (!newsOpen) return;
      const el = newsRef.current;
      if (el && !el.contains(e.target as Node)) {
        setNewsOpen(false);
      }
    }
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") {
        setNewsOpen(false);
        setMenuOpen(false);
      }
    }
    document.addEventListener("mousedown", onDocClick);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDocClick);
      document.removeEventListener("keydown", onKey);
    };
  }, [newsOpen]);

  function onSearch(e: React.FormEvent) {
    e.preventDefault();
    const value = q.trim();
    if (!value) return;
    router.push(`/search?q=${encodeURIComponent(value)}`);
  }

  return (
    <header
      suppressHydrationWarning
      className={cn(
        "sticky top-0 z-40 w-full border-b-2 border-ink-950 bg-white transition-shadow",
        scrolled && "shadow-[0_4px_0_0_#0A0A0A]",
      )}
    >
      <div className="w-full">
        <div className="bg-ink-950 text-white/90 border-b-2 border-ink-950">
          <div className="mx-auto max-w-7xl px-3 sm:px-4 h-9 sm:h-10 flex items-center gap-3 sm:gap-4 text-[10px] sm:text-[11px] font-bold uppercase tracking-widest" suppressHydrationWarning>
            <div className="hidden sm:flex items-center gap-2 flex-shrink-0" suppressHydrationWarning>
              <span className="h-2 w-2 rounded-full bg-news animate-pulseDot" />
              {t.brand.tagline}
            </div>
            <form onSubmit={onSearch} className="flex-1 max-w-md mx-auto flex items-center border border-white/25 bg-white/5 hover:bg-white/10 focus-within:bg-white/10 focus-within:border-white/50 transition-colors">
              <input
                value={q}
                onChange={(e) => setQ(e.target.value)}
                className="flex-1 h-7 px-2 bg-transparent outline-none text-[11px] sm:text-xs font-sans text-white placeholder:text-white/45"
                placeholder={t.header.searchPlaceholder}
                aria-label="Search"
              />
              <button type="submit" aria-label={t.common.search} className="h-7 w-8 flex items-center justify-center text-white/80 hover:text-white hover:bg-news transition-colors">
                <Search className="h-3.5 w-3.5" />
              </button>
            </form>
            <div className="hidden sm:flex items-center gap-3 text-white/80 flex-shrink-0">
              <LanguageToggle size="sm" variant="dark" />
              <span className="h-4 w-px bg-white/20" />
              <a href={`mailto:${SITE.email}`} className="hover:text-white whitespace-nowrap">
                {SITE.email}
              </a>
              <span className="h-3 w-px bg-white/20" />
              <a href={`tel:${SITE.phone}`} className="hover:text-white whitespace-nowrap">
                {SITE.phone}
              </a>
            </div>
          </div>
        </div>

        <nav className="hidden md:block bg-ink-950 text-white border-b-2 border-ink-950" suppressHydrationWarning>
          <div className="mx-auto max-w-7xl px-3 sm:px-4 flex items-center gap-2 sm:gap-3 py-1" suppressHydrationWarning>
            <Link href="/" aria-label="MapMyTimes — Home" className="flex items-center gap-2 flex-shrink-0 group mr-1 sm:mr-2">
              <BrandLogo className="h-10 sm:h-11 md:h-14 lg:h-16 xl:h-20 w-auto drop-shadow-[0_3px_0_rgba(227,30,36,0.45)] group-hover:scale-[1.03] transition-transform origin-left" />
            </Link>
            <div className="h-8 w-px bg-white/15 mx-1" aria-hidden="true" />
            <div className="flex items-center gap-0.5 sm:gap-1 flex-1">
              {NAV.map((item) => {
                const active =
                  item.key === "news"
                    ? pathname?.startsWith("/category/") || pathname === "/sections" || pathname?.startsWith("/sections")
                    : pathname === item.href || pathname?.startsWith(item.href + "/");
                const labelKey = item.key as string;
                const label = dict[labelKey] ?? (labelKey === "videos" ? "Videos" : labelKey === "news" ? "News" : labelKey);
                if ((item as any).hasDropdown) {
                  return (
                    <div ref={item.key === "news" ? newsRef : undefined} key={item.href} className="relative shrink-0">
                      <button
                        type="button"
                        onClick={() => setNewsOpen((v) => !v)}
                        aria-haspopup="menu"
                        aria-expanded={newsOpen}
                        className={cn(
                          "px-2 sm:px-3 h-8 sm:h-9 flex items-center gap-1 text-[11px] sm:text-xs font-bold uppercase tracking-[0.14em] whitespace-nowrap",
                          active
                            ? "bg-news text-white"
                            : "text-white/85 hover:text-white hover:bg-ink-800",
                        )}
                      >
                        <Newspaper className="w-3.5 h-3.5" />
                        {label}
                        <ChevronDown className={cn("w-3.5 h-3.5 transition-transform", newsOpen ? "rotate-180" : "")} />
                      </button>
                      {newsOpen ? (
                        <div
                          role="menu"
                          className="absolute left-0 mt-1 w-64 border-2 border-ink-950 bg-white text-ink-950 shadow-hard z-50"
                        >
                          <div className="px-3 py-2 border-b-2 border-ink-950 bg-news text-white">
                            <div className="text-[10px] font-bold uppercase tracking-[0.25em] opacity-90">
                              {t.sections.title ?? "All Categories"}
                            </div>
                            <Link
                              href="/sections"
                              onClick={() => setNewsOpen(false)}
                              className="mt-0.5 font-headline text-sm uppercase hover:underline inline-flex items-center gap-1"
                            >
                              {t.common.exploreMore ?? "Explore all sections"} →
                            </Link>
                          </div>
                          <ul className="p-1.5 flex flex-col">
                            {NEWS_CATEGORIES.map((c) => {
                              const cActive = pathname === c.href || pathname?.startsWith(c.href + "/");
                              const cLabel = dict[c.key] ?? c.key;
                              return (
                                <li key={c.href}>
                                  <Link
                                    href={c.href}
                                    onClick={() => setNewsOpen(false)}
                                    role="menuitem"
                                    className={cn(
                                      "flex items-center justify-between gap-2 px-2.5 py-2 text-sm font-bold uppercase tracking-wide rounded-sm",
                                      cActive
                                        ? "bg-news text-white"
                                        : "hover:bg-ink-950 hover:text-white",
                                    )}
                                  >
                                    <span className="truncate">{cLabel}</span>
                                    <ChevronDown className="w-3.5 h-3.5 -rotate-90 opacity-60 shrink-0" />
                                  </Link>
                                </li>
                              );
                            })}
                          </ul>
                        </div>
                      ) : null}
                    </div>
                  );
                }
                if (item.key === "videos") {
                  return (
                    <Link
                      key={item.href}
                      href={item.href}
                      className={cn(
                        "px-2 sm:px-3 h-8 sm:h-9 flex items-center gap-1 text-[11px] sm:text-xs font-bold uppercase tracking-[0.14em] whitespace-nowrap",
                        active
                          ? "bg-news text-white"
                          : "text-white/85 hover:text-white hover:bg-ink-800",
                      )}
                    >
                      <PlayCircle className="w-3.5 h-3.5" />
                      {label}
                    </Link>
                  );
                }
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    className={cn(
                      "px-2 sm:px-3 h-8 sm:h-9 flex items-center text-[11px] sm:text-xs font-bold uppercase tracking-[0.14em] whitespace-nowrap",
                      active
                        ? "bg-news text-white"
                        : "text-white/85 hover:text-white hover:bg-ink-800",
                    )}
                  >
                    {label}
                  </Link>
                );
              })}
            </div>
            <div className="h-8 w-px bg-white/15 mx-1 sm:mx-2" aria-hidden="true" />
            <div className="flex-shrink-0 flex items-center gap-1 sm:gap-2">
              <NotificationBell />
              {auth.isAuthenticated ? (
                <div className="relative">
                  <button
                    onClick={() => setMenuOpen((v) => !v)}
                    className="flex items-center gap-1.5 h-8 pl-1 pr-2 border border-white/30 hover:border-white hover:shadow-hard-sm transition-shadow text-white"
                    aria-haspopup="menu"
                    aria-expanded={menuOpen}
                  >
                    <span className="h-6 w-6 bg-news text-white flex items-center justify-center font-bold text-[10px]">
                      {auth.user
                        ? `${auth.user.firstName.slice(0, 1)}${auth.user.lastName.slice(0, 1)}`
                        : <User className="h-3.5 w-3.5" />}
                    </span>
                    <span className="hidden sm:inline text-[11px] font-bold uppercase tracking-wider">
                      {auth.user?.firstName}
                    </span>
                  </button>
                  {menuOpen ? (
                    <div
                      className="absolute right-0 mt-2 w-60 border-2 border-ink-950 bg-white text-ink-950 shadow-hard z-50"
                      role="menu"
                    >
                      <div className="px-3 py-3 border-b-2 border-ink-950">
                        <div className="text-xs text-ink-600 font-semibold uppercase tracking-widest">
                          {t.header.menu.signedInAs}
                        </div>
                        <div className="font-bold truncate">
                          {auth.user?.firstName} {auth.user?.lastName}
                        </div>
                        <div className="text-xs text-ink-700 truncate">{auth.user?.email}</div>
                      </div>
                      <nav className="flex flex-col p-2">
                        <MenuItemLink href="/dashboard" onClick={() => setMenuOpen(false)} icon={<LayoutDashboard className="h-4 w-4" />}>{t.header.menu.dashboard}</MenuItemLink>
                        <MenuItemLink href="/dashboard/posts" onClick={() => setMenuOpen(false)} icon={<Mic className="h-4 w-4" />}>{t.header.menu.myPosts}</MenuItemLink>
                        <MenuItemLink href="/dashboard/applications" onClick={() => setMenuOpen(false)} icon={<Briefcase className="h-4 w-4" />}>{t.header.menu.myApps}</MenuItemLink>
                        <MenuItemLink href="/dashboard/settings" onClick={() => setMenuOpen(false)} icon={<Settings className="h-4 w-4" />}>{t.header.menu.profileSettings}</MenuItemLink>
                        <div className="my-2 h-px bg-ink-950/10" />
                        <button
                          type="button"
                          onClick={() => {
                            setMenuOpen(false);
                            void auth.logout();
                          }}
                          className="flex items-center gap-2 px-3 py-2 text-sm font-bold uppercase tracking-wider hover:bg-ink-950 hover:text-white text-left text-news"
                        >
                          <LogOut className="h-4 w-4" />
                          {t.header.menu.signOut}
                        </button>
                      </nav>
                    </div>
                  ) : null}
                </div>
              ) : (
                <div className="flex items-center gap-1 sm:gap-2">
                  <Link href="/login">
                    <span className="inline-flex items-center justify-center px-2.5 sm:px-3 h-8 sm:h-9 text-[11px] sm:text-xs font-bold uppercase tracking-[0.15em] border border-white/60 text-white hover:bg-white hover:text-ink-950 transition-colors">
                      {t.header.signIn}
                    </span>
                  </Link>
                  <Link href="/signup">
                    <span className="inline-flex items-center justify-center px-2.5 sm:px-3 h-8 sm:h-9 text-[11px] sm:text-xs font-bold uppercase tracking-[0.15em] bg-news text-white border border-news hover:bg-ink-900 hover:border-ink-800 transition-colors">
                      {t.header.join}
                    </span>
                  </Link>
                </div>
              )}
            </div>
          </div>
        </nav>

        <div className="md:hidden flex items-center gap-2 px-3 py-2 border-b-2 border-ink-950">
          <Link href="/" aria-label="MapMyTimes — Home" className="flex items-center gap-2 flex-shrink-0 group">
            <BrandLogo className="h-10 sm:h-12 w-auto drop-shadow-[0_3px_0_rgba(10,10,10,0.9)]" />
          </Link>
          <div className="ml-auto flex items-center gap-1">
            <LanguageToggle size="sm" variant="light" />
            <NotificationBell />
            <IconButton
              variant="outline"
              size="sm"
              className="h-9 w-9 !p-0"
              aria-label={mobileOpen ? t.header.menu.close : t.header.menu.open}
              onClick={() => setMobileOpen((v) => !v)}
            >
              {mobileOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
            </IconButton>
          </div>
        </div>

        {mobileOpen ? (
          <div className="md:hidden border-t-0 border-b-2 border-ink-950 bg-white">
            <form onSubmit={onSearch} className="p-3 flex items-center border-b-2 border-ink-950">
              <input
                value={q}
                onChange={(e) => setQ(e.target.value)}
                className="flex-1 h-10 px-3 border-2 border-ink-950 outline-none text-sm"
                placeholder={t.header.menu.searchPlaceholder}
              />
              <button type="submit" aria-label={t.common.search} className="ml-2 h-10 w-10 bg-ink-950 text-white flex items-center justify-center">
                <Search className="h-4 w-4" />
              </button>
            </form>
            <nav className="flex flex-col">
              {NAV.map((item) => {
                const active = pathname === item.href || pathname?.startsWith(item.href + "/");
                const label =
                  (t.nav as Record<string, string>)[item.key] ?? item.key;
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    className={cn(
                      "px-4 h-12 flex items-center border-b border-ink-950/10 text-sm font-bold uppercase tracking-wider",
                      active ? "bg-news text-white" : "hover:bg-ink-900 hover:text-white",
                    )}
                  >
                    {label}
                  </Link>
                );
              })}
            </nav>
            <div className="p-3 border-t-2 border-ink-950 flex flex-wrap items-center gap-2 bg-ink-50">
              {auth.isAuthenticated ? (
                <Link href="/dashboard">
                  <Button variant="outline" size="sm">{t.nav.dashboard}</Button>
                </Link>
              ) : (
                <>
                  <Link href="/login">
                    <Button variant="outline" size="sm">{t.header.signIn}</Button>
                  </Link>
                  <Link href="/signup">
                    <Button variant="news" size="sm">{t.header.join}</Button>
                  </Link>
                </>
              )}
            </div>
          </div>
        ) : null}
      </div>
    </header>
  );
}

function MenuItemLink({
  href,
  onClick,
  icon,
  children,
}: {
  href: string;
  onClick?: () => void;
  icon?: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <Link
      href={href}
      onClick={onClick}
      className="flex items-center gap-2 px-3 py-2 text-sm font-bold uppercase tracking-wider hover:bg-ink-950 hover:text-white"
    >
      {icon}
      {children}
    </Link>
  );
}
