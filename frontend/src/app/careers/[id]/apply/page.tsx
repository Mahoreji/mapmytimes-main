"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { careersApi } from "@/lib/api/careersApi";
import { getApiError } from "@/lib/api/client";
import { useAuth } from "@/lib/auth/AuthProvider";
import type { JobApplicationResponse, JobPostingResponse } from "@/types/careers";
import { Button } from "@/components/ui/Button";
import { Input, Textarea } from "@/components/ui/Input";
import { Card, PageHeader } from "@/components/dashboard/Panels";
import {
  ArrowLeft,
  Upload,
  FileUp,
  Send,
  CheckCircle2,
  AlertTriangle,
  ExternalLink,
  Loader2,
  FileText,
} from "lucide-react";
import { cn } from "@/lib/utils";

const ACCEPT_RESUME = ".pdf,.doc,.docx";
const ACCEPT_MIME = new Set([
  "application/pdf",
  "application/msword",
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
]);

export default function ApplyToJobPage({
  params,
}: {
  params: { id: string };
}) {
  const router = useRouter();
  const auth = useAuth();

  const [job, setJob] = useState<JobPostingResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [fetchErr, setFetchErr] = useState<string | null>(null);

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [coverLetter, setCoverLetter] = useState("");
  const [currentCtc, setCurrentCtc] = useState("");
  const [expectedCtc, setExpectedCtc] = useState("");
  const [noticePeriod, setNoticePeriod] = useState("");
  const [yearsOfExperience, setYearsOfExperience] = useState<string>("");
  const [resume, setResume] = useState<File | null>(null);

  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState<JobApplicationResponse | null>(null);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => {
    if (!auth.user) return;
    const u: any = auth.user;
    if (u?.firstName || u?.lastName) {
      setName((prev) => prev || `${u.firstName ?? ""} ${u.lastName ?? ""}`.trim());
    }
    if (u?.email) setEmail((prev) => prev || u.email);
    if (u?.phone) setPhone((prev) => prev || u.phone);
    if (u?.mobile) setPhone((prev) => prev || u.mobile);
  }, [auth.user]);

  useEffect(() => {
    let active = true;
    setLoading(true);
    careersApi.jobs
      .get(params.id)
      .then((j) => {
        if (!active) return;
        setJob(j ?? null);
        setLoading(false);
      })
      .catch((e) => {
        if (!active) return;
        setFetchErr(e?.response?.data?.message || e?.message || "Posting unavailable.");
        setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [params.id]);

  const onResume = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0] ?? null;
    if (!f) {
      setResume(null);
      return;
    }
    const nameOk = /\.(pdf|doc|docx)$/i.test(f.name);
    const mimeOk = ACCEPT_MIME.has(f.type) || nameOk;
    if (!mimeOk) {
      setErr("Resume must be a PDF, DOC, or DOCX file.");
      setResume(null);
      return;
    }
    setErr(null);
    setResume(f);
  };

  const canSubmit =
    name.trim() &&
    email.trim() &&
    phone.trim() &&
    resume !== null &&
    job?.isActive &&
    !submitting;

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!canSubmit || !job) return;
    setErr(null);
    setSubmitting(true);
    try {
      const result = await careersApi.applications.submit({
        jobId: job.id,
        applicantName: name.trim(),
        applicantEmail: email.trim(),
        applicantPhone: phone.trim(),
        coverLetter: coverLetter.trim() || undefined,
        currentCtc: currentCtc.trim() || undefined,
        expectedCtc: expectedCtc.trim() || undefined,
        noticePeriod: noticePeriod.trim() || undefined,
        yearsOfExperience:
          yearsOfExperience.trim() && !Number.isNaN(Number(yearsOfExperience))
            ? Number(yearsOfExperience)
            : undefined,
        resume: resume!,
      });
      setSubmitted(result);
    } catch (e: any) {
      setErr(getApiError(e) || "Application failed. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  const deadlinePast = (() => {
    if (!job?.applicationDeadline) return false;
    return new Date(job.applicationDeadline).getTime() < Date.now();
  })();

  if (auth.status !== "loading" && !auth.user) {
    const next = encodeURIComponent(`/careers/${params.id}/apply`);
    return (
      <main className="mx-auto max-w-3xl px-4 py-20 text-center">
        <div className="border-2 border-ink-950 bg-white shadow-hard-sm p-10">
          <FileText className="h-12 w-12 text-news mx-auto mb-4" />
          <h1 className="font-headline text-3xl uppercase mb-2">Sign in to apply</h1>
          <p className="text-sm text-ink-700 mb-6 max-w-md mx-auto">
            Only verified, logged-in users can submit applications at MapMyTimes.
          </p>
          <div className="flex flex-wrap items-center justify-center gap-2">
            <Link href={`/login?redirect=${next}`}>
              <Button variant="news" size="lg">Sign in</Button>
            </Link>
            <Link href={`/signup?redirect=${next}`}>
              <Button variant="outline" size="lg">Create account</Button>
            </Link>
          </div>
        </div>
      </main>
    );
  }

  if (submitted) {
    return (
      <main className="mx-auto max-w-3xl px-4 py-12 sm:py-16">
        <div className="border-2 border-ink-950 bg-white shadow-hard-md p-8 sm:p-10">
          <div className="flex items-start gap-4">
            <div className="h-14 w-14 shrink-0 bg-news border-2 border-ink-950 shadow-hard-sm flex items-center justify-center">
              <CheckCircle2 className="h-7 w-7 text-white" />
            </div>
            <div className="space-y-2 flex-1 min-w-0">
              <div className="ribbon text-xs">Application received</div>
              <h1 className="font-headline text-3xl sm:text-4xl uppercase leading-tight">
                You&apos;re in the running.
              </h1>
              <p className="text-sm text-ink-700">
                We&apos;ve received your application for{" "}
                <span className="font-semibold text-ink-950">{job?.title ?? "this role"}</span>.
                Track its status from your dashboard.
              </p>
            </div>
          </div>

          <div className="mt-8 grid grid-cols-1 sm:grid-cols-2 gap-3 border-t border-ink-950/15 pt-6">
            <InfoRow label="Reference ID" value={submitted.id} mono />
            <InfoRow label="Role" value={submitted.jobTitle} />
            <InfoRow label="Applicant" value={submitted.applicantName} />
            <InfoRow label="Resume" value={submitted.resumeOriginalFileName ?? "uploaded"} />
            <InfoRow
              label="Submitted at"
              value={
                submitted.appliedAt
                  ? new Date(submitted.appliedAt).toLocaleString()
                  : "Just now"
              }
            />
            <InfoRow label="Email" value={submitted.applicantEmail} />
          </div>

          <div className="mt-8 flex flex-wrap gap-2">
            <Link href="/dashboard/applications">
              <Button variant="news" size="lg">
                <ExternalLink className="h-4 w-4" /> My applications
              </Button>
            </Link>
            <Link href="/careers">
              <Button variant="outline" size="lg">
                <ArrowLeft className="h-4 w-4" /> Browse other roles
              </Button>
            </Link>
          </div>
        </div>
      </main>
    );
  }

  if (loading) return <ApplySkeleton />;

  if (fetchErr || !job) {
    return (
      <main className="mx-auto max-w-3xl px-4 py-20">
        <div className="border-2 border-ink-950 bg-white shadow-hard-sm p-10 text-center">
          <AlertTriangle className="h-12 w-12 text-news mx-auto mb-4" />
          <h1 className="font-headline text-3xl uppercase mb-2">
            {fetchErr || "Posting unavailable"}
          </h1>
          <p className="text-sm text-ink-700 mb-6 max-w-md mx-auto">
            The opening may have been closed or removed.
          </p>
          <Link href="/careers">
            <Button variant="news" size="lg">
              <ArrowLeft className="h-4 w-4" /> Back to careers
            </Button>
          </Link>
        </div>
      </main>
    );
  }

  if (!job.isActive || deadlinePast) {
    return (
      <main className="mx-auto max-w-3xl px-4 py-20">
        <div className="border-2 border-ink-950 bg-white shadow-hard-sm p-10 text-center">
          <AlertTriangle className="h-12 w-12 text-amber-500 mx-auto mb-4" />
          <h1 className="font-headline text-3xl uppercase mb-2">Applications closed</h1>
          <p className="text-sm text-ink-700 mb-6 max-w-md mx-auto">
            {deadlinePast
              ? "The application deadline has passed for this role."
              : "This posting is no longer active."}
          </p>
          <Link href="/careers">
            <Button variant="news" size="lg">
              <ArrowLeft className="h-4 w-4" /> See other openings
            </Button>
          </Link>
        </div>
      </main>
    );
  }

  return (
    <main className="mx-auto max-w-6xl px-4 py-8 sm:py-10">
      <PageHeader
        eyebrow="Apply for role"
        title={job.title}
        description="Fill out the form to submit your résumé and application details."
        action={
          <Link href={`/careers/${job.id}`}>
            <Button variant="outline" size="sm">
              <ArrowLeft className="h-4 w-4" /> Back to posting
            </Button>
          </Link>
        }
      />

      <div className="mt-6 grid grid-cols-1 lg:grid-cols-3 gap-6">
        <form onSubmit={submit} className="lg:col-span-2 space-y-5">
          <Card>
            <h3 className="font-headline uppercase tracking-wide mb-1">
              Candidate information
            </h3>
            <p className="text-xs text-ink-600 mb-4 font-semibold">
              Prefilled from your profile — edit if needed.
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input
                label="Full name *"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
                placeholder="Jane Doe"
              />
              <Input
                label="Email *"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                placeholder="jane@example.com"
              />
              <Input
                label="Phone *"
                type="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                required
                placeholder="+91 98XXXXXX12"
              />
              <Input
                label="Years of experience"
                type="number"
                min={0}
                max={50}
                step={1}
                value={yearsOfExperience}
                onChange={(e) => setYearsOfExperience(e.target.value)}
                placeholder="e.g. 3"
              />
            </div>
          </Card>

          <Card>
            <h3 className="font-headline uppercase tracking-wide mb-1">
              Compensation & joining
            </h3>
            <p className="text-xs text-ink-600 mb-4 font-semibold">
              All fields optional — helps us match your expectations.
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <Input
                label="Current CTC"
                value={currentCtc}
                onChange={(e) => setCurrentCtc(e.target.value)}
                placeholder="e.g. 8 LPA"
              />
              <Input
                label="Expected CTC"
                value={expectedCtc}
                onChange={(e) => setExpectedCtc(e.target.value)}
                placeholder="e.g. 12 LPA"
              />
              <Input
                label="Notice period"
                value={noticePeriod}
                onChange={(e) => setNoticePeriod(e.target.value)}
                placeholder="e.g. Immediate / 30 days"
              />
            </div>
          </Card>

          <Card>
            <h3 className="font-headline uppercase tracking-wide mb-1">
              Cover letter
            </h3>
            <p className="text-xs text-ink-600 mb-4 font-semibold">
              Tell us why you&apos;d be great at MapMyTimes. Optional.
            </p>
            <Textarea
              label="Cover letter (optional)"
              rows={8}
              value={coverLetter}
              onChange={(e) => setCoverLetter(e.target.value)}
              placeholder="A few sentences or bullet points about you, relevant work, and why this role at MapMyTimes."
            />
          </Card>

          <Card>
            <h3 className="font-headline uppercase tracking-wide mb-1">
              Résumé upload *
            </h3>
            <p className="text-xs text-ink-600 mb-4 font-semibold">
              Upload your latest CV. Accepted: PDF, DOC, DOCX.
            </p>
            <label
              className={cn(
                "block cursor-pointer border-2 border-dashed border-ink-950 p-6 bg-ink-950/[0.02] hover:bg-white transition-colors space-y-3",
                resume && "bg-white border-solid",
              )}
            >
              <input
                type="file"
                accept={ACCEPT_RESUME}
                className="hidden"
                onChange={onResume}
              />
              {resume ? (
                <div className="flex items-start gap-3">
                  <div className="h-11 w-11 shrink-0 border-2 border-ink-950 bg-news text-white flex items-center justify-center">
                    <FileUp className="h-5 w-5" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold truncate">{resume.name}</p>
                    <p className="text-[11px] uppercase tracking-widest text-ink-600 font-bold mt-0.5">
                      {humanBytes(resume.size)}
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={(e) => {
                      e.preventDefault();
                      setResume(null);
                    }}
                    className="text-xs font-bold uppercase tracking-widest text-news hover:text-ink-950"
                  >
                    Remove
                  </button>
                </div>
              ) : (
                <div className="flex flex-col items-center justify-center text-center gap-2 py-4">
                  <Upload className="h-8 w-8 text-ink-700" />
                  <p className="font-bold text-sm">
                    Click to upload or drag your résumé here
                  </p>
                  <p className="text-[11px] uppercase tracking-widest text-ink-600 font-bold">
                    PDF · DOC · DOCX
                  </p>
                </div>
              )}
            </label>
          </Card>

          {err ? (
            <div className="border-2 border-news bg-news/[0.06] p-4 text-sm font-semibold text-news inline-flex items-start gap-2">
              <AlertTriangle className="h-4 w-4 shrink-0 mt-0.5" />
              <span>{err}</span>
            </div>
          ) : null}

          <div className="flex flex-wrap gap-3 pb-8">
            <Button
              variant="news"
              size="lg"
              type="submit"
              disabled={!canSubmit || submitting}
            >
              {submitting ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Send className="h-4 w-4" />
              )}
              {submitting ? "Submitting…" : "Submit application"}
            </Button>
            <Link href={`/careers/${job.id}`}>
              <Button variant="outline" size="lg" type="button">
                <ArrowLeft className="h-4 w-4" /> Cancel
              </Button>
            </Link>
          </div>
        </form>

        <aside className="lg:col-span-1 space-y-5 h-fit">
          <Card>
            <div className="ribbon text-xs mb-2 inline-block">{job.department}</div>
            <h3 className="font-headline text-lg uppercase leading-tight mb-4">
              {job.title}
            </h3>
            <div className="space-y-2 text-sm">
              <InfoRow label="Location" value={job.location} />
              <InfoRow label="Type" value={friendly(job.jobType)} />
              <InfoRow label="Level" value={friendly(job.experienceLevel)} />
              {job.applicationDeadline ? (
                <InfoRow label="Apply by" value={toDate(job.applicationDeadline)} />
              ) : null}
            </div>
            <div className="mt-5 pt-4 border-t border-ink-950/15 text-[11px] text-ink-600 font-semibold leading-relaxed space-y-2">
              <p>• Apply only once per posting — duplicates are filtered.</p>
              <p>• Upload a single résumé file. Max size subject to server limits.</p>
              <p>• You will receive email confirmation at {email || "your email"}.</p>
              <p>• Track progress anytime: Dashboard → My Applications.</p>
            </div>
          </Card>
        </aside>
      </div>
    </main>
  );
}

