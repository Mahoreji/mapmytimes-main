"use client";

import { useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/Button";
import { Input, Textarea, Checkbox } from "@/components/ui/Input";
import { notificationApi } from "@/lib/api/notificationApi";
import { getApiError } from "@/lib/api/client";
import { Badge, SectionTitle } from "@/components/posts/PostCard";
import { Mail, Phone, MapPin, Send, CheckCircle2, AlertTriangle } from "lucide-react";
import { SITE } from "@/lib/utils";

type Status = "idle" | "sending" | "success" | "error";

export default function ContactPage() {
  const [status, setStatus] = useState<Status>("idle");
  const [error, setError] = useState<string>("");
  const [form, setForm] = useState({
    name: "",
    email: "",
    phone: "",
    subject: "",
    message: "",
    consent: false,
  });

  function onField<K extends keyof typeof form>(k: K, v: (typeof form)[K]) {
    setForm((f) => ({ ...f, [k]: v }));
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setStatus("sending");
    setError("");
    try {
      await notificationApi.contactForm({
        name: form.name.trim(),
        email: form.email.trim(),
        phone: form.phone.trim() || undefined,
        subject: form.subject.trim() || undefined,
        message: form.message.trim(),
        source: "mapmytimes.com/contact",
      });
      setStatus("success");
      setForm({ name: "", email: "", phone: "", subject: "", message: "", consent: false });
    } catch (err) {
      setStatus("error");
      setError(getApiError(err));
    }
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:py-10 grid grid-cols-1 lg:grid-cols-5 gap-8">
      <div className="lg:col-span-3 space-y-8">
        <div>
          <div className="ribbon text-xs mb-3">Contact</div>
          <h1 className="font-headline text-3xl sm:text-5xl uppercase leading-none">
            Write to the MapMyTimes newsroom
          </h1>
          <p className="mt-3 max-w-2xl text-ink-700">
            Pitch a story, share a tip, ask a correction, or reach our editors directly — we read
            every message. Serious tips are routed straight to the desk.
          </p>
        </div>

        {status === "success" ? (
          <div className="border-2 border-ink-950 bg-white shadow-hard-sm p-6 flex gap-4 items-start">
            <CheckCircle2 className="h-8 w-8 text-news flex-shrink-0" />
            <div>
              <h2 className="font-headline uppercase text-xl mb-1">Message received</h2>
              <p className="text-sm text-ink-700">
                Thank you for reaching out. Our team will respond as quickly as we can — usually
                within 1–2 working days.
              </p>
              <div className="mt-4 flex gap-2 flex-wrap">
                <Link href="/">
                  <Button size="sm" variant="primary">Back to homepage</Button>
                </Link>
                <button
                  type="button"
                  onClick={() => setStatus("idle")}
                  className="text-xs font-bold uppercase tracking-widest hover:text-news"
                >
                  Send another message
                </button>
              </div>
            </div>
          </div>
        ) : (
          <form
            onSubmit={onSubmit}
            className="border-2 border-ink-950 bg-white shadow-hard-sm p-5 sm:p-6 space-y-4"
          >
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input
                label="Full name"
                required
                value={form.name}
                onChange={(e) => onField("name", e.target.value)}
                placeholder="Ada Lovelace"
              />
              <Input
                label="Email"
                type="email"
                required
                value={form.email}
                onChange={(e) => onField("email", e.target.value)}
                placeholder="you@example.com"
              />
              <Input
                label="Phone (optional)"
                type="tel"
                value={form.phone}
                onChange={(e) => onField("phone", e.target.value)}
                placeholder="+91…"
              />
              <Input
                label="Subject"
                required
                value={form.subject}
                onChange={(e) => onField("subject", e.target.value)}
                placeholder="Story tip, correction, partnership…"
              />
            </div>
            <Textarea
              label="Message"
              required
              value={form.message}
              onChange={(e) => onField("message", e.target.value)}
              placeholder="Please include the essentials: who, what, when, where, why. Attach links and evidence wherever possible."
            />
            <Checkbox
              required
              checked={form.consent}
              onChange={(e) => onField("consent", e.target.checked)}
              label={
                <>
                  I consent to MapMyTimes processing my details to respond to this message.
                  See our privacy practices for more.
                </>
              }
            />
            {status === "error" ? (
              <div className="flex items-start gap-3 border-2 border-news bg-news-50 p-3 text-sm">
                <AlertTriangle className="h-5 w-5 text-news flex-shrink-0" />
                <div>
                  <div className="font-bold uppercase tracking-wider text-news-700">
                    Could not send your message
                  </div>
                  <div className="text-ink-800 mt-1">
                    {error || "Please check your network and try again."}
                  </div>
                </div>
              </div>
            ) : null}
            <div className="flex flex-wrap items-center justify-between gap-3 pt-2">
              <p className="text-[11px] text-ink-600 uppercase tracking-widest font-semibold max-w-md">
                Messages route through our notification service to the newsroom desk.
              </p>
              <Button type="submit" variant="news" size="lg" disabled={status === "sending"}>
                <Send className="h-4 w-4" />
                {status === "sending" ? "Sending…" : "Send message"}
              </Button>
            </div>
          </form>
        )}
      </div>

      <aside className="lg:col-span-2 space-y-6">
        <div className="bg-ink-950 text-white border-2 border-ink-950 p-5 sm:p-6 shadow-hard-sm">
          <Badge variant="news">Newsroom</Badge>
          <h2 className="font-headline uppercase text-2xl mt-3 leading-none">
            Reach MapMyTimes
          </h2>
          <p className="text-sm text-white/75 mt-2">
            Fastest response times on working days, 10am–7pm IST.
          </p>
          <ul className="mt-5 flex flex-col gap-4 text-sm">
            <li className="flex items-start gap-3">
              <Mail className="h-5 w-5 text-news mt-0.5" />
              <div>
                <div className="text-[11px] uppercase tracking-widest font-bold text-white/60">
                  Email
                </div>
                <a href={`mailto:${SITE.email}`} className="hover:text-news break-all">
                  {SITE.email}
                </a>
              </div>
            </li>
            <li className="flex items-start gap-3">
              <Phone className="h-5 w-5 text-news mt-0.5" />
              <div>
                <div className="text-[11px] uppercase tracking-widest font-bold text-white/60">
                  Phone / Desk
                </div>
                <a href={`tel:${SITE.phone}`} className="hover:text-news">
                  {SITE.phone}
                </a>
              </div>
            </li>
            <li className="flex items-start gap-3">
              <MapPin className="h-5 w-5 text-news mt-0.5" />
              <div>
                <div className="text-[11px] uppercase tracking-widest font-bold text-white/60">
                  Office
                </div>
                <div>MAPMYTOUR LLP, India</div>
              </div>
            </li>
          </ul>
        </div>

        <div className="border-2 border-ink-950 p-5 bg-white">
          <SectionTitle eyebrow="Guidelines" title="Before you write" />
          <ul className="mt-4 text-sm text-ink-700 space-y-3 list-disc pl-5 marker:text-news">
            <li>Include specific, verifiable details for any tip you share.</li>
            <li>
              For corrections, link the story and state the exact issue in one sentence.
            </li>
            <li>
              Press and partnership inquiries go to the same form — mark them in the subject.
            </li>
            <li>
              We never publish anonymous submissions without editorial review and consent.
            </li>
          </ul>
        </div>
      </aside>
    </div>
  );
}
