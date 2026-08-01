"use client";

import Link from "next/link";
import { Suspense, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import AuthShell, { AuthAlert } from "@/components/auth/AuthShell";
import { useAuth } from "@/lib/auth/AuthProvider";
import { getApiError } from "@/lib/api/client";

type Form = { otp: string };

function VerifyPageInner() {
  const auth = useAuth();
  const router = useRouter();
  const sp = useSearchParams();
  const emailFromQs = (sp?.get("email") || "").trim();

  const { register, handleSubmit, formState, setValue } = useForm<Form>({
    defaultValues: { otp: "" },
    mode: "onTouched",
  });
  const [email, setEmail] = useState(emailFromQs);
  const [busy, setBusy] = useState(false);
  const [resending, setResending] = useState(false);
  const [err, setErr] = useState<string>("");
  const [msg, setMsg] = useState<string>("");

  useEffect(() => {
    if (sp?.get("email")) setEmail(emailFromQs);
  }, [sp, emailFromQs]);

  async function onSubmit(data: Form) {
    if (!email) {
      setErr("Your email is required to verify. Go back and try again.");
      return;
    }
    setBusy(true);
    setErr("");
    try {
      const r = await auth.verifyOtp({ email, otp: data.otp.trim() });
      if (!r.verified) {
        setErr(r.message || "OTP verification failed.");
        return;
      }
      if (r.accessToken) {
        router.replace("/dashboard?verified=1");
      } else {
        router.replace("/login?verified=1");
      }
    } catch (e) {
      setErr(getApiError(e) || "Could not verify OTP.");
    } finally {
      setBusy(false);
    }
  }

  async function resend() {
    if (!email) {
      setErr("Enter your email first so we can resend the code.");
      return;
    }
    setResending(true);
    setErr("");
    try {
      const m = await auth.resendOtp({ email });
      setMsg(m || "A new OTP has been sent to your email.");
    } catch (e) {
      setErr(getApiError(e) || "Could not resend the code right now.");
    } finally {
      setResending(false);
    }
  }

  return (
    <AuthShell
      eyebrow="Verify email"
      title="Enter the 6-digit OTP"
      subtitle={
        email
          ? `We sent a verification code to ${email}. Check your inbox (and spam folder).`
          : "First enter the email you signed up with, then the OTP we sent you."
      }
    >
      {msg ? <AuthAlert type="success">{msg}</AuthAlert> : null}
      {err ? <AuthAlert type="error">{err}</AuthAlert> : null}
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        {!email ? (
          <Input
            label="Your email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="you@example.com"
            required
          />
        ) : (
          <div className="border-2 border-ink-950/10 p-3 text-sm flex items-center justify-between">
            <div>
              <div className="text-[10px] uppercase tracking-widest font-bold text-ink-600">
                Verifying
              </div>
              <div className="font-semibold">{email}</div>
            </div>
            <button
              type="button"
              onClick={() => setEmail("")}
              className="text-[11px] font-bold uppercase tracking-widest hover:text-news"
            >
              Change
            </button>
          </div>
        )}
        <div>
          <Input
            label="One-time code"
            inputMode="numeric"
            placeholder="123456"
            autoComplete="one-time-code"
            required
            maxLength={8}
            error={formState.errors.otp?.message}
            {...register("otp", {
              required: "Enter the OTP from your email.",
              minLength: { value: 4, message: "OTP is too short." },
            })}
            onChange={(e) => {
              const cleaned = e.target.value.replace(/\D/g, "").slice(0, 6);
              setValue("otp", cleaned, { shouldValidate: true });
            }}
          />
        </div>
        <Button type="submit" variant="news" size="lg" block disabled={busy}>
          {busy ? "Verifying…" : "Verify & continue"}
        </Button>
      </form>

      <div className="border-t-2 border-ink-950/10 pt-4 space-y-3">
        <div className="flex items-center justify-between text-[11px] uppercase tracking-widest font-semibold text-ink-600">
          <span>Didn&apos;t get the code?</span>
          <button
            type="button"
            onClick={resend}
            disabled={resending}
            className="text-news hover:underline disabled:opacity-60"
          >
            {resending ? "Resending…" : "Resend OTP"}
          </button>
        </div>
        <p className="text-[11px] uppercase tracking-widest text-ink-600 font-semibold">
          Wrong email?{" "}
          <Link href="/signup" className="hover:text-news">
            Go back to signup
          </Link>{" "}
          or{" "}
          <Link href="/login" className="hover:text-news">
            sign in instead
          </Link>
        </p>
      </div>
    </AuthShell>
  );
}

export default function VerifyPage() {
  return (
    <Suspense fallback={
      <AuthShell
        eyebrow="Verify email"
        title="Enter the 6-digit OTP"
        subtitle="Loading…"
      >
        <div className="space-y-4 animate-pulse">
          <div className="h-11 border-2 border-ink-950/20" />
          <div className="h-11 border-2 border-ink-950/20" />
          <div className="h-11 bg-news/10" />
        </div>
      </AuthShell>
    }>
      <VerifyPageInner />
    </Suspense>
  );
}
