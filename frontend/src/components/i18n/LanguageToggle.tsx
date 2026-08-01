"use client";

import { useLanguage } from "@/lib/i18n/LanguageContext";
import type { LanguageCode } from "@/lib/i18n/types";
import { cn } from "@/lib/utils";

interface Props {
  className?: string;
  size?: "sm" | "md";
  variant?: "light" | "dark";
}

export function LanguageToggle({ className, size = "sm", variant = "dark" }: Props) {
  const { lang, setLang, t } = useLanguage();

  const options: { value: LanguageCode; label: string }[] = [
    { value: "en", label: t.header.languageShort.en },
    { value: "hi", label: t.header.languageShort.hi },
  ];

  const outer = cn(
    "inline-flex items-center border-2 border-ink-950 shadow-hard-sm overflow-hidden",
    size === "sm" ? "h-8" : "h-10",
    variant === "dark" ? "bg-ink-950" : "bg-white",
    className,
  );

  return (
    <div
      className={outer}
      role="group"
      aria-label={t.header.language}
    >
      {options.map((o) => {
        const active = lang === o.value;
        return (
          <button
            key={o.value}
            type="button"
            onClick={() => setLang(o.value)}
            aria-pressed={active}
            className={cn(
              "select-none font-headline font-bold tracking-wider transition-colors",
              size === "sm" ? "h-8 px-2 sm:px-2.5 text-[10px] sm:text-[11px]" : "h-10 px-3 text-xs",
              active
                ? variant === "dark"
                  ? "bg-news text-white"
                  : "bg-ink-950 text-white"
                : variant === "dark"
                  ? "bg-ink-950 text-white/80 hover:text-white hover:bg-ink-800"
                  : "bg-white text-ink-950 hover:bg-ink-50",
              o.value === "hi" ? (variant === "dark" ? "border-l border-white/15" : "border-l border-ink-950") : "",
            )}
          >
            {o.label}
          </button>
        );
      })}
    </div>
  );
}
