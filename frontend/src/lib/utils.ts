import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export const SITE = {
  name: process.env.NEXT_PUBLIC_SITE_NAME ?? "MapMyTimes",
  url: process.env.NEXT_PUBLIC_SITE_URL ?? "https://mapmytimes.com",
  tagline: "JOURNALISM OF INTEGRITY",
  email: process.env.NEXT_PUBLIC_CONTACT_EMAIL ?? "admin@mapmytimes.com",
  phone: process.env.NEXT_PUBLIC_CONTACT_PHONE ?? "+91 80859 27274",
  apiBase:
    process.env.NEXT_PUBLIC_API_BASE_URL && process.env.NEXT_PUBLIC_API_BASE_URL.trim() !== ""
      ? process.env.NEXT_PUBLIC_API_BASE_URL
      : "",
  socials: {
    facebook: process.env.NEXT_PUBLIC_SOCIAL_FACEBOOK ?? "https://facebook.com/mapmytimes",
    twitter: process.env.NEXT_PUBLIC_SOCIAL_TWITTER ?? "https://x.com/mapmytimes",
    instagram: process.env.NEXT_PUBLIC_SOCIAL_INSTAGRAM ?? "https://instagram.com/mapmytimes",
    youtube: process.env.NEXT_PUBLIC_SOCIAL_YOUTUBE ?? "https://youtube.com/@mapmytimes",
    linkedin: process.env.NEXT_PUBLIC_SOCIAL_LINKEDIN ?? "https://linkedin.com/company/mapmytimes",
  },
};

export function formatDate(iso?: string, opts: Intl.DateTimeFormatOptions = {}) {
  if (!iso) return "";
  try {
    return new Date(iso).toLocaleDateString("en-IN", {
      day: "2-digit",
      month: "short",
      year: "numeric",
      ...opts,
    });
  } catch {
    return "";
  }
}

export function formatDateTime(iso?: string) {
  return formatDate(iso, { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" });
}

export function formatRelative(iso?: string) {
  if (!iso) return "";
  const now = Date.now();
  const then = new Date(iso).getTime();
  const diff = Math.max(0, now - then);
  const m = Math.floor(diff / 60000);
  if (m < 1) return "just now";
  if (m < 60) return `${m}m ago`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}h ago`;
  const d = Math.floor(h / 24);
  if (d < 30) return `${d}d ago`;
  return formatDate(iso);
}

export function readingTimeLabel(minutes?: number) {
  if (!minutes) return "";
  return `${minutes} min read`;
}

export function slugify(text: string) {
  return text
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9\s-]/g, "")
    .replace(/\s+/g, "-")
    .replace(/-+/g, "-");
}

export function truncate(str: string, max = 160) {
  if (!str || str.length <= max) return str;
  return str.slice(0, max - 1).trimEnd() + "\u2026";
}

export function initials(a = "", b = "") {
  return `${(a || "").trim().charAt(0).toUpperCase()}${(b || "").trim().charAt(0).toUpperCase()}`;
}

let compactFormatter: Intl.NumberFormat | null = null;
export function formatCount(n: number | null | undefined): string {
  if (n === undefined || n === null) return "0";
  const num = Number(n) || 0;
  if (num < 1000) return String(Math.floor(num));
  if (!compactFormatter) {
    try {
      compactFormatter = new Intl.NumberFormat("en", { notation: "compact", maximumFractionDigits: 1 });
    } catch {
      compactFormatter = null;
    }
  }
  if (compactFormatter) return compactFormatter.format(num);
  if (num < 1000000) return `${(num / 1000).toFixed(1).replace(/\.0$/, "")}K`;
  return `${(num / 1000000).toFixed(1).replace(/\.0$/, "")}M`;
}
