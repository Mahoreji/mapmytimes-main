"use client";

import Link from "next/link";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/Button";
import { Input, Checkbox } from "@/components/ui/Input";
import AuthShell, { AuthAlert } from "@/components/auth/AuthShell";
import { SocialDivider, SocialLoginButtons } from "@/components/auth/SocialLogin";
import { useAuth } from "@/lib/auth/AuthProvider";
import { getApiError } from "@/lib/api/client";

type Form = {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  password: string;
  confirmPassword: string;
  agreeToTerms: boolean;
};

export default function SignupPage() {
  const auth = useAuth();
  const router = useRouter();
  const { register, handleSubmit, formState, watch } = useForm<Form>({
    defaultValues: {
      firstName: "",
      lastName: "",
      email: "",
      phone: "",
      password: "",
      confirmPassword: "",
      agreeToTerms: false,
    },
    mode: "onTouched",
  });
  const password = watch("password");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string>("");
  const [ok, setOk] = useState<null | { email: string }>(null);

  async function onSubmit(data: Form) {
    setBusy(true);
    setErr("");
    try {
      const r = await auth.register({
        firstName: data.firstName.trim(),
        lastName: data.lastName.trim(),
        email: data.email.trim(),
        phone: data.phone.trim() || undefined,
        password: data.password,
        confirmPassword: data.confirmPassword,
        agreeToTerms: data.agreeToTerms,
      });
      setOk({ email: data.email.trim() });
      setTimeout(() => {
        router.replace(`/verify?email=${encodeURIComponent(data.email.trim())}`);
      }, 400);
    } catch (e) {
      setErr(getApiError(e) || "Could not create your account. Please try again.");
    } finally {
      setBusy(false);
    }
  }

  if (ok) {
    return (
      <AuthShell
        eyebrow="Check your email"
        title="Verify your email address"
        subtitle={`We sent a one-time code to ${ok.email}. Enter it on the next screen to activate your account.`}
      >
        <AuthAlert type="success">
          Account created. Now let&apos;s confirm it&apos;s really you.
        </AuthAlert>
        <Link href={`/verify?email=${encodeURIComponent(ok.email)}`}>
          <Button variant="news" size="lg" block>Continue to OTP</Button>
        </Link>
      </AuthShell>
    );
  }

  return (
    <AuthShell
      eyebrow="Create account"
      title="Join MapMyTimes"
      subtitle="Create your free user account — follow stories, save bookmarks, rate reports, comment on posts, and get personalised news alerts in your feed."
      footer={
        <p className="text-xs text-ink-600 text-center">
          Already have an account?{" "}
          <Link href="/login" className="font-bold uppercase tracking-widest hover:text-news">
            Sign in
          </Link>
          <span className="block mt-2 text-ink-500">
            Want to publish stories as a journalist or editor?{" "}
            <Link href="/careers" className="font-bold text-news hover:underline">Apply via Careers</Link>
          </span>
        </p>
      }
    >
      {err ? <AuthAlert type="error">{err}</AuthAlert> : null}
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div className="grid grid-cols-2 gap-4">
          <Input
            label="First name"
            required
            error={formState.errors.firstName?.message}
            {...register("firstName", { required: "Required.", minLength: 2 })}
          />
          <Input
            label="Last name"
            required
            error={formState.errors.lastName?.message}
            {...register("lastName", { required: "Required.", minLength: 2 })}
          />
        </div>
        <Input
          label="Email"
          type="email"
          autoComplete="email"
          required
          error={formState.errors.email?.message}
          {...register("email", {
            required: "Email is required.",
            pattern: { value: /\S+@\S+\.\S+/, message: "That email doesn't look right." },
          })}
        />
        <Input
          label="Phone (optional)"
          type="tel"
          error={formState.errors.phone?.message}
          {...register("phone", {
            pattern: { value: /^[+\d][\d\s\-()]{6,}$/, message: "That phone number doesn't look right." },
          })}
          hint="Used only for account recovery and important newsroom updates."
        />
        <Input
          label="Password"
          type="password"
          autoComplete="new-password"
          required
          error={formState.errors.password?.message}
          hint="Use 8+ characters — combine letters, numbers, and symbols."
          {...register("password", {
            required: "Create a password.",
            minLength: { value: 8, message: "Use 8 characters minimum." },
          })}
        />
        <Input
          label="Confirm password"
          type="password"
          autoComplete="new-password"
          required
          error={formState.errors.confirmPassword?.message}
          {...register("confirmPassword", {
            required: "Confirm your password.",
            validate: (v) => v === password || "Passwords don't match.",
          })}
        />
        <Checkbox
          required
          label={
            <>
              I agree to the MapMyTimes Terms and consent to the site&apos;s privacy practices.
            </>
          }
          error={formState.errors.agreeToTerms?.message as any}
          {...register("agreeToTerms", {
            required: "You must accept the terms.",
          })}
        />
        <Button type="submit" variant="news" size="lg" block disabled={busy}>
          {busy ? "Creating your account…" : "Create account & send OTP"}
        </Button>
      </form>

      <SocialDivider />
      <SocialLoginButtons mode="signup" />
    </AuthShell>
  );
}
