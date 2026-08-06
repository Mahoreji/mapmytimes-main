"use client";

import { useState, useEffect } from "react";
import Image from "next/image";
import Link from "next/link";
import {
  Home,
  Newspaper,
  Video,
  UtensilsCrossed,
  Menu as MenuIcon,
  Search,
  PlayCircle,
  Flame,
  Share2,
  Check,
  Copy,
  Mail,
  Phone,
  MapPin,
  Facebook,
  X,
  Instagram,
  Youtube,
  Linkedin,
  ChevronRight,
  Eye,
  Heart,
  MessageSquare,
  ArrowRight,
  Bookmark,
  BookmarkCheck,
} from "lucide-react";

type ScreenKey =
  | "home"
  | "news"
  | "videos"
  | "menu"
  | "article"
  | "shorts"
  | "search"
  | "about"
  | "contact"
  | "careers"
  | "career-detail"
  | "login"
  | "dashboard";

const SCREENS: { key: ScreenKey; label: string; icon: React.ComponentType<{ className?: string }> }[] = [
  { key: "home", label: "Home", icon: Home },
  { key: "news", label: "News", icon: Newspaper },
  { key: "videos", label: "Videos", icon: Video },
  { key: "article", label: "Article", icon: Newspaper },
  { key: "shorts", label: "Shorts", icon: UtensilsCrossed },
  { key: "search", label: "Search", icon: Search },
  { key: "menu", label: "Menu", icon: MenuIcon },
  { key: "about", label: "About", icon: Bookmark },
  { key: "contact", label: "Contact", icon: Mail },
  { key: "careers", label: "Careers", icon: Newspaper },
  { key: "career-detail", label: "Career · Detail", icon: ArrowRight },
  { key: "login", label: "Login", icon: Heart },
  { key: "dashboard", label: "Dashboard", icon: Flame },
];

const NEWS = "#E31E24";
const INK = "#0A0A0A";
const INK700 = "#242424";
const INK600 = "#2E2E2E";
const NEWS50 = "#FDECEE";
const SURFACE = "#FFFBF8";
const MUTED = "#525252";
const DIVIDER = "#ECECEC";

function hardShadow(offset = 4 as number | `${number}px ${number}px`) {
  const off = typeof offset === "number" ? `${offset}px ${offset}px` : offset;
  return `${off} 0 0 ${INK}`;
}

export default function MobilePreviewPage() {
  const [screen, setScreen] = useState<ScreenKey>("home");

  return (
    <div className="min-h-screen bg-[#FFFBF8] text-black">
      {/* Header */}
      <div className="border-b-[2px] border-black bg-white">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div
              className="w-10 h-10 flex items-center justify-center text-white font-black text-xl"
              style={{ backgroundColor: NEWS, border: `2px solid ${INK}`, boxShadow: hardShadow(4) }}
            >
              M
            </div>
            <div>
              <div className="text-[22px] leading-none font-black tracking-tight" style={{ fontFamily: "'Archivo Black', 'Inter', system-ui, sans-serif" }}>
                MapMyTimes · Mobile Preview
              </div>
              <div className="mt-1 text-xs uppercase tracking-[2.2px] font-bold text-[#7A7A7A]">
                Flutter · Option B · Same Repo · mobile/ folder · 13 screens
              </div>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <Link
              href="/"
              className="px-4 py-2 text-sm font-bold uppercase tracking-wider"
              style={{ border: `2px solid ${INK}`, boxShadow: hardShadow(4), background: "white" }}
            >
              ← Web Home
            </Link>
            <Link
              href="/videos"
              className="px-4 py-2 text-sm font-bold uppercase tracking-wider text-white"
              style={{ border: `2px solid ${INK}`, boxShadow: hardShadow(4), backgroundColor: NEWS }}
            >
              Videos Page
            </Link>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-6 py-8 grid grid-cols-12 gap-8">
        {/* Sidebar screen picker */}
        <aside className="col-span-3 space-y-4">
          <div
            className="p-5"
            style={{ border: `2px solid ${INK}`, boxShadow: hardShadow(6), backgroundColor: "white" }}
          >
            <div
              className="text-[11px] font-black tracking-[2.4px] uppercase mb-4"
              style={{ color: NEWS, fontFamily: "'Archivo Black', sans-serif" }}
            >
              Jump to Screen
            </div>
            <div className="space-y-2">
              {SCREENS.map((s) => {
                const Icon = s.icon;
                const active = screen === s.key;
                return (
                  <button
                    key={s.key}
                    onClick={() => setScreen(s.key)}
                    className="w-full flex items-center gap-3 px-3 py-2.5 text-left transition"
                    style={{
                      border: `2px solid ${INK}`,
                      backgroundColor: active ? NEWS : "white",
                      color: active ? "white" : INK,
                      boxShadow: active ? hardShadow(4) : "none",
                    }}
                  >
                    <Icon className="w-4 h-4" />
                    <span className="text-sm font-bold uppercase tracking-wider">{s.label}</span>
                    {active && <ChevronRight className="w-4 h-4 ml-auto" />}
                  </button>
                );
              })}
            </div>
          </div>

          <div
            className="p-5 space-y-3"
            style={{ border: `2px solid ${INK}`, boxShadow: hardShadow(6), backgroundColor: NEWS50 }}
          >
            <div
              className="text-[11px] font-black tracking-[2.4px] uppercase"
              style={{ color: NEWS, fontFamily: "'Archivo Black', sans-serif" }}
            >
              Design Tokens · Neo-Brutalist
            </div>
            <div className="grid grid-cols-5 gap-1.5">
              {[NEWS, "#D0121A", "#A80D14", INK, "#121212", INK700, INK600, "#ECECEC", NEWS50, SURFACE].map((c) => (
                <div
                  key={c}
                  title={c}
                  className="aspect-square"
                  style={{ border: `2px solid ${INK}`, backgroundColor: c }}
                />
              ))}
            </div>
            <div className="pt-1 text-xs space-y-1 text-[#242424]">
              <div><span className="font-bold">Border:</span> 2px solid #0A0A0A</div>
              <div><span className="font-bold">Shadow:</span> 4×4 zero blur (hard)</div>
              <div><span className="font-bold">Headline:</span> Archivo Black · 900</div>
              <div><span className="font-bold">Body:</span> Inter · 400</div>
              <div><span className="font-bold">Radius:</span> 0 (mostly)</div>
            </div>
          </div>

          <div
            className="p-5 space-y-3"
            style={{ border: `2px solid ${INK}`, boxShadow: hardShadow(6), backgroundColor: "white" }}
          >
            <div
              className="text-[11px] font-black tracking-[2.4px] uppercase"
              style={{ color: NEWS, fontFamily: "'Archivo Black', sans-serif" }}
            >
              How to Build on Flutter
            </div>
            <ol className="list-decimal list-inside text-xs space-y-2 text-[#242424]">
              <li>Install Flutter SDK (brew install --cask flutter)</li>
              <li><code>cd mobile</code></li>
              <li><code>flutter pub get</code></li>
              <li><code>flutter run -d chrome</code> → web preview</li>
              <li><code>flutter run</code> → iOS/Android</li>
            </ol>
          </div>
        </aside>

        {/* Device frame */}
        <main className="col-span-9 flex flex-col items-center">
          <div
            className="relative"
            style={{
              width: 390,
              padding: "14px",
              borderRadius: 46,
              background: INK,
              boxShadow: `12px 12px 0 0 ${INK}, 0 0 0 2px ${INK}`,
            }}
          >
            {/* Notch */}
            <div
              className="absolute z-20 left-1/2 -translate-x-1/2 top-3 w-32 h-7 rounded-b-2xl"
              style={{ backgroundColor: INK }}
            />
            {/* Screen */}
            <div
              className="relative overflow-hidden"
              style={{
                width: "100%",
                height: 780,
                borderRadius: 34,
                border: `2px solid ${INK600}`,
                backgroundColor: SURFACE,
              }}
            >
              {/* Status bar */}
              <div className="relative z-10 h-11 px-6 flex items-center justify-between text-[11px] font-bold text-black bg-white/80 backdrop-blur">
                <span>9:41</span>
                <div className="flex items-center gap-1.5">
                  <span>●●●●</span>
                  <span>📶</span>
                  <span>🔋</span>
                </div>
              </div>

              <div className="h-[calc(780px-44px)] overflow-y-auto">
                {screen === "home" && <HomeMock />}
                {screen === "news" && <NewsMock />}
                {screen === "videos" && <VideosMock />}
                {screen === "article" && <ArticleMock />}
                {screen === "shorts" && <ShortsMock />}
                {screen === "search" && <SearchMock />}
                {screen === "menu" && <MenuMock />}
                {screen === "about" && <AboutMock />}
                {screen === "contact" && <ContactMock />}
                {screen === "careers" && <CareersMock />}
                {screen === "career-detail" && <CareerDetailMock />}
                {screen === "login" && <LoginMock />}
                {screen === "dashboard" && <DashboardMock />}
              </div>
            </div>
            {/* Bottom nav indicator */}
            <div className="absolute left-1/2 -translate-x-1/2 bottom-2 w-32 h-1 rounded-full bg-white/70" />
          </div>
          {/* Bottom nav label under device */}
          <div className="mt-6 flex items-center gap-6 text-sm font-bold uppercase tracking-[1.8px] text-[#525252]">
            {(["home", "news", "videos", "shorts", "menu"] as const).map((k) => {
              const LabelIcon = { home: Home, news: Newspaper, videos: Video, shorts: UtensilsCrossed, menu: MenuIcon }[k];
              const active = screen === k || (k === "shorts" && screen === "shorts");
              return (
                <button
                  key={k}
                  onClick={() => setScreen(k as ScreenKey)}
                  className="flex items-center gap-1.5"
                  style={{ color: active ? NEWS : MUTED }}
                >
                  <LabelIcon className="w-4 h-4" />
                  <span className="text-[11px]">{k}</span>
                </button>
              );
            })}
          </div>
        </main>
      </div>
    </div>
  );
}

/* =========================================================
   SHARED UI PRIMITIVES
   ========================================================= */

function BrandLogo({ size = 16, tagline = false }: { size?: number; tagline?: boolean }) {
  return (
    <div className="flex items-center gap-2">
      <div
        className="flex items-center justify-center text-white font-black"
        style={{
          width: size + 14,
          height: size + 14,
          backgroundColor: `color-mix(in srgb, ${NEWS} 92%, transparent)`,
          border: `0.8px solid color-mix(in srgb, white 22%, transparent)`,
          borderRadius: 999,
          fontSize: size + 2,
          backdropFilter: "blur(12px)",
          WebkitBackdropFilter: "blur(12px)",
          boxShadow: `0 3px 10px color-mix(in srgb, ${NEWS} 25%, transparent)`,
        }}
      >
        M
      </div>
      <div>
        <div
          style={{
            fontFamily: "'Archivo Black', sans-serif",
            fontSize: size + 2,
            lineHeight: 1,
            color: INK,
          }}
        >
          MapMyTimes
        </div>
        {tagline && (
          <div className="mt-1 text-[9px] tracking-[1.6px] uppercase font-bold text-[#7A7A7A]">
            Journalism · of · Integrity
          </div>
        )}
      </div>
    </div>
  );
}

