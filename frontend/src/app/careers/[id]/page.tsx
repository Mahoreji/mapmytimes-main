"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { careersApi } from "@/lib/api/careersApi";
import { useAuth } from "@/lib/auth/AuthProvider";
import type { JobPostingResponse } from "@/types/careers";
import { EXPERIENCE_LABELS, JOB_TYPE_LABELS } from "@/types/careers";
import { Button } from "@/components/ui/Button";
import {
  MapPin,
  Briefcase,
  TrendingUp,
  Banknote,
  Calendar,
  Users,
  Clock,
  ArrowRight,
  FileText,
  ArrowLeft,
  AlertTriangle,
} from "lucide-react";
import { cn } from "@/lib/utils";

export default async function JobDetailPage({
  params: paramsPromise,
}: {
  params: Promise<{ id: string }>;
}) {
  const params = await paramsPromise;
  const router = useRouter();
  const auth = useAuth();
  const [job, setJob] = useState<JobPostingResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);
    careersApi.jobs
      .get(params.id)
      .then((j) => {
        if (!active) return;
        setJob(j ?? null);
        setLoading(false);
      })
      .catch((e) => {
        if (!active) return;
        setError(e?.response?.data?.message || e?.message || "Job not found.");
        setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [params.id]);

  const { timeLeftLabel, closed } = useCountdown(job?.applicationDeadline ?? null);

  const hasSalary =
    (job?.salaryMin ?? 0) > 0 || (job?.salaryMax ?? 0) > 0;

  const deadlinePast = closed;
  const canApply = job?.isActive && !deadlinePast;

  const applyHref = `/careers/${params.id}/apply`;
  const applyLoginHref = `/login?redirect=${encodeURIComponent(applyHref)}`;

  if (loading) {
    return <JobDetailSkeleton />;
  }

  if (!job || error) {
    return (
      <main className="mx-auto max-w-4xl px-4 py-16 sm:py-20">
        <div className="border-2 border-ink-950 bg-white p-10 shadow-hard-sm text-center">
          <FileText className="h-12 w-12 text-news mx-auto mb-4" />
          <h1 className="font-headline text-3xl uppercase mb-2">
            {error || "Posting not found"}
          </h1>
          <p className="text-sm text-ink-700 mb-6">
            The job you&apos;re looking for may have been closed or never
            existed.
          </p>
          <Link href="/careers">
            <Button variant="news" size="lg">
              <ArrowLeft className="h-4 w-4" /> Back to all roles
            </Button>
          </Link>
        </div>
      </main>
    );
  }

  return (
    <main className="mx-auto max-w-6xl px-4 py-8 sm:py-10">
      <div className="mb-6">
        <Link
          href="/careers"
          className="inline-flex items-center gap-2 text-xs font-bold uppercase tracking-widest text-ink-700 hover:text-news"
        >
          <ArrowLeft className="h-3.5 w-3.5" /> All open roles
        </Link>
      </div>

      <section className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <div className="border-2 border-ink-950 bg-white shadow-hard-md p-6 sm:p-8">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div className="space-y-2.5 min-w-0">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="ribbon text-xs">{job.department}</span>
                  <span className="badge">{JOB_TYPE_LABELS[job.jobType] ?? job.jobType}</span>
                  <span className="badge badge-outline">
                    <TrendingUp className="h-3 w-3" />
                    {EXPERIENCE_LABELS[job.experienceLevel] ?? job.experienceLevel}
                  </span>
                  {!job.isActive ? (
                    <span className="badge badge-ink">Closed</span>
                  ) : deadlinePast ? (
                    <span className="badge badge-ink">Deadline passed</span>
                  ) : null}
                </div>
                <h1 className="font-headline text-3xl sm:text-4xl uppercase leading-[1.05]">
                  {job.title}
                </h1>
                <div className="flex flex-wrap items-center gap-x-4 gap-y-2 text-sm font-semibold text-ink-700">
                  <span className="inline-flex items-center gap-1.5">
                    <MapPin className="h-4 w-4" /> {job.location}
                  </span>
                  <span className="inline-flex items-center gap-1.5">
                    <Briefcase className="h-4 w-4" /> {job.department}
                  </span>
                  {hasSalary ? (
                    <span className="inline-flex items-center gap-1.5 text-ink-950">
                      <Banknote className="h-4 w-4" />
                      {formatSalary(job.salaryMin!, job.salaryMax ?? null, job.salaryCurrency)}
                    </span>
                  ) : null}
                </div>
              </div>
            </div>

            <div className="mt-6 grid grid-cols-2 sm:grid-cols-4 gap-3 border-t-2 border-ink-950/15 pt-5">
              <Meta icon={<Calendar className="h-4 w-4" />} label="Apply by">
                {job.applicationDeadline ? (
                  <span className={deadlinePast ? "text-news" : ""}>
                    {formatDate(job.applicationDeadline)}
                  </span>
                ) : (
                  "Rolling"
                )}
              </Meta>
              <Meta icon={<Clock className="h-4 w-4" />} label="Deadline">
                <span className={cn(deadlinePast && "text-news")}>
                  {timeLeftLabel}
                </span>
              </Meta>
              <Meta icon={<Users className="h-4 w-4" />} label="Applied">
                {job.totalApplications ?? "—"} candidates
              </Meta>
              <Meta icon={<FileText className="h-4 w-4" />} label="Posted">
                {job.createdAt ? formatDate(job.createdAt) : "Recent"}
              </Meta>
            </div>
          </div>

          {job.description ? (
            <RichBlock title="About the role" body={job.description} />
          ) : null}
          {job.responsibilities ? (
            <RichBlock title="What you'll do" body={job.responsibilities} list />
          ) : null}
          {job.requirements ? (
            <RichBlock title="What we're looking for" body={job.requirements} list />
          ) : null}
        </div>

        <aside className="lg:col-span-1 space-y-5">
          <div className="border-2 border-ink-950 bg-white shadow-hard-sm p-5 sm:p-6 sticky top-24">
            <div className="ribbon text-xs mb-2 inline-block">
              {canApply ? "Apply now" : "Not accepting"}
            </div>
            <h3 className="font-headline text-xl uppercase leading-tight mb-4">
              Ready to join?
            </h3>

            {canApply ? (
              <div className="space-y-3">
                <ol className="text-xs text-ink-700 font-semibold space-y-1.5 list-decimal list-inside">
                  <li>Check requirements on the left</li>
                  <li>Prepare PDF/DOC/DOCX resume (max 10 MB)</li>
                  <li>Sign in or create account to apply</li>
                </ol>
                {auth.user ? (
                  <Button
                    variant="news"
                    size="lg"
                    className="w-full"
                    onClick={() => router.push(applyHref)}
                  >
                    Apply to this role <ArrowRight className="h-4 w-4" />
                  </Button>
                ) : (
                  <div className="space-y-2">
                    <Link href={applyLoginHref} className="block">
                      <Button variant="news" size="lg" className="w-full">
                        Sign in to apply <ArrowRight className="h-4 w-4" />
                      </Button>
                    </Link>
                    <Link href={`/signup?redirect=${encodeURIComponent(applyHref)}`} className="block">
                      <Button variant="outline" size="sm" className="w-full">
                        Create account instead
                      </Button>
                    </Link>
                  </div>
                )}
              </div>
            ) : (
              <div className="space-y-3">
                <div className="border-2 border-amber-400 bg-amber-50 p-3 inline-flex items-start gap-2">
                  <AlertTriangle className="h-4 w-4 text-amber-700 shrink-0 mt-0.5" />
                  <p className="text-xs font-semibold text-amber-900 leading-snug">
                    {deadlinePast
                      ? "Application deadline has passed."
                      : "This posting is no longer active."}
                  </p>
                </div>
                <Link href="/careers">
                  <Button variant="outline" size="lg" className="w-full">
                    <ArrowLeft className="h-4 w-4" /> See other openings
                  </Button>
                </Link>
              </div>
            )}

            {auth.user ? (
              <div className="mt-5 pt-4 border-t border-ink-950/15 text-xs text-ink-700 font-semibold space-y-1">
                <p>Applying as</p>
                <p className="text-ink-950">
                  {(auth.user as any)?.firstName || ""}{" "}
                  {(auth.user as any)?.lastName || ""}
                </p>
                <p className="text-ink-600 text-[11px]">{(auth.user as any)?.email ?? ""}</p>
              </div>
            ) : null}
          </div>
        </aside>
      </section>
    </main>
  );
}

