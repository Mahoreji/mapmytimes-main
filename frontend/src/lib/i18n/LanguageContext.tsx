"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { en } from "./en";
import { hi } from "./hi";
import type { Dict, LanguageCode } from "./types";

const STORAGE_KEY = "mmt.lang";

const dictionaries: Record<LanguageCode, Dict> = {
  en,
  hi,
};

function detectInitialLanguage(): LanguageCode {
  if (typeof window === "undefined") return "en";
  try {
    const saved = window.localStorage.getItem(STORAGE_KEY);
    if (saved === "en" || saved === "hi") return saved;
    const nav = window.navigator?.language?.toLowerCase() ?? "";
    if (nav.startsWith("hi")) return "hi";
    return "en";
  } catch {
    return "en";
  }
}

interface LanguageContextValue {
  lang: LanguageCode;
  setLang: (l: LanguageCode) => void;
  t: Dict;
}

const LanguageContext = createContext<LanguageContextValue | null>(null);

export function LanguageProvider({ children }: { children: ReactNode }) {
  const [lang, setLangState] = useState<LanguageCode>(() => detectInitialLanguage());

  useEffect(() => {
    const root = document.documentElement;
    if (!root) return;
    root.lang = lang === "hi" ? "hi-IN" : "en-IN";
    const existing = document.querySelector('meta[property="og:locale"]');
    if (existing) existing.setAttribute("content", lang === "hi" ? "hi_IN" : "en_IN");
    else {
      const meta = document.createElement("meta");
      meta.setAttribute("property", "og:locale");
      meta.setAttribute("content", lang === "hi" ? "hi_IN" : "en_IN");
      document.head.appendChild(meta);
    }
  }, [lang]);

  const setLang = useCallback((l: LanguageCode) => {
    setLangState(l);
    try {
      window.localStorage.setItem(STORAGE_KEY, l);
    } catch {
      /* noop */
    }
    try {
      window.dispatchEvent(new Event("langchange"));
    } catch {
      /* noop */
    }
  }, []);

  const value = useMemo<LanguageContextValue>(
    () => ({ lang, setLang, t: dictionaries[lang] }),
    [lang, setLang],
  );

  return (
    <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>
  );
}

export function useLanguage() {
  const ctx = useContext(LanguageContext);
  if (!ctx) {
    // Return fallback so SSR/static paths without provider don't crash.
    return {
      lang: "en" as LanguageCode,
      setLang: (_l: LanguageCode) => {},
      t: en,
    };
  }
  return ctx;
}

export function useDictionary() {
  return useLanguage().t;
}

export { dictionaries };