function InfoRow({
  label,
  value,
  mono = false,
}: {
  label: string;
  value: string;
  mono?: boolean;
}) {
  return (
    <div className="flex items-start justify-between gap-3 py-1.5 border-b border-ink-950/10 last:border-b-0">
      <span className="text-[10px] font-bold uppercase tracking-widest text-ink-600 shrink-0 pt-0.5">
        {label}
      </span>
      <span
        className={cn(
          "text-sm font-semibold text-right break-words",
          mono && "font-mono text-xs",
        )}
      >
        {value}
      </span>
    </div>
  );
}

function friendly(v: string | null | undefined) {
  if (!v) return "—";
  const map: Record<string, string> = {
    FULL_TIME: "Full Time",
    PART_TIME: "Part Time",
    INTERNSHIP: "Internship",
    CONTRACT: "Contract",
    FREELANCE: "Freelance",
    FRESHER: "Fresher",
    JUNIOR: "Junior",
    MID: "Mid",
    SENIOR: "Senior",
    LEAD: "Lead",
  };
  return map[v] ?? v;
}

function toDate(iso: string) {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleDateString(undefined, {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
}

function humanBytes(b: number) {
  if (!Number.isFinite(b) || b <= 0) return "0 B";
  const units = ["B", "KB", "MB", "GB"];
  let i = 0;
  let n = b;
  while (n >= 1024 && i < units.length - 1) {
    n /= 1024;
    i += 1;
  }
  return `${n.toFixed(n >= 10 || i === 0 ? 0 : 1)} ${units[i]}`;
}

function ApplySkeleton() {
  return (
    <main className="mx-auto max-w-6xl px-4 py-10">
      <div className="animate-pulse space-y-6">
        <div className="h-4 w-40 bg-ink-900/15 mb-1" />
        <div className="h-10 w-2/3 bg-ink-900/20 mb-4" />
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="space-y-5 lg:col-span-2">
            <div className="h-64 border-2 border-ink-950 p-6" />
            <div className="h-56 border-2 border-ink-950 p-6" />
            <div className="h-80 border-2 border-ink-950 p-6" />
            <div className="h-56 border-2 border-ink-950 p-6" />
          </div>
          <div className="h-72 border-2 border-ink-950" />
        </div>
      </div>
    </main>
  );
}
