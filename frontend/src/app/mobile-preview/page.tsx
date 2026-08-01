"use client";

import { useState } from "react";
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
          backgroundColor: NEWS,
          border: `2px solid ${INK}`,
          fontSize: size + 2,
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

function Card({ children, style }: { children: React.ReactNode; style?: React.CSSProperties }) {
  return (
    <div
      style={{
        border: `2px solid ${INK}`,
        boxShadow: hardShadow(4),
        backgroundColor: "white",
        ...style,
      }}
    >
      {children}
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
    <div style={{ borderTop: `2px solid ${INK}`, background: "white" }}>
      <div className="grid grid-cols-5">
        {items.map((it) => {
          const I = it.I;
          const on = active === it.key;
          return (
            <button key={it.key} onClick={() => go(it.key)} className="py-2.5 flex flex-col items-center gap-1" style={{ color: on ? NEWS : INK600 }}>
              <I className="w-4 h-4" />
              <span className="text-[9px] font-bold uppercase tracking-wider">{it.label}</span>
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
      {/* App bar */}
      <div className="sticky top-0 z-10 bg-white px-5 py-3" style={{ borderBottom: `2px solid ${INK}` }}>
        <div className="flex items-center gap-2.5">
          <BrandLogo size={14} />
          <div className="ml-auto flex items-center gap-2.5">
            <div className="flex-1 flex items-center gap-2 px-2.5 h-9" style={{ border: `2px solid ${INK}`, minWidth: 130 }}>
              <Search className="w-3.5 h-3.5" />
              <span className="text-[11px] font-semibold text-[#7A7A7A]">Search…</span>
            </div>
            <div
              className="h-9 w-12 flex items-center justify-center text-[11px] font-black"
              style={{ backgroundColor: NEWS, color: "white", border: `2px solid ${INK}` }}
            >
              EN
            </div>
          </div>
        </div>
      </div>

      {/* Hero */}
      <div className="px-5 py-6" style={{ background: NEWS50, borderBottom: `2px solid ${INK}` }}>
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
              <div className="h-36 w-full" style={{ background: i === 0 ? "linear-gradient(135deg,#FDECEE,#E31E24)" : "linear-gradient(135deg,#0A0A0A,#242424)", borderBottom: `2px solid ${INK}` }} />
              <div className="p-3.5 space-y-2">
                <div className="inline-block px-2 py-1 text-[9px] font-black tracking-[1.4px] text-white" style={{ backgroundColor: NEWS }}>
                  {x.cat}
                </div>
                <div className="text-[15px] leading-snug font-black" style={{ fontFamily: "'Archivo Black', sans-serif", color: INK }}>
                  {x.t}
                </div>
                <div className="text-[10.5px] text-[#7A7A7A] font-semibold">
                  By Prakhar Shukla · 18 Jul 2026 · 7 min read
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
            <div key={i} className="flex gap-3 items-start" style={{ border: `2px solid ${INK}`, boxShadow: hardShadow(2), background: "white", padding: "10px" }}>
              <div
                className="w-10 h-10 shrink-0 flex items-center justify-center text-[15px] font-black text-white"
                style={{ backgroundColor: NEWS, border: `2px solid ${INK}` }}
              >
                {String(i + 1).padStart(2, "0")}
              </div>
              <div className="flex-1 min-w-0">
                <div className="text-[12.5px] leading-5 font-black line-clamp-3" style={{ fontFamily: "'Archivo Black', sans-serif" }}>
                  {t}
                </div>
                <div className="mt-1 text-[10px] text-[#7A7A7A] font-semibold">
                  MapMyTimes · {i + 1}h ago · {(i + 2) * 1200} views
                </div>
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
              <div className="h-20" style={{ background: i % 2 ? NEWS50 : "#F4F4F5", border: `2px solid ${INK}` }} />
              <div className="mt-2.5 inline-block px-1.5 py-0.5 text-[8px] font-black tracking-widest text-white" style={{ backgroundColor: NEWS }}>
                {["NATIONAL", "STATE", "CRIME", "BUSINESS", "SPORTS", "OPINION"][i]}
              </div>
              <div className="mt-2 text-[11px] leading-snug font-black" style={{ fontFamily: "'Archivo Black', sans-serif" }}>
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
            <div key={c} className="px-3 py-1.5 text-[10.5px] font-bold uppercase tracking-wider" style={{ border: `2px solid ${INK}`, backgroundColor: i === 0 ? NEWS : "white", color: i === 0 ? "white" : INK }}>
              {c}
            </div>
          ))}
        </div>
      </div>

      <div className="mx-5 mt-6" style={{ height: 2, background: INK }} />
      <div className="px-5 pt-5 pb-6">
        <Eyebrow label="Follow us" />
        <p className="mt-4 text-[12px] leading-6 text-[#242424]">
          MapMyTimes is an independent news platform committed to verified, unflinching journalism — reports, investigations, and storytelling that serves the public good.
        </p>
        <div className="mt-5 flex gap-2.5">
          {[Facebook, X, Instagram, Youtube, Linkedin].map((I, i) => (
            <div key={i} className="w-9 h-9 flex items-center justify-center" style={{ border: `2px solid ${INK}`, background: "white" }}>
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
      <div className="sticky top-0 z-10 bg-[#0A0A0A] text-white px-5 py-3.5" style={{ borderBottom: `2px solid ${INK}` }}>
        <div
          className="text-[22px] font-black leading-none tracking-tight"
          style={{ fontFamily: "'Archivo Black', sans-serif" }}
        >
          News
        </div>
      </div>
      <div className="flex-1 overflow-y-auto px-5 py-5 space-y-4">
        {Array.from({ length: 8 }).map((_, i) => (
          <div key={i} className="flex" style={{ border: `2px solid ${INK}`, boxShadow: hardShadow(4), background: "white" }}>
            <div className="w-[124px] shrink-0" style={{ background: i % 2 ? INK700 : NEWS50, borderRight: `2px solid ${INK}` }} />
            <div className="flex-1 p-3 space-y-2">
              <div className="inline-block px-2 py-1 text-[8.5px] font-black tracking-[1.4px] text-white" style={{ backgroundColor: NEWS }}>
                {["POLITICS", "STATE", "CRIME", "BUSINESS", "SPORTS", "EDUCATION", "HEALTH", "WORLD"][i]}
              </div>
              <div className="text-[14.5px] leading-snug font-black line-clamp-3" style={{ fontFamily: "'Archivo Black', sans-serif" }}>
                Long-form headline sample for the MapMyTimes news list tile with three-line Archivo Black clamp — item #{i + 1}.
              </div>
              <div className="text-[10.5px] text-[#7A7A7A] font-semibold">
                Prakhar · 1{(i + 1) % 9} Jul · {(i + 3) * 900} views
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

/* =========================================================
   VIDEOS GRID
   ========================================================= */

function VideosMock() {
  return (
    <div className="flex flex-col h-full bg-[#FFFBF8]">
      <div className="sticky top-0 z-10 bg-white px-5 py-3.5" style={{ borderBottom: `2px solid ${INK}` }}>
        <Eyebrow label="Watch — #1" />
        <div className="mt-2 flex items-start justify-between gap-3">
          <div
            className="text-[20px] font-black leading-tight flex-1"
            style={{ fontFamily: "'Archivo Black', sans-serif", color: INK }}
          >
            Bhopal: 10 minutes that changed the monsoon session
          </div>
          <button
            className="shrink-0 px-3 py-1.5 text-[10.5px] font-black uppercase tracking-wider flex items-center gap-1.5 text-white"
            style={{ background: NEWS, border: `2px solid ${INK}`, boxShadow: hardShadow(3) }}
          >
            <PlayCircle className="w-3.5 h-3.5" />
            Watch More
          </button>
        </div>
      </div>
      <div className="flex-1 overflow-y-auto px-4 py-4">
        <div className="grid grid-cols-2 gap-3.5">
          {Array.from({ length: 8 }).map((_, i) => (
            <div key={i} style={{ border: `2px solid ${INK}`, boxShadow: hardShadow(4), background: "white" }}>
              <div className="relative h-[110px]" style={{ background: `linear-gradient(135deg, ${i % 2 ? INK : NEWS} 0%, ${INK700} 100%)`, borderBottom: `2px solid ${INK}` }}>
                <div
                  className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-12 h-12 rounded-full flex items-center justify-center text-white"
                  style={{ backgroundColor: NEWS, border: `2px solid ${INK}` }}
                >
                  <PlayCircle className="w-6 h-6" />
                </div>
              </div>
              <div className="p-2.5">
                <div className="text-[11.5px] leading-snug font-black line-clamp-3" style={{ fontFamily: "'Archivo Black', sans-serif" }}>
                  Video title card #{i + 1} — two-column grid thumbnail with hard shadow border.
                </div>
                <div className="mt-2 text-[9.5px] font-bold text-[#7A7A7A]">
                  {(i + 1) * 32}K views · {i + 2}d
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
   ARTICLE
   ========================================================= */

function ArticleMock() {
  return (
    <div className="flex flex-col h-full bg-[#FFFBF8]">
      <div
        className="h-[260px] w-full relative"
        style={{ background: `linear-gradient(180deg, ${NEWS50}, ${NEWS})`, borderBottom: `2px solid ${INK}` }}
      >
        <div className="absolute top-3 left-3">
          <div className="w-8 h-8 flex items-center justify-center" style={{ background: "white", border: `2px solid ${INK}` }}>
            <ChevronRight className="w-4 h-4 -rotate-180" />
          </div>
        </div>
      </div>
      <div className="flex-1 overflow-y-auto px-5 py-5 space-y-4 pb-6">
        <div className="flex flex-wrap gap-2">
          {["POLITICS", "MADHYA PRADESH"].map((x) => (
            <div key={x} className="px-2.5 py-1.5 text-[10px] font-black tracking-[1.4px] uppercase text-white" style={{ backgroundColor: NEWS, border: `2px solid ${INK}` }}>
              {x}
            </div>
          ))}
        </div>
        <div className="text-[26px] leading-tight font-black" style={{ fontFamily: "'Archivo Black', sans-serif", color: INK }}>
          Bhopal monsoon session: 10-minute debate that reshaped MP&apos;s rural healthcare bill.
        </div>
        <div className="text-[13.5px] italic leading-6 text-[#242424]">
          On-ground reporting from the Assembly press gallery — what the bill means for 52 district hospitals and 1,200 sub-centres across the state.
        </div>
        <div style={{ height: 2, background: INK }} />
        <div className="flex flex-wrap items-center gap-x-3 gap-y-2 text-[11px] font-semibold text-[#525252]">
          <span>By <strong className="text-black">Prakhar Shukla</strong></span>
          <span>·</span>
          <span>18 Jul 2026</span>
          <span className="px-2 py-0.5" style={{ border: `2px solid ${INK}` }}>7 MIN READ</span>
          <span className="flex items-center gap-1"><Eye className="w-3.5 h-3.5" />2.4K</span>
        </div>
        {Array.from({ length: 6 }).map((_, i) => (
          <p key={i} className="text-[13px] leading-7 text-[#121212]">
            {i === 0
              ? "BHOPAL — The monsoon session of the Madhya Pradesh Assembly concluded late on Thursday with a 214-page majority report on rural healthcare funding that, for the first time in 17 years, ties annual budgetary allocations to outpatient footfall at block-level primary health centres."
              : "The changes, tabled by the health minister after three rounds of standing committee review, create a rolling performance-linked tranche of ₹240 Cr distributed every quarter. District collectors will be required to publish PHC-level utilisation dashboards on the state health portal within 15 days of each quarter end — a demand that opposition parties had been raising since the 2024 supplementary budget."}
          </p>
        ))}
        <div className="flex flex-wrap gap-2">
          {["#Healthcare", "#MPAssembly", "#Bhopal", "#PublicPolicy"].map((t) => (
            <div key={t} className="px-2.5 py-1.5 text-[10px] font-bold uppercase tracking-wider" style={{ border: `2px solid ${INK}`, backgroundColor: NEWS50 }}>
              {t}
            </div>
          ))}
        </div>
        <div style={{ height: 2, background: INK }} />
        <div className="flex items-center gap-2.5">
          <button className="flex-1 py-2.5 text-[11px] font-black uppercase tracking-wider flex items-center justify-center gap-1.5" style={{ border: `2px solid ${INK}`, boxShadow: hardShadow(3), background: "white" }}>
            <Share2 className="w-3.5 h-3.5" />
            Share
          </button>
          <button className="flex-1 py-2.5 text-[11px] font-black uppercase tracking-wider flex items-center justify-center gap-1.5" style={{ border: `2px solid ${INK}`, boxShadow: hardShadow(3), background: "white" }}>
            <Copy className="w-3.5 h-3.5" />
            Copy Link
          </button>
        </div>
        <div className="text-[10.5px] text-[#7A7A7A] font-semibold pt-2">© 2026 MAPMYTOUR LLP. All rights reserved.</div>
      </div>
    </div>
  );
}

/* =========================================================
   SHORTS FEED (FULLSCREEN DARK)
   ========================================================= */

function ShortsMock() {
  return (
    <div className="relative h-full w-full overflow-hidden" style={{ backgroundColor: INK }}>
      {/* gradient cover */}
      <div className="absolute inset-0" style={{ background: `linear-gradient(180deg, #000 0%, ${NEWS} 120%)` }} />
      <div className="absolute inset-0 flex flex-col justify-end px-5 pb-10 text-white">
        <Eyebrow label="Shorts · 02" />
        <div
          className="mt-3 text-[24px] leading-tight font-black"
          style={{ fontFamily: "'Archivo Black', sans-serif" }}
        >
          30 seconds: how MP&apos;s new PHC dashboard actually works — explained by the district collector of Hoshangabad.
        </div>
        <p className="mt-3 text-[12.5px] leading-6 text-white/70 line-clamp-5">
          Bhopal bureau chief Prakhar Shukla breaks down the new quarterly-healthcare-dashboard rule — the 3 key numbers you need to track, and why opposition MLAs say it still doesn&apos;t fix staff vacancies at CHCs.
        </p>
        <div className="mt-4 flex flex-wrap gap-2">
          {[
            { I: Eye, n: "24K" },
            { I: Heart, n: "1.8K" },
            { I: MessageSquare, n: "312" },
          ].map(({ I, n }) => (
            <div key={n} className="px-2.5 py-1.5 text-[10px] font-bold flex items-center gap-1.5" style={{ border: `2px solid rgba(255,255,255,0.4)` }}>
              <I className="w-3 h-3" />
              <span>{n}</span>
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
   SEARCH
   ========================================================= */

function SearchMock() {
  return (
    <div className="flex flex-col h-full bg-[#FFFBF8]">
      <div className="sticky top-0 z-10 bg-white px-5 py-3" style={{ borderBottom: `2px solid ${INK}` }}>
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 flex items-center justify-center shrink-0" style={{ border: `2px solid ${INK}` }}>
            <ChevronRight className="w-4 h-4 -rotate-180" />
          </div>
          <div className="flex-1 flex items-center gap-2 px-3 h-10" style={{ border: `3px solid ${NEWS}` }}>
            <Search className="w-4 h-4" />
            <input readOnly defaultValue="healthcare" className="flex-1 text-[12.5px] font-bold outline-none bg-transparent placeholder:text-[#7A7A7A]" placeholder="Search stories…" />
          </div>
        </div>
      </div>
      <div className="flex-1 overflow-y-auto px-5 py-4 space-y-3">
        <div className="text-[11px] font-bold text-[#525252] uppercase tracking-wider">
          3 results for “healthcare”
        </div>
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="p-3.5" style={{ border: `2px solid ${INK}`, boxShadow: hardShadow(3), background: "white" }}>
            <div className="inline-block px-2 py-1 text-[8.5px] font-black tracking-widest text-white" style={{ backgroundColor: NEWS }}>
              {["POLITICS", "HEALTH", "OPINION"][i]}
            </div>
            <div className="mt-2.5 text-[14px] leading-snug font-black" style={{ fontFamily: "'Archivo Black', sans-serif" }}>
              Search result title #{i + 1} — long-form Archivo Black headline three-line sample with healthcare keyword in it.
            </div>
            <div className="mt-2 text-[10.5px] text-[#7A7A7A] font-semibold">
              MapMyTimes · {i + 1} day ago
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

/* =========================================================
   MENU (tab 5)
   ========================================================= */

function MenuMock() {
  return (
    <div className="flex flex-col h-full bg-[#FFFBF8]">
      <div className="px-5 pt-5">
        <BrandLogo size={18} tagline />
      </div>
      <div className="flex-1 overflow-y-auto px-5 py-5 space-y-1">
        {[
          { I: Home, l: "Home" },
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
          <div key={i} className="flex items-center gap-3 px-3.5 py-3" style={{ border: `2px solid ${INK}`, borderBottom: i === 9 ? `2px solid ${INK}` : "none", background: i === 0 ? NEWS50 : "white" }}>
            <m.I className="w-4 h-4" />
            <span className="text-[13px] font-bold uppercase tracking-wider">{m.l}</span>
            <ChevronRight className="w-4 h-4 ml-auto text-[#7A7A7A]" />
          </div>
        ))}

        <div className="pt-5">
          <Eyebrow label="Follow us" />
        </div>
        <div className="flex gap-2.5 pt-3">
          {[Facebook, X, Instagram, Youtube, Linkedin].map((I, i) => (
            <div key={i} className="w-9 h-9 flex items-center justify-center" style={{ border: `2px solid ${INK}`, background: "white" }}>
              <I className="w-3.5 h-3.5" />
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

/* =========================================================
   ABOUT
   ========================================================= */

function AboutMock() {
  return (
    <div className="flex flex-col h-full bg-[#FFFBF8]">
      <div className="sticky top-0 z-10 bg-white px-5 py-3" style={{ borderBottom: `2px solid ${INK}` }}>
        <div className="text-[20px] font-black" style={{ fontFamily: "'Archivo Black', sans-serif" }}>About</div>
      </div>
      <div className="flex-1 overflow-y-auto px-5 py-5 space-y-5">
        <p className="text-[13px] leading-7 text-[#121212]">
          MapMyTimes is an independent news platform committed to verified, unflinching journalism — reports, investigations, and storytelling that serves the public good.
        </p>
        <div className="space-y-3">
          {[
            { I: Mail, h: "Contact Newsroom", s: "newsroom@mapmytimes.com" },
            { I: Flame, h: "Join as Journalist", s: "Submit portfolio + 3 samples" },
            { I: Phone, h: "Phone", s: "+91 9893989395" },
          ].map((r, i) => (
            <div key={i} className="p-4 flex items-start gap-3" style={{ border: `2px solid ${INK}`, boxShadow: hardShadow(3), background: "white" }}>
              <div className="w-9 h-9 flex items-center justify-center shrink-0" style={{ backgroundColor: NEWS, color: "white", border: `2px solid ${INK}` }}>
                <r.I className="w-4 h-4" />
              </div>
              <div>
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
   CONTACT
   ========================================================= */

function ContactMock() {
  return (
    <div className="flex flex-col h-full bg-[#FFFBF8]">
      <div className="sticky top-0 z-10 bg-white px-5 py-3" style={{ borderBottom: `2px solid ${INK}` }}>
        <div className="text-[20px] font-black" style={{ fontFamily: "'Archivo Black', sans-serif" }}>Contact</div>
      </div>
      <div className="flex-1 overflow-y-auto px-5 py-5 space-y-5">
        <Card style={{ padding: 14 }}>
          <div className="space-y-1.5">
            {[Mail, Phone, MapPin].map((I, i) => (
              <div key={i} className="flex items-center gap-3 text-[12px] font-bold">
                <I className="w-4 h-4" style={{ color: NEWS }} />
                <span>{["admin@mapmytimes.com", "+91 9893989395", "MP Nagar, Bhopal · 462011"][i]}</span>
              </div>
            ))}
          </div>
        </Card>

        <div className="space-y-3.5">
          {["Name", "Email", "Message"].map((l, i) => (
            <div key={l}>
              <div className="text-[10.5px] font-black tracking-[1.4px] uppercase mb-1.5" style={{ color: NEWS }}>{l}</div>
              <textarea
                rows={i === 2 ? 4 : 1}
                readOnly
                placeholder={l === "Email" ? "you@domain.com" : `Your ${l.toLowerCase()}`}
                className="w-full resize-none px-3 py-2.5 text-[12.5px] font-semibold outline-none"
                style={{ border: `2px solid ${INK}`, background: "white", minHeight: i === 2 ? 110 : 40 }}
              />
            </div>
          ))}
          <button
            className="w-full py-3 text-[11.5px] font-black uppercase tracking-[2px] text-white"
            style={{ backgroundColor: NEWS, border: `2px solid ${INK}`, boxShadow: hardShadow(4) }}
          >
            Send Message →
          </button>
        </div>
      </div>
    </div>
  );
}

/* =========================================================
   CAREERS LIST
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
      <div className="sticky top-0 z-10 bg-white px-5 py-3" style={{ borderBottom: `2px solid ${INK}` }}>
        <div className="text-[20px] font-black" style={{ fontFamily: "'Archivo Black', sans-serif" }}>Careers</div>
      </div>
      <div className="flex-1 overflow-y-auto px-5 py-5 space-y-4">
        {jobs.map((j) => (
          <Card key={j.id} style={{ padding: 14 }}>
            <div className="flex flex-wrap gap-1.5">
              {[j.type, ...(j.loc.startsWith("Remote") ? ["REMOTE"] : []), j.level].map((x) => (
                <div key={x} className="px-2 py-1 text-[8.5px] font-black tracking-widest uppercase" style={{ border: `2px solid ${INK}`, background: x === j.type ? NEWS : "white", color: x === j.type ? "white" : INK }}>
                  {x}
                </div>
              ))}
            </div>
            <div className="mt-3 text-[19px] leading-tight font-black" style={{ fontFamily: "'Archivo Black', sans-serif" }}>{j.t}</div>
            <div className="mt-2 text-[11.5px] font-bold text-[#525252]">{j.dept} · {j.loc}</div>
            <p className="mt-3 text-[12px] leading-6 line-clamp-4 text-[#242424]">{j.d}</p>
            <button className="mt-4 w-full py-2.5 text-[11px] font-black uppercase tracking-[2px] text-white" style={{ backgroundColor: NEWS, border: `2px solid ${INK}`, boxShadow: hardShadow(3) }}>
              Apply Now
            </button>
          </Card>
        ))}
      </div>
    </div>
  );
}

/* =========================================================
   CAREER DETAIL
   ========================================================= */

function CareerDetailMock() {
  return (
    <div className="flex flex-col h-full bg-[#FFFBF8]">
      <div className="sticky top-0 z-10 bg-white px-5 py-3" style={{ borderBottom: `2px solid ${INK}` }}>
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 flex items-center justify-center" style={{ border: `2px solid ${INK}` }}>
            <ChevronRight className="w-4 h-4 -rotate-180" />
          </div>
          <div className="text-[17px] font-black truncate" style={{ fontFamily: "'Archivo Black', sans-serif" }}>Product Engineer — Flutter</div>
        </div>
      </div>
      <div className="flex-1 overflow-y-auto px-5 py-5 space-y-5">
        <div className="text-[10.5px] font-black tracking-[1.6px] uppercase" style={{ color: NEWS }}>Engineering · Open role</div>
        <div className="text-[26px] leading-tight font-black" style={{ fontFamily: "'Archivo Black', sans-serif", color: INK }}>
          Product Engineer — Flutter
        </div>
        <div className="flex flex-wrap gap-2">
          {["Full-time", "REMOTE", "3–6 yrs", "Remote · India"].map((x, i) => (
            <div key={x} className="px-2.5 py-1.5 text-[9.5px] font-black tracking-widest uppercase" style={{ border: `2px solid ${INK}`, backgroundColor: i === 0 ? NEWS : "white", color: i === 0 ? "white" : INK }}>
              {x}
            </div>
          ))}
        </div>
        <Card style={{ padding: 14 }}>
          <p className="text-[12.5px] leading-7 text-[#121212]">
            Build the MapMyTimes iOS/Android companion app with Riverpod, Dio and go_router. Drive the design system port end-to-end — neo-brutalist tokens, Archivo Black headlines, hard 4×4 ink shadows — integrate blog + auth + notification microservices with the API gateway. Work with the web team to ship parity features across home, news list, videos, shorts, article, search and journalist dashboard screens.
          </p>
        </Card>
        <button className="w-full py-3 text-[11.5px] font-black uppercase tracking-[2px] text-white" style={{ backgroundColor: NEWS, border: `2px solid ${INK}`, boxShadow: hardShadow(4) }}>
          Apply Now
        </button>
      </div>
    </div>
  );
}

/* =========================================================
   LOGIN (FULLSCREEN DIALOG)
   ========================================================= */

function LoginMock() {
  return (
    <div className="flex flex-col h-full bg-[#FFFBF8]">
      <div className="sticky top-0 z-10 bg-white px-5 py-3 flex items-center gap-2" style={{ borderBottom: `2px solid ${INK}` }}>
        <div className="w-8 h-8 flex items-center justify-center ml-auto" style={{ border: `2px solid ${INK}` }}>
          <span className="text-[12px] font-bold">×</span>
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

        <div className="space-y-3.5">
          {[
            { l: "Email", ph: "you@mapmytimes.com", k: "email" },
            { l: "Password", ph: "••••••••", k: "pass" },
          ].map((f) => (
            <div key={f.k}>
              <div className="text-[10.5px] font-black tracking-[1.4px] uppercase mb-1.5" style={{ color: NEWS }}>{f.l}</div>
              <input
                readOnly
                placeholder={f.ph}
                className="w-full px-3 py-2.5 text-[12.5px] font-semibold outline-none"
                style={{ border: `2px solid ${INK}`, background: "white" }}
              />
            </div>
          ))}
          <div className="text-right text-[11.5px] font-bold underline text-[#242424]">Forgot password?</div>
        </div>

        <button
          className="w-full py-3 text-[11.5px] font-black uppercase tracking-[2px] text-white"
          style={{ backgroundColor: NEWS, border: `2px solid ${INK}`, boxShadow: hardShadow(4) }}
        >
          Sign In
        </button>

        <div className="flex items-center gap-3">
          <div className="flex-1 h-px" style={{ background: DIVIDER }} />
          <div className="text-[10.5px] font-black tracking-[1.6px] uppercase text-[#525252]">or</div>
          <div className="flex-1 h-px" style={{ background: DIVIDER }} />
        </div>

        <div className="space-y-2.5">
          {[
            { I: Search, l: "Continue with Google" },
            { I: Facebook, l: "Continue with Facebook", blue: true },
          ].map((b, i) => (
            <button
              key={i}
              className="w-full py-2.5 text-[10.5px] font-black uppercase tracking-[1.4px] flex items-center justify-center gap-2.5"
              style={{ border: `2px solid ${INK}`, boxShadow: hardShadow(3), background: "white" }}
            >
              <b.I className="w-3.5 h-3.5" style={{ color: b.blue ? "#1877F2" : INK }} />
              {b.l}
            </button>
          ))}
        </div>

        <div className="pt-1 space-y-2 text-center">
          <div className="text-[12px] font-bold text-[#525252]">Don&apos;t have an account yet?</div>
          <button
            className="w-full py-2.5 text-[11px] font-black uppercase tracking-[2px]"
            style={{ border: `2px solid ${INK}`, boxShadow: hardShadow(3), background: "white" }}
          >
            Join MapMyTimes
          </button>
        </div>
      </div>
    </div>
  );
}

/* =========================================================
   DASHBOARD (Journalist)
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
      <div className="sticky top-0 z-10 bg-white px-5 py-3" style={{ borderBottom: `2px solid ${INK}` }}>
        <div className="text-[20px] font-black" style={{ fontFamily: "'Archivo Black', sans-serif" }}>Dashboard</div>
      </div>
      <div className="flex-1 overflow-y-auto px-5 py-5 space-y-5">
        <div className="p-5" style={{ backgroundColor: NEWS, color: "white", border: `2px solid ${INK}`, boxShadow: hardShadow(5) }}>
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

        <div className="grid grid-cols-2 gap-3.5">
          {tiles.map((t, i) => (
            <div key={i} className="p-4 flex flex-col gap-3" style={{ border: `2px solid ${INK}`, boxShadow: hardShadow(3), background: "white", minHeight: 120 }}>
              <div className="w-9 h-9 flex items-center justify-center text-white" style={{ backgroundColor: t.c, border: `2px solid ${INK}` }}>
                <t.I className="w-4 h-4" />
              </div>
              <div className="mt-auto text-[11.5px] leading-tight font-black" style={{ fontFamily: "'Archivo Black', sans-serif" }}>
                {t.l}
              </div>
              <div className="text-[10px] font-bold text-[#7A7A7A] uppercase tracking-wider">
                {["Start a new draft", "14 published", "3 pending", "2 new", "Preferences"][i]}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
