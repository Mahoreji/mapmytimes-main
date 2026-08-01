"use client";

import Link from "next/link";
import { Suspense, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/Button";
import { Input, Checkbox } from "@/components/ui/Input";
import AuthShell, { AuthAlert } from "@/components/auth/AuthShell";
import {
  OtpModeToggle,
  SocialDivider,
  SocialLoginButtons,
} from "@/components/auth/SocialLogin";
import { useAuth } from "@/lib/auth/AuthProvider";
import { getApiError } from "@/lib/api/client";

type Form = { email: string; password: string; remember: boolean; otp: string };

function LoginPageInner() {
  const auth = useAuth();
  const router = useRouter();
  const sp = useSearchParams();
  const next = sp?.get("next") || "/dashboard";
  const [mode, setMode] = useState<"password" | "otp">("password");
  const { register, handleSubmit, formState, setValue, watch, getValues } = useForm<Form>({
    defaultValues: { email: "", password: "", otp: "", remember: true },
    mode: "onTouched",
  });
  const [busy, setBusy] = useState(false);
  const [sendingOtp, setSendingOtp] = useState(false);
  const [sentOtpMsg, setSentOtpMsg] = useState<string>("");
  const [err, setErr] = useState<string>("");
  const [sentVerified, setSentVerified] = useState<null | string>(null);

  useEffect(() => {
    if (sp?.get("verified") === "1") setSentVerified("Email verified — you can now sign in.");
    if (sp?.get("reset") === "1") setSentVerified("Password updated — please sign in with your new password.");
  }, [sp]);

  async function sendLoginOtp() {
    const email = (getValues("email") || "").trim();
    if (!email) {
      setErr("Enter your email first so we can send the one-time code.");
      return;
    }
    setSendingOtp(true);
    setErr("");
    try {
      const m = await auth.sendLoginOtp({ email });
      setSentOtpMsg(m || "We sent a one-time code to your email.");
    } catch (e) {
      setErr(getApiError(e) || "Could not send the one-time code right now.");
    } finally {
      setSendingOtp(false);
    }
  }

  async function onSubmit(data: Form) {
    setBusy(true);
    setErr("");
    setSentOtpMsg("");
    try {
      if (mode === "password") {
        await auth.login({ email: data.email.trim(), password: data.password, rememberMe: data.remember });
      } else {
        await auth.loginWithOtp({ email: data.email.trim(), otp: data.otp.trim() });
      }
      router.replace(next);
    } catch (e) {
      setErr(getApiError(e) || "Could not sign you in. Please check your credentials.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthShell
      eyebrow="Sign in"
      title="Welcome back to MapMyTimes"
      subtitle="Your stories feed, saved bookmarks, comments, and personalised news alerts are one step away."
      footer={
        <p className="text-xs text-ink-600 text-center">
          Don&apos;t have an account?{" "}
          <Link href="/signup" className="font-bold uppercase tracking-widest hover:text-news">
            Create free account
          </Link>
        </p>
      }
    >
      {sentVerified ? <AuthAlert type="success">{sentVerified}</AuthAlert> : null}
      {err ? <AuthAlert type="error">{err}</AuthAlert> : null}
      {sentOtpMsg ? <AuthAlert type="success">{sentOtpMsg}</AuthAlert> : null}
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Input
          label="Email"
          type="email"
          autoComplete="email"
          required
          error={formState.errors.email?.message}
          {...register("email", {
            required: "Enter your email.",
            pattern: { value: /\S+@\S+\.\S+/, message: "That email doesn't look right." },
          })}
        />
        {mode === "password" ? (
          <div>
            <Input
              label="Password"
              type="password"
              autoComplete="current-password"
              required
              error={formState.errors.password?.message}
              {...register("password", {
                required: mode === "password" ? "Enter your password." : undefined,
                minLength: { value: 6, message: "Password is too short." },
              })}
            />
            <div className="mt-1.5 text-right">
              <Link
                href="/forgot-password"
                className="text-[11px] font-bold uppercase tracking-widest hover:text-news"
              >
                Forgot password?
              </Link>
            </div>
          </div>
        ) : (
          <div>
            <div className="flex items-end justify-between gap-2">
              <div className="flex-1">
                <Input
                  label="One-time code"
                  inputMode="numeric"
                  placeholder="123456"
                  autoComplete="one-time-code"
                  required
                  maxLength={8}
                  error={formState.errors.otp?.message}
                  {...register("otp", {
                    required: mode === "otp" ? "Enter the 6-digit code." : undefined,
                    minLength: { value: 4, message: "OTP is too short." },
                  })}
                  onChange={(e) => {
                    const cleaned = e.target.value.replace(/\D/g, "").slice(0, 6);
                    setValue("otp", cleaned, { shouldValidate: true });
                  }}
                />
              </div>
              <button
                type="button"
                onClick={sendLoginOtp}
                disabled={sendingOtp}
                className="h-11 px-3 shrink-0 text-[11px] font-bold uppercase tracking-widest border-2 border-ink-950/20 hover:border-news hover:text-news disabled:opacity-60"
              >
                {sendingOtp ? "Sending…" : "Send code"}
              </button>
            </div>
          </div>
        )}
        {mode === "password" ? (
          <Checkbox
            label="Keep me signed in on this device."
            {...register("remember")}
          />
        ) : null}
        <Button type="submit" variant="news" size="lg" block disabled={busy}>
          {busy ? "Signing you in…" : "Sign in"}
        </Button>
        <OtpModeToggle mode={mode} setMode={setMode} />
      </form>

      <SocialDivider />
      <SocialLoginButtons mode="login" />

      <div className="pt-2 flex items-center gap-3 text-[11px] uppercase tracking-widest text-ink-600 font-semibold">
        <span className="h-px flex-1 bg-ink-950/10" />
        New to MapMyTimes?
        <span className="h-px flex-1 bg-ink-950/10" />
      </div>
      <div className="grid grid-cols-2 gap-2">
        <Link href="/">
          <Button variant="outline" size="sm" block>Home</Button>
        </Link>
        <Link href="/explore">
          <Button variant="outline" size="sm" block>Explore news</Button>
        </Link>
      </div>
    </AuthShell>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={
      <AuthShell
        eyebrow="Sign in"
        title="Welcome back to the newsroom"
        subtitle="Loading…"
      >
        <div className="space-y-4 animate-pulse">
          <div className="h-11 border-2 border-ink-950/20" />
          <div className="h-11 border-2 border-ink-950/20" />
          <div className="h-11 bg-news/10" />
        </div>
      </AuthShell>
    }>
      <LoginPageInner />
    </Suspense>
  );
}