function Meta({
  icon,
  label,
  children,
}: {
  icon: React.ReactNode;
  label: string;
  children: React.ReactNode;
}) {
  return (
    <div>
      <div className="flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-widest text-ink-600 mb-1">
        {icon} {label}
      </div>
      <div className="text-sm font-semibold">{children}</div>
    </div>
  );
}

function RichBlock({
  title,
  body,
  list = false,
}: {
  title: string;
  body: string;
  list?: boolean;
}) {
  const items = useMemo(() => parseLines(body), [body]);
  return (
    <div className="border-2 border-ink-950 bg-white shadow-hard-sm p-6 sm:p-8">
      <div className="ribbon text-xs mb-3 inline-block">Role details</div>
      <h2 className="font-headline text-2xl uppercase mb-4 leading-tight">
        {title}
      </h2>
      {list ? (
        <ul className="space-y-2">
          {items.length === 1 ? (
            <RichContent html={items[0]?.text ?? ""} />
          ) : (
            items.map((it, i) => (
              <li key={i} className="flex items-start gap-3 text-sm text-ink-800 leading-relaxed">
                <span className="h-2 w-2 shrink-0 mt-2 bg-news border border-ink-950" />
                <span className="min-w-0">
                  {it.bullet ? (
                    <RichContent html={it.text} />
                  ) : (
                    <p className="font-semibold text-ink-950 mb-1">
                      <RichContent html={it.text} />
                    </p>
                  )}
                </span>
              </li>
            ))
          )}
        </ul>
      ) : (
        <div className="prose-mmt text-sm text-ink-800 leading-relaxed">
          {items.map((p, i) => (
            <RichContent key={i} html={p.text} />
          ))}
        </div>
      )}
    </div>
  );
}

