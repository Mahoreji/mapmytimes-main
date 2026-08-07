"use client";

import { Suspense, useEffect, useState } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import Link from "next/link";
import {
  ShieldCheck,
  ShieldAlert,
  Search,
  ArrowLeft,
  CheckCircle2,
  XCircle,
  Info,
  Printer,
  Download,
} from "lucide-react";
import { blogApi } from "@/lib/api/blogApi";
import PressIdCard from "@/components/staff/PressIdCard";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { getApiError } from "@/lib/api/client";
import { formatDateCompact } from "@/lib/staff";
import { cn, formatDate } from "@/lib/utils";
import type { StaffPressIdDTO, StaffVerifyResponseDTO } from "@/types/blog";

function VerifyPressInner() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const [idInput, setIdInput] = useState("");
  const [submittedId, setSubmittedId] = useState<string | null>(null);
  const [card, setCard] = useState<StaffPressIdDTO | null>(null);
  const [verify, setVerify] = useState<StaffVerifyResponseDTO | null>(null);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState("");

  const initialId = searchParams?.get("id");

  useEffect(() => {
    if (initialId && !submittedId) {
      setIdInput(initialId);
      setSubmittedId(initialId);
      runVerify(initialId);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialId]);

  async function runVerify(idNumber: string) {
    const id = idNumber.trim();
    if (!id) {
      setErr("Enter a Press ID number to verify.");
      return;
    }
    setLoading(true);
    setErr("");
    setCard(null);
    setVerify(null);
    try {
      const [c, v] = await Promise.all([
        blogApi.staff.public.getByIdNumber(id).catch(() => null as any),
        blogApi.staff.public.verify(id).catch(() => null as any),
      ]);
      if (!c) {
        setErr(`No credential found for ID: ${id}`);
      } else {
        setCard(c);
        if (v) setVerify(v);
      }
    } catch (e: any) {
      setErr(getApiError(e) || "Verification failed. Try again.");
    } finally {
      setLoading(false);
    }
  }

  function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    const id = idInput.trim();
    setSubmittedId(id || null);
    if (id) {
      router.replace(`/verify-press?id=${encodeURIComponent(id)}`, { scroll: false });
    }
    runVerify(id);
  }

  async function downloadPdfClient() {
    const hasHtml2Canvas =
      typeof window !== "undefined" && !!(window as any).html2canvas;
    const hasJsPdf =
      typeof window !== "undefined" &&
      (!!(window as any).jspdf ||
        !!(window as any).jspdf?.jsPDF ||
        !!((window as any).jspdf && (window as any).jspdf.jsPDF));

    if (!card) return;

    if (!hasHtml2Canvas || !hasJsPdf) {
      const fallback = window.confirm(
        "PDF download libraries (jsPDF + html2canvas) not loaded yet. Print via browser instead?\n\nTip: In the print dialog, choose 'Save as PDF'.",
      );
      if (fallback) window.print();
      return;
    }

    const h2c = (window as any).html2canvas;
    const jsPdfCtor =
      ((window as any).jspdf && (window as any).jspdf.jsPDF) ||
      (window as any).jsPDF ||
      ((window as any).jspdf && (window as any).jspdf.jsPDF
        ? (window as any).jspdf.jsPDF
        : (window as any).jsPDF);

    if (!h2c || !jsPdfCtor) {
      window.print();
      return;
    }

    const stage = document.querySelector<HTMLDivElement>(".press-stage");
    if (!stage) return;
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
      pdf.save(`${card.idNumber}-press-id.pdf`);
    } catch (e) {
      console.error(e);
      alert("PDF could not be generated. Use PRINT → Save as PDF instead.");
    }
  }

  return (
    <main className="min-h-[calc(100vh-180px)] bg-[#1c1e22]">
      <section className="border-b border-white/5 bg-[#15171b]">
        <div className="mx-auto max-w-7xl px-4 py-6 flex items-center justify-between flex-wrap gap-3">
          <div className="flex items-center gap-3">
            <Link
              href="/"
              className="inline-flex items-center gap-2 border-2 border-white/15 hover:border-white/40 text-white px-3 py-2 text-xs font-bold uppercase tracking-widest"
            >
              <ArrowLeft className="h-3.5 w-3.5" /> Home
            </Link>
            <div className="inline-flex items-center gap-2 px-3 py-2 border-2 border-white/10 text-white/80">
              <ShieldCheck className="h-3.5 w-3.5 text-emerald-400" />
              <span className="text-[10px] font-bold uppercase tracking-[0.2em]">
                Credential Verification
              </span>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Link
              href="/our-team"
              className="inline-flex items-center gap-2 border-2 border-white/15 hover:border-news hover:text-news text-white/90 px-3 py-2 text-xs font-bold uppercase tracking-widest"
            >
              Our Team
            </Link>
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-4xl px-4 py-10">
        <div className="text-center mb-8">
          <div className="inline-flex items-center gap-2 px-4 py-1.5 border-2 border-emerald-500/30 bg-emerald-500/5 text-emerald-300 rounded-full text-[11px] font-bold uppercase tracking-[0.2em]">
            <ShieldCheck className="h-3.5 w-3.5" /> Official Verification Portal
          </div>
          <h1 className="mt-4 font-headline text-3xl md:text-4xl uppercase tracking-tight text-white">
            Verify a <span className="text-news">Press Credential</span>
          </h1>
          <p className="mt-3 text-sm text-white/70 max-w-xl mx-auto leading-relaxed">
            Enter the Press ID number printed on the front of the official MapMyTimes ID card
            (format <code className="bg-black/40 px-1.5 py-0.5 rounded text-white/85">STATE-RTO-INITIALS-DD-MM-YY-######</code>)
            to verify its authenticity and current status in real time.
          </p>
        </div>

        <form
          onSubmit={onSubmit}
          className="border-2 border-white/10 bg-[#15171b] rounded-xl p-5 md:p-6 shadow-hard-sm"
        >
          <label className="block text-[11px] uppercase tracking-[0.2em] font-bold text-white/60 mb-3">
            Enter Press ID Number
          </label>
          <div className="flex gap-3 flex-col sm:flex-row">
            <div className="flex-1 relative">
              <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-white/40" />
              <Input
                type="text"
                value={idInput}
                onChange={(e) => setIdInput(e.target.value.toUpperCase())}
                placeholder="e.g. MP-28-PM-07-08-26-000001"
                className="pl-10 h-12 font-mono text-base tracking-wider uppercase"
                autoComplete="off"
                spellCheck={false}
              />
            </div>
            <Button
              type="submit"
              variant="news"
              size="lg"
              disabled={loading}
              className="h-12 sm:w-auto w-full"
            >
              {loading ? "Verifying…" : <> <ShieldCheck className="h-4 w-4" /> Verify ID</>}
            </Button>
          </div>
        </form>

        <div className="mt-10">
          {loading ? (
            <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_360px] gap-8">
              <div className="h-[620px] border border-white/10 rounded-xl animate-pulse bg-white/5" />
              <div className="h-[620px] border border-white/10 rounded-xl animate-pulse bg-white/5" />
            </div>
          ) : err && submittedId ? (
            <div className="border-2 border-rose-500/60 bg-rose-500/5 text-rose-100 p-6 rounded-lg">
              <div className="flex items-start gap-3">
                <ShieldAlert className="h-6 w-6 flex-shrink-0 text-rose-400" />
                <div>
                  <div className="font-headline uppercase text-xl tracking-wide text-white">
                    Credential Not Verified
                  </div>
                  <p className="mt-1 text-sm text-rose-200/90">{err}</p>
                  <div className="mt-4 flex gap-3 flex-wrap">
                    <Button variant="outline" onClick={() => router.push("/our-team")}>
                      Browse Our Team
                    </Button>
                  </div>
                </div>
              </div>
            </div>
          ) : card ? (
            <div className="space-y-6">
              <div
                className={cn(
                  "rounded-lg border-2 p-5 shadow-hard-sm",
                  verify?.isValid
                    ? "border-emerald-600 bg-emerald-50"
                    : "border-rose-500 bg-rose-50",
                )}
              >
                <div className="flex items-start gap-3">
                  {verify?.isValid ? (
                    <CheckCircle2 className="h-6 w-6 text-emerald-700 flex-shrink-0" />
                  ) : (
                    <XCircle className="h-6 w-6 text-rose-700 flex-shrink-0" />
                  )}
                  <div className="min-w-0">
                    <div className="font-headline uppercase text-lg tracking-wide text-ink-950">
                      {verify?.isValid ? "✓ ID Verified — Authentic Credential" : "✗ Not a Valid Credential"}
                    </div>
                    <p className="text-sm mt-1 text-ink-800">
                      {verify?.verificationMessage ??
                        "Server verification did not return a result."}
                    </p>
                  </div>
                </div>
                {verify?.verifyTimestamp ? (
                  <div className="mt-3 pt-3 border-t border-black/10 text-[11px] uppercase tracking-widest font-bold text-ink-600 flex items-center gap-2">
                    <Info className="h-3.5 w-3.5" />
                    Verified at {new Date(verify.verifyTimestamp).toLocaleString()}
                  </div>
                ) : null}
              </div>

              <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_380px] gap-8 items-start">
                <PressIdCard
                  data={card}
                  variant="public"
                  onDownloadPdf={downloadPdfClient}
                />

                <aside className="space-y-5">
                  <div className="border-2 border-white/10 rounded-lg p-5 bg-[#15171b] text-white">
                    <div className="flex items-center gap-2 text-[11px] uppercase tracking-[0.2em] font-bold text-white/60">
                      <Printer className="h-3.5 w-3.5" />
                      Print &amp; Save
                    </div>
                    <p className="text-sm mt-2 text-white/80">
                      Print a physical copy or save the verified credential as a PDF.
                    </p>
                    <div className="mt-4 grid grid-cols-2 gap-3">
                      <Button variant="news" block onClick={() => window.print()}>
                        🖨️ Print
                      </Button>
                      <Button variant="outline" block onClick={() => void downloadPdfClient()}>
                        📥 PDF
                      </Button>
                    </div>
                    <p className="mt-3 text-[11px] text-white/60 leading-relaxed">
                      For CR80 card printing: open Print dialog, set Margins → None, Scale → 100%,
                      Paper size → CR80 (85.6 × 53.98 mm) or Custom 86×54 mm.
                    </p>
                  </div>

                  <div className="rounded-lg border-2 border-white/10 bg-[#15171b] p-5 text-white">
                    <div className="text-[11px] uppercase tracking-[0.2em] font-bold text-white/60 mb-3">
                      Verified Details
                    </div>
                    <dl className="divide-y divide-white/10 text-sm">
                      {[
                        ["ID Number", card.idNumber],
                        ["Full Name", card.fullName],
                        [
                          "Designation",
                          (card.designation || "") +
                            (card.designation && card.department ? " · " : "") +
                            card.department.replace(/_/g, " "),
                        ],
                        [
                          "Location",
                          [card.city, card.district, card.state]
                            .filter(Boolean)
                            .join(", ") || "—",
                        ],
                        ["Mobile", card.mobileMasked],
                        ["Work Email", card.workEmailMasked],
                        ["Issued", formatDateCompact(card.issueDate)],
                        ["Valid Till", formatDateCompact(card.validTill)],
                      ].map(([k, v]) => (
                        <div key={k} className="py-2 grid grid-cols-3 gap-3">
                          <dt className="col-span-1 text-[10px] uppercase tracking-widest font-bold text-white/50 self-center">
                            {k}
                          </dt>
                          <dd className="col-span-2 font-semibold text-white break-words">
                            {v || "—"}
                          </dd>
                        </div>
                      ))}
                    </dl>
                  </div>

                  <div className="rounded-lg border-2 border-emerald-500/20 bg-emerald-500/5 p-4 text-emerald-100">
                    <div className="text-[10px] uppercase tracking-[0.2em] font-bold text-emerald-300 mb-2">
                      ✓ How to spot a fake
                    </div>
                    <ul className="text-[11px] space-y-1.5 leading-relaxed text-emerald-50/90">
                      <li>• Gold conic-gradient hologram on bottom-right front</li>
                      <li>• Guilloché texture & microtext MAPMYTIMES border</li>
                      <li>• NFC symbol · QR code on the back</li>
                      <li>• Always verify the ID number on this page</li>
                    </ul>
                  </div>
                </aside>
              </div>
            </div>
          ) : null}
        </div>
      </section>
    </main>
  );
}

export default function VerifyPressPage() {
  return (
    <Suspense
      fallback={
        <main className="min-h-[calc(100vh-180px)] bg-[#1c1e22] flex items-center justify-center text-white/70 text-sm">
          Loading verification portal…
        </main>
      }
    >
      <VerifyPressInner />
    </Suspense>
  );
}
