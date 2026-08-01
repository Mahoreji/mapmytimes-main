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

type Form = { email: string; otp: string; newPassword: string; confirmPassword: string };

function ResetPageInner() {
  const auth = useAuth();
  const router = useRouter();
  const sp = useSearchParams();
  const emailFromQs = (sp?.get("email") || "").trim();

  const { register, handleSubmit, formState, setValue, watch } = useForm<Form>({
    defaultValues: { email: emailFromQs, otp: "", newPassword: "", confirmPassword: "" },
    mode: "onTouched",
  });
  const newPassword = watch("newPassword");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string>("");
  const [ok, setOk] = useState<string>("");

  useEffect(() => {
    if (emailFromQs) setValue("email", emailFromQs);
  }, [emailFromQs, setValue]);

  async function onSubmit(data: Form) {
    setBusy(true);
    setErr("");
    setOk("");
    try {
      const r = await auth.resetPassword({
        email: data.email.trim(),
        otp: data.otp.trim(),
        newPassword: data.newPassword,
      });
      if (!r.reset) {
        setErr(r.message || "Could not reset your password.");
        return;
      }
      setOk(r.message || "Password reset successfully.");
      setTimeout(() => router.replace("/login?reset=1"), 700);
    } catch (e) {
      setErr(getApiError(e) || "Could not reset your password. Please check the OTP.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthShell
      eyebrow="Set new password"
      title="Reset your password"
      subtitle="Use the OTP sent to your email, then choose a strong new password for your newsroom account."
      footer={
        <p className="text-xs text-ink-600 text-center">
          Back to{" "}
          <Link href="/login" className="font-bold uppercase tracking-widest hover:text-news">
            Sign in
          </Link>
        </p>
      }
    >
      {ok ? <AuthAlert type="success">{ok}</AuthAlert> : null}
      {err ? <AuthAlert type="error">{err}</AuthAlert> : null}
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Input
          label="Your email"
          type="email"
          required
          error={formState.errors.email?.message}
          {...register("email", {
            required: "Enter your email.",
            pattern: { value: /\S+@\S+\.\S+/, message: "That email doesn't look right." },
          })}
        />
        <Input
          label="OTP from email"
          inputMode="numeric"
          placeholder="123456"
          required
          maxLength={8}
          error={formState.errors.otp?.message}
          {...register("otp", { required: "Enter the OTP from the email.", minLength: 4 })}
          onChange={(e) => {
            const cleaned = e.target.value.replace(/\D/g, "").slice(0, 6);
            setValue("otp", cleaned, { shouldValidate: true });
          }}
        />
        <Input
          label="New password"
          type="password"
          autoComplete="new-password"
          required
          error={formState.errors.newPassword?.message}
          hint="At least 8 characters."
          {...register("newPassword", {
            required: "Create a new password.",
            minLength: { value: 8, message: "Use 8 characters minimum." },
          })}
        />
        <Input
          label="Confirm new password"
          type="password"
          autoComplete="new-password"
          required
          error={formState.errors.confirmPassword?.message}
          {...register("confirmPassword", {
            required: "Confirm the new password.",
            validate: (v) => v === newPassword || "Passwords don't match.",
          })}
        />
        <Button type="submit" variant="news" size="lg" block disabled={busy}>
          {busy ? "Resetting password…" : "Set new password"}
        </Button>
      </form>
    </AuthShell>
  );
}

export default function ResetPage() {
  return (
    <Suspense fallback={
      <AuthShell
        eyebrow="Set new password"
        title="Reset your password"
        subtitle="Loading…"
      >
        <div className="space-y-4 animate-pulse">
          <div className="h-11 border-2 border-ink-950/20" />
          <div className="h-11 border-2 border-ink-950/20" />
          <div className="h-11 border-2 border-ink-950/20" />
          <div className="h-11 border-2 border-ink-950/20" />
          <div className="h-11 bg-news/10" />
        </div>
      </AuthShell>
    }>
      <ResetPageInner />
    </Suspense>
  );
}