function RichContent({ html }: { html: string }) {
  return (
    <div
      className="whitespace-pre-wrap break-words text-inherit"
      dangerouslySetInnerHTML={{ __html: String(html ?? "") }}
    />
  );
}

function parseLines(raw: string): { text: string; bullet: boolean }[] {
  if (!raw) return [];
  const asText = raw.replace(/\r\n/g, "\n");
  if (/<[a-z][\s\S]*>/i.test(asText)) {
    return [{ text: asText, bullet: false }];
  }
  return asText
    .split(/\n+/)
    .map((s) => s.trim())
    .filter(Boolean)
    .map((line) => ({
      text: line.replace(/^\s*[-•*+]\s*/, ""),
      bullet: /^\s*[-•*+]\s*/.test(line) || /^\d+\.\s+/.test(line),
    }));
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

function useCountdown(deadlineIso: string | null | undefined) {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 60_000);
    return () => clearInterval(id);
  }, []);
  return useMemo(() => {
    if (!deadlineIso) return { timeLeftLabel: "Open applications", closed: false };
    const dl = new Date(deadlineIso).getTime();
    if (Number.isNaN(dl)) return { timeLeftLabel: "—", closed: false };
    const diff = dl - now;
    if (diff <= 0) return { timeLeftLabel: "Deadline passed", closed: true };
    const day = 1000 * 60 * 60 * 24;
    const days = Math.floor(diff / day);
    const hours = Math.floor((diff % day) / (1000 * 60 * 60));
    if (days >= 60) return { timeLeftLabel: `${Math.floor(days / 30)}mo left`, closed: false };
    if (days >= 1) return { timeLeftLabel: `${days}d ${hours}h left`, closed: false };
    const mins = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    return { timeLeftLabel: `${hours}h ${mins}m left`, closed: false };
  }, [deadlineIso, now]);
}

function JobDetailSkeleton() {
  return (
    <main className="mx-auto max-w-6xl px-4 py-8 sm:py-10">
      <div className="h-4 w-28 bg-ink-900/15 mb-6" />
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <div className="animate-pulse border-2 border-ink-950 p-6 sm:p-8 space-y-5">
            <div className="space-y-3">
              <div className="h-4 w-40 bg-ink-900/15" />
              <div className="h-10 w-3/4 bg-ink-900/20" />
              <div className="h-4 w-1/2 bg-ink-900/15" />
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 border-t pt-5">
              {[0, 1, 2, 3].map((i) => (
                <div key={i} className="space-y-2">
                  <div className="h-3 w-16 bg-ink-900/10" />
                  <div className="h-4 w-24 bg-ink-900/15" />
                </div>
              ))}
            </div>
          </div>
          <div className="animate-pulse border-2 border-ink-950 p-6 sm:p-8 space-y-3">
            <div className="h-6 w-1/3 bg-ink-900/15" />
            <div className="h-4 w-full bg-ink-900/10" />
            <div className="h-4 w-5/6 bg-ink-900/10" />
            <div className="h-4 w-4/6 bg-ink-900/10" />
          </div>
        </div>
        <aside className="lg:col-span-1">
          <div className="animate-pulse border-2 border-ink-950 p-5 sm:p-6 space-y-4 h-56" />
        </aside>
      </div>
    </main>
  );
}
