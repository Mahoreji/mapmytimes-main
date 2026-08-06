"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/Button";
import {
  User,
  LogOut,
  Settings,
  Bookmark,
  Mail,
  ShieldCheck,
  Globe2,
  Sun,
  Moon,
  Sparkles,
  LogIn,
  UserPlus,
  ArrowLeft,
} from "lucide-react";
import { useAuth } from "@/lib/auth/AuthProvider";
import { SITE, cn, initials } from "@/lib/utils";
import { avatarOrDefault } from "@/lib/assets";

type ThemePref = "light" | "dark" | "system";
type LangPref = "en" | "hi";

const THEME_KEY = "mmt:theme";
const LANG_KEY = "mmt:lang";

function readPref<K extends string>(key: string, fallback: K): K {
  if (typeof window === "undefined") return fallback;
  try {
    const raw = window.localStorage.getItem(key);
    if (!raw) return fallback;
    return raw as K;
  } catch {
    return fallback;
  }
}

export default function ProfilePage() {
  const auth = useAuth();
  const [theme, setTheme] = useState<ThemePref>("system");
  const [lang, setLang] = useState<LangPref>("en");
  const [hydrated, setHydrated] = useState(false);
  const [savedCount, setSavedCount] = useState(0);

  useEffect(() => {
    setTheme(readPref<ThemePref>(THEME_KEY, "system"));
    setLang(readPref<LangPref>(LANG_KEY, "en"));
    setHydrated(true);
    try {
      const raw = window.localStorage.getItem("mmt:saved-articles");
      if (raw) setSavedCount((JSON.parse(raw) ?? []).length || 0);
    } catch {
      /* ignore */
    }
  }, []);

  const applyTheme = (t: ThemePref) => {
    setTheme(t);
    try {
      window.localStorage.setItem(THEME_KEY, t);
    } catch {
      /* ignore */
    }
    const root = document.documentElement;
    const effective =
      t === "system"
        ? window.matchMedia("(prefers-color-scheme: dark)").matches
          ? "dark"
          : "light"
        : t;
    root.classList.toggle("dark", effective === "dark");
  };

  const applyLang = (l: LangPref) => {
    setLang(l);
    try {
      window.localStorage.setItem(LANG_KEY, l);
    } catch {
      /* ignore */
    }
    document.documentElement.lang = l;
  };

  const user = auth.user;
  const loggedIn = !!user && auth.status === "authenticated";

  return (
    <div>
      <section className="bg-ink-950 text-white border-b-4 border-news">
        <div className="mx-auto max-w-5xl px-4 py-10 sm:py-14">
          <Link
            href="/"
            className="inline-flex items-center gap-2 text-xs font-bold uppercase tracking-widest text-white/70 hover:text-white"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to today&apos;s stories
          </Link>
          <div className="mt-6">
            <div className="ribbon text-xs mb-3">Profile</div>
            <h1 className="font-headline text-4xl sm:text-6xl uppercase leading-none">
              Your MapMyTimes.
            </h1>
            <p className="mt-4 max-w-2xl text-white/80 text-lg leading-relaxed">
              One place for your account, saved stories, reading preferences, and language settings.
            </p>
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-5xl px-4 py-10 sm:py-14 grid grid-cols-1 lg:grid-cols-5 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <div className="relative border-4 border-ink-950 overflow-hidden">
            <div className="absolute inset-0 bg-gradient-to-br from-news via-red-700 to-red-950" />
            <div className="relative p-6 text-white">
              <div className="flex items-center gap-4">
                <div className="h-20 w-20 flex-shrink-0 border-4 border-white bg-white text-ink-950 inline-flex items-center justify-center font-headline text-2xl uppercase overflow-hidden">
                  {loggedIn && user ? (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img
                      src={avatarOrDefault(user.avatarUrl)}
                      alt=""
                      className="h-full w-full object-cover"
                    />
                  ) : user ? (
                    <span>{initials([user.firstName, user.lastName].filter(Boolean).join(" ") || user.email || "Guest Reader")}</span>
                  ) : (
                    <User className="h-9 w-9" />
                  )}
                </div>
                <div>
                  <div className="text-xs uppercase tracking-widest text-white/80 font-bold">
                    {loggedIn ? "Reader account" : "Signed out"}
                  </div>
                  <div className="font-headline uppercase text-2xl leading-tight mt-1">
                    {loggedIn && user
                      ? [user.firstName, user.lastName].filter(Boolean).join(" ") || user.email
                      : "Guest Reader"}
                  </div>
                  {loggedIn && user?.email ? (
                    <div className="mt-1 text-sm text-white/85 inline-flex items-center gap-1.5">
                      <Mail className="h-3.5 w-3.5" />
                      {user.email}
                    </div>
                  ) : (
                    <div className="mt-1 text-sm text-white/85 inline-flex items-center gap-1.5">
                      <ShieldCheck className="h-3.5 w-3.5" />
                      Preferences saved only on this device.
                    </div>
                  )}
                </div>
              </div>
              <div className="mt-6 grid grid-cols-3 gap-3 text-center">
                <div className="bg-white/10 border border-white/15 px-3 py-3 backdrop-blur">
                  <div className="font-headline text-2xl uppercase leading-none">
                    {savedCount}
                  </div>
                  <div className="text-[10px] uppercase tracking-widest text-white/80 mt-1.5">
                    Saved
                  </div>
                </div>
                <div className="bg-white/10 border border-white/15 px-3 py-3 backdrop-blur">
                  <div className="font-headline text-2xl uppercase leading-none">
                    {loggedIn ? "1" : "0"}
                  </div>
                  <div className="text-[10px] uppercase tracking-widest text-white/80 mt-1.5">
                    Session
                  </div>
                </div>
                <div className="bg-white/10 border border-white/15 px-3 py-3 backdrop-blur">
                  <div className="font-headline text-2xl uppercase leading-none">
                    {lang === "hi" ? "हिं" : "EN"}
                  </div>
                  <div className="text-[10px] uppercase tracking-widest text-white/80 mt-1.5">
                    Language
                  </div>
                </div>
              </div>
              <div className="mt-6 flex flex-wrap gap-2">
                {loggedIn ? (
                  <>
                    <Link href="/dashboard/settings">
                      <Button variant="ink" size="sm" className="bg-white text-ink-950 hover:bg-ink-950 hover:text-white border-white">
                        <Settings className="h-4 w-4 mr-1.5" />
                        Full settings
                      </Button>
                    </Link>
                    <Button
                      variant="outline"
                      size="sm"
                      className="bg-transparent border-white text-white hover:bg-white hover:text-ink-950"
                      onClick={() => auth.logout()}
                    >
                      <LogOut className="h-4 w-4 mr-1.5" />
                      Sign out
                    </Button>
                  </>
                ) : (
                  <>
                    <Link href="/login">
                      <Button variant="ink" size="sm" className="bg-white text-ink-950 hover:bg-ink-950 hover:text-white border-white">
                        <LogIn className="h-4 w-4 mr-1.5" />
                        Sign in
                      </Button>
                    </Link>
                    <Link href="/signup">
                      <Button
                        variant="outline"
                        size="sm"
                        className="bg-transparent border-white text-white hover:bg-white hover:text-ink-950"
                      >
                        <UserPlus className="h-4 w-4 mr-1.5" />
                        Create account
                      </Button>
                    </Link>
                  </>
                )}
              </div>
            </div>
          </div>

          <div className="border-2 border-ink-950 bg-white p-5 space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Bookmark className="h-5 w-5 text-news" />
                <div className="font-headline uppercase text-lg">Saved articles</div>
              </div>
              <span className="text-xs font-bold uppercase tracking-widest text-ink-600">
                {savedCount} items
              </span>
            </div>
            <p className="text-sm text-ink-700">
              Stories you bookmarked in this browser. Works offline — no account required.
            </p>
            <Link href="/saved">
              <Button variant="outline" size="sm" className="w-full justify-center">
                <Bookmark className="h-4 w-4 mr-1.5" />
                Open reading list
              </Button>
            </Link>
          </div>
        </div>

        <div className="lg:col-span-3 space-y-6">
          <div className="border-2 border-ink-950 bg-white">
            <div className="px-5 py-4 border-b-2 border-ink-950 bg-ink-50 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Globe2 className="h-5 w-5 text-news" />
                <div className="font-headline uppercase tracking-tight">Language</div>
              </div>
              {hydrated ? (
                <span className="text-xs font-bold uppercase tracking-widest text-ink-600">
                  Current: {lang === "hi" ? "हिंदी" : "English"}
                </span>
              ) : null}
            </div>
            <div className="p-5 grid grid-cols-1 sm:grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => applyLang("en")}
                className={cn(
                  "text-left border-2 p-4 hover:shadow-hard-sm transition-shadow",
                  lang === "en" && hydrated
                    ? "border-news bg-news/5 ring-2 ring-news/30"
                    : "border-ink-950 bg-white",
                )}
              >
                <div className="font-headline uppercase text-lg">English</div>
                <div className="text-xs text-ink-600 mt-1">Default newsroom language</div>
              </button>
              <button
                type="button"
                onClick={() => applyLang("hi")}
                className={cn(
                  "text-left border-2 p-4 hover:shadow-hard-sm transition-shadow",
                  lang === "hi" && hydrated
                    ? "border-news bg-news/5 ring-2 ring-news/30"
                    : "border-ink-950 bg-white",
                )}
              >
                <div className="font-headline uppercase text-lg">हिंदी</div>
                <div className="text-xs text-ink-600 mt-1">हिंदी में समाचार पढ़ें</div>
              </button>
            </div>
          </div>

          <div className="border-2 border-ink-950 bg-white">
            <div className="px-5 py-4 border-b-2 border-ink-950 bg-ink-50 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Sparkles className="h-5 w-5 text-news" />
                <div className="font-headline uppercase tracking-tight">Theme</div>
              </div>
              {hydrated ? (
                <span className="text-xs font-bold uppercase tracking-widest text-ink-600">
                  Current: {theme}
                </span>
              ) : null}
            </div>
            <div className="p-5 grid grid-cols-1 sm:grid-cols-3 gap-3">
              {(
                [
                  { k: "light", label: "Light", icon: <Sun className="h-4 w-4" /> },
                  { k: "dark", label: "Dark", icon: <Moon className="h-4 w-4" /> },
                  { k: "system", label: "System", icon: <Sparkles className="h-4 w-4" /> },
                ] as const
              ).map((opt) => (
                <button
                  key={opt.k}
                  type="button"
                  onClick={() => applyTheme(opt.k)}
                  className={cn(
                    "flex items-center gap-2 border-2 px-4 py-3 hover:shadow-hard-sm transition-shadow",
                    theme === opt.k && hydrated
                      ? "border-news bg-news/5 ring-2 ring-news/30"
                      : "border-ink-950 bg-white",
                  )}
                >
                  {opt.icon}
                  <span className="font-headline uppercase">{opt.label}</span>
                </button>
              ))}
            </div>
          </div>

          <div className="border-2 border-ink-950 bg-white p-5">
            <div className="flex items-start justify-between gap-4 flex-wrap">
              <div>
                <div className="flex items-center gap-2">
                  <ShieldCheck className="h-5 w-5 text-news" />
                  <div className="font-headline uppercase tracking-tight">Account & privacy</div>
                </div>
                <p className="mt-2 text-sm text-ink-700 max-w-xl">
                  Change your password, manage sessions, update notification preferences, or download
                  a copy of your data.
                </p>
              </div>
              <div className="flex flex-wrap gap-2">
                {loggedIn ? (
                  <Link href="/dashboard/settings">
                    <Button variant="news" size="sm">
                      <Settings className="h-4 w-4 mr-1.5" />
                      Open full settings
                    </Button>
                  </Link>
                ) : (
                  <Link href="/login">
                    <Button variant="news" size="sm">
                      <LogIn className="h-4 w-4 mr-1.5" />
                      Sign in to unlock
                    </Button>
                  </Link>
                )}
                <Link href="/contact">
                  <Button variant="outline" size="sm">
                    <Mail className="h-4 w-4 mr-1.5" />
                    Contact support
                  </Button>
                </Link>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