function Eyebrow({ label, dot = true }: { label: string; dot?: boolean }) {
  return (
    <div className="flex items-center gap-2">
      {dot && <span className="w-1.5 h-1.5 rounded-full" style={{ backgroundColor: NEWS }} />}
      <span
        className="text-[10px] font-black tracking-[1.8px] uppercase"
        style={{ color: NEWS, fontFamily: "'Archivo Black', sans-serif" }}
      >
        {label}
      </span>
    </div>
  );
}

function MiniShareButton({
  size = "md",
  variant = "dark",
  slug,
  title,
  excerpt,
}: {
  size?: "sm" | "md" | "lg";
  variant?: "dark" | "news" | "light";
  slug?: string;
  title?: string;
  excerpt?: string;
}) {
  const [copied, setCopied] = useState(false);
  const dim = size === "sm" ? 28 : size === "lg" ? 38 : 34;
  const icon = size === "sm" ? 14 : size === "lg" ? 19 : 17;
  const bg = variant === "news" ? NEWS : variant === "light" ? "#FFFFFF" : INK;
  const fg = variant === "light" ? INK : "#FFFFFF";
  const hairline =
    variant === "news"
      ? "rgba(255,255,255,0.20)"
      : variant === "light"
      ? "rgba(10,10,10,0.10)"
      : "rgba(255,255,255,0.16)";
  const shadow =
    variant === "news"
      ? `0 3px 10px color-mix(in srgb, ${NEWS} 28%, transparent)`
      : "0 2px 6px rgba(0,0,0,0.15)";
  async function onClick(e: React.MouseEvent<HTMLButtonElement>) {
    e.preventDefault();
    e.stopPropagation();
    try {
      const shareTitle = title || "MapMyTimes";
      const shareText = excerpt ?? shareTitle;
      let absolute = slug ? `/news/${encodeURIComponent(slug)}` : typeof window !== "undefined" ? window.location.href : "";
      if (typeof window !== "undefined") {
        absolute = new URL(absolute, window.location.origin).toString();
      }
      if (typeof navigator !== "undefined" && typeof (navigator as any).share === "function") {
        await (navigator as any).share({ title: shareTitle, text: shareText, url: absolute });
        return;
      }
      if (typeof navigator !== "undefined" && typeof (navigator as any).clipboard?.writeText === "function") {
        await (navigator as any).clipboard.writeText(absolute);
        setCopied(true);
        window.setTimeout(() => setCopied(false), 1700);
      }
    } catch {}
  }
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label="Share article"
      title="Share article"
      style={{
        width: dim,
        height: dim,
        backgroundColor: `color-mix(in srgb, ${bg} 88%, transparent)`,
        color: fg,
        border: `0.8px solid ${hairline}`,
        boxShadow: shadow,
        borderRadius: 999,
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        cursor: "pointer",
        opacity: 1,
        transition: "opacity 120ms ease",
        userSelect: "none",
        backdropFilter: "blur(12px)",
        WebkitBackdropFilter: "blur(12px)",
      }}
      onMouseDown={(e) => ((e.currentTarget as HTMLButtonElement).style.opacity = "0.7")}
      onMouseUp={(e) => ((e.currentTarget as HTMLButtonElement).style.opacity = "1")}
      onMouseLeave={(e) => ((e.currentTarget as HTMLButtonElement).style.opacity = "1")}
    >
      {copied ? (
        <Check style={{ width: icon, height: icon }} />
      ) : (
        <Share2 style={{ width: icon, height: icon }} />
      )}
    </button>
  );
}

function MiniSaveButton({
  size = "md",
  variant = "dark",
  id,
  title,
  slug,
  excerpt,
  featuredImageUrl,
  publishedAt,
}: {
  size?: "sm" | "md" | "lg";
  variant?: "dark" | "news" | "light";
  id?: string | number;
  title?: string;
  slug?: string;
  excerpt?: string;
  featuredImageUrl?: string;
  publishedAt?: string;
}) {
  const [saved, setSaved] = useState(false);
  const dim = size === "sm" ? 28 : size === "lg" ? 38 : 34;
  const icon = size === "sm" ? 14 : size === "lg" ? 19 : 17;
  const key = id != null ? String(id) : `mock-${slug || title || "x"}`;

  useEffect(() => {
    try {
      const raw = window.localStorage.getItem("mmt:saved-articles");
      if (!raw) return;
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) {
        setSaved(parsed.some((p: any) => String(p?.id || "") === key));
      }
    } catch {}
  }, [key]);

  function onClick(e: React.MouseEvent<HTMLButtonElement>) {
    e.preventDefault();
    e.stopPropagation();
    try {
      const raw = window.localStorage.getItem("mmt:saved-articles");
      const list: any[] = raw && Array.isArray(JSON.parse(raw)) ? JSON.parse(raw) : [];
      const idx = list.findIndex((p: any) => String(p?.id || "") === key);
      let next: any[];
      if (idx >= 0) {
        next = list.filter((_, i) => i !== idx);
        setSaved(false);
      } else {
        next = [
          {
            id: key,
            title: title || "Untitled",
            slug: slug || key,
            excerpt: excerpt ?? undefined,
            featuredImageUrl: featuredImageUrl ?? undefined,
            publishedAt: publishedAt ?? undefined,
            savedAt: Date.now(),
          },
          ...list,
        ];
        setSaved(true);
      }
      window.localStorage.setItem("mmt:saved-articles", JSON.stringify(next));
    } catch {}
  }

  const bg = saved
    ? NEWS
    : variant === "news"
    ? NEWS
    : variant === "light"
    ? "#FFFFFF"
    : INK;
  const fg = saved || variant !== "light" ? "#FFFFFF" : INK;
  const hairline = saved
    ? "rgba(255,255,255,0.22)"
    : variant === "news"
    ? "rgba(255,255,255,0.20)"
    : variant === "light"
    ? "rgba(10,10,10,0.10)"
    : "rgba(255,255,255,0.16)";
  const shadow = saved
    ? `0 3px 10px color-mix(in srgb, ${NEWS} 28%, transparent)`
    : variant === "news"
    ? `0 3px 10px color-mix(in srgb, ${NEWS} 28%, transparent)`
    : "0 2px 6px rgba(0,0,0,0.15)";

  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={saved ? "Remove from saved" : "Save article"}
      title={saved ? "Remove from saved" : "Save article"}
      style={{
        width: dim,
        height: dim,
        backgroundColor: `color-mix(in srgb, ${bg} ${saved ? 92 : 88}%, transparent)`,
        color: fg,
        border: `0.8px solid ${hairline}`,
        boxShadow: shadow,
        borderRadius: 999,
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        cursor: "pointer",
        opacity: 1,
        transition: "all 200ms ease",
        userSelect: "none",
        backdropFilter: "blur(12px)",
        WebkitBackdropFilter: "blur(12px)",
      }}
      onMouseDown={(e) => ((e.currentTarget as HTMLButtonElement).style.opacity = "0.7")}
      onMouseUp={(e) => ((e.currentTarget as HTMLButtonElement).style.opacity = "1")}
      onMouseLeave={(e) => ((e.currentTarget as HTMLButtonElement).style.opacity = "1")}
    >
      {saved ? (
        <BookmarkCheck style={{ width: icon, height: icon, fill: "currentColor" }} />
      ) : (
        <Bookmark style={{ width: icon, height: icon }} />
      )}
    </button>
  );
}

function Card({ children, style }: { children: React.ReactNode; style?: React.CSSProperties }) {
  return (
    <div
      style={{
        position: "relative",
        overflow: "hidden",
        borderRadius: 14,
        ...style,
      }}
    >
      {/* Glass outer: backdrop + alpha bg + hairline + soft shadow */}
      <div
        aria-hidden
        style={{
          position: "absolute",
          inset: 0,
          borderRadius: 14,
          backgroundColor: "color-mix(in srgb, white 82%, transparent)",
          border: `0.8px solid color-mix(in srgb, ${INK} 8%, transparent)`,
          boxShadow:
            "0 6px 18px color-mix(in srgb, #0A0A0A 14%, transparent), 0 2px 4px color-mix(in srgb, #0A0A0A 6%, transparent)",
          backdropFilter: "blur(20px)",
          WebkitBackdropFilter: "blur(20px)",
        }}
      />
      <div style={{ position: "relative", zIndex: 1, borderRadius: 14, overflow: "hidden" }}>{children}</div>
    </div>
  );
}

