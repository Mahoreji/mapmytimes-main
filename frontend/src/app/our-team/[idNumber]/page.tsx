"use client";

import Link from "next/link";
import { Suspense, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, Users, IdCard, User, Building2, MapPin, CalendarDays, CalendarRange, ShieldAlert } from "lucide-react";
import { blogApi } from "@/lib/api/blogApi";
import type { StaffPressIdDTO } from "@/types/blog";
import { Button } from "@/components/ui/Button";
import { getApiError } from "@/lib/api/client";
import { avatarOrDefault } from "@/lib/assets";
import { departmentLabel, formatDateCompact } from "@/lib/staff";
import type { Department } from "@/types/blog";

function PressIdDetailInner() {
  const params = useParams<{ idNumber: string }>();
  const router = useRouter();
  const [card, setCard] = useState<StaffPressIdDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  const idNumber = params?.idNumber ? decodeURIComponent(String(params.idNumber)) : "";

  useEffect(() => {
    if (!idNumber) return;
    let active = true;
    setLoading(true);
    blogApi.staff.public
      .getByIdNumber(idNumber)
      .then((c) => {
        if (active) setCard(c);
      })
      .catch((e) => {
        if (active) setErr(getApiError(e) || "Staff member not found or unavailable right now.");
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [idNumber]);

  return (
    <main className="min-h-[calc(100vh-180px)] bg-ink-900/5">
      <section className="border-b-2 border-ink-950 bg-white">
        <div className="mx-auto max-w-4xl px-4 py-6 flex items-center justify-between flex-wrap gap-3">
          <div className="flex items-center gap-3">
            <Link
              href="/our-team"
              className="inline-flex items-center gap-2 border-2 border-ink-950 bg-white hover:bg-ink-50 px-3 py-2 text-xs font-bold uppercase tracking-widest text-ink-950"
            >
              <ArrowLeft className="h-3.5 w-3.5" /> Our Team
            </Link>
            <div className="inline-flex items-center gap-2 px-3 py-2 border-2 border-ink-950 bg-ink-950 text-white">
              <Users className="h-3.5 w-3.5 text-news" />
              <span className="text-[10px] font-bold uppercase tracking-[0.2em]">
                Quick Reference
              </span>
            </div>
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-4xl px-4 py-10 sm:py-14">
        {loading ? (
          <div className="max-w-xl mx-auto animate-pulse">
            <div className="w-48 h-48 mx-auto rounded-2xl border border-gray-200 shadow-md bg-ink-100" />
            <div className="mt-10 space-y-4">
              {Array.from({ length: 6 }).map((_, i) => (
                <div key={i} className="flex items-center gap-4">
                  <div className="h-10 w-10 rounded-lg border-2 border-ink-950 bg-ink-50" />
                  <div className="flex-1">
                    <div className="h-3 w-24 bg-ink-200 rounded mb-2" />
                    <div className="h-5 w-3/4 bg-ink-100 rounded" />
                  </div>
                </div>
              ))}
            </div>
          </div>
        ) : err ? (
          <div className="max-w-2xl mx-auto border-2 border-news bg-news-50 text-news-800 p-6 rounded-xl">
            <div className="flex items-start gap-3">
              <ShieldAlert className="h-6 w-6 flex-shrink-0" />
              <div>
                <div className="font-headline uppercase text-xl tracking-wide">
                  Not Available
                </div>
                <p className="mt-1 text-sm">{err}</p>
                <div className="mt-4 flex gap-3 flex-wrap">
                  <Button variant="news" onClick={() => router.push("/our-team")}>
                    Back to Our Team
                  </Button>
                </div>
              </div>
            </div>
          </div>
        ) : card ? (
          <div className="max-w-xl mx-auto">
            <div className="w-52 h-52 mx-auto rounded-3xl overflow-hidden border border-gray-200 shadow-2xl bg-white">
              <img
                src={avatarOrDefault(card.photoUrl)}
                alt={card.fullName}
                className="h-full w-full object-cover"
              />
            </div>

            <h2 className="mt-10 text-center font-headline uppercase tracking-wide text-3xl sm:text-4xl leading-tight">
              {card.fullName}
            </h2>
            <p className="mt-2 text-center text-ink-600 font-semibold text-lg">
              {card.designation || departmentLabel((card.department || "") as Department)}
            </p>

            <div className="mt-10 border-2 border-ink-950 rounded-2xl bg-white shadow-hard-sm overflow-hidden">
              <div className="bg-ink-950 text-white px-6 py-4">
                <div className="inline-flex items-center gap-2 text-[10px] font-bold uppercase tracking-[0.2em] text-white/70">
                  <IdCard className="h-3.5 w-3.5 text-news" />
                  Reference Fields
                </div>
              </div>
              <dl className="divide-y divide-ink-950/10">
                <ReferenceRow icon={<IdCard className="h-5 w-5" />} label="ID Number" value={card.idNumber} mono />
                <ReferenceRow icon={<User className="h-5 w-5" />} label="Name" value={card.fullName} />
                <ReferenceRow
                  icon={<Building2 className="h-5 w-5" />}
                  label="Department"
                  value={
                    (card.designation || "") +
                    (card.designation && card.department ? " · " : "") +
                    (card.department
                      ? card.department.replace(/_/g, " ").replace(/\b\w/g, (l) => l.toUpperCase())
                      : "")
                  }
                />
                <ReferenceRow
                  icon={<MapPin className="h-5 w-5 text-news" />}
                  label="Location"
                  value={[card.city, card.district, card.state].filter(Boolean).join(", ") || "—"}
                />
                <ReferenceRow
                  icon={<CalendarDays className="h-5 w-5" />}
                  label="Issued"
                  value={formatDateCompact(card.issueDate)}
                />
                <ReferenceRow
                  icon={<CalendarRange className="h-5 w-5" />}
                  label="Valid Till"
                  value={formatDateCompact(card.validTill)}
                />
              </dl>
            </div>

            <div className="mt-8 max-w-lg mx-auto">
              <div className="border-2 border-dashed border-ink-950/20 rounded-xl p-5 bg-white/60 text-center">
                <p className="text-[11px] uppercase tracking-[0.2em] font-bold text-ink-500">
                  Note
                </p>
                <p className="mt-2 text-sm text-ink-700 leading-relaxed">
                  Full credential verification, digital ID download, and card printing is
                  restricted to the credential holder via their authenticated role dashboard
                  only. This page provides a public quick reference for directory purposes.
                </p>
              </div>
            </div>
          </div>
        ) : null}
      </section>
    </main>
  );
}

function ReferenceRow({
  icon,
  label,
  value,
  mono = false,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
  mono?: boolean;
}) {
  return (
    <div className="px-6 py-4 grid grid-cols-[44px_minmax(0,1fr)] items-start gap-4 hover:bg-ink-50/60 transition-colors">
      <div className="h-11 w-11 flex items-center justify-center rounded-xl border-2 border-ink-950 bg-white shadow-[2px_2px_0_0_rgba(10,10,10,1)] text-ink-800 shrink-0">
        {icon}
      </div>
      <div className="min-w-0 pt-1">
        <div className="text-[10px] uppercase tracking-[0.18em] font-bold text-ink-500">
          {label}
        </div>
        <div
          className={
            "mt-1 font-semibold text-ink-950 text-base break-all " +
            (mono ? " font-mono tracking-wide" : "")
          }
          title={value}
        >
          {value || "—"}
        </div>
      </div>
    </div>
  );
}

export default function PressIdDetailPage() {
  return (
    <Suspense
      fallback={
        <main className="min-h-[calc(100vh-180px)] bg-ink-900/5 flex items-center justify-center text-ink-700 text-sm">
          Loading reference…
        </main>
      }
    >
      <PressIdDetailInner />
    </Suspense>
  );
}
