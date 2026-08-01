"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { careersApi } from "@/lib/api/careersApi";
import { getApiError } from "@/lib/api/client";
import { PageHeader, Card } from "@/components/dashboard/Panels";
import { Button } from "@/components/ui/Button";
import type {
  JobApplicationResponse,
  JobApplicationSummaryResponse,
} from "@/types/careers";
import { StatusPill } from "@/components/careers/JobCard";
import { Pagination } from "@/components/ui/Pagination";
import {
  Briefcase,
  Clock,
  Mail,
  FileText,
  LogOut,
  AlertTriangle,
  CheckCircle2,
  Loader2,
  ArrowRight,
  ExternalLink,
  ChevronRight,
  X,
  Calendar,
  Phone,
  BadgeDollarSign,
} from "lucide-react";
import { cn } from "@/lib/utils";

export default function DashboardApplicationsPage() {
  const [list, setList] = useState<JobApplicationSummaryResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);

  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [detail, setDetail] = useState<JobApplicationResponse | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);

  const [confirmId, setConfirmId] = useState<string | null>(null);
  const [withdrawing, setWithdrawing] = useState(false);
  const [flash, setFlash] = useState<{ kind: "ok" | "err"; text: string } | null>(null);

  const load = async (p = page) => {
    setLoading(true);
    try {
      const r = (await careersApi.applications.my({ page: p, size: 10 })) as any;
      setList(r.content ?? []);
      setTotalPages(Math.max(1, r.totalPages ?? 1));
      setTotalElements(r.totalElements ?? (r.content ?? []).length);
    } catch (e: any) {
      setFlash({ kind: "err", text: getApiError(e) });
      setList([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load(page);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  useEffect(() => {
    if (!selectedId) {
      setDetail(null);
      return;
    }
    let active = true;
    setDetailLoading(true);
    careersApi.applications
      .get(selectedId)
      .then((d) => {
        if (!active) return;
        setDetail(d as any);
      })
      .catch((e: any) => {
        if (!active) return;
        setFlash({ kind: "err", text: getApiError(e) || "Could not load application." });
        setDetail(null);
      })
      .finally(() => {
        if (active) setDetailLoading(false);
      });
    return () => {
      active = false;
    };
  }, [selectedId]);

  const confirmWithdraw = async () => {
    if (!confirmId) return;
    setWithdrawing(true);
    try {
      await careersApi.applications.withdraw(confirmId);
      setFlash({ kind: "ok", text: "Application withdrawn." });
      setConfirmId(null);
      setDetail(null);
      setSelectedId(null);
      void load(page);
    } catch (e: any) {
      setFlash({ kind: "err", text: getApiError(e) });
    } finally {
      setWithdrawing(false);
    }
  };

  return (
    <>
      <PageHeader
        eyebrow="Careers"
        title="My applications"
        description="Track the status of applications you've submitted to MapMyTimes."
        action={
          <Link href="/careers">
            <Button variant="outline" size="lg">
              <Briefcase className="h-4 w-4" /> Browse roles <ArrowRight className="h-4 w-4" />
            </Button>
          </Link>
        }
      />

      {flash ? (
        <div
          className={cn(
            "border-2 p-4 inline-flex items-start gap-3",
            flash.kind === "ok"
              ? "bg-emerald-50 border-emerald-500"
              : "bg-news-50 border-news",
          )}
        >
          {flash.kind === "ok" ? (
            <CheckCircle2 className="h-4 w-4 mt-0.5 text-emerald-600" />
          ) : (
            <AlertTriangle className="h-4 w-4 mt-0.5 text-news" />
          )}
          <span
            className={cn(
              "text-sm font-semibold",
              flash.kind === "ok" ? "text-emerald-900" : "text-news-700",
            )}
          >
            {flash.text}
          </span>
          <button
            type="button"
            onClick={() => setFlash(null)}
            className="ml-auto text-ink-700 hover:text-ink-950"
            aria-label="Dismiss"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      ) : null}

      <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
        <section className="lg:col-span-3 space-y-4">
          <Card className="!p-0 overflow-hidden">
            <div className="flex items-center justify-between p-4 sm:p-5 border-b-2 border-ink-950/15 bg-ink-950/[0.03]">
              <div>
                <h3 className="font-headline uppercase tracking-wide text-sm">
                  Applications
                </h3>
                <p className="text-xs text-ink-600 font-semibold mt-1">
                  {loading
                    ? "Loading…"
                    : `${totalElements} total · Page ${page + 1} / ${totalPages}`}
                </p>
              </div>
            </div>

            {loading ? (
              <ul className="divide-y divide-ink-950/10">
                {Array.from({ length: 5 }).map((_, i) => (
                  <li
                    key={i}
                    className="p-4 sm:p-5 animate-pulse flex items-center gap-4"
                  >
                    <div className="h-10 w-10 bg-ink-900/10 border-2 border-ink-950/10" />
                    <div className="flex-1 space-y-2">
                      <div className="h-4 w-3/4 bg-ink-900/15" />
                      <div className="h-3 w-1/3 bg-ink-900/10" />
                    </div>
                    <div className="h-7 w-24 bg-ink-900/10" />
                  </li>
                ))}
              </ul>
            ) : list.length === 0 ? (
              <div className="p-10 text-center">
                <Briefcase className="h-10 w-10 text-news mx-auto mb-4" />
                <h3 className="font-headline text-2xl uppercase mb-2">
                  No applications yet
                </h3>
                <p className="text-sm text-ink-700 mb-6 max-w-md mx-auto">
                  Apply to an open role and your submission will show up here.
                </p>
                <Link href="/careers">
                  <Button variant="news" size="lg">
                    <ExternalLink className="h-4 w-4" /> Browse open roles
                  </Button>
                </Link>
              </div>
            ) : (
              <ul className="divide-y divide-ink-950/10">
                {list.map((a) => {
                  const active = selectedId === a.id;
                  return (
                    <li key={a.id}>
                      <button
                        type="button"
                        onClick={() => setSelectedId(active ? null : a.id)}
                        className={cn(
                          "w-full text-left p-4 sm:p-5 flex items-start gap-4 hover:bg-ink-950/[0.03] transition-colors focus:outline-none focus:bg-ink-950/[0.05]",
                          active && "bg-ink-950/[0.06]",
                        )}
                      >
                        <div className="h-11 w-11 shrink-0 bg-news/10 border-2 border-ink-950 flex items-center justify-center">
                          <Briefcase className="h-4 w-4 text-news" />
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="flex flex-wrap items-center gap-2 mb-1">
                            <p className="font-headline uppercase tracking-wide text-sm truncate">
                              {a.jobTitle}
                            </p>
                            <StatusPill status={a.status} />
                          </div>
                          <p className="text-xs font-semibold text-ink-700 flex flex-wrap items-center gap-x-3 gap-y-1">
                            <span className="inline-flex items-center gap-1.5">
                              <Mail className="h-3 w-3" />
                              {a.applicantEmail}
                            </span>
                            <span className="inline-flex items-center gap-1.5">
                              <Clock className="h-3 w-3" />
                              {a.appliedAt
                                ? new Date(a.appliedAt).toLocaleDateString(undefined, {
                                    day: "2-digit",
                                    month: "short",
                                    year: "numeric",
                                  })
                                : "Recent"}
                            </span>
                          </p>
                        </div>
                        <ChevronRight
                          className={cn(
                            "h-4 w-4 shrink-0 mt-1 text-ink-700 transition-transform",
                            active && "rotate-90",
                          )}
                        />
                      </button>
                    </li>
                  );
                })}
              </ul>
            )}
          </Card>
          <Pagination page={page} setPage={setPage} totalPages={totalPages} />
        </section>

        <aside className="lg:col-span-2 h-fit space-y-4">
          <Card className="!p-0 overflow-hidden">
            <div className="p-4 sm:p-5 border-b-2 border-ink-950/15 bg-ink-950/[0.03] flex items-center justify-between">
              <h3 className="font-headline uppercase tracking-wide text-sm">
                Details
              </h3>
              {selectedId ? (
                <button
                  type="button"
                  onClick={() => {
                    setSelectedId(null);
                    setDetail(null);
                  }}
                  className="text-xs font-bold uppercase tracking-widest text-ink-700 hover:text-news inline-flex items-center gap-1"
                >
                  Close <X className="h-3 w-3" />
                </button>
              ) : null}
            </div>

            {!selectedId ? (
              <div className="p-6 sm:p-8 text-center">
                <FileText className="h-10 w-10 text-ink-500 mx-auto mb-3" />
                <p className="font-semibold text-sm">Select an application to view details.</p>
              </div>
            ) : detailLoading || !detail ? (
              <div className="p-6 sm:p-8 space-y-3 animate-pulse">
                <div className="h-4 w-1/2 bg-ink-900/15" />
                <div className="h-4 w-3/4 bg-ink-900/10" />
                <div className="h-20 bg-ink-900/10 border-2 border-ink-950/10" />
                <div className="h-4 w-2/3 bg-ink-900/10" />
                {detailLoading ? (
                  <div className="inline-flex items-center gap-2 text-xs font-bold uppercase tracking-widest text-ink-700">
                    <Loader2 className="h-3.5 w-3.5 animate-spin" /> Loading
                  </div>
                ) : null}
              </div>
            ) : (
              <div className="p-4 sm:p-5 space-y-4">
                <div>
                  <div className="flex flex-wrap items-center gap-2 mb-2">
                    <h4 className="font-headline uppercase tracking-wide text-lg leading-tight">
                      {detail.jobTitle}
                    </h4>
                  </div>
                  <StatusPill status={detail.status} />
                </div>

                <div className="grid grid-cols-2 gap-x-3 gap-y-2 border-t border-ink-950/10 pt-4 text-sm">
                  <Info icon={<Mail className="h-3.5 w-3.5" />} label="Email" value={detail.applicantEmail} />
                  <Info icon={<Phone className="h-3.5 w-3.5" />} label="Phone" value={detail.applicantPhone ?? "—"} />
                  <Info
                    icon={<Calendar className="h-3.5 w-3.5" />}
                    label="Applied"
                    value={
                      detail.appliedAt
                        ? new Date(detail.appliedAt).toLocaleDateString()
                        : "—"
                    }
                  />
                  <Info
                    icon={<Clock className="h-3.5 w-3.5" />}
                    label="Updated"
                    value={
                      detail.updatedAt
                        ? new Date(detail.updatedAt).toLocaleDateString()
                        : "—"
                    }
                  />
                  {detail.yearsOfExperience !== undefined && detail.yearsOfExperience !== null ? (
                    <Info
                      icon={<Briefcase className="h-3.5 w-3.5" />}
                      label="Experience"
                      value={`${detail.yearsOfExperience} yr`}
                    />
                  ) : null}
                  {detail.noticePeriod ? (
                    <Info
                      icon={<LogOut className="h-3.5 w-3.5" />}
                      label="Notice period"
                      value={detail.noticePeriod}
                    />
                  ) : null}
                  {detail.currentCtc || detail.expectedCtc ? (
                    <>
                      {detail.currentCtc ? (
                        <Info
                          icon={<BadgeDollarSign className="h-3.5 w-3.5" />}
                          label="Current CTC"
                          value={detail.currentCtc}
                        />
                      ) : null}
                      {detail.expectedCtc ? (
                        <Info
                          icon={<BadgeDollarSign className="h-3.5 w-3.5" />}
                          label="Expected"
                          value={detail.expectedCtc}
                        />
                      ) : null}
                    </>
                  ) : null}
                </div>

                {detail.interviewScheduledAt ? (
                  <div className="border-2 border-emerald-500 bg-emerald-50 p-3 text-xs font-semibold text-emerald-900 inline-flex items-start gap-2 w-full">
                    <Calendar className="h-3.5 w-3.5 shrink-0 mt-0.5" />
                    <span>
                      Interview scheduled:{" "}
                      <span className="font-bold">
                        {new Date(detail.interviewScheduledAt).toLocaleString()}
                      </span>
                    </span>
                  </div>
                ) : null}

                {detail.rejectionReason ? (
                  <div className="border-2 border-news bg-news-50 p-3 text-xs font-semibold text-news-700 inline-flex items-start gap-2 w-full">
                    <AlertTriangle className="h-3.5 w-3.5 shrink-0 mt-0.5" />
                    <div>
                      <p className="font-bold text-news mb-0.5">Update from team</p>
                      <p className="whitespace-pre-wrap">{detail.rejectionReason}</p>
                    </div>
                  </div>
                ) : null}

                {detail.coverLetter ? (
                  <div className="border-t border-ink-950/10 pt-4 space-y-2">
                    <h5 className="text-[10px] font-bold uppercase tracking-widest text-ink-600">
                      Cover letter
                    </h5>
                    <p className="text-sm text-ink-800 leading-relaxed whitespace-pre-wrap">
                      {detail.coverLetter}
                    </p>
                  </div>
                ) : null}

                {detail.resumeOriginalFileName || detail.resumeUrl ? (
                  <div className="border-t border-ink-950/10 pt-4 flex items-center justify-between gap-3">
                    <div className="min-w-0">
                      <h5 className="text-[10px] font-bold uppercase tracking-widest text-ink-600">
                        Résumé
                      </h5>
                      <p className="text-sm font-semibold truncate">
                        {detail.resumeOriginalFileName ?? "Attached"}
                      </p>
                    </div>
                    {detail.resumeUrl ? (
                      <a
                        href={detail.resumeUrl}
                        target="_blank"
                        rel="noreferrer noopener"
                      >
                        <Button variant="outline" size="sm">
                          <ExternalLink className="h-3.5 w-3.5" /> View
                        </Button>
                      </a>
                    ) : null}
                  </div>
                ) : null}

                {detail.status !== "WITHDRAWN" &&
                detail.status !== "SELECTED" &&
                detail.status !== "REJECTED" ? (
                  <div className="pt-4 border-t border-ink-950/10">
                    <Button
                      variant="outline"
                      size="sm"
                      className="w-full !text-news hover:!bg-news hover:!text-white border-news"
                      onClick={() => setConfirmId(detail.id)}
                    >
                      <LogOut className="h-3.5 w-3.5" /> Withdraw application
                    </Button>
                  </div>
                ) : null}
              </div>
            )}
          </Card>
        </aside>
      </div>

      {confirmId ? (
        <ConfirmDialog
          title="Withdraw application?"
          description="You won't be considered for this role after withdrawing. You can always re-apply later if the posting is still open."
          confirmLabel={withdrawing ? "Withdrawing…" : "Withdraw"}
          cancelLabel="Keep application"
          danger
          loading={withdrawing}
          onCancel={() => !withdrawing && setConfirmId(null)}
          onConfirm={confirmWithdraw}
        />
      ) : null}
    </>
  );
}

function Info({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
}) {
  return (
    <div>
      <div className="flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-widest text-ink-600 mb-0.5">
        {icon} {label}
      </div>
      <div className="text-sm font-semibold break-words">{value}</div>
    </div>
  );
}

function ConfirmDialog({
  title,
  description,
  confirmLabel,
  cancelLabel,
  danger,
  loading,
  onConfirm,
  onCancel,
}: {
  title: string;
  description?: string;
  confirmLabel: string;
  cancelLabel: string;
  danger?: boolean;
  loading?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-3 sm:p-6 bg-ink-950/50"
      onClick={!loading ? onCancel : undefined}
    >
      <div
        className="w-full max-w-md border-2 border-ink-950 bg-white shadow-hard-md"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="p-5 sm:p-6 border-b-2 border-ink-950/15">
          <div className="flex items-start gap-3">
            <div
              className={cn(
                "h-10 w-10 shrink-0 border-2 border-ink-950 flex items-center justify-center",
                danger ? "bg-news text-white" : "bg-ink-950 text-white",
              )}
            >
              <AlertTriangle className="h-5 w-5" />
            </div>
            <div className="min-w-0 flex-1">
              <h3 className="font-headline text-xl uppercase leading-tight mb-1">
                {title}
              </h3>
              {description ? (
                <p className="text-sm text-ink-700">{description}</p>
              ) : null}
            </div>
          </div>
        </div>
        <div className="p-3 sm:p-4 flex flex-wrap items-center justify-end gap-2">
          <Button variant="outline" size="sm" onClick={onCancel} disabled={loading}>
            {cancelLabel}
          </Button>
          <Button
            variant={danger ? "news" : "primary"}
            size="sm"
            onClick={onConfirm}
            disabled={loading}
          >
            {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
            {confirmLabel}
          </Button>
        </div>
      </div>
    </div>
  );
}
