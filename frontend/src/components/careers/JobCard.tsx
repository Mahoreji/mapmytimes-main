"use client";

import Link from "next/link";
import {
  MapPin,
  Briefcase,
  TrendingUp,
  Banknote,
  Calendar,
  ChevronRight,
} from "lucide-react";
import type {
  ApplicationStatus,
  JobPostingSummaryResponse,
} from "@/types/careers";
import {
  APPLICATION_STATUS_META,
  EXPERIENCE_LABELS,
  JOB_TYPE_LABELS,
} from "@/types/careers";
import { cn } from "@/lib/utils";

export function SectionTitle({
  eyebrow,
  title,
  action,
  className,
}: {
  eyebrow?: string;
  title: string;
  action?: React.ReactNode;
  className?: string;
}) {
  return (
    <div
      className={cn(
        "flex flex-wrap items-end justify-between gap-4 pb-4 border-b-2 border-ink-950/20",
        className,
      )}
    >
      <div>
        {eyebrow ? (
          <div className="ribbon text-xs mb-2 inline-block">{eyebrow}</div>
        ) : null}
        <h2 className="font-headline text-2xl sm:text-3xl uppercase leading-none tracking-wide">
          {title}
        </h2>
      </div>
      {action}
    </div>
  );
}

export function JobCard({ job }: { job: JobPostingSummaryResponse }) {
  const hasSalary =
    job.salaryMin !== undefined &&
    job.salaryMin !== null &&
    (job.salaryMax !== undefined && job.salaryMax !== null || job.salaryMin > 0);

  return (
    <Link
      href={`/careers/${job.id}`}
      className="group block border-2 border-ink-950 bg-white shadow-hard-sm hover:shadow-hard-md transition-shadow p-5 sm:p-6 space-y-4 focus:outline-none focus:ring-2 focus:ring-news focus:ring-offset-2"
    >
      <div className="flex items-start justify-between gap-4">
        <div className="space-y-1.5 min-w-0">
          <h3 className="font-headline text-xl uppercase leading-tight group-hover:text-news transition-colors">
            {job.title}
          </h3>
          <div className="flex flex-wrap items-center gap-x-3 gap-y-1.5 text-xs">
            <span className="inline-flex items-center gap-1.5 text-ink-700 font-semibold">
              <Briefcase className="h-3.5 w-3.5" />
              {job.department}
            </span>
            <span className="inline-flex items-center gap-1.5 text-ink-700 font-semibold">
              <MapPin className="h-3.5 w-3.5" />
              {job.location}
            </span>
          </div>
        </div>
        <ChevronRight className="h-5 w-5 text-ink-700 group-hover:translate-x-1 group-hover:text-news transition shrink-0 mt-0.5" />
      </div>

      <div className="flex flex-wrap gap-2">
        <span className="badge">
          {JOB_TYPE_LABELS[job.jobType] ?? job.jobType}
        </span>
        <span className="badge badge-outline">
          <TrendingUp className="h-3 w-3" />
          {EXPERIENCE_LABELS[job.experienceLevel] ?? job.experienceLevel}
        </span>
        {hasSalary ? (
          <span className="badge badge-news">
            <Banknote className="h-3 w-3" />
            {formatSalary(job.salaryMin!, job.salaryMax ?? null, job.salaryCurrency)}
          </span>
        ) : null}
      </div>

      <div className="pt-2 border-t border-ink-950/10 flex items-center justify-between text-[11px] font-bold uppercase tracking-widest text-ink-600">
        {job.applicationDeadline ? (
          <span className="inline-flex items-center gap-1.5">
            <Calendar className="h-3.5 w-3.5" />
            Apply by {formatDate(job.applicationDeadline)}
          </span>
        ) : (
          <span className="inline-flex items-center gap-1.5">
            <Calendar className="h-3.5 w-3.5" />
            Rolling applications
          </span>
        )}
        <span className="group-hover:text-news">View posting →</span>
      </div>
    </Link>
  );
}

export function StatusPill({ status }: { status: ApplicationStatus }) {
  const meta = APPLICATION_STATUS_META[status];
  return (
    <span
      className={cn(
        "inline-flex items-center h-7 px-3 text-[10px] font-bold uppercase tracking-[0.2em] border-2 border-ink-950",
        meta.tone === "news" && "bg-news text-white",
        meta.tone === "ink" && "bg-ink-950 text-white",
        meta.tone === "green" && "bg-emerald-500 text-white",
        meta.tone === "amber" && "bg-amber-400 text-ink-950",
        meta.tone === "red" && "bg-rose-500 text-white",
        meta.tone === "blue" && "bg-sky-500 text-white",
        meta.tone === "default" && "bg-white text-ink-950",
      )}
    >
      {meta.label}
    </span>
  );
}

function formatSalary(min: number, max: number | null, currency?: string | null) {
  const c = currency?.toUpperCase() ?? "INR";
  const sym = c === "INR" ? "₹" : c === "USD" ? "$" : `${c} `;
  const fmt = (n: number) =>
    n >= 1_000_000
      ? `${(n / 1_000_000).toFixed(n % 1_000_000 === 0 ? 0 : 1)}M`
      : n >= 1_000
        ? `${(n / 1_000).toFixed(n % 1_000 === 0 ? 0 : 1)}K`
        : String(n);
  if (max && max !== min) return `${sym}${fmt(min)} – ${sym}${fmt(max)}`;
  return `From ${sym}${fmt(min)}`;
}

function formatDate(iso: string) {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleDateString(undefined, {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
}