function BottomNav({ active, go }: { active: ScreenKey; go: (k: ScreenKey) => void }) {
  const items: { key: ScreenKey; label: string; I: React.ComponentType<{ className?: string }> }[] = [
    { key: "home", label: "Home", I: Home },
    { key: "news", label: "News", I: Newspaper },
    { key: "videos", label: "Videos", I: Video },
    { key: "shorts", label: "Shorts", I: UtensilsCrossed },
    { key: "menu", label: "Menu", I: MenuIcon },
  ];
  return (
    <div
      style={{
        position: "relative",
        margin: "0 14px 14px",
        borderRadius: 999,
        overflow: "hidden",
        backdropFilter: "blur(20px)",
        WebkitBackdropFilter: "blur(20px)",
        backgroundColor: "color-mix(in srgb, white 72%, transparent)",
        border: "0.8px solid color-mix(in srgb, #0A0A0A 10%, transparent)",
        boxShadow: "0 -4px 28px color-mix(in srgb, #0A0A0A 18%, transparent)",
      }}
    >
      <div className="grid grid-cols-5">
        {items.map((it) => {
          const I = it.I;
          const on = active === it.key;
          const isBig = it.key === "shorts";
          return (
            <button
              key={it.key}
              onClick={() => go(it.key)}
              className="relative py-3 flex flex-col items-center gap-1"
              style={{ color: on ? NEWS : INK600 }}
            >
              {on && (
                <span
                  aria-hidden
                  className="absolute left-1/2 -translate-x-1/2 top-1.5"
                  style={{
                    height: isBig ? 40 : 30,
                    width: isBig ? 40 : 44,
                    borderRadius: 999,
                    backgroundColor: `color-mix(in srgb, ${isBig ? NEWS : INK} 90%, transparent)`,
                    border: `0.8px solid color-mix(in srgb, white 20%, transparent)`,
                    boxShadow: isBig
                      ? `0 0 24px color-mix(in srgb, ${NEWS} 55%, transparent), 0 6px 16px color-mix(in srgb, ${NEWS} 30%, transparent)`
                      : "0 2px 6px rgba(0,0,0,0.12)",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    zIndex: 0,
                  }}
                />
              )}
              <span className={on ? "relative z-10" : ""} style={{ color: on ? "white" : undefined }}>
                <I className={isBig ? "w-5 h-5" : "w-4 h-4"} />
              </span>
              <span
                className={"text-[9px] font-bold uppercase tracking-wider relative z-10 " + (on ? "text-white" : "")}
              >
                {it.label}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
}

/* =========================================================
   HOME MOCK
   ========================================================= */

function HomeMock() {
  return (
    <div className="pb-2 bg-[#FFFBF8]">
      {/* App bar — LIQUID GLASS sticky header */}
      <div
        className="sticky top-0 z-10 px-5 py-3"
        style={{
          backdropFilter: "blur(30px)",
          WebkitBackdropFilter: "blur(30px)",
          backgroundColor: "color-mix(in srgb, white 72%, transparent)",
          borderBottom: `0.8px solid color-mix(in srgb, ${INK} 6%, transparent)`,
          borderBottomLeftRadius: 20,
          borderBottomRightRadius: 20,
          boxShadow: "0 4px 18px color-mix(in srgb, #0A0A0A 10%, transparent)",
        }}
      >
        <div className="flex items-center gap-2.5">
          <BrandLogo size={14} />
          <div className="ml-auto flex items-center gap-2.5">
            <div
              className="flex-1 flex items-center gap-2 px-3 h-9"
              style={{
                border: `0.8px solid color-mix(in srgb, ${INK} 10%, transparent)`,
                minWidth: 130,
                borderRadius: 999,
                backgroundColor: "color-mix(in srgb, white 85%, transparent)",
                backdropFilter: "blur(12px)",
                WebkitBackdropFilter: "blur(12px)",
                boxShadow: "0 2px 6px rgba(0,0,0,0.08)",
              }}
            >
              <Search className="w-3.5 h-3.5" />
              <span className="text-[11px] font-semibold text-[#7A7A7A]">Search…</span>
            </div>
            <div
              className="h-9 w-12 flex items-center justify-center text-[11px] font-black"
              style={{
                backgroundColor: `color-mix(in srgb, ${NEWS} 92%, transparent)`,
                color: "white",
                border: `0.8px solid color-mix(in srgb, white 20%, transparent)`,
                borderRadius: 8,
                backdropFilter: "blur(12px)",
                WebkitBackdropFilter: "blur(12px)",
                boxShadow: `0 3px 10px color-mix(in srgb, ${NEWS} 25%, transparent)`,
              }}
            >
              EN
            </div>
          </div>
        </div>
      </div>

      {/* Hero */}
      <div
        className="px-5 py-6 my-4 mx-4"
        style={{
          borderRadius: 20,
          border: `0.8px solid color-mix(in srgb, ${NEWS} 20%, transparent)`,
          background:
            "linear-gradient(135deg, color-mix(in srgb, #FDECEE 90%, transparent) 0%, color-mix(in srgb, #E31E24 12%, transparent) 100%)",
          backdropFilter: "blur(20px)",
          WebkitBackdropFilter: "blur(20px)",
          boxShadow: `0 12px 30px color-mix(in srgb, ${NEWS} 16%, transparent)`,
        }}
      >
        <Eyebrow label="Journalism of Integrity" />
        <div
          className="mt-3 text-[30px] leading-[1.02] font-black tracking-tight"
          style={{ fontFamily: "'Archivo Black', sans-serif", color: INK }}
        >
          Reports that hold power to account.
        </div>
        <p className="mt-3 text-[12.5px] leading-5 text-[#242424]">
          Ground reporting from Madhya Pradesh and beyond — verified facts, on-ground sources, and storytelling that serves the public good.
        </p>
      </div>

      {/* Featured Reports */}
      <div className="px-5 pt-6">
        <Eyebrow label="Featured Reports" />
        <div className="mt-3 space-y-3.5">
          {[
            { cat: "POLITICS", t: "MP Assembly passes landmark bill on rural healthcare funding" },
            { cat: "INVESTIGATION", t: "Inside the 3-year trail of illegal river mining along the Narmada" },
          ].map((x, i) => (
            <Card key={i}>
              <div className="h-36 w-full" style={{ background: i === 0 ? "linear-gradient(135deg,#FDECEE,#E31E24)" : "linear-gradient(135deg,#0A0A0A,#242424)" }} />
              <div className="p-3.5 space-y-2.5">
                <div
                  className="inline-block px-2.5 py-1.5 text-[9px] font-black tracking-[1.4px] text-white"
                  style={{
                    backgroundColor: `color-mix(in srgb, ${NEWS} 92%, transparent)`,
                    borderRadius: 8,
                    border: `0.8px solid rgba(255,255,255,0.22)`,
                    boxShadow: `0 3px 10px color-mix(in srgb, ${NEWS} 28%, transparent)`,
                    backdropFilter: "blur(12px)",
                    WebkitBackdropFilter: "blur(12px)",
                  }}
                >
                  {x.cat}
                </div>
                <div className="text-[15px] leading-snug font-black" style={{ fontFamily: "'Archivo Black', sans-serif", color: INK }}>
                  {x.t}
                </div>
                <div className="flex items-center justify-between gap-2 pt-0.5">
                  <div className="text-[10.5px] text-[#7A7A7A] font-semibold">
                    By Prakhar Shukla · 18 Jul 2026 · 7 min read
                  </div>
                </div>
                <div className="flex items-center gap-2 pt-1 flex-wrap">
                  <button
                    className="inline-flex items-center gap-1.5 px-3.5 py-2 text-[10.5px] font-black uppercase tracking-wider text-white"
                    style={{
                      backgroundColor: `color-mix(in srgb, ${NEWS} 90%, transparent)`,
                      border: `0.8px solid rgba(255,255,255,0.20)`,
                      borderRadius: 8,
                      boxShadow: `0 4px 12px color-mix(in srgb, ${NEWS} 30%, transparent)`,
                      backdropFilter: "blur(12px)",
                      WebkitBackdropFilter: "blur(12px)",
                      height: 40,
                      minHeight: 40,
                    }}
                  >
                    Full Story
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M5 12h14" />
                      <path d="m12 5 7 7-7 7" />
                    </svg>
                  </button>
                  <div className="flex items-center gap-2">
                    <MiniShareButton
                      size="md"
                      variant="dark"
                      slug={`featured-report-${i + 1}`}
                      title={x.t}
                    />
                    <MiniSaveButton
                      size="md"
                      variant="dark"
                      id={`feat-${i + 1}`}
                      slug={`featured-report-${i + 1}`}
                      title={x.t}
                    />
                  </div>
                </div>
              </div>
            </Card>
          ))}
        </div>
      </div>

      {/* Trending */}
      <div className="px-5 pt-6">
        <Eyebrow label="Trending Now" />
        <div className="mt-3 space-y-2">
          {[
            "Bhopal civic body proposes new parking policy for old city",
            "Monsoon update: Narmada level crosses 930 mm mark",
            "Indore–Bhopal Vande Bharat Express extended till Nagpur",
            "Crime branch busts interstate fake job racket, 6 held",
            "Govt releases ₹240 Cr for roads in 14 tribal districts",
            "MPSEB announces 8-hour weekly power cut schedule for July",
          ].map((t, i) => (
            <div
              key={i}
              className="flex gap-3 items-start relative"
              style={{
                padding: "12px",
                borderRadius: 14,
                backdropFilter: "blur(12px)",
                WebkitBackdropFilter: "blur(12px)",
                backgroundColor: "color-mix(in srgb, white 70%, transparent)",
                border: `0.8px solid color-mix(in srgb, ${INK} 6%, transparent)`,
                boxShadow: "0 2px 6px rgba(0,0,0,0.08)",
              }}
            >
              <div
                className="w-10 h-10 shrink-0 flex items-center justify-center text-[15px] font-black text-white"
                style={{
                  backgroundColor: `color-mix(in srgb, ${NEWS} 92%, transparent)`,
                  borderRadius: 8,
                  border: `0.8px solid rgba(255,255,255,0.22)`,
                  boxShadow: `0 3px 10px color-mix(in srgb, ${NEWS} 25%, transparent)`,
                }}
              >
                {String(i + 1).padStart(2, "0")}
              </div>
              <div className="flex-1 min-w-0 pr-12">
                <div className="text-[12.5px] leading-5 font-black line-clamp-3" style={{ fontFamily: "'Archivo Black', sans-serif" }}>
                  {t}
                </div>
                <div className="mt-1 text-[10px] text-[#7A7A7A] font-semibold">
                  MapMyTimes · {i + 1}h ago · {(i + 2) * 1200} views
                </div>
              </div>
              <div className="absolute top-3 right-3 flex items-center gap-2">
                <MiniShareButton size="sm" variant="news" slug={`trending-${i + 1}`} title={t} />
                <MiniSaveButton size="sm" variant="news" id={`trend-${i + 1}`} slug={`trending-${i + 1}`} title={t} />
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Latest */}
      <div className="px-5 pt-6">
        <Eyebrow label="Latest Stories" />
        <div className="mt-3 grid grid-cols-2 gap-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <Card key={i} style={{ padding: 10 }}>
              <div className="absolute top-1.5 right-1.5 z-[2] flex items-center gap-1.5">
                <MiniShareButton size="sm" variant="dark" slug={`latest-${i + 1}`} title={`Compact card ${i + 1}`} />
                <MiniSaveButton size="sm" variant="dark" id={`latest-${i + 1}`} slug={`latest-${i + 1}`} title={`Compact card ${i + 1}`} />
              </div>
              <div
                className="h-20"
                style={{
                  background: i % 2 ? NEWS50 : "#F4F4F5",
                  borderRadius: 8,
                  border: `0.8px solid color-mix(in srgb, ${INK} 6%, transparent)`,
                }}
              />
              <div
                className="mt-2.5 inline-block px-2 py-1 text-[8px] font-black tracking-widest text-white pr-10"
                style={{
                  backgroundColor: `color-mix(in srgb, ${NEWS} 92%, transparent)`,
                  borderRadius: 6,
                  border: `0.8px solid rgba(255,255,255,0.22)`,
                  boxShadow: `0 2px 6px color-mix(in srgb, ${NEWS} 22%, transparent)`,
                }}
              >
                {["NATIONAL", "STATE", "CRIME", "BUSINESS", "SPORTS", "OPINION"][i]}
              </div>
              <div className="mt-2 text-[11px] leading-snug font-black pr-1.5" style={{ fontFamily: "'Archivo Black', sans-serif" }}>
                Short headline in {["Hindi", "English"][i % 2]} sample for compact card {i + 1}.
              </div>
            </Card>
          ))}
        </div>
      </div>

      {/* Categories */}
      <div className="px-5 pt-6">
        <Eyebrow label="Categories" />
        <div className="mt-3 flex flex-wrap gap-2">
          {["Politics", "MP News", "Crime", "Business", "Sports", "Opinion", "Tech", "Education", "Health", "Culture", "World", "Weather"].map((c, i) => (
            <div
              key={c}
              className="px-3 py-1.5 text-[10.5px] font-bold uppercase tracking-wider"
              style={{
                borderRadius: 999,
                border: `0.8px solid color-mix(in srgb, ${i === 0 ? "white" : INK} ${i === 0 ? "22%" : "10%"}, transparent)`,
                backgroundColor: i === 0
                  ? `color-mix(in srgb, ${NEWS} 90%, transparent)`
                  : "color-mix(in srgb, white 85%, transparent)",
                color: i === 0 ? "white" : INK,
                backdropFilter: "blur(12px)",
                WebkitBackdropFilter: "blur(12px)",
                boxShadow: i === 0 ? `0 3px 10px color-mix(in srgb, ${NEWS} 25%, transparent)` : "0 2px 6px rgba(0,0,0,0.06)",
              }}
            >
              {c}
            </div>
          ))}
        </div>
      </div>

      <div className="mx-5 mt-6" style={{ height: 0.8, background: `color-mix(in srgb, ${INK} 10%, transparent)` }} />
      <div className="px-5 pt-5 pb-6">
        <Eyebrow label="Follow us" />
        <p className="mt-4 text-[12px] leading-6 text-[#242424]">
          MapMyTimes is an independent news platform committed to verified, unflinching journalism — reports, investigations, and storytelling that serves the public good.
        </p>
        <div className="mt-5 flex gap-2.5">
          {[Facebook, X, Instagram, Youtube, Linkedin].map((I, i) => (
            <div
              key={i}
              className="w-9 h-9 flex items-center justify-center"
              style={{
                borderRadius: 999,
                border: `0.8px solid color-mix(in srgb, ${INK} 10%, transparent)`,
                background: "color-mix(in srgb, white 85%, transparent)",
                backdropFilter: "blur(12px)",
                WebkitBackdropFilter: "blur(12px)",
                boxShadow: "0 2px 6px rgba(0,0,0,0.08)",
              }}
            >
              <I className="w-3.5 h-3.5" />
            </div>
          ))}
        </div>
        <p className="mt-6 text-[10.5px] text-[#7A7A7A] font-semibold">© 2026 MAPMYTOUR LLP, India</p>
      </div>
    </div>
  );
}

/* =========================================================
   NEWS LIST
   ========================================================= */

function NewsMock() {
  return (
    <div className="flex flex-col h-full bg-[#FFFBF8]">
      <div
        className="sticky top-0 z-10 text-white px-5 py-3.5"
        style={{
          backdropFilter: "blur(30px)",
          WebkitBackdropFilter: "blur(30px)",
          backgroundColor: "color-mix(in srgb, #0A0A0A 62%, transparent)",
          borderBottom: `0.8px solid rgba(255,255,255,0.08)`,
          borderBottomLeftRadius: 20,
          borderBottomRightRadius: 20,
          boxShadow: "0 4px 18px rgba(0,0,0,0.18)",
        }}
      >
        <div
          className="text-[22px] font-black leading-none tracking-tight"
          style={{ fontFamily: "'Archivo Black', sans-serif" }}
        >
          News
        </div>
      </div>
      <div className="flex-1 overflow-y-auto px-5 py-5 space-y-4">
        {Array.from({ length: 8 }).map((_, i) => (
          <div
            key={i}
            className="flex relative h-full"
            style={{
              borderRadius: 14,
              overflow: "hidden",
              backdropFilter: "blur(20px)",
              WebkitBackdropFilter: "blur(20px)",
              backgroundColor: "color-mix(in srgb, white 80%, transparent)",
              border: `0.8px solid color-mix(in srgb, ${INK} 8%, transparent)`,
              boxShadow: "0 6px 18px color-mix(in srgb, #0A0A0A 14%, transparent), 0 2px 4px color-mix(in srgb, #0A0A0A 6%, transparent)",
            }}
          >
            <div className="w-[124px] shrink-0 aspect-[3/4]" style={{ background: i % 2 ? INK700 : NEWS50 }} />
            <div className="flex-1 p-3 space-y-2 pr-12">
              <div
                className="inline-block px-2 py-1 text-[8.5px] font-black tracking-[1.4px] text-white"
                style={{
                  backgroundColor: `color-mix(in srgb, ${NEWS} 92%, transparent)`,
                  borderRadius: 6,
                  border: "0.8px solid rgba(255,255,255,0.22)",
                  boxShadow: `0 2px 6px color-mix(in srgb, ${NEWS} 22%, transparent)`,
                  backdropFilter: "blur(12px)",
                  WebkitBackdropFilter: "blur(12px)",
                }}
              >
                {["POLITICS", "STATE", "CRIME", "BUSINESS", "SPORTS", "EDUCATION", "HEALTH", "WORLD"][i]}
              </div>
              <div className="text-[14.5px] leading-snug font-black line-clamp-3" style={{ fontFamily: "'Archivo Black', sans-serif" }}>
                Long-form headline sample for the MapMyTimes news list tile with three-line Archivo Black clamp — item #{i + 1}.
              </div>
              <div className="text-[10.5px] text-[#7A7A7A] font-semibold">
                Prakhar · 1{(i + 1) % 9} Jul · {(i + 3) * 900} views
              </div>
            </div>
            <div className="absolute top-2 right-2 z-[2] flex items-center gap-2">
              <MiniShareButton
                size="sm"
                variant="dark"
                slug={`news-list-${i + 1}`}
                title={`News list tile ${i + 1}`}
              />
              <MiniSaveButton
                size="sm"
                variant="dark"
                id={`newslist-${i + 1}`}
                slug={`news-list-${i + 1}`}
                title={`News list tile ${i + 1}`}
              />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

/* =========================================================
   VIDEOS GRID (LIQUID GLASS)
   ========================================================= */

function VideosMock() {
  return (
    <div className="flex flex-col h-full bg-[#FFFBF8]">
      {/* Sticky Glass Header */}
      <div className="sticky top-0 z-10 relative overflow-hidden" style={{ borderBottomLeftRadius: 20, borderBottomRightRadius: 20 }}>
        <div aria-hidden className="absolute inset-0" style={{
          backdropFilter: "blur(30px)",
          WebkitBackdropFilter: "blur(30px)",
          backgroundColor: "color-mix(in srgb, white 72%, transparent)",
          borderBottom: "0.8px solid color-mix(in srgb, #0A0A0A 10%, transparent)",
          boxShadow: "0 6px 18px color-mix(in srgb, #0A0A0A 10%, transparent), 0 2px 4px color-mix(in srgb, #0A0A0A 6%, transparent)"
        }} />
        <div className="relative z-10 px-5 py-3.5">
          <Eyebrow label="Watch — #1" />
          <div className="mt-2 flex items-start justify-between gap-3">
            <div
              className="text-[20px] font-black leading-tight flex-1"
              style={{ fontFamily: "'Archivo Black', sans-serif", color: INK }}
            >
              Bhopal: 10 minutes that changed the monsoon session
            </div>
            {/* Watch More Glass Pill Button */}
            <button
              className="shrink-0 px-3 py-1.5 text-[10.5px] font-black uppercase tracking-wider flex items-center gap-1.5 text-white relative overflow-hidden"
              style={{ borderRadius: 999 }}
            >
              <div aria-hidden className="absolute inset-0" style={{
                backdropFilter: "blur(12px)",
                WebkitBackdropFilter: "blur(12px)",
                backgroundColor: "color-mix(in srgb, #E31E24 92%, transparent)",
                border: "0.8px solid color-mix(in srgb, white 22%, transparent)",
                boxShadow: "0 4px 10px color-mix(in srgb, #E31E24 25%, transparent)",
                borderRadius: 999
              }} />
              <span className="relative z-10 flex items-center gap-1.5">
                <PlayCircle className="w-3.5 h-3.5" />
                Watch More
              </span>
            </button>
          </div>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto px-4 py-4">
        <div className="grid grid-cols-2 gap-3.5">
          {Array.from({ length: 8 }).map((_, i) => (
            <div key={i} className="relative rounded-[14px] overflow-hidden">
              {/* Glass outer surface */}
              <div aria-hidden className="absolute inset-0" style={{
                backdropFilter: "blur(20px)",
                WebkitBackdropFilter: "blur(20px)",
                backgroundColor: "color-mix(in srgb, white 82%, transparent)",
                border: "0.8px solid color-mix(in srgb, #0A0A0A 8%, transparent)",
                boxShadow: "0 6px 18px color-mix(in srgb, #0A0A0A 14%, transparent), 0 2px 4px color-mix(in srgb, #0A0A0A 6%, transparent)",
                borderRadius: 14
              }} />
              <div className="relative z-10">
                <div className="absolute top-1.5 right-1.5 z-[2] flex items-center gap-1.5">
                <MiniShareButton
                  size="sm"
                  variant="light"
                  slug={`video-${i + 1}`}
                  title={`Video card ${i + 1}`}
                />
                <MiniSaveButton
                  size="sm"
                  variant="light"
                  id={`video-${i + 1}`}
                  slug={`video-${i + 1}`}
                  title={`Video card ${i + 1}`}
                />
              </div>
                <div className="relative h-[110px]" style={{
                  background: `linear-gradient(135deg, ${i % 2 ? INK : NEWS} 0%, ${INK700} 100%)`,
                  borderTopLeftRadius: 14,
                  borderTopRightRadius: 14
                }}>
                  {/* Play button glass */}
                  <div
                    className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-12 h-12 flex items-center justify-center text-white overflow-hidden"
                    style={{ borderRadius: 999 }}
                  >
                    <div aria-hidden className="absolute inset-0" style={{
                      backdropFilter: "blur(12px)",
                      WebkitBackdropFilter: "blur(12px)",
                      backgroundColor: "color-mix(in srgb, #E31E24 88%, transparent)",
                      border: "0.8px solid color-mix(in srgb, white 22%, transparent)",
                      boxShadow: "0 4px 10px color-mix(in srgb, #E31E24 28%, transparent)",
                      borderRadius: 999
                    }} />
                    <PlayCircle className="w-6 h-6 relative z-10" />
                  </div>
                </div>
                <div className="p-2.5 pr-10">
                  <div className="text-[11.5px] leading-snug font-black line-clamp-3" style={{ fontFamily: "'Archivo Black', sans-serif" }}>
                    Video title card #{i + 1} — two-column grid thumbnail with frosted glass surface.
                  </div>
                  <div className="mt-2 text-[9.5px] font-bold text-[#7A7A7A]">
                    {(i + 1) * 32}K views · {i + 2}d
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

/* =========================================================
   ARTICLE (LIQUID GLASS)
   ========================================================= */

function ArticleMock() {
  return (
    <div className="flex flex-col h-full bg-[#FFFBF8]">
      {/* Hero with glass back button */}
      <div
        className="h-[260px] w-full relative"
        style={{ background: `linear-gradient(180deg, ${NEWS50}, ${NEWS})`, borderBottomLeftRadius: 20, borderBottomRightRadius: 20 }}
      >
        <div className="absolute top-3 left-3">
          <div className="w-8 h-8 flex items-center justify-center overflow-hidden" style={{ borderRadius: 8 }}>
            <div aria-hidden className="absolute inset-0" style={{
              backdropFilter: "blur(12px)",
              WebkitBackdropFilter: "blur(12px)",
              backgroundColor: "color-mix(in srgb, white 88%, transparent)",
              border: "0.8px solid color-mix(in srgb, #0A0A0A 8%, transparent)",
              boxShadow: "0 4px 10px color-mix(in srgb, #0A0A0A 12%, transparent)",
              borderRadius: 8
            }} />
            <ChevronRight className="w-4 h-4 -rotate-180 relative z-10" />
          </div>
        </div>
      </div>
      <div className="flex-1 overflow-y-auto px-5 py-5 space-y-4 pb-6">
        {/* Category chips glass */}
        <div className="flex flex-wrap gap-2">
          {["POLITICS", "MADHYA PRADESH"].map((x) => (
            <div key={x} className="px-2.5 py-1.5 text-[10px] font-black tracking-[1.4px] uppercase text-white relative overflow-hidden" style={{ borderRadius: 999 }}>
              <div aria-hidden className="absolute inset-0" style={{
                backdropFilter: "blur(12px)",
                WebkitBackdropFilter: "blur(12px)",
                backgroundColor: "color-mix(in srgb, #E31E24 92%, transparent)",
                border: "0.8px solid color-mix(in srgb, white 22%, transparent)",
                boxShadow: "0 4px 10px color-mix(in srgb, #E31E24 25%, transparent)",
                borderRadius: 999
              }} />
              <span className="relative z-10">{x}</span>
            </div>
          ))}
        </div>
        <div className="text-[26px] leading-tight font-black" style={{ fontFamily: "'Archivo Black', sans-serif", color: INK }}>
          Bhopal monsoon session: 10-minute debate that reshaped MP&apos;s rural healthcare bill.
        </div>
        <div className="text-[13.5px] italic leading-6 text-[#242424]">
          On-ground reporting from the Assembly press gallery — what the bill means for 52 district hospitals and 1,200 sub-centres across the state.
        </div>
        {/* Hairline divider */}
        <div style={{ height: 0.8, background: "color-mix(in srgb, #0A0A0A 12%, transparent)" }} />
        <div className="flex flex-wrap items-center gap-x-3 gap-y-2 text-[11px] font-semibold text-[#525252]">
          <span>By <strong className="text-black">Prakhar Shukla</strong></span>
          <span>·</span>
          <span>18 Jul 2026</span>
          {/* Read time badge glass */}
          <span className="px-2 py-0.5 relative overflow-hidden" style={{ borderRadius: 6 }}>
            <div aria-hidden className="absolute inset-0" style={{
              backdropFilter: "blur(12px)",
              WebkitBackdropFilter: "blur(12px)",
              backgroundColor: "color-mix(in srgb, white 82%, transparent)",
              border: "0.8px solid color-mix(in srgb, #0A0A0A 10%, transparent)",
              borderRadius: 6
            }} />
            <span className="relative z-10 font-bold">7 MIN READ</span>
          </span>
          <span className="flex items-center gap-1"><Eye className="w-3.5 h-3.5" />2.4K</span>
        </div>
        {Array.from({ length: 6 }).map((_, i) => (
          <p key={i} className="text-[13px] leading-7 text-[#121212]">
            {i === 0
              ? "BHOPAL — The monsoon session of the Madhya Pradesh Assembly concluded late on Thursday with a 214-page majority report on rural healthcare funding that, for the first time in 17 years, ties annual budgetary allocations to outpatient footfall at block-level primary health centres."
              : "The changes, tabled by the health minister after three rounds of standing committee review, create a rolling performance-linked tranche of ₹240 Cr distributed every quarter. District collectors will be required to publish PHC-level utilisation dashboards on the state health portal within 15 days of each quarter end — a demand that opposition parties had been raising since the 2024 supplementary budget."}
          </p>
        ))}
        {/* Hash tags NEWS50 glass */}
        <div className="flex flex-wrap gap-2">
          {["#Healthcare", "#MPAssembly", "#Bhopal", "#PublicPolicy"].map((t) => (
            <div key={t} className="px-2.5 py-1.5 text-[10px] font-bold uppercase tracking-wider relative overflow-hidden" style={{ borderRadius: 8 }}>
              <div aria-hidden className="absolute inset-0" style={{
                backdropFilter: "blur(12px)",
                WebkitBackdropFilter: "blur(12px)",
                backgroundColor: `color-mix(in srgb, ${NEWS50} 85%, transparent)`,
                border: "0.8px solid color-mix(in srgb, #E31E24 20%, transparent)",
                borderRadius: 8
              }} />
              <span className="relative z-10">{t}</span>
            </div>
          ))}
        </div>
        <div style={{ height: 0.8, background: "color-mix(in srgb, #0A0A0A 12%, transparent)" }} />
        {/* Share + Copy buttons */}
        <div className="flex items-center gap-2.5">
          <button className="flex-1 py-2.5 text-[11px] font-black uppercase tracking-wider flex items-center justify-center gap-1.5 relative overflow-hidden" style={{ borderRadius: 12 }}>
            <div aria-hidden className="absolute inset-0" style={{
              backdropFilter: "blur(12px)",
              WebkitBackdropFilter: "blur(12px)",
              backgroundColor: "color-mix(in srgb, white 85%, transparent)",
              border: "0.8px solid color-mix(in srgb, #0A0A0A 10%, transparent)",
              boxShadow: "0 4px 10px color-mix(in srgb, #0A0A0A 10%, transparent)",
              borderRadius: 12
            }} />
            <span className="relative z-10 flex items-center justify-center gap-1.5">
              <Share2 className="w-3.5 h-3.5" />
              Share
            </span>
          </button>
          <button className="flex-1 py-2.5 text-[11px] font-black uppercase tracking-wider flex items-center justify-center gap-1.5 relative overflow-hidden" style={{ borderRadius: 12 }}>
            <div aria-hidden className="absolute inset-0" style={{
              backdropFilter: "blur(12px)",
              WebkitBackdropFilter: "blur(12px)",
              backgroundColor: "color-mix(in srgb, white 85%, transparent)",
              border: "0.8px solid color-mix(in srgb, #0A0A0A 10%, transparent)",
              boxShadow: "0 4px 10px color-mix(in srgb, #0A0A0A 10%, transparent)",
              borderRadius: 12
            }} />
            <span className="relative z-10 flex items-center justify-center gap-1.5">
              <Copy className="w-3.5 h-3.5" />
              Copy Link
            </span>
          </button>
        </div>
        <div className="text-[10.5px] text-[#7A7A7A] font-semibold pt-2">© 2026 MAPMYTOUR LLP. All rights reserved.</div>
      </div>
    </div>
  );
}

/* =========================================================
   SHORTS FEED (FULLSCREEN DARK) (LIQUID GLASS)
   ========================================================= */

function ShortsMock() {
  return (
    <div className="relative h-full w-full overflow-hidden" style={{ backgroundColor: INK }}>
      {/* gradient cover */}
      <div className="absolute inset-0" style={{ background: `linear-gradient(180deg, #000 0%, ${NEWS} 120%)` }} />
      {/* Eyebrow chip glass */}
      <div className="absolute top-3 left-5">
        <Eyebrow label="Shorts · 02" />
      </div>
      <div className="absolute inset-0 flex flex-col justify-end px-5 pb-10 text-white">
        <div
          className="mt-3 text-[24px] leading-tight font-black"
          style={{ fontFamily: "'Archivo Black', sans-serif" }}
        >
          30 seconds: how MP&apos;s new PHC dashboard actually works — explained by the district collector of Hoshangabad.
        </div>
        <p className="mt-3 text-[12.5px] leading-6 text-white/70 line-clamp-5">
          Bhopal bureau chief Prakhar Shukla breaks down the new quarterly-healthcare-dashboard rule — the 3 key numbers you need to track, and why opposition MLAs say it still doesn&apos;t fix staff vacancies at CHCs.
        </p>
        {/* Stats chips glass */}
        <div className="mt-4 flex flex-wrap gap-2">
          {[
            { I: Eye, n: "24K" },
            { I: Heart, n: "1.8K" },
            { I: MessageSquare, n: "312" },
          ].map(({ I, n }) => (
            <div key={n} className="px-2.5 py-1.5 text-[10px] font-bold flex items-center gap-1.5 relative overflow-hidden" style={{ borderRadius: 8 }}>
              <div aria-hidden className="absolute inset-0" style={{
                backdropFilter: "blur(12px)",
                WebkitBackdropFilter: "blur(12px)",
                backgroundColor: "color-mix(in srgb, white 12%, transparent)",
                border: "0.8px solid color-mix(in srgb, white 22%, transparent)",
                borderRadius: 8
              }} />
              <span className="relative z-10 flex items-center gap-1.5">
                <I className="w-3 h-3" />
                <span>{n}</span>
              </span>
            </div>
          ))}
        </div>
        <div className="mt-4 text-[10.5px] text-white/60 font-semibold uppercase tracking-wider">
          Swipe up for next · Prakhar Shukla · MapMyTimes
        </div>
      </div>
    </div>
  );
}

/* =========================================================
   SEARCH (LIQUID GLASS)
   ========================================================= */

function SearchMock() {
  return (
    <div className="flex flex-col h-full bg-[#FFFBF8]">
      {/* Sticky glass header with search */}
      <div className="sticky top-0 z-10 relative overflow-hidden" style={{ borderBottomLeftRadius: 20, borderBottomRightRadius: 20 }}>
        <div aria-hidden className="absolute inset-0" style={{
          backdropFilter: "blur(30px)",
          WebkitBackdropFilter: "blur(30px)",
          backgroundColor: "color-mix(in srgb, white 72%, transparent)",
          borderBottom: "0.8px solid color-mix(in srgb, #0A0A0A 10%, transparent)",
          boxShadow: "0 6px 18px color-mix(in srgb, #0A0A0A 10%, transparent), 0 2px 4px color-mix(in srgb, #0A0A0A 6%, transparent)"
        }} />
        <div className="relative z-10 px-5 py-3">
          <div className="flex items-center gap-2">
            {/* Back button glass */}
            <div className="w-8 h-8 flex items-center justify-center shrink-0 relative overflow-hidden" style={{ borderRadius: 8 }}>
              <div aria-hidden className="absolute inset-0" style={{
                backdropFilter: "blur(12px)",
                WebkitBackdropFilter: "blur(12px)",
                backgroundColor: "color-mix(in srgb, white 85%, transparent)",
                border: "0.8px solid color-mix(in srgb, #0A0A0A 10%, transparent)",
                boxShadow: "0 2px 6px color-mix(in srgb, #0A0A0A 10%, transparent)",
                borderRadius: 8
              }} />
              <ChevronRight className="w-4 h-4 -rotate-180 relative z-10" />
            </div>
            {/* Search input glass */}
            <div className="flex-1 flex items-center gap-2 px-3 h-10 relative overflow-hidden" style={{ borderRadius: 999 }}>
              <div aria-hidden className="absolute inset-0" style={{
                backdropFilter: "blur(12px)",
                WebkitBackdropFilter: "blur(12px)",
                backgroundColor: "color-mix(in srgb, #E31E24 90%, transparent)",
                border: "0.8px solid color-mix(in srgb, white 22%, transparent)",
                boxShadow: "0 4px 10px color-mix(in srgb, #E31E24 20%, transparent)",
                borderRadius: 999
              }} />
              <Search className="w-4 h-4 relative z-10 text-white" />
              <input readOnly defaultValue="healthcare" className="flex-1 text-[12.5px] font-bold outline-none bg-transparent placeholder:text-white/70 relative z-10 text-white" placeholder="Search stories…" />
            </div>
          </div>
        </div>
      </div>
      <div className="flex-1 overflow-y-auto px-5 py-4 space-y-3">
        <div className="text-[11px] font-bold text-[#525252] uppercase tracking-wider">
          3 results for “healthcare”
        </div>
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="relative rounded-[14px] overflow-hidden">
            {/* Result card glass */}
            <div aria-hidden className="absolute inset-0" style={{
              backdropFilter: "blur(20px)",
              WebkitBackdropFilter: "blur(20px)",
              backgroundColor: "color-mix(in srgb, white 82%, transparent)",
              border: "0.8px solid color-mix(in srgb, #0A0A0A 8%, transparent)",
              boxShadow: "0 6px 18px color-mix(in srgb, #0A0A0A 14%, transparent), 0 2px 4px color-mix(in srgb, #0A0A0A 6%, transparent)",
              borderRadius: 14
            }} />
            <div className="relative z-10 p-3.5">
              {/* Category chip */}
              <div className="inline-block px-2 py-1 text-[8.5px] font-black tracking-widest text-white relative overflow-hidden" style={{ borderRadius: 6 }}>
                <div aria-hidden className="absolute inset-0" style={{
                  backdropFilter: "blur(12px)",
                  WebkitBackdropFilter: "blur(12px)",
                  backgroundColor: "color-mix(in srgb, #E31E24 92%, transparent)",
                  border: "0.8px solid color-mix(in srgb, white 20%, transparent)",
                  borderRadius: 6
                }} />
                <span className="relative z-10">{["POLITICS", "HEALTH", "OPINION"][i]}</span>
              </div>
              <div className="mt-2.5 text-[14px] leading-snug font-black" style={{ fontFamily: "'Archivo Black', sans-serif" }}>
                Search result title #{i + 1} — long-form Archivo Black headline three-line sample with healthcare keyword in it.
              </div>
              <div className="mt-2 text-[10.5px] text-[#7A7A7A] font-semibold">
                MapMyTimes · {i + 1} day ago
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

/* =========================================================
   MENU (tab 5) (LIQUID GLASS)
   ========================================================= */

function MenuMock() {
  return (
    <div className="flex flex-col h-full bg-[#FFFBF8]">
      <div className="px-5 pt-5">
        <BrandLogo size={18} tagline />
      </div>
      <div className="flex-1 overflow-y-auto px-5 py-5 space-y-2">
        {[
          { I: Home, l: "Home", active: true },
          { I: Newspaper, l: "All News" },
          { I: Video, l: "Videos" },
          { I: UtensilsCrossed, l: "Shorts" },
          { I: Search, l: "Search" },
          { I: Bookmark, l: "About" },
          { I: Mail, l: "Contact" },
          { I: Flame, l: "Careers" },
          { I: Flame, l: "Dashboard" },
          { I: Heart, l: "Sign in" },
        ].map((m, i) => (
          <div key={i} className="flex items-center gap-3 px-3.5 py-3 relative overflow-hidden" style={{ borderRadius: 14 }}>
            <div aria-hidden className="absolute inset-0" style={{
              backdropFilter: "blur(12px)",
              WebkitBackdropFilter: "blur(12px)",
              backgroundColor: m.active ? `color-mix(in srgb, ${NEWS50} 80%, transparent)` : "color-mix(in srgb, white 70%, transparent)",
              border: "0.8px solid color-mix(in srgb, #0A0A0A 8%, transparent)",
              boxShadow: "0 4px 10px color-mix(in srgb, #0A0A0A 8%, transparent), inset 0 2px 4px color-mix(in srgb, #0A0A0A 4%, transparent)",
              borderRadius: 14
            }} />
            <m.I className="w-4 h-4 relative z-10" />
            <span className="text-[13px] font-bold uppercase tracking-wider relative z-10">{m.l}</span>
            <ChevronRight className="w-4 h-4 ml-auto text-[#7A7A7A] relative z-10" />
          </div>
        ))}

        <div className="pt-5">
          <Eyebrow label="Follow us" />
        </div>
        {/* Social glass pills */}
        <div className="flex gap-2.5 pt-3">
          {[Facebook, X, Instagram, Youtube, Linkedin].map((I, i) => (
            <div key={i} className="w-9 h-9 flex items-center justify-center relative overflow-hidden" style={{ borderRadius: 999 }}>
              <div aria-hidden className="absolute inset-0" style={{
                backdropFilter: "blur(12px)",
                WebkitBackdropFilter: "blur(12px)",
                backgroundColor: "color-mix(in srgb, white 85%, transparent)",
                border: "0.8px solid color-mix(in srgb, #0A0A0A 10%, transparent)",
                boxShadow: "0 4px 10px color-mix(in srgb, #0A0A0A 10%, transparent)",
                borderRadius: 999
              }} />
              <I className="w-3.5 h-3.5 relative z-10" />
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

/* =========================================================
   ABOUT (LIQUID GLASS)
   ========================================================= */

function AboutMock() {
  return (
    <div className="flex flex-col h-full bg-[#FFFBF8]">
      {/* Sticky glass header */}
      <div className="sticky top-0 z-10 relative overflow-hidden" style={{ borderBottomLeftRadius: 20, borderBottomRightRadius: 20 }}>
        <div aria-hidden className="absolute inset-0" style={{
          backdropFilter: "blur(30px)",
          WebkitBackdropFilter: "blur(30px)",
          backgroundColor: "color-mix(in srgb, white 72%, transparent)",
          borderBottom: "0.8px solid color-mix(in srgb, #0A0A0A 10%, transparent)",
          boxShadow: "0 6px 18px color-mix(in srgb, #0A0A0A 10%, transparent), 0 2px 4px color-mix(in srgb, #0A0A0A 6%, transparent)"
        }} />
        <div className="relative z-10 px-5 py-3">
          <div className="text-[20px] font-black" style={{ fontFamily: "'Archivo Black', sans-serif" }}>About</div>
        </div>
      </div>
      <div className="flex-1 overflow-y-auto px-5 py-5 space-y-5">
        <p className="text-[13px] leading-7 text-[#121212]">
          MapMyTimes is an independent news platform committed to verified, unflinching journalism — reports, investigations, and storytelling that serves the public good.
        </p>
        <div className="space-y-3">
          {[
            { I: Mail, h: "Contact Newsroom", s: "newsroom@mapmytimes.com" },
            { I: Flame, h: "Join as Journalist", s: "Submit portfolio + 3 samples" },
            { I: Phone, h: "Phone", s: "+91 80859 27274" },
          ].map((r, i) => (
            <div key={i} className="p-4 flex items-start gap-3 relative overflow-hidden" style={{ borderRadius: 14 }}>
              <div aria-hidden className="absolute inset-0" style={{
                backdropFilter: "blur(20px)",
                WebkitBackdropFilter: "blur(20px)",
                backgroundColor: "color-mix(in srgb, white 82%, transparent)",
                border: "0.8px solid color-mix(in srgb, #0A0A0A 8%, transparent)",
                boxShadow: "0 6px 18px color-mix(in srgb, #0A0A0A 14%, transparent), 0 2px 4px color-mix(in srgb, #0A0A0A 6%, transparent)",
                borderRadius: 14
              }} />
              <div className="relative z-10 w-9 h-9 flex items-center justify-center shrink-0 overflow-hidden" style={{ borderRadius: 8 }}>
                <div aria-hidden className="absolute inset-0" style={{
                  backdropFilter: "blur(12px)",
                  WebkitBackdropFilter: "blur(12px)",
                  backgroundColor: "color-mix(in srgb, #E31E24 90%, transparent)",
                  border: "0.8px solid color-mix(in srgb, white 22%, transparent)",
                  boxShadow: "0 4px 10px color-mix(in srgb, #E31E24 22%, transparent)",
                  borderRadius: 8
                }} />
                <r.I className="w-4 h-4 relative z-10 text-white" />
              </div>
              <div className="relative z-10">
                <div className="text-[12px] font-black uppercase tracking-wider">{r.h}</div>
                <div className="mt-1 text-[12px] text-[#242424] font-semibold">{r.s}</div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

/* =========================================================
   CONTACT (LIQUID GLASS)
   ========================================================= */

function ContactMock() {
  return (
    <div className="flex flex-col h-full bg-[#FFFBF8]">
      {/* Sticky glass header */}
      <div className="sticky top-0 z-10 relative overflow-hidden" style={{ borderBottomLeftRadius: 20, borderBottomRightRadius: 20 }}>
        <div aria-hidden className="absolute inset-0" style={{
          backdropFilter: "blur(30px)",
          WebkitBackdropFilter: "blur(30px)",
          backgroundColor: "color-mix(in srgb, white 72%, transparent)",
          borderBottom: "0.8px solid color-mix(in srgb, #0A0A0A 10%, transparent)",
          boxShadow: "0 6px 18px color-mix(in srgb, #0A0A0A 10%, transparent), 0 2px 4px color-mix(in srgb, #0A0A0A 6%, transparent)"
        }} />
        <div className="relative z-10 px-5 py-3">
          <div className="text-[20px] font-black" style={{ fontFamily: "'Archivo Black', sans-serif" }}>Contact</div>
        </div>
      </div>
      <div className="flex-1 overflow-y-auto px-5 py-5 space-y-5">
        <Card style={{ padding: 14 }}>
          <div className="space-y-1.5">
            {[Mail, Phone, MapPin].map((I, i) => (
              <div key={i} className="flex items-center gap-3 text-[12px] font-bold">
                <I className="w-4 h-4" style={{ color: NEWS }} />
                <span>{["admin@mapmytimes.com", "+91 80859 27274", "MP Nagar, Bhopal · 462011"][i]}</span>
              </div>
            ))}
          </div>
        </Card>

        <div className="space-y-3.5">
          {["Name", "Email", "Message"].map((l, i) => (
            <div key={l}>
              <div className="text-[10.5px] font-black tracking-[1.4px] uppercase mb-1.5" style={{ color: NEWS }}>{l}</div>
              <div className="relative overflow-hidden" style={{ borderRadius: 12 }}>
                <div aria-hidden className="absolute inset-0" style={{
                  backdropFilter: "blur(12px)",
                  WebkitBackdropFilter: "blur(12px)",
                  backgroundColor: "color-mix(in srgb, white 85%, transparent)",
                  border: "0.8px solid color-mix(in srgb, #0A0A0A 10%, transparent)",
                  borderRadius: 12
                }} />
                <textarea
                  rows={i === 2 ? 4 : 1}
                  readOnly
                  placeholder={l === "Email" ? "you@domain.com" : `Your ${l.toLowerCase()}`}
                  className="w-full resize-none px-3 py-2.5 text-[12.5px] font-semibold outline-none relative z-10 bg-transparent"
                  style={{ minHeight: i === 2 ? 110 : 40 }}
                />
              </div>
            </div>
          ))}
          {/* Send button glass */}
          <button
            className="w-full py-3 text-[11.5px] font-black uppercase tracking-[2px] text-white relative overflow-hidden"
            style={{ borderRadius: 14 }}
          >
            <div aria-hidden className="absolute inset-0" style={{
              backdropFilter: "blur(12px)",
              WebkitBackdropFilter: "blur(12px)",
              backgroundColor: "color-mix(in srgb, #E31E24 92%, transparent)",
              border: "0.8px solid color-mix(in srgb, white 22%, transparent)",
              boxShadow: "0 6px 18px color-mix(in srgb, #E31E24 22%, transparent)",
              borderRadius: 14
            }} />
            <span className="relative z-10">Send Message →</span>
          </button>
        </div>
      </div>
    </div>
  );
}

/* =========================================================
   CAREERS LIST (LIQUID GLASS)
   ========================================================= */

function CareersMock() {
  const jobs = [
    { id: "1", dept: "Newsroom", type: "Full-time", level: "Sr. 5+ yrs", loc: "Bhopal · Hybrid", t: "Senior Political Correspondent", d: "Cover state politics and government affairs. Pitch, report and file 2–3 investigative stories every week with the MapMyTimes desk." },
    { id: "2", dept: "Video", type: "Full-time", level: "2–4 yrs", loc: "Remote · India", t: "Video Editor — Shorts / Reels", d: "Edit short-form 30–90s videos for MapMyTimes Shorts feed. Sync subtitles, sound design and kinetic typography. ~14 titles/week." },
    { id: "3", dept: "Engineering", type: "Full-time", level: "3–6 yrs", loc: "Remote · India", t: "Product Engineer — Flutter", d: "Build the MapMyTimes iOS/Android companion app with Riverpod, Dio and go_router. Port neo-brutalist tokens end-to-end." },
    { id: "4", dept: "Audience", type: "Internship", level: "0–1 yrs", loc: "Bhopal · On-site", t: "Community & Audience Intern", d: "Curate wires, moderate comments, publish 2–3 newsletters/mo. Coordinate social team on trending reports + WhatsApp channel." },
  ];
  return (
    <div className="flex flex-col h-full bg-[#FFFBF8]">
      {/* Sticky glass header */}
      <div className="sticky top-0 z-10 relative overflow-hidden" style={{ borderBottomLeftRadius: 20, borderBottomRightRadius: 20 }}>
        <div aria-hidden className="absolute inset-0" style={{
          backdropFilter: "blur(30px)",
          WebkitBackdropFilter: "blur(30px)",
          backgroundColor: "color-mix(in srgb, white 72%, transparent)",
          borderBottom: "0.8px solid color-mix(in srgb, #0A0A0A 10%, transparent)",
          boxShadow: "0 6px 18px color-mix(in srgb, #0A0A0A 10%, transparent), 0 2px 4px color-mix(in srgb, #0A0A0A 6%, transparent)"
        }} />
        <div className="relative z-10 px-5 py-3">
          <div className="text-[20px] font-black" style={{ fontFamily: "'Archivo Black', sans-serif" }}>Careers</div>
        </div>
      </div>
      <div className="flex-1 overflow-y-auto px-5 py-5 space-y-4">
        {jobs.map((j) => (
          <Card key={j.id} style={{ padding: 14 }}>
            {/* Job tags glass */}
            <div className="flex flex-wrap gap-1.5">
              {[j.type, ...(j.loc.startsWith("Remote") ? ["REMOTE"] : []), j.level].map((x) => (
                <div key={x} className="px-2 py-1 text-[8.5px] font-black tracking-widest uppercase relative overflow-hidden" style={{ borderRadius: 6 }}>
                  <div aria-hidden className="absolute inset-0" style={{
                    backdropFilter: "blur(12px)",
                    WebkitBackdropFilter: "blur(12px)",
                    backgroundColor: x === j.type ? "color-mix(in srgb, #E31E24 90%, transparent)" : "color-mix(in srgb, white 85%, transparent)",
                    border: "0.8px solid color-mix(in srgb, " + (x === j.type ? "white 22%" : "#0A0A0A 10%") + ", transparent)",
                    boxShadow: x === j.type ? "0 2px 6px color-mix(in srgb, #E31E24 20%, transparent)" : "none",
                    borderRadius: 6
                  }} />
                  <span className="relative z-10" style={{ color: x === j.type ? "white" : INK }}>{x}</span>
                </div>
              ))}
            </div>
            <div className="mt-3 text-[19px] leading-tight font-black" style={{ fontFamily: "'Archivo Black', sans-serif" }}>{j.t}</div>
            <div className="mt-2 text-[11.5px] font-bold text-[#525252]">{j.dept} · {j.loc}</div>
            <p className="mt-3 text-[12px] leading-6 line-clamp-4 text-[#242424]">{j.d}</p>
            {/* Apply button glass */}
            <button className="mt-4 w-full py-2.5 text-[11px] font-black uppercase tracking-[2px] text-white relative overflow-hidden" style={{ borderRadius: 12 }}>
              <div aria-hidden className="absolute inset-0" style={{
                backdropFilter: "blur(12px)",
                WebkitBackdropFilter: "blur(12px)",
                backgroundColor: "color-mix(in srgb, #E31E24 92%, transparent)",
                border: "0.8px solid color-mix(in srgb, white 22%, transparent)",
                boxShadow: "0 4px 10px color-mix(in srgb, #E31E24 20%, transparent)",
                borderRadius: 12
              }} />
              <span className="relative z-10">Apply Now</span>
            </button>
          </Card>
        ))}
      </div>
    </div>
  );
}

/* =========================================================
   CAREER DETAIL (LIQUID GLASS)
   ========================================================= */

function CareerDetailMock() {
  return (
    <div className="flex flex-col h-full bg-[#FFFBF8]">
      {/* Sticky glass header with back */}
      <div className="sticky top-0 z-10 relative overflow-hidden" style={{ borderBottomLeftRadius: 20, borderBottomRightRadius: 20 }}>
        <div aria-hidden className="absolute inset-0" style={{
          backdropFilter: "blur(30px)",
          WebkitBackdropFilter: "blur(30px)",
          backgroundColor: "color-mix(in srgb, white 72%, transparent)",
          borderBottom: "0.8px solid color-mix(in srgb, #0A0A0A 10%, transparent)",
          boxShadow: "0 6px 18px color-mix(in srgb, #0A0A0A 10%, transparent), 0 2px 4px color-mix(in srgb, #0A0A0A 6%, transparent)"
        }} />
        <div className="relative z-10 px-5 py-3">
          <div className="flex items-center gap-2">
            {/* Back button glass */}
            <div className="w-8 h-8 flex items-center justify-center relative overflow-hidden" style={{ borderRadius: 8 }}>
              <div aria-hidden className="absolute inset-0" style={{
                backdropFilter: "blur(12px)",
                WebkitBackdropFilter: "blur(12px)",
                backgroundColor: "color-mix(in srgb, white 85%, transparent)",
                border: "0.8px solid color-mix(in srgb, #0A0A0A 10%, transparent)",
                boxShadow: "0 2px 6px color-mix(in srgb, #0A0A0A 10%, transparent)",
                borderRadius: 8
              }} />
              <ChevronRight className="w-4 h-4 -rotate-180 relative z-10" />
            </div>
            <div className="text-[17px] font-black truncate" style={{ fontFamily: "'Archivo Black', sans-serif" }}>Product Engineer — Flutter</div>
          </div>
        </div>
      </div>
      <div className="flex-1 overflow-y-auto px-5 py-5 space-y-5">
        <div className="text-[10.5px] font-black tracking-[1.6px] uppercase" style={{ color: NEWS }}>Engineering · Open role</div>
        <div className="text-[26px] leading-tight font-black" style={{ fontFamily: "'Archivo Black', sans-serif", color: INK }}>
          Product Engineer — Flutter
        </div>
        {/* Role tags glass */}
        <div className="flex flex-wrap gap-2">
          {["Full-time", "REMOTE", "3–6 yrs", "Remote · India"].map((x, i) => (
            <div key={x} className="px-2.5 py-1.5 text-[9.5px] font-black tracking-widest uppercase relative overflow-hidden" style={{ borderRadius: 8 }}>
              <div aria-hidden className="absolute inset-0" style={{
                backdropFilter: "blur(12px)",
                WebkitBackdropFilter: "blur(12px)",
                backgroundColor: i === 0 ? "color-mix(in srgb, #E31E24 92%, transparent)" : "color-mix(in srgb, white 85%, transparent)",
                border: "0.8px solid color-mix(in srgb, " + (i === 0 ? "white 22%" : "#0A0A0A 10%") + ", transparent)",
                boxShadow: i === 0 ? "0 4px 10px color-mix(in srgb, #E31E24 22%, transparent)" : "none",
                borderRadius: 8
              }} />
              <span className="relative z-10" style={{ color: i === 0 ? "white" : INK }}>{x}</span>
            </div>
          ))}
        </div>
        <Card style={{ padding: 14 }}>
          <p className="text-[12.5px] leading-7 text-[#121212]">
            Build the MapMyTimes iOS/Android companion app with Riverpod, Dio and go_router. Drive the design system port end-to-end — neo-brutalist tokens, Archivo Black headlines, hard 4×4 ink shadows — integrate blog + auth + notification microservices with the API gateway. Work with the web team to ship parity features across home, news list, videos, shorts, article, search and journalist dashboard screens.
          </p>
        </Card>
        {/* Apply Now button glass */}
        <button className="w-full py-3 text-[11.5px] font-black uppercase tracking-[2px] text-white relative overflow-hidden" style={{ borderRadius: 14 }}>
          <div aria-hidden className="absolute inset-0" style={{
            backdropFilter: "blur(12px)",
            WebkitBackdropFilter: "blur(12px)",
            backgroundColor: "color-mix(in srgb, #E31E24 92%, transparent)",
            border: "0.8px solid color-mix(in srgb, white 22%, transparent)",
            boxShadow: "0 6px 18px color-mix(in srgb, #E31E24 24%, transparent)",
            borderRadius: 14
          }} />
          <span className="relative z-10">Apply Now</span>
        </button>
      </div>
    </div>
  );
}

/* =========================================================
   LOGIN (FULLSCREEN DIALOG) (LIQUID GLASS)
   ========================================================= */

function LoginMock() {
  return (
    <div className="flex flex-col h-full bg-[#FFFBF8]">
      {/* Sticky glass header with close */}
      <div className="sticky top-0 z-10 relative overflow-hidden" style={{ borderBottomLeftRadius: 20, borderBottomRightRadius: 20 }}>
        <div aria-hidden className="absolute inset-0" style={{
          backdropFilter: "blur(30px)",
          WebkitBackdropFilter: "blur(30px)",
          backgroundColor: "color-mix(in srgb, white 72%, transparent)",
          borderBottom: "0.8px solid color-mix(in srgb, #0A0A0A 10%, transparent)",
          boxShadow: "0 6px 18px color-mix(in srgb, #0A0A0A 10%, transparent), 0 2px 4px color-mix(in srgb, #0A0A0A 6%, transparent)"
        }} />
        <div className="relative z-10 px-5 py-3 flex items-center gap-2">
          {/* Close glass pill */}
          <div className="w-8 h-8 flex items-center justify-center ml-auto relative overflow-hidden" style={{ borderRadius: 8 }}>
            <div aria-hidden className="absolute inset-0" style={{
              backdropFilter: "blur(12px)",
              WebkitBackdropFilter: "blur(12px)",
              backgroundColor: "color-mix(in srgb, white 85%, transparent)",
              border: "0.8px solid color-mix(in srgb, #0A0A0A 10%, transparent)",
              boxShadow: "0 2px 6px color-mix(in srgb, #0A0A0A 10%, transparent)",
              borderRadius: 8
            }} />
            <span className="text-[12px] font-bold relative z-10">×</span>
          </div>
        </div>
      </div>
      <div className="flex-1 overflow-y-auto px-5 py-6 space-y-6">
        <BrandLogo size={20} tagline />
        <div>
          <div
            className="text-[24px] leading-tight font-black"
            style={{ fontFamily: "'Archivo Black', sans-serif" }}
          >
            Welcome back.
          </div>
          <div className="mt-1 text-[13px] text-[#525252] font-semibold">Sign in to MapMyTimes</div>
        </div>

        {/* Input fields glass */}
        <div className="space-y-3.5">
          {[
            { l: "Email", ph: "you@mapmytimes.com", k: "email" },
            { l: "Password", ph: "••••••••", k: "pass" },
          ].map((f) => (
            <div key={f.k}>
              <div className="text-[10.5px] font-black tracking-[1.4px] uppercase mb-1.5" style={{ color: NEWS }}>{f.l}</div>
              <div className="relative overflow-hidden" style={{ borderRadius: 12 }}>
                <div aria-hidden className="absolute inset-0" style={{
                  backdropFilter: "blur(12px)",
                  WebkitBackdropFilter: "blur(12px)",
                  backgroundColor: "color-mix(in srgb, white 85%, transparent)",
                  border: "0.8px solid color-mix(in srgb, #0A0A0A 10%, transparent)",
                  borderRadius: 12
                }} />
                <input
                  readOnly
                  placeholder={f.ph}
                  className="w-full px-3 py-2.5 text-[12.5px] font-semibold outline-none relative z-10 bg-transparent"
                />
              </div>
            </div>
          ))}
          <div className="text-right text-[11.5px] font-bold underline text-[#242424]">Forgot password?</div>
        </div>

        {/* Sign In button glass */}
        <button
          className="w-full py-3 text-[11.5px] font-black uppercase tracking-[2px] text-white relative overflow-hidden"
          style={{ borderRadius: 14 }}
        >
          <div aria-hidden className="absolute inset-0" style={{
            backdropFilter: "blur(12px)",
            WebkitBackdropFilter: "blur(12px)",
            backgroundColor: "color-mix(in srgb, #E31E24 92%, transparent)",
            border: "0.8px solid color-mix(in srgb, white 22%, transparent)",
            boxShadow: "0 6px 18px color-mix(in srgb, #E31E24 24%, transparent)",
            borderRadius: 14
          }} />
          <span className="relative z-10">Sign In</span>
        </button>

        <div className="flex items-center gap-3">
          <div className="flex-1 h-px" style={{ background: "color-mix(in srgb, #0A0A0A 12%, transparent)" }} />
          <div className="text-[10.5px] font-black tracking-[1.6px] uppercase text-[#525252]">or</div>
          <div className="flex-1 h-px" style={{ background: "color-mix(in srgb, #0A0A0A 12%, transparent)" }} />
        </div>

        {/* Social sign in buttons glass */}
        <div className="space-y-2.5">
          {[
            { I: Search, l: "Continue with Google" },
            { I: Facebook, l: "Continue with Facebook", blue: true },
          ].map((b, i) => (
            <button
              key={i}
              className="w-full py-2.5 text-[10.5px] font-black uppercase tracking-[1.4px] flex items-center justify-center gap-2.5 relative overflow-hidden"
              style={{ borderRadius: 12 }}
            >
              <div aria-hidden className="absolute inset-0" style={{
                backdropFilter: "blur(12px)",
                WebkitBackdropFilter: "blur(12px)",
                backgroundColor: "color-mix(in srgb, white 85%, transparent)",
                border: "0.8px solid color-mix(in srgb, #0A0A0A 10%, transparent)",
                boxShadow: "0 4px 10px color-mix(in srgb, #0A0A0A 10%, transparent)",
                borderRadius: 12
              }} />
              <span className="relative z-10 flex items-center justify-center gap-2.5">
                <b.I className="w-3.5 h-3.5" style={{ color: b.blue ? "#1877F2" : INK }} />
                {b.l}
              </span>
            </button>
          ))}
        </div>

        <div className="pt-1 space-y-2 text-center">
          <div className="text-[12px] font-bold text-[#525252]">Don&apos;t have an account yet?</div>
          {/* Join button glass */}
          <button
            className="w-full py-2.5 text-[11px] font-black uppercase tracking-[2px] relative overflow-hidden"
            style={{ borderRadius: 12 }}
          >
            <div aria-hidden className="absolute inset-0" style={{
              backdropFilter: "blur(12px)",
              WebkitBackdropFilter: "blur(12px)",
              backgroundColor: "color-mix(in srgb, white 85%, transparent)",
              border: "0.8px solid color-mix(in srgb, #0A0A0A 10%, transparent)",
              boxShadow: "0 4px 10px color-mix(in srgb, #0A0A0A 10%, transparent)",
              borderRadius: 12
            }} />
            <span className="relative z-10">Join MapMyTimes</span>
          </button>
        </div>
      </div>
    </div>
  );
}

/* =========================================================
   DASHBOARD (Journalist) (LIQUID GLASS)
   ========================================================= */

function DashboardMock() {
  const tiles = [
    { l: "Write story", I: Newspaper, c: NEWS },
    { l: "My posts", I: Bookmark, c: INK },
    { l: "Moderation", I: MessageSquare, c: INK },
    { l: "Notifications", I: Heart, c: INK },
    { l: "Settings", I: Search, c: INK },
  ];
  return (
    <div className="flex flex-col h-full bg-[#FFFBF8]">
      {/* Sticky glass header */}
      <div className="sticky top-0 z-10 relative overflow-hidden" style={{ borderBottomLeftRadius: 20, borderBottomRightRadius: 20 }}>
        <div aria-hidden className="absolute inset-0" style={{
          backdropFilter: "blur(30px)",
          WebkitBackdropFilter: "blur(30px)",
          backgroundColor: "color-mix(in srgb, white 72%, transparent)",
          borderBottom: "0.8px solid color-mix(in srgb, #0A0A0A 10%, transparent)",
          boxShadow: "0 6px 18px color-mix(in srgb, #0A0A0A 10%, transparent), 0 2px 4px color-mix(in srgb, #0A0A0A 6%, transparent)"
        }} />
        <div className="relative z-10 px-5 py-3">
          <div className="text-[20px] font-black" style={{ fontFamily: "'Archivo Black', sans-serif" }}>Dashboard</div>
        </div>
      </div>
      <div className="flex-1 overflow-y-auto px-5 py-5 space-y-5">
        {/* Hero banner glass */}
        <div className="p-5 relative overflow-hidden" style={{ borderRadius: 20 }}>
          <div aria-hidden className="absolute inset-0" style={{
            backdropFilter: "blur(20px)",
            WebkitBackdropFilter: "blur(20px)",
            background: `linear-gradient(135deg, color-mix(in srgb, #E31E24 95%, transparent) 0%, color-mix(in srgb, #C0181D 95%, transparent) 100%)`,
            border: "0.8px solid color-mix(in srgb, white 18%, transparent)",
            boxShadow: "0 10px 30px color-mix(in srgb, #E31E24 22%, transparent), 0 4px 10px color-mix(in srgb, #E31E24 12%, transparent)",
            borderRadius: 20
          }} />
          <div className="relative z-10">
            <div className="text-[9.5px] font-black tracking-[2.2px] uppercase text-white/90" style={{ fontFamily: "'Archivo Black', sans-serif" }}>
              Journalism of Integrity
            </div>
            <div
              className="mt-3 text-[22px] leading-tight font-black text-white"
              style={{ fontFamily: "'Archivo Black', sans-serif" }}
            >
              Publish stories that matter.
            </div>
            <p className="mt-2.5 text-[11.5px] leading-6 text-white/80">
              Your MapMyTimes journalist workspace — drafts, moderation queue, notifications and team settings, all in one place.
            </p>
          </div>
        </div>

        {/* Tiles grid glass */}
        <div className="grid grid-cols-2 gap-3.5">
          {tiles.map((t, i) => (
            <div key={i} className="p-4 flex flex-col gap-3 relative overflow-hidden" style={{ borderRadius: 14, minHeight: 120 }}>
              <div aria-hidden className="absolute inset-0" style={{
                backdropFilter: "blur(20px)",
                WebkitBackdropFilter: "blur(20px)",
                backgroundColor: "color-mix(in srgb, white 82%, transparent)",
                border: "0.8px solid color-mix(in srgb, #0A0A0A 8%, transparent)",
                boxShadow: "0 6px 18px color-mix(in srgb, #0A0A0A 14%, transparent), 0 2px 4px color-mix(in srgb, #0A0A0A 6%, transparent)",
                borderRadius: 14
              }} />
              <div className="relative z-10 w-9 h-9 flex items-center justify-center text-white overflow-hidden" style={{ borderRadius: 8 }}>
                <div aria-hidden className="absolute inset-0" style={{
                  backdropFilter: "blur(12px)",
                  WebkitBackdropFilter: "blur(12px)",
                  backgroundColor: "color-mix(in srgb, " + t.c + " 90%, transparent)",
                  border: "0.8px solid color-mix(in srgb, " + (t.c === NEWS ? "white 22%" : "white 16%") + ", transparent)",
                  boxShadow: "0 4px 10px color-mix(in srgb, " + t.c + " 20%, transparent)",
                  borderRadius: 8
                }} />
                <t.I className="w-4 h-4 relative z-10" />
              </div>
              <div className="mt-auto text-[11.5px] leading-tight font-black relative z-10" style={{ fontFamily: "'Archivo Black', sans-serif" }}>
                {t.l}
              </div>
              <div className="text-[10px] font-bold text-[#7A7A7A] uppercase tracking-wider relative z-10">
                {["Start a new draft", "14 published", "3 pending", "2 new", "Preferences"][i]}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
