"use client";

import Link from "next/link";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import AuthShell, { AuthAlert } from "@/components/auth/AuthShell";
import { useAuth } from "@/lib/auth/AuthProvider";
import { getApiError } from "@/lib/api/client";

type Form = { email: string };

export default function ForgotPage() {
  const auth = useAuth();
  const router = useRouter();
  const { register, handleSubmit, formState } = useForm<Form>({ defaultValues: { email: "" }, mode: "onTouched" });
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string>("");
  const [ok, setOk] = useState<string>("");

  async function onSubmit(data: Form) {
    setBusy(true);
    setErr("");
    setOk("");
    try {
      const r = await auth.forgotPassword({ email: data.email.trim() });
      setOk(r.message || "We sent a password-reset OTP to your email.");
      setTimeout(() => {
        router.replace(
          `/reset-password?email=${encodeURIComponent(data.email.trim())}`,
        );
      }, 500);
    } catch (e) {
      setErr(getApiError(e) || "Could not start the reset process right now.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthShell
      eyebrow="Password reset"
      title="Forgot your password?"
      subtitle="Enter the email you use for MapMyTimes. We'll send a one-time code so you can set a new password."
      footer={
        <p className="text-xs text-ink-600 text-center">
          Remember it now?{" "}
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
          label="Email"
          type="email"
          autoComplete="email"
          required
          placeholder="you@example.com"
          error={formState.errors.email?.message}
          {...register("email", {
            required: "Enter your email.",
            pattern: { value: /\S+@\S+\.\S+/, message: "That email doesn't look right." },
          })}
        />
        <Button type="submit" variant="news" size="lg" block disabled={busy}>
          {busy ? "Sending reset code…" : "Send reset OTP"}
        </Button>
      </form>
    </AuthShell>
  );
}
