"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  ShieldCheck,
  ShieldAlert,
  Printer,
  Download,
  RefreshCcw,
  CalendarDays,
  Clock,
  AlertTriangle,
  CheckCircle2,
  Phone,
  Mail,
  MapPin,
  User,
  Briefcase,
  Heart,
  AlertOctagon,
  Send,
  ChevronRight,
  ExternalLink,
} from "lucide-react";
import { blogApi } from "@/lib/api/blogApi";
import PressIdCard from "@/components/staff/PressIdCard";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { getApiError } from "@/lib/api/client";
import {
  cn,
  formatDate,
} from "@/lib/utils";
import { DEPARTMENT_LABELS, statusPill } from "@/lib/staff";
import type { StaffProfileForSelfDTO } from "@/types/blog";
import { useAuth } from "@/lib/auth/AuthProvider";
import { PageHeader, Card } from "@/components/dashboard/Panels";

function calcExpiryBreakdown(targetIso: string | null | undefined) {
  if (!targetIso) return null;
  const target = new Date(targetIso).getTime();
  const now = Date.now();
  const diffMs = target - now;
  const totalDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));
  const absDays = Math.abs(totalDays);
  const years = Math.floor(absDays / 365);
  const months = Math.floor((absDays % 365) / 30);
  const days = absDays - years * 365 - months * 30;
  return {
    totalDays,
    years,
    months,
    days,
    expired: totalDays < 0,
    urgent: totalDays >= 0 && totalDays <= 90,
  };
}

export default function ReporterMyIdPage() {
  const router = useRouter();
  const auth = useAuth();
  const [profile, setProfile] = useState<StaffProfileForSelfDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");
  const [busyReissue, setBusyReissue] = useState(false);
  const [reissueReason, setReissueReason] = useState("");
  const [reissueSuccess, setReissueSuccess] = useState(false);
  const [busyPdf, setBusyPdf] = useState(false);
  const stageRef = useRef<HTMLDivElement>(null);

  const userRole = auth.user?.role;
  const canAccess =
    userRole === "PRESS_REPORTER" ||
    userRole === "STAFF_ADMIN" ||
    userRole === "ADMIN" ||
    userRole === "SUPER_ADMIN";

  useEffect(() => {
    if (!canAccess && auth.status !== "loading") {
      router.replace("/dashboard");
      return;
    }
    if (auth.status === "loading") return;

    let active = true;
    setLoading(true);
    blogApi.staff.me
      .get()
      .then((p) => {
        if (!active) return;
        setProfile(p);
      })
      .catch((e) => {
        if (!active) return;
        const msg = getApiError(e);
        if (msg?.toLowerCase().includes("no staff profile") || msg?.toLowerCase().includes("not found")) {
          setErr(
            "No Press ID profile is linked to your account yet. Contact your STAFF_ADMIN or newsroom administrator to issue your official credential.",
          );
        } else {
          setErr(msg || "Could not load your Press ID profile.");
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [auth.status, canAccess, router]);

  const expiry = useMemo(
    () => calcExpiryBreakdown(profile?.validTill),
    [profile?.validTill],
  );
  const statusColors = profile ? statusPill(profile.status) : null;

  async function requestReissue() {
    if (!profile) return;
    setBusyReissue(true);
    setReissueSuccess(false);
    try {
      await blogApi.staff.me.requestReissue(reissueReason.trim() || undefined);
      setReissueSuccess(true);
      setReissueReason("");
      const refreshed = await blogApi.staff.me.get();
      setProfile(refreshed);
    } catch (e) {
      alert(getApiError(e) || "Could not submit reissue request.");
    } finally {
      setBusyReissue(false);
    }
  }

  async function downloadPdfClient() {
    const hasHtml2Canvas =
      typeof window !== "undefined" && !!(window as any).html2canvas;
    const hasJsPdf =
      typeof window !== "undefined" &&
      (!!(window as any).jspdf ||
        !!(window as any).jspdf?.jsPDF ||
        !!((window as any).jspdf && (window as any).jspdf.jsPDF));

    if (!profile) return;

    if (profile.downloadUrl) {
      window.open(profile.downloadUrl, "_blank", "noopener,noreferrer");
      return;
    }

    if (!hasHtml2Canvas || !hasJsPdf) {
      const fallback = window.confirm(
        "PDF download libraries (jsPDF + html2canvas) not loaded yet. Print via browser instead?\n\nTip: In the print dialog, choose 'Save as PDF'.",
      );
      if (fallback) window.print();
      return;
    }

    setBusyPdf(true);
    const h2c = (window as any).html2canvas;
    const jsPdfCtor =
      ((window as any).jspdf && (window as any).jspdf.jsPDF) ||
      (window as any).jsPDF ||
      ((window as any).jspdf && (window as any).jspdf.jsPDF
        ? (window as any).jspdf.jsPDF
        : (window as any).jsPDF);

    if (!h2c || !jsPdfCtor) {
      window.print();
      setBusyPdf(false);
      return;
    }

    const stage =
      stageRef.current ||
      document.querySelector<HTMLDivElement>(".press-stage");
    if (!stage) {
      setBusyPdf(false);
      return;
    }
    try {
      const canvas = await h2c(stage, {
        backgroundColor: "#ffffff",
        scale: 3,
        useCORS: true,
        logging: false,
      });
      const { jsPDF } = (window as any).jspdf || { jsPDF: jsPdfCtor };
      const PDF = (window as any).jspdf ? jsPDF : jsPdfCtor;
      const pdf = new PDF({
        orientation: "landscape",
        unit: "mm",
        format: [110, 72],
      });
      const dataUrl = canvas.toDataURL("image/jpeg", 0.95);
      const pageW = pdf.internal.pageSize.getWidth();
      const pageH = pdf.internal.pageSize.getHeight();
      pdf.addImage(dataUrl, "JPEG", 0, 0, pageW, pageH);
      pdf.save(`${profile.idNumber}-press-id.pdf`);
    } catch (e) {
      console.error(e);
      alert("PDF could not be generated. Use PRINT → Save as PDF instead.");
    } finally {
      setBusyPdf(false);
    }
  }

  function doPrint() {
    if (profile?.printUrl) {
      window.open(profile.printUrl, "_blank", "noopener,noreferrer");
    } else {
      window.print();
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="Newsroom Credential"
        title="My Official Press ID"
        description="Your digital ID card, verification status, and credential management."
        action={
          <div className="flex gap-2 flex-wrap">
            <Link
              href="/verify-press"
              className="inline-flex items-center gap-2 border-2 border-ink-950 hover:border-news hover:text-news px-3 py-2 text-xs font-bold uppercase tracking-widest"
            >
              <ShieldCheck className="h-4 w-4" /> Public Verify
              <ChevronRight className="h-3.5 w-3.5" />
            </Link>
          </div>
        }
      />

      {loading ? (
        <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_380px] gap-6">
          <div className="h-[620px] border border-ink-900/20 rounded-xl animate-pulse bg-ink-900/5" />
          <div className="h-[620px] border border-ink-900/20 rounded-xl animate-pulse bg-ink-900/5" />
        </div>
      ) : err && !profile ? (
        <Card className="p-6">
          <div className="flex items-start gap-3">
            <ShieldAlert className="h-6 w-6 flex-shrink-0 text-news" />
            <div className="min-w-0">
              <div className="font-headline uppercase text-lg tracking-wide text-ink-950">
                Credential Not Issued
              </div>
              <p className="mt-2 text-sm text-ink-800 leading-relaxed">{err}</p>
              <div className="mt-5 flex gap-3 flex-wrap">
                <Button variant="outline" onClick={() => router.refresh()}>
                  <RefreshCcw className="h-4 w-4" /> Refresh
                </Button>
              </div>
            </div>
          </div>
        </Card>
      ) : profile ? (
        <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_380px] gap-6 items-start">
          <div ref={stageRef}>
            <PressIdCard
              data={profile}
              variant="self"
              showButtons={false}
              busyPdf={busyPdf}
              onDownloadPdf={downloadPdfClient}
              onPrint={doPrint}
            />
          </div>

          <div className="space-y-5">
            {/* Status + Print/PDF */}
            <Card className="p-5">
              <div className="flex items-start justify-between gap-4">
                <div className="min-w-0">
                  <div className="text-[10px] uppercase tracking-[0.25em] font-bold text-ink-600">
                    Credential Status
                  </div>
                  <div className="mt-1 flex items-center gap-2">
                    <span
                      className={cn(
                        "inline-flex items-center gap-1 text-[11px] font-bold uppercase tracking-widest px-2.5 py-1 rounded-sm border",
                        statusColors?.bg,
                        statusColors?.text,
                        `ring-1 ring-inset ${statusColors?.ring ?? "ring-ink-900/10"}`,
                      )}
                    >
                      {profile.status.replace(/_/g, " ")}
                    </span>
                    {expiry?.expired ? (
                      <span className="inline-flex items-center gap-1 text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-sm bg-rose-50 border border-rose-200 text-rose-700">
                        <AlertOctagon className="h-3 w-3" /> Expired
                      </span>
                    ) : expiry?.urgent ? (
                      <span className="inline-flex items-center gap-1 text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-sm bg-amber-50 border border-amber-200 text-amber-700">
                        <AlertTriangle className="h-3 w-3" /> Expiring Soon
                      </span>
                    ) : profile.status === "ACTIVE" ? (
                      <span className="inline-flex items-center gap-1 text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-sm bg-emerald-50 border border-emerald-200 text-emerald-700">
                        <CheckCircle2 className="h-3 w-3" /> Valid
                      </span>
                    ) : null}
                  </div>
                  <div className="mt-2 font-mono text-sm tracking-wide text-ink-800 break-all">
                    {profile.idNumber}
                  </div>
                </div>
                <div className="flex-shrink-0">
                  <div className="w-14 h-14 rounded-lg bg-ink-900 text-white flex items-center justify-center font-headline text-xl">
                    {(profile.firstName?.[0] || profile.fullName?.[0] || "?").toUpperCase()}
                    {(profile.lastName?.[0] || profile.fullName?.split(" ")?.[1]?.[0] || "").toUpperCase()}
                  </div>
                </div>
              </div>

              <div className="mt-5 grid grid-cols-2 gap-3">
                <Button variant="news" block onClick={doPrint}>
                  <Printer className="h-4 w-4" /> Print
                </Button>
                <Button
                  variant="ink"
                  block
                  onClick={() => void downloadPdfClient()}
                  disabled={busyPdf}
                >
                  <Download className="h-4 w-4" /> {busyPdf ? "Generating…" : "Download PDF"}
                </Button>
              </div>
              {profile.downloadUrl ? (
                <Link
                  href={profile.downloadUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="mt-3 flex items-center justify-center gap-1.5 text-[11px] font-bold uppercase tracking-widest text-ink-600 hover:text-ink-950"
                >
                  <ExternalLink className="h-3.5 w-3.5" /> Server-generated PDF
                </Link>
              ) : null}
            </Card>

            {/* Expiry Countdown */}
            <Card className="p-5">
              <div className="flex items-center gap-2 text-[10px] uppercase tracking-[0.25em] font-bold text-ink-600 mb-3">
                <Clock className="h-3.5 w-3.5" /> Validity Timeline
              </div>
              {expiry ? (
                <>
                  <div className="grid grid-cols-4 gap-2">
                    <StatBox label="Years" value={expiry.years} faded={expiry.expired} />
                    <StatBox label="Months" value={expiry.months} faded={expiry.expired} />
                    <StatBox label="Days" value={expiry.days} faded={expiry.expired} />
                    <StatBox
                      label="Total"
                      value={Math.abs(expiry.totalDays)}
                      faded={expiry.expired}
                      suffix="d"
                    />
                  </div>
                  <div className="mt-4 space-y-1.5 text-xs">
                    <div className="flex items-center justify-between text-ink-700">
                      <span className="flex items-center gap-1.5 font-semibold uppercase tracking-wider text-[10px] text-ink-500">
                        <CalendarDays className="h-3.5 w-3.5" /> Issue Date
                      </span>
                      <span className="font-bold">{formatDate(profile.issueDate ?? undefined)}</span>
                    </div>
                    <div className="flex items-center justify-between text-ink-700">
                      <span className="flex items-center gap-1.5 font-semibold uppercase tracking-wider text-[10px] text-ink-500">
                        <CalendarDays className="h-3.5 w-3.5" /> Last Renewed
                      </span>
                      <span className="font-bold">
                        {profile.lastRenewedDate
                          ? formatDate(profile.lastRenewedDate)
                          : "—"}
                      </span>
                    </div>
                    <div className="flex items-center justify-between text-ink-700">
                      <span className="flex items-center gap-1.5 font-semibold uppercase tracking-wider text-[10px] text-ink-500">
                        <AlertTriangle className="h-3.5 w-3.5 text-news" /> Valid Till
                      </span>
                      <span
                        className={cn(
                          "font-bold",
                          expiry.expired
                            ? "text-rose-700"
                            : expiry.urgent
                            ? "text-amber-700"
                            : "text-ink-900",
                        )}
                      >
                        {formatDate(profile.validTill ?? undefined)}
                      </span>
                    </div>
                  </div>

                  {(expiry.expired || expiry.urgent) && !profile.reissueRequested ? (
                    <div className="mt-5 pt-4 border-t border-ink-900/10">
                      <div className="text-[10px] uppercase tracking-[0.25em] font-bold text-ink-600 mb-2">
                        Request Re-Issue / Renewal
                      </div>
                      <textarea
                        value={reissueReason}
                        onChange={(e) => setReissueReason(e.target.value)}
                        rows={3}
                        placeholder="Reason (e.g. Lost card, Expiring soon, Updated photo required, Address change)…"
                        className="w-full rounded-none border-2 border-ink-900 bg-white px-3 py-2 text-sm font-medium focus:outline-none focus:border-news"
                      />
                      <Button
                        variant="news"
                        block
                        className="mt-3"
                        disabled={busyReissue}
                        onClick={() => void requestReissue()}
                      >
                        <Send className="h-4 w-4" />
                        {busyReissue ? "Submitting…" : "Submit Re-Issue Request"}
                      </Button>
                      {reissueSuccess ? (
                        <div className="mt-3 text-[11px] font-bold uppercase tracking-wider text-emerald-700 flex items-center gap-1.5">
                          <CheckCircle2 className="h-3.5 w-3.5" /> Request submitted. STAFF_ADMIN will review.
                        </div>
                      ) : null}
                    </div>
                  ) : profile.reissueRequested ? (
                    <div className="mt-5 pt-4 border-t border-ink-900/10 rounded-sm">
                      <div className="inline-flex items-center gap-1.5 px-2.5 py-1 border border-amber-200 bg-amber-50 text-amber-800 text-[11px] font-bold uppercase tracking-widest">
                        <Clock className="h-3.5 w-3.5" /> Reissue Requested · Pending Review
                      </div>
                      {profile.reissueReason ? (
                        <p className="mt-2 text-xs text-ink-700 leading-relaxed">
                          <span className="font-bold text-ink-950">Your note: </span>
                          {profile.reissueReason}
                        </p>
                      ) : null}
                    </div>
                  ) : null}
                </>
              ) : null}
            </Card>

            {/* Contact & Personal Info */}
            <Card className="p-5">
              <div className="text-[10px] uppercase tracking-[0.25em] font-bold text-ink-600 mb-3">
                Unmasked Profile (Private)
              </div>
              <dl className="divide-y divide-ink-900/10 text-sm">
                <InfoRow icon={<User className="h-4 w-4" />} label="Full Name" value={profile.fullName} />
                <InfoRow
                  icon={<Briefcase className="h-4 w-4" />}
                  label="Designation / Dept"
                  value={
                    (profile.designation
                      ? profile.designation + " · "
                      : "") + DEPARTMENT_LABELS[profile.department]
                  }
                />
                <InfoRow
                  icon={<Phone className="h-4 w-4" />}
                  label="Mobile (Private)"
                  value={profile.mobilePrivate || "—"}
                />
                <InfoRow
                  icon={<Phone className="h-4 w-4" />}
                  label="Work Mobile"
                  value={profile.workMobile || "—"}
                />
                <InfoRow
                  icon={<Mail className="h-4 w-4" />}
                  label="Work Email"
                  value={profile.workEmail || "—"}
                />
                <InfoRow
                  icon={<Mail className="h-4 w-4" />}
                  label="Personal Email"
                  value={profile.personalEmail || "—"}
                />
                <InfoRow
                  icon={<MapPin className="h-4 w-4" />}
                  label="Location"
                  value={
                    [profile.city, profile.district, profile.state]
                      .filter(Boolean)
                      .join(", ") || "—"
                  }
                />
                <InfoRow
                  icon={<MapPin className="h-4 w-4" />}
                  label="Full Address"
                  value={profile.address || "—"}
                  mono={false}
                />
                <InfoRow
                  icon={<Heart className="h-4 w-4 text-news" />}
                  label="Blood Group"
                  value={profile.bloodGroup || "—"}
                  highlight
                />
                <InfoRow
                  icon={<User className="h-4 w-4" />}
                  label="Date of Birth"
                  value={formatDate(profile.dateOfBirth ?? undefined)}
                />
                <InfoRow
                  icon={<AlertOctagon className="h-4 w-4 text-news" />}
                  label="Emergency Contact"
                  value={
                    profile.emergencyContactName && profile.emergencyNumber
                      ? `${profile.emergencyContactName} · ${profile.emergencyNumber}`
                      : "—"
                  }
                  highlight
                />
                <InfoRow
                  icon={<ShieldCheck className="h-4 w-4" />}
                  label="Reporter Batch ID"
                  value={profile.reporterBatchId || "—"}
                />
              </dl>
            </Card>

            <Card className="p-5 bg-ink-950 text-white">
              <div className="text-[10px] uppercase tracking-[0.25em] font-bold text-white/60 mb-3">
                Quick Actions
              </div>
              <div className="grid grid-cols-2 gap-3">
                <Link
                  href={`/our-team/${encodeURIComponent(profile.idNumber)}`}
                  className="h-10 px-3 text-xs font-bold uppercase tracking-widest flex items-center justify-center gap-2 border-2 border-white/20 hover:border-white text-white"
                >
                  <ExternalLink className="h-4 w-4" /> Public Page
                </Link>
                <Link
                  href={`/verify-press?id=${encodeURIComponent(profile.idNumber)}`}
                  className="h-10 px-3 text-xs font-bold uppercase tracking-widest flex items-center justify-center gap-2 border-2 border-white/20 hover:border-news hover:text-news text-white"
                >
                  <ShieldCheck className="h-4 w-4" /> Verify Link
                </Link>
              </div>
            </Card>
          </div>
        </div>
      ) : null}
    </>
  );
}

function StatBox({
  label,
  value,
  faded,
  suffix,
}: {
  label: string;
  value: number;
  faded?: boolean;
  suffix?: string;
}) {
  return (
    <div
      className={cn(
        "rounded-sm border-2 p-2 text-center",
        faded
          ? "border-rose-200 bg-rose-50"
          : "border-ink-950/10 bg-ink-900/[0.03]",
      )}
    >
      <div
        className={cn(
          "font-headline text-xl leading-tight",
          faded ? "text-rose-700" : "text-ink-950",
        )}
      >
        {value}
        {suffix ? <span className="text-[10px] font-bold ml-0.5">{suffix}</span> : null}
      </div>
      <div className="mt-0.5 text-[9px] font-bold uppercase tracking-[0.2em] text-ink-500">
        {label}
      </div>
    </div>
  );
}

function InfoRow({
  icon,
  label,
  value,
  highlight,
  mono = true,
}: {
  icon: React.ReactNode;
  label: string;
  value: React.ReactNode;
  highlight?: boolean;
  mono?: boolean;
}) {
  return (
    <div className="py-2.5 flex items-start gap-3">
      <div className="flex-shrink-0 pt-0.5 text-ink-500">{icon}</div>
      <div className="min-w-0 flex-1">
        <div className="text-[10px] uppercase tracking-[0.2em] font-bold text-ink-500">
          {label}
        </div>
        <div
          className={cn(
            "mt-0.5 text-sm font-semibold text-ink-900 break-words whitespace-pre-wrap",
            mono ? "font-mono tracking-tight" : "",
            highlight ? "text-news" : "",
          )}
        >
          {value || "—"}
        </div>
      </div>
    </div>
  );
}
